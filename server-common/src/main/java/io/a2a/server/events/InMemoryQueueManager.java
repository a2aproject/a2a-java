package io.a2a.server.events;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class InMemoryQueueManager implements QueueManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryQueueManager.class);

    private final ConcurrentMap<String, EventQueue> queues = new ConcurrentHashMap<>();
    private final EventQueueFactory factory;

    public InMemoryQueueManager() {
        this.factory = new DefaultEventQueueFactory();
    }

    public InMemoryQueueManager(EventQueueFactory factory) {
        this.factory = factory;
    }

    @Override
    public void add(String taskId, EventQueue queue) {
        EventQueue existing = queues.putIfAbsent(taskId, queue);
        if (existing != null) {
            throw new TaskQueueExistsException();
        }
    }

    @Override
    public EventQueue get(String taskId) {
        return queues.get(taskId);
    }

    @Override
    public EventQueue tap(String taskId) {
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
        LOGGER.info("createOrTap called for task {}, current map size: {}", taskId, queues.size());
        EventQueue existing = queues.get(taskId);

        // Lazy cleanup: remove closed queues from map
        if (existing != null && existing.isClosed()) {
            LOGGER.debug("Removing closed queue {} for task {}", System.identityHashCode(existing), taskId);
            queues.remove(taskId);
            existing = null;
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
        EventQueue result = main.tap();  // Always return ChildQueue

        if (existing == null) {
            LOGGER.info("Created new MainQueue {} for task {}, returning ChildQueue {} (map size: {})",
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

    private class DefaultEventQueueFactory implements EventQueueFactory {
        @Override
        public EventQueue.EventQueueBuilder builder(String taskId) {
            // Return builder with callback that removes queue from map when closed
            return EventQueue.builder()
                    .taskId(taskId)
                    .onClose(() -> {
                        LOGGER.debug("Queue close callback invoked for task {}, removing from map", taskId);
                        EventQueue removed = queues.remove(taskId);
                        if (removed != null) {
                            LOGGER.info("Removed closed queue for task {} from QueueManager (map size: {})",
                                    taskId, queues.size());
                        } else {
                            LOGGER.debug("Queue for task {} was already removed from map", taskId);
                        }
                    });
        }
    }
}
