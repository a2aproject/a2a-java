package io.a2a.server.events;

import java.util.concurrent.CompletableFuture;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskManager;
import io.a2a.server.tasks.TaskPersistenceException;
import io.a2a.server.tasks.TaskSerializationException;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.A2AError;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.Event;
import io.a2a.spec.InternalError;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskStatusUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Background processor for the MainEventBus.
 * <p>
 * This processor runs in a dedicated background thread, consuming events from the MainEventBus
 * and performing two critical operations in order:
 * </p>
 * <ol>
 *   <li>Update TaskStore with event data (persistence FIRST)</li>
 *   <li>Distribute event to ChildQueues (clients see it AFTER persistence)</li>
 * </ol>
 * <p>
 * This architecture ensures clients never receive events before they're persisted,
 * eliminating race conditions and enabling reliable event replay.
 * </p>
 * <p>
 * <b>Note:</b> This bean is eagerly initialized by {@link MainEventBusProcessorInitializer}
 * to ensure the background thread starts automatically when the application starts.
 * </p>
 *
 * <h2>Exception Handling</h2>
 * TaskStore persistence failures are caught and handled gracefully:
 * <ul>
 *   <li>{@link TaskSerializationException} - Data corruption or schema mismatch (permanent).
 *       Logged at ERROR level, distributed as {@link InternalError} to clients.</li>
 *   <li>{@link TaskPersistenceException} - Database failure (transient or permanent).
 *       Transient failures logged at WARN level, permanent at ERROR level.
 *       InternalError message indicates retry viability.</li>
 * </ul>
 *
 * <p>Processing continues after errors - the failed event is distributed as InternalError
 * to all ChildQueues, and the MainEventBusProcessor continues consuming subsequent events.</p>
 */
