package io.a2a.extras.queuemanager.replicated.core;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.a2a.extras.common.events.TaskFinalizedEvent;
import io.a2a.server.tasks.TaskStateProvider;
import io.a2a.server.events.EventEnqueueHook;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.EventQueueFactory;
import io.a2a.server.events.EventQueueItem;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.QueueManager;

@ApplicationScoped
@Alternative
@Priority(50)
public class ReplicatedQueueManager implements QueueManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicatedQueueManager.class);

    private final InMemoryQueueManager delegate;

    @Inject
    private ReplicationStrategy replicationStrategy;

    @Inject
    private TaskStateProvider taskStateProvider;

    @Inject
    public ReplicatedQueueManager(TaskStateProvider taskStateProvider) {
        this.delegate = new InMemoryQueueManager(new ReplicatingEventQueueFactory(), taskStateProvider);
        this.taskStateProvider = taskStateProvider;
    }

    // For testing
    ReplicatedQueueManager(
            ReplicationStrategy replicationStrategy,
            TaskStateProvider taskStateProvider) {
        this.delegate = new InMemoryQueueManager(new ReplicatingEventQueueFactory(), taskStateProvider);
        this.replicationStrategy = replicationStrategy;
        this.taskStateProvider = taskStateProvider;
    }

    @Override
    public void add(String taskId, EventQueue queue) {
        delegate.add(taskId, queue);
    }

    @Override
    public EventQueue get(String taskId) {
        return delegate.get(taskId);
    }

    @Override
    public EventQueue tap(String taskId) {
        return delegate.tap(taskId);
    }

    @Override
    public void close(String taskId) {
        // Close the local queue - this will trigger onClose callbacks
        // The poison pill callback will check isTaskFinalized() and send if needed
        // The cleanup callback will remove the queue from the map
        delegate.close(taskId);
    }

    @Override
    public EventQueue createOrTap(String taskId) {
        EventQueue queue = delegate.createOrTap(taskId);
        return queue;
    }

    @Override
    public void awaitQueuePollerStart(EventQueue eventQueue) throws InterruptedException {
        delegate.awaitQueuePollerStart(eventQueue);
    }

    public void onReplicatedEvent(@Observes ReplicatedEventQueueItem replicatedEvent) {
        // Check if task is still active before processing replicated event (unless it's a QueueClosedEvent)
        // QueueClosedEvent should always be processed to terminate streams, even for inactive tasks
        if (!replicatedEvent.isClosedEvent()
                && !taskStateProvider.isTaskActive(replicatedEvent.getTaskId())) {
            // Task is no longer active - skip processing this replicated event
            // This prevents creating queues for tasks that have been finalized beyond the grace period
            LOGGER.debug("Skipping replicated event for inactive task {}", replicatedEvent.getTaskId());
            return;
        }

        // Get or create a ChildQueue for this task (consistent with normal queue access pattern)
        // This will create the MainQueue if it doesn't exist, and return a ChildQueue
        EventQueue queue = delegate.createOrTap(replicatedEvent.getTaskId());

        // Enqueue using the parent's enqueueEvent to ensure proper distribution
        // We use enqueueEvent (not enqueueItem) on the ChildQueue, which delegates to parent's enqueueEvent
        // The parent will then wrap it and distribute to all children, but we need enqueueItem to preserve type
        // So we need to call the MainQueue's enqueueItem directly
        EventQueue mainQueue = delegate.get(replicatedEvent.getTaskId());
        if (mainQueue != null) {
            mainQueue.enqueueItem(replicatedEvent);
        }
    }

    /**
     * Observes task finalization events fired AFTER database transaction commits.
     * This guarantees the task's final state is durably stored before sending the poison pill.
     *
     * @param event the task finalized event containing the task ID
     */
    public void onTaskFinalized(@Observes(during = TransactionPhase.AFTER_SUCCESS) TaskFinalizedEvent event) {
        String taskId = event.getTaskId();
        LOGGER.info("Task {} finalized - sending poison pill (QueueClosedEvent) after transaction commit", taskId);

        // Send poison pill directly via replication strategy
        // The transaction has committed, so the final state is guaranteed to be in the database
        io.a2a.server.events.QueueClosedEvent closedEvent = new io.a2a.server.events.QueueClosedEvent(taskId);
        if (replicationStrategy != null) {
            replicationStrategy.send(taskId, closedEvent);
        }
    }

    @Override
    public EventQueue.EventQueueBuilder getEventQueueBuilder(String taskId) {
        return QueueManager.super.getEventQueueBuilder(taskId)
                .hook(new ReplicationHook(taskId));
    }

    @Override
    public int getActiveChildQueueCount(String taskId) {
        return delegate.getActiveChildQueueCount(taskId);
    }

    private class ReplicatingEventQueueFactory implements EventQueueFactory {
        @Override
        public EventQueue.EventQueueBuilder builder(String taskId) {
            // We need to send poison pill before cleanup, but we have a circular dependency:
            // - Callback needs queue reference
            // - Queue doesn't exist until builder.build() is called
            // - Callbacks must be added BEFORE build()
            //
            // Solution: Use AtomicReference that's captured by the lambda closure.
            // The reference will be set after build() but before the callback is invoked.
            final java.util.concurrent.atomic.AtomicReference<EventQueue> queueRef =
                    new java.util.concurrent.atomic.AtomicReference<>();

            // Poison pill callback is no longer needed - CDI event handles this
            // The TaskFinalizedEvent observer (onTaskFinalized) sends poison pill after transaction commit
            // Keep empty callback for backward compatibility with test constructor
            Runnable poisonPillCallback = () -> {
                LOGGER.debug("Poison pill callback invoked for task {} (no-op - using CDI events)", taskId);
            };

            // Get the base builder with callbacks
            EventQueue.EventQueueBuilder baseBuilder = delegate.getEventQueueBuilder(taskId)
                    .taskId(taskId)
                    .hook(new ReplicationHook(taskId))
                    .addOnCloseCallback(poisonPillCallback)
                    .addOnCloseCallback(delegate.getCleanupCallback(taskId));

            // Return a custom builder that captures the queue when build() is called
            return new EventQueue.EventQueueBuilder() {
                @Override
                public EventQueue.EventQueueBuilder queueSize(int queueSize) {
                    baseBuilder.queueSize(queueSize);
                    return this;
                }

                @Override
                public EventQueue.EventQueueBuilder hook(EventEnqueueHook hook) {
                    baseBuilder.hook(hook);
                    return this;
                }

                @Override
                public EventQueue.EventQueueBuilder taskId(String taskId) {
                    baseBuilder.taskId(taskId);
                    return this;
                }

                @Override
                public EventQueue.EventQueueBuilder addOnCloseCallback(Runnable onCloseCallback) {
                    baseBuilder.addOnCloseCallback(onCloseCallback);
                    return this;
                }

                @Override
                public EventQueue.EventQueueBuilder taskStateProvider(TaskStateProvider taskStateProvider) {
                    baseBuilder.taskStateProvider(taskStateProvider);
                    return this;
                }

                @Override
                public EventQueue build() {
                    // Build the queue using the base builder
                    EventQueue queue = baseBuilder.build();
                    // Capture the queue in the AtomicReference for the callback
                    queueRef.set(queue);
                    return queue;
                }
            };
        }
    }


    private class ReplicationHook implements EventEnqueueHook {
        private final String taskId;

        public ReplicationHook(String taskId) {
            this.taskId = taskId;
        }

        @Override
        public void onEnqueue(EventQueueItem item) {
            // Only replicate if this isn't already a replicated event
            // This prevents replication loops
            if (!item.isReplicated()) {
                if (replicationStrategy != null && taskId != null) {
                    replicationStrategy.send(taskId, item.getEvent());
                }
            }
        }
    }
}