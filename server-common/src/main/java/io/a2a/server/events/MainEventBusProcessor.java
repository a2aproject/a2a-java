package io.a2a.server.events;

import java.util.concurrent.CompletableFuture;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskManager;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.Event;
import io.a2a.spec.InternalError;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
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

    private final MainEventBus eventBus;

    private final TaskStore taskStore;

    private final PushNotificationSender pushSender;

    private final QueueManager queueManager;

    private volatile boolean running = true;
    private @Nullable Thread processorThread;

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

        // Determine if this is a temp ID scenario
        // If MainQueue was created with a tempId, then isTempId is true ONLY for events
        // BEFORE the queue key is switched. After switching, mainQueue.taskId != tempId,
        // so isTempId should be false for subsequent events.
        String tempId = mainQueue.getTempId();
        boolean isTempId = (tempId != null && mainQueue.getTaskId().equals(tempId));
        LOGGER.debug("MainEventBusProcessor: Processing event for task {} (tempId={}, isTempId={}): {}",
                    taskId, tempId, isTempId, event.getClass().getSimpleName());

        Event eventToDistribute = null;
        try {
            // Step 1: Update TaskStore FIRST (persistence before clients see it)
            // If this throws, we distribute an error to ensure "persist before client visibility"

            try {
                updateTaskStore(taskId, event, isTempId, mainQueue);

                eventToDistribute = event; // Success - distribute original event

                // Trigger replication AFTER successful persistence (moved from MainQueue.enqueueEvent)
                // This ensures replicated events have real task IDs, not temp-UUIDs
                EventEnqueueHook hook = mainQueue.getEnqueueHook();
                if (hook != null) {
                    LOGGER.debug("Triggering replication hook for task {} after successful persistence", taskId);
                    hook.onEnqueue(context.eventQueueItem());
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

            // Step 2: Send push notification AFTER successful persistence
            if (eventToDistribute == event) {
                // Capture task state immediately after persistence, before going async
                // This ensures we send the task as it existed when THIS event was processed,
                // not whatever state might exist later when the async callback executes
                Task taskSnapshot = taskStore.get(taskId);
                if (taskSnapshot != null) {
                    sendPushNotification(taskId, taskSnapshot);
                } else {
                    LOGGER.warn("Task {} not found in TaskStore after successful persistence, skipping push notification", taskId);
                }
            }

            // Step 3: Then distribute to ChildQueues (clients see either event or error AFTER persistence attempt)
            if (eventToDistribute == null) {
                LOGGER.error("MainEventBusProcessor: eventToDistribute is NULL for task {} - this should never happen!", taskId);
                eventToDistribute = new InternalError("Internal error: event processing failed");
            }

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
     * @param taskId the task ID (may be temporary)
     * @param event the event to persist
     * @param isTempId whether the task ID is temporary (from MainQueue.tempId)
     * @param mainQueue the main queue (for updating taskId after ID switch)
     * @throws InternalError if persistence fails
     */
    private void updateTaskStore(String taskId, Event event, boolean isTempId, EventQueue.MainQueue mainQueue) throws InternalError {
        try {
            // Extract contextId from event (all relevant events have it)
            String contextId = extractContextId(event);

            String eventTaskId = null;
            if (isTempId) {
                eventTaskId = extractTaskId(event);
                LOGGER.debug("Temp ID scenario: taskId={}, event={}", taskId, eventTaskId);
            }

            // For temp ID scenarios (before switch), use the MainQueue's current taskId (temp ID) for TaskManager.
            // This ensures duplicate detection works when switching from temp to real ID.
            // After switch (isTempId=false), use the submission taskId (real ID).
            String taskIdForManager = isTempId ? mainQueue.getTaskId() : taskId;

            // Create temporary TaskManager instance for this event
            // Use taskIdForManager (temp ID if switching, real ID otherwise)
            // isTempId allows TaskManager.checkIdsAndUpdateIfNecessary() to switch to real ID
            TaskManager taskManager = new TaskManager(taskIdForManager, contextId, taskStore, null, isTempId);

            // Use TaskManager.process() - handles all event types with existing logic
            taskManager.process(event);
            LOGGER.debug("TaskStore updated via TaskManager.process() for task {}: {}",
                        taskId, event.getClass().getSimpleName());

            // If this was a temp ID scenario and the event had a different task ID,
            // then TaskManager switched from temp to real ID. Update the queue key and MainQueue taskId.
            if (isTempId && eventTaskId != null && !eventTaskId.equals(taskIdForManager)) {
                LOGGER.debug("Switching queue key from temp {} to real {}", taskIdForManager, eventTaskId);
                queueManager.switchKey(taskIdForManager, eventTaskId);
                // Also update the MainQueue's taskId so subsequent events use the real ID
                mainQueue.setTaskId(eventTaskId);
            }
        } catch (InternalError e) {
            LOGGER.error("Error updating TaskStore via TaskManager for task {}", taskId, e);
            // Rethrow to prevent distributing unpersisted event to clients
            throw e;
        } catch (Exception e) {
            LOGGER.error("Unexpected error updating TaskStore for task {}", taskId, e);
            // Rethrow to prevent distributing unpersisted event to clients
            throw new InternalError("TaskStore persistence failed: " + e.getMessage());
        }
    }

    /**
     * Sends push notification for the task AFTER persistence.
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
     * <b>IMPORTANT:</b> The task parameter is a snapshot captured immediately after
     * persistence. This ensures we send the task state as it existed when THIS event
     * was processed, not whatever state might exist in TaskStore when the async
     * callback executes (subsequent events may have already updated the store).
     * </p>
     * <p>
     * <b>NOTE:</b> Tests can inject a synchronous executor via setPushNotificationExecutor()
     * to ensure deterministic ordering of push notifications in the test environment.
     * </p>
     *
     * @param taskId the task ID
     * @param task the task snapshot to send (captured immediately after persistence)
     */
    private void sendPushNotification(String taskId, Task task) {
        Runnable pushTask = () -> {
            try {
                if (task != null) {
                    LOGGER.debug("Sending push notification for task {}", taskId);
                    pushSender.sendNotification(task);
                } else {
                    LOGGER.debug("Skipping push notification - task snapshot is null for task {}", taskId);
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
     * Extracts taskId from an event.
     * Returns null if the event type doesn't have a taskId (e.g., Throwable).
     */
    @Nullable
    private String extractTaskId(Event event) {
        if (event instanceof Task task) {
            return task.id();
        } else if (event instanceof TaskStatusUpdateEvent statusUpdate) {
            return statusUpdate.taskId();
        } else if (event instanceof TaskArtifactUpdateEvent artifactUpdate) {
            return artifactUpdate.taskId();
        } else if (event instanceof Message message) {
            return message.taskId();
        }
        // Other events (Throwable, etc.) don't have taskId
        return null;
    }

    /**
     * Checks if an event represents a final task state.
     *
     * @param event the event to check
     * @return true if the event represents a final state (COMPLETED, FAILED, CANCELED, REJECTED, UNKNOWN)
     */
    private boolean isFinalEvent(Event event) {
        if (event instanceof Task task) {
            return task.status() != null && task.status().state() != null
                    && task.status().state().isFinal();
        } else if (event instanceof TaskStatusUpdateEvent statusUpdate) {
            return statusUpdate.isFinal();
        }
        return false;
    }
}
