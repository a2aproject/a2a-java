package io.a2a.server.events;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.a2a.server.tasks.TaskStateProvider;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class InMemoryQueueManager implements QueueManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryQueueManager.class);

    private final ConcurrentMap<String, EventQueue> queues = new ConcurrentHashMap<>();
    // Fields set by constructor injection cannot be final. We need a noargs constructor for
    // Jakarta compatibility, and it seems that making fields set by constructor injection
    // final, is not proxyable in all runtimes
    private EventQueueFactory factory;
    private TaskStateProvider taskStateProvider;

    /**
     * No-args constructor for CDI proxy creation.
     * CDI requires a non-private constructor to create proxies for @ApplicationScoped beans.
     * All fields are initialized by the @Inject constructor during actual bean creation.
     */
    @SuppressWarnings("NullAway")
    protected InMemoryQueueManager() {
        // For CDI proxy creation
        this.factory = null;
        this.taskStateProvider = null;
    }

    @Inject
    public InMemoryQueueManager(TaskStateProvider taskStateProvider) {
        this.factory = new DefaultEventQueueFactory();
        this.taskStateProvider = taskStateProvider;
    }

    // For testing with custom factory
    public InMemoryQueueManager(EventQueueFactory factory, TaskStateProvider taskStateProvider) {
        this.factory = factory;
        this.taskStateProvider = taskStateProvider;
    }

    @Override
    public void add(String taskId, EventQueue queue) {
        EventQueue existing = queues.putIfAbsent(taskId, queue);
        if (existing != null) {
            throw new TaskQueueExistsException();
        }
    }

    @Override
    public @Nullable EventQueue get(String taskId) {
        return queues.get(taskId);
    }

    @Override
    public @Nullable EventQueue tap(String taskId) {
        EventQueue queue = queues.get(taskId);
        return queue == null ? null : queue.tap();
    }

    @Override
    public void close(String taskId) {
        EventQueue existing = queues.remove(taskId);
        if (existing == null) {
            throw new NoTaskQueueException();
        }
        // Close the queue to stop EventConsumer polling loop
        LOGGER.debug("Closing queue {} for task {}", System.identityHashCode(existing), taskId);
        existing.close();
    }

    @Override
    public EventQueue createOrTap(String taskId) {
        LOGGER.debug("createOrTap called for task {}, current map size: {}", taskId, queues.size());
        EventQueue existing = queues.get(taskId);

        // Lazy cleanup: only remove closed queues if task is finalized
        // Don't remove queues for non-finalized tasks - they must stay for late-arriving events
        if (existing != null && existing.isClosed()) {
            boolean isFinalized = (taskStateProvider != null) && taskStateProvider.isTaskFinalized(taskId);
            if (isFinalized) {
                LOGGER.debug("Removing closed queue {} for finalized task {}", System.identityHashCode(existing), taskId);
                queues.remove(taskId);
                existing = null;
            } else {
                LOGGER.debug("Queue {} for task {} is closed but task not finalized - keeping for late-arriving events",
                        System.identityHashCode(existing), taskId);
                // Don't remove or recreate - existing closed queue stays in map
                // This is critical for replication where events may arrive after MainQueue closes
                // The closed MainQueue can still receive enqueued events via enqueueEvent()
            }
        }

        EventQueue newQueue = null;
        if (existing == null) {
            // Use builder pattern for cleaner queue creation
            // Use the new taskId-aware builder method if available
            newQueue = factory.builder(taskId).build();
            // Make sure an existing queue has not been added in the meantime
            existing = queues.putIfAbsent(taskId, newQueue);
        }

        EventQueue main = existing == null ? newQueue : existing;
        if (main == null) {
            throw new IllegalStateException("Failed to create or retrieve queue for task " + taskId);
        }
        EventQueue result = main.tap();  // Always return ChildQueue

        if (existing == null) {
            LOGGER.debug("Created new MainQueue {} for task {}, returning ChildQueue {} (map size: {})",
                System.identityHashCode(main), taskId, System.identityHashCode(result), queues.size());
        } else {
            LOGGER.debug("Tapped existing MainQueue {} -> ChildQueue {} for task {}",
                System.identityHashCode(main), System.identityHashCode(result), taskId);
        }
        return result;
    }

    @Override
    public void awaitQueuePollerStart(EventQueue eventQueue) throws InterruptedException {
        eventQueue.awaitQueuePollerStart();
    }

    @Override
    public int getActiveChildQueueCount(String taskId) {
        EventQueue queue = queues.get(taskId);
        if (queue == null || queue.isClosed()) {
            return -1; // Queue doesn't exist or is closed
        }
        // Cast to MainQueue to access getActiveChildCount()
        if (queue instanceof EventQueue.MainQueue mainQueue) {
            return mainQueue.getActiveChildCount();
        }
        // This should not happen in normal operation since we only store MainQueues
        return -1;
    }

    /**
     * Get the cleanup callback that removes a queue from the map when it closes.
     * This is exposed so that subclasses (like ReplicatedQueueManager) can reuse
     * this cleanup logic while adding their own callbacks in the correct order.
     * <p>
     * The cleanup callback checks if the task is finalized before removing the queue.
     * If the task is not finalized, the queue remains in the map to handle late-arriving events.
     * </p>
     *
     * @param taskId the task ID for the queue
     * @return a Runnable that removes the queue from the map if appropriate
     */
    public Runnable getCleanupCallback(String taskId) {
        return () -> {
            LOGGER.debug("Queue close callback invoked for task {}", taskId);

            // Check if task is finalized before removing queue
            boolean isFinalized = (taskStateProvider != null) && taskStateProvider.isTaskFinalized(taskId);

            if (!isFinalized) {
                LOGGER.debug("Task {} is not finalized, keeping queue in map for late-arriving events", taskId);
                return;  // Don't remove from map - task is still active
            }

            LOGGER.debug("Task {} is finalized, removing queue from map", taskId);

            EventQueue removed = queues.remove(taskId);
            if (removed != null) {
                LOGGER.debug("Removed closed queue for task {} from QueueManager (map size: {})",
                        taskId, queues.size());
            } else {
                LOGGER.debug("Queue for task {} was already removed from map", taskId);
            }
        };
    }

    private class DefaultEventQueueFactory implements EventQueueFactory {
        @Override
        public EventQueue.EventQueueBuilder builder(String taskId) {
            // Return builder with callback that removes queue from map when closed
            return EventQueue.builder()
                    .taskId(taskId)
                    .addOnCloseCallback(getCleanupCallback(taskId))
                    .taskStateProvider(taskStateProvider);
        }
    }
}
