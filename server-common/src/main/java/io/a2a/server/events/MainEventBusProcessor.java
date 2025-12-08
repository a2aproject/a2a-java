package io.a2a.server.events;

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
    private static volatile MainEventBusProcessorCallback callback = MainEventBusProcessorCallback.NOOP;

    private final MainEventBus eventBus;

    private final TaskStore taskStore;

    private final PushNotificationSender pushSender;

    private volatile boolean running = true;
    private @Nullable Thread processorThread;

    @Inject
    public MainEventBusProcessor(MainEventBus eventBus, TaskStore taskStore, PushNotificationSender pushSender) {
        this.eventBus = eventBus;
        this.taskStore = taskStore;
        this.pushSender = pushSender;
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
    public static void setCallback(MainEventBusProcessorCallback callback) {
        MainEventBusProcessor.callback = callback != null ? callback : MainEventBusProcessorCallback.NOOP;
    }

    @PostConstruct
    void start() {
        processorThread = new Thread(this, "MainEventBusProcessor");
        processorThread.setDaemon(false); // Keep JVM alive
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
                processorThread.join(5000); // Wait up to 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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
        EventQueue eventQueue = context.eventQueue();

        LOGGER.debug("MainEventBusProcessor: Processing event for task {}: {} (queue type: {})",
                    taskId, event.getClass().getSimpleName(), eventQueue.getClass().getSimpleName());

        // Step 1: Update TaskStore FIRST (persistence before clients see it)
        updateTaskStore(taskId, event);

        // Step 2: Send push notification AFTER persistence (ensures notification sees latest state)
        sendPushNotification(taskId);

        // Step 3: Then distribute to ChildQueues (clients see it AFTER persistence + notification)
        if (eventQueue instanceof EventQueue.MainQueue mainQueue) {
            int childCount = mainQueue.getChildCount();
            LOGGER.debug("MainEventBusProcessor: Distributing event to {} children for task {}", childCount, taskId);
            mainQueue.distributeToChildren(context.eventQueueItem());
            LOGGER.debug("MainEventBusProcessor: Distributed event {} to {} children for task {}",
                        event.getClass().getSimpleName(), childCount, taskId);
        } else {
            LOGGER.warn("MainEventBusProcessor: Expected MainQueue but got {} for task {}",
                    eventQueue.getClass().getSimpleName(), taskId);
        }

        LOGGER.debug("MainEventBusProcessor: Completed processing event for task {}", taskId);

        // Step 4: Notify callback after all processing is complete
        callback.onEventProcessed(taskId, event);

        // Step 5: If this is a final event, notify task finalization
        if (isFinalEvent(event)) {
            callback.onTaskFinalized(taskId);
        }
    }

    /**
     * Updates TaskStore using TaskManager.process().
     * <p>
     * Creates a temporary TaskManager instance for this event and delegates to its process() method,
     * which handles all event types (Task, TaskStatusUpdateEvent, TaskArtifactUpdateEvent).
     * This leverages existing TaskManager logic for status updates, artifact appending, message history, etc.
     * </p>
     */
    private void updateTaskStore(String taskId, Event event) {
        try {
            // Extract contextId from event (all relevant events have it)
            String contextId = extractContextId(event);

            // Create temporary TaskManager instance for this event
            TaskManager taskManager = new TaskManager(taskId, contextId, taskStore, null);

            // Use TaskManager.process() - handles all event types with existing logic
            taskManager.process(event);
            LOGGER.debug("TaskStore updated via TaskManager.process() for task {}: {}",
                        taskId, event.getClass().getSimpleName());
        } catch (A2AServerException e) {
            LOGGER.error("Error updating TaskStore via TaskManager for task {}", taskId, e);
            // Don't rethrow - we still want to distribute to ChildQueues
        } catch (Exception e) {
            LOGGER.error("Unexpected error updating TaskStore for task {}", taskId, e);
            // Don't rethrow - we still want to distribute to ChildQueues
        }
    }

    /**
     * Sends push notification for the task AFTER persistence.
     * <p>
     * This is called after updateTaskStore() to ensure the notification contains
     * the latest persisted state, avoiding race conditions.
     * </p>
     */
    private void sendPushNotification(String taskId) {
        try {
            Task task = taskStore.get(taskId);
            if (task != null) {
                LOGGER.debug("Sending push notification for task {}", taskId);
                pushSender.sendNotification(task);
            } else {
                LOGGER.debug("Skipping push notification - task {} not found in TaskStore", taskId);
            }
        } catch (Exception e) {
            LOGGER.error("Error sending push notification for task {}", taskId, e);
            // Don't rethrow - we still want to distribute to ChildQueues
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