@ApplicationScoped
public class MainEventBusProcessor implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainEventBusProcessor.class);

    /**
     * Callback for testing synchronization with async event processing.
     * Default is NOOP to avoid null checks in production code.
     * Tests can inject their own callback via setCallback().
     */
    private volatile MainEventBusProcessorCallback callback = MainEventBusProcessorCallback.NOOP;

    /**
     * Optional executor for push notifications.
     * If null, uses default ForkJoinPool (async).
     * Tests can inject a synchronous executor to ensure deterministic ordering.
     */
    private volatile @Nullable java.util.concurrent.Executor pushNotificationExecutor = null;

    private MainEventBus eventBus;

    private TaskStore taskStore;

    private PushNotificationSender pushSender;

    private QueueManager queueManager;

    private volatile boolean running = true;
    private @Nullable Thread processorThread;

    /**
     * No-arg constructor for CDI proxying.
     * CDI requires this for @ApplicationScoped beans.
     * Fields are initialized via the @Inject constructor.
     */
    @SuppressWarnings("NullAway")
    protected MainEventBusProcessor() {
    }

    @Inject
    public MainEventBusProcessor(MainEventBus eventBus, TaskStore taskStore, PushNotificationSender pushSender, QueueManager queueManager) {
        this.eventBus = eventBus;
        this.taskStore = taskStore;
        this.pushSender = pushSender;
        this.queueManager = queueManager;
    }

    /**
     * Set a callback for testing synchronization with async event processing.
     * <p>
     * This is primarily intended for tests that need to wait for event processing to complete.
     * Pass null to reset to the default NOOP callback.
     * </p>
     *
     * @param callback the callback to invoke during event processing, or null for NOOP
     */
    public void setCallback(MainEventBusProcessorCallback callback) {
        this.callback = callback != null ? callback : MainEventBusProcessorCallback.NOOP;
    }

    /**
     * Set a custom executor for push notifications (primarily for testing).
     * <p>
     * By default, push notifications are sent asynchronously using CompletableFuture.runAsync()
     * with the default ForkJoinPool. For tests that need deterministic ordering of push
     * notifications, inject a synchronous executor that runs tasks immediately on the calling thread.
     * </p>
     * Example synchronous executor for tests:
     * <pre>{@code
     * Executor syncExecutor = Runnable::run;
     * mainEventBusProcessor.setPushNotificationExecutor(syncExecutor);
     * }</pre>
     *
     * @param executor the executor to use for push notifications, or null to use default ForkJoinPool
     */
    public void setPushNotificationExecutor(java.util.concurrent.Executor executor) {
        this.pushNotificationExecutor = executor;
    }

    @PostConstruct
    void start() {
        processorThread = new Thread(this, "MainEventBusProcessor");
        processorThread.setDaemon(true); // Allow JVM to exit even if this thread is running
        processorThread.start();
        LOGGER.info("MainEventBusProcessor started");
    }

    /**
     * No-op method to force CDI proxy resolution and ensure @PostConstruct has been called.
     * Called by MainEventBusProcessorInitializer during application startup.
     */
    public void ensureStarted() {
        // Method intentionally empty - just forces proxy resolution
    }

    @PreDestroy
    void stop() {
        LOGGER.info("MainEventBusProcessor stopping...");
        running = false;
        if (processorThread != null) {
            processorThread.interrupt();
            try {
                long start = System.currentTimeMillis();
                processorThread.join(5000); // Wait up to 5 seconds
                long elapsed = System.currentTimeMillis() - start;
                LOGGER.info("MainEventBusProcessor thread stopped in {}ms", elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Interrupted while waiting for MainEventBusProcessor thread to stop");
            }
        }
        LOGGER.info("MainEventBusProcessor stopped");
    }

    @Override
    public void run() {
        LOGGER.info("MainEventBusProcessor processing loop started");
        while (running) {
            try {
                LOGGER.debug("MainEventBusProcessor: Waiting for event from MainEventBus...");
                MainEventBusContext context = eventBus.take();
                LOGGER.debug("MainEventBusProcessor: Retrieved event for task {} from MainEventBus",
                            context.taskId());
                processEvent(context);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.info("MainEventBusProcessor interrupted, shutting down");
                break;
            } catch (Exception e) {
                LOGGER.error("Error processing event from MainEventBus", e);
                // Continue processing despite errors
            }
        }
        LOGGER.info("MainEventBusProcessor processing loop ended");
    }

    private void processEvent(MainEventBusContext context) {
        String taskId = context.taskId();
        Event event = context.eventQueueItem().getEvent();
        // MainEventBus.submit() guarantees this is always a MainQueue
        EventQueue.MainQueue mainQueue = (EventQueue.MainQueue) context.eventQueue();

        LOGGER.debug("MainEventBusProcessor: Processing event for task {}: {}",
                    taskId, event.getClass().getSimpleName());

        Event eventToDistribute = null;
        boolean isReplicated = context.eventQueueItem().isReplicated();
        try {
            // Step 1: Update TaskStore FIRST (persistence before clients see it)
            // If this throws, we distribute an error to ensure "persist before client visibility"

            try {
                boolean isFinal = updateTaskStore(taskId, event, isReplicated);

                eventToDistribute = event; // Success - distribute original event

                // Trigger replication AFTER successful persistence
                // SKIP replication if task is final - ReplicatedQueueManager handles this via TaskFinalizedEvent
                // to ensure final Task is sent before poison pill (QueueClosedEvent)
                if (!isFinal) {
                    EventEnqueueHook hook = mainQueue.getEnqueueHook();
                    if (hook != null) {
                        LOGGER.debug("Triggering replication hook for task {} after successful persistence", taskId);
                        hook.onEnqueue(context.eventQueueItem());
                    }
                } else {
                    LOGGER.debug("Task {} is final - skipping replication hook (handled by ReplicatedQueueManager)", taskId);
                }
            } catch (InternalError e) {
                // Persistence failed - create error event to distribute instead
                LOGGER.error("Failed to persist event for task {}, distributing error to clients", taskId, e);
                String errorMessage = "Failed to persist event: " + e.getMessage();
                eventToDistribute = e;
            } catch (Exception e) {
                LOGGER.error("Failed to persist event for task {}, distributing error to clients", taskId, e);
                String errorMessage = "Failed to persist event: " + e.getMessage();
                eventToDistribute = new InternalError(errorMessage);
            }

            // Step 2: Send push notification AFTER successful persistence (only from active node)
            // Skip push notifications for replicated events to avoid duplicate notifications in multi-instance deployments
            // Push notifications are sent for all StreamingEventKind events (Task, Message, TaskStatusUpdateEvent, TaskArtifactUpdateEvent)
            // per A2A spec section 4.3.3
            if (!isReplicated && event instanceof StreamingEventKind streamingEvent) {
                // Send the streaming event directly - it will be wrapped in StreamResponse format by PushNotificationSender
                sendPushNotification(taskId, streamingEvent);
            }

            // Step 3: Then distribute to ChildQueues (clients see either event or error AFTER persistence attempt)
            int childCount = mainQueue.getChildCount();
            LOGGER.debug("MainEventBusProcessor: Distributing {} to {} children for task {}",
                        eventToDistribute.getClass().getSimpleName(), childCount, taskId);
            // Create new EventQueueItem with the event to distribute (original or error)
            EventQueueItem itemToDistribute = new LocalEventQueueItem(eventToDistribute);
            mainQueue.distributeToChildren(itemToDistribute);
            LOGGER.debug("MainEventBusProcessor: Distributed {} to {} children for task {}",
                        eventToDistribute.getClass().getSimpleName(), childCount, taskId);

            LOGGER.debug("MainEventBusProcessor: Completed processing event for task {}", taskId);

        } finally {
            try {
                // Step 4: Notify callback after all processing is complete
                // Call callback with the distributed event (original or error)
                if (eventToDistribute != null) {
                    callback.onEventProcessed(taskId, eventToDistribute);

                    // Step 5: If this is a final event, notify task finalization
                    // Only for successful persistence (not for errors)
                    if (eventToDistribute == event && isFinalEvent(event)) {
                        callback.onTaskFinalized(taskId);
                    }
                }
            } finally {
                 // ALWAYS release semaphore, even if processing fails
                // Balances the acquire() in MainQueue.enqueueEvent()
                mainQueue.releaseSemaphore();
            }
        }
    }

    /**
     * Updates TaskStore using TaskManager.process().
     * <p>
     * Creates a temporary TaskManager instance for this event and delegates to its process() method,
     * which handles all event types (Task, TaskStatusUpdateEvent, TaskArtifactUpdateEvent).
     * This leverages existing TaskManager logic for status updates, artifact appending, message history, etc.
     * </p>
     * <p>
     * If persistence fails, the exception is propagated to processEvent() which distributes an
     * InternalError to clients instead of the original event, ensuring "persist before visibility".
     * See Gemini's comment: https://github.com/a2aproject/a2a-java/pull/515#discussion_r2604621833
     * </p>
     *
     * @param taskId the task ID
     * @param event the event to persist
     * @return true if the task reached a final state, false otherwise
     * @throws InternalError if persistence fails
     */
    private boolean updateTaskStore(String taskId, Event event, boolean isReplicated) throws InternalError {
        try {
            // Extract contextId from event (all relevant events have it)
            String contextId = extractContextId(event);

            // Create temporary TaskManager instance for this event
            TaskManager taskManager = new TaskManager(taskId, contextId, taskStore, null);

            // Use TaskManager.process() - handles all event types with existing logic
            boolean isFinal = taskManager.process(event, isReplicated);
            LOGGER.debug("TaskStore updated via TaskManager.process() for task {}: {} (final: {}, replicated: {})",
                        taskId, event.getClass().getSimpleName(), isFinal, isReplicated);
            return isFinal;

        } catch (TaskSerializationException e) {
            // Data corruption or schema mismatch - ALWAYS permanent
            LOGGER.error("Task {} event serialization failed - data corruption detected: {}",
                        taskId, e.getMessage(), e);
            throw new InternalError("Failed to serialize task " + taskId + ": " + e.getMessage());

        } catch (TaskPersistenceException e) {
            // Database failure - check if transient for retry guidance
            if (e.isTransient()) {
                LOGGER.warn("Task {} event persistence failed (TRANSIENT) - retry may succeed: {}",
                           taskId, e.getMessage());
                throw new InternalError("Temporary storage failure for task " + taskId +
                                       " (retry may succeed): " + e.getMessage());
            } else {
                LOGGER.error("Task {} event persistence failed (PERMANENT) - manual intervention required: {}",
                            taskId, e.getMessage(), e);
                throw new InternalError("Permanent storage failure for task " + taskId +
                                       " (requires intervention): " + e.getMessage());
            }

        } catch (InternalError e) {
            // Already an InternalError from TaskManager validation - pass through
            LOGGER.error("Error updating TaskStore via TaskManager for task {}", taskId, e);
            // Rethrow to prevent distributing unpersisted event to clients
            throw e;

        } catch (Exception e) {
            // Unexpected exception type - treat as permanent failure
            LOGGER.error("Unexpected error updating TaskStore for task {}", taskId, e);
            // Rethrow to prevent distributing unpersisted event to clients
            throw new InternalError("TaskStore persistence failed: " + e.getMessage());
        }
    }

    /**
     * Sends push notification for the streaming event AFTER persistence.
     * <p>
     * This is called after updateTaskStore() to ensure the notification contains
     * the latest persisted state, avoiding race conditions.
     * </p>
     * <p>
     * <b>CRITICAL:</b> Push notifications are sent asynchronously in the background
     * to avoid blocking event distribution to ChildQueues. The 83ms overhead from
     * PushNotificationSender.sendNotification() was causing streaming delays.
     * </p>
     * <p>
     * <b>IMPORTANT:</b> The event parameter is the actual event being processed.
     * This ensures we send the event as it was when processed, not whatever state
     * might exist in TaskStore when the async callback executes (subsequent events
     * may have already updated the store).
     * </p>
     * <p>
     * Supports all StreamingEventKind event types per A2A spec section 4.3.3:
     * Task, Message, TaskStatusUpdateEvent, TaskArtifactUpdateEvent.
     * The event will be automatically wrapped in StreamResponse format by JsonUtil.
     * </p>
     * <p>
     * <b>NOTE:</b> Tests can inject a synchronous executor via setPushNotificationExecutor()
     * to ensure deterministic ordering of push notifications in the test environment.
     * </p>
     *
     * @param taskId the task ID
     * @param event the streaming event to send (Task, Message, TaskStatusUpdateEvent, or TaskArtifactUpdateEvent)
     */
    private void sendPushNotification(String taskId, StreamingEventKind event) {
        Runnable pushTask = () -> {
            try {
                if (event != null) {
                    LOGGER.debug("Sending push notification for task {}", taskId);
                    pushSender.sendNotification(event);
                } else {
                    LOGGER.debug("Skipping push notification - event is null for task {}", taskId);
                }
            } catch (Exception e) {
                LOGGER.error("Error sending push notification for task {}", taskId, e);
                // Don't rethrow - push notifications are best-effort
            }
        };

        // Use custom executor if set (for tests), otherwise use default ForkJoinPool (async)
        if (pushNotificationExecutor != null) {
            pushNotificationExecutor.execute(pushTask);
        } else {
            CompletableFuture.runAsync(pushTask);
        }
    }

    /**
     * Extracts contextId from an event.
     * Returns null if the event type doesn't have a contextId (e.g., Message).
     */
    @Nullable
    private String extractContextId(Event event) {
        if (event instanceof Task task) {
            return task.contextId();
        } else if (event instanceof TaskStatusUpdateEvent statusUpdate) {
            return statusUpdate.contextId();
        } else if (event instanceof TaskArtifactUpdateEvent artifactUpdate) {
            return artifactUpdate.contextId();
        }
        // Message and other events don't have contextId
        return null;
    }

    /**
     * Checks if an event represents a final task state.
     *
     * @param event the event to check
     * @return true if the event represents a final state (COMPLETED, FAILED, CANCELED, REJECTED, UNKNOWN, or A2AError)
     */
    private boolean isFinalEvent(Event event) {
        if (event instanceof Task task) {
            return task.status() != null && task.status().state() != null
                    && task.status().state().isFinal();
        } else if (event instanceof TaskStatusUpdateEvent statusUpdate) {
            return statusUpdate.isFinal();
        } else if (event instanceof A2AError) {
            // A2AError events are terminal - they trigger FAILED state transition
            return true;
        }
        return false;
    }
}
