package io.a2a.extras.queuemanager.replicated.core;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import io.a2a.extras.common.events.TaskFinalizedEvent;
import io.a2a.server.events.EventEnqueueHook;
import io.a2a.server.events.EventQueue;
import io.a2a.server.events.EventQueueFactory;
import io.a2a.server.events.EventQueueItem;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.MainEventBus;
import io.a2a.server.events.QueueManager;
import io.a2a.server.tasks.TaskStateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Alternative
@Priority(50)
public class ReplicatedQueueManager implements QueueManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplicatedQueueManager.class);

    // Fields set by constructor injection cannot be final. We need a noargs constructor for
    // Jakarta compatibility, and it seems that making fields set by constructor injection
    // final, is not proxyable in all runtimes
    private InMemoryQueueManager delegate;
    private ReplicationStrategy replicationStrategy;
    private TaskStateProvider taskStateProvider;

    /**
     * No-args constructor for CDI proxy creation.
     * CDI requires a non-private constructor to create proxies for @ApplicationScoped beans.
     * All fields are initialized by the @Inject constructor during actual bean creation.
     */
    @SuppressWarnings("NullAway")
    protected ReplicatedQueueManager() {
        // For CDI proxy creation
        this.delegate = null;
        this.replicationStrategy = null;
        this.taskStateProvider = null;
    }

    @Inject
    public ReplicatedQueueManager(ReplicationStrategy replicationStrategy,
                                    TaskStateProvider taskStateProvider,
                                    MainEventBus mainEventBus) {
        this.replicationStrategy = replicationStrategy;
        this.taskStateProvider = taskStateProvider;
        this.delegate = new InMemoryQueueManager(new ReplicatingEventQueueFactory(), taskStateProvider, mainEventBus);
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

        // Get or create a ChildQueue for this task (creates MainQueue if it doesn't exist)
        EventQueue childQueue = delegate.createOrTap(replicatedEvent.getTaskId());

        try {
            // Get the MainQueue to enqueue the replicated event item
            // We must use enqueueItem (not enqueueEvent) to preserve the isReplicated() flag
            // and avoid triggering the replication hook again (which would cause a replication loop)
            EventQueue mainQueue = delegate.get(replicatedEvent.getTaskId());
            if (mainQueue != null) {
                mainQueue.enqueueItem(replicatedEvent);
            } else {
                LOGGER.warn("MainQueue not found for task {}, cannot enqueue replicated event. This may happen if the queue was already cleaned up.",
                    replicatedEvent.getTaskId());
            }
        } finally {
            // Close the temporary ChildQueue to prevent leaks
            // The MainQueue remains open for other consumers
            childQueue.close();
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
        LOGGER.debug("Task {} finalized - sending poison pill (QueueClosedEvent) after transaction commit", taskId);

        // Send poison pill directly via replication strategy
        // The transaction has committed, so the final state is guaranteed to be in the database
        io.a2a.server.events.QueueClosedEvent closedEvent = new io.a2a.server.events.QueueClosedEvent(taskId);
        replicationStrategy.send(taskId, closedEvent);
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
            // Poison pill sending is handled by the TaskFinalizedEvent observer (onTaskFinalized)
            // which sends the QueueClosedEvent after the database transaction commits.
            // This ensures proper ordering and transactional guarantees.

            // Call createBaseEventQueueBuilder() directly to avoid infinite recursion
            // (getEventQueueBuilder() would delegate back to this factory, creating a loop)
            // The base builder already includes: taskId, cleanup callback, taskStateProvider, mainEventBus
            return delegate.createBaseEventQueueBuilder(taskId)
                    .hook(new ReplicationHook(taskId));
        }
    }


    private class ReplicationHook implements EventEnqueueHook {
        private final String taskId;

        public ReplicationHook(String taskId) {
            this.taskId = taskId;
        }

        @Override
        public void onEnqueue(EventQueueItem item) {
            if (!item.isReplicated()) {
                // Only replicate if this isn't already a replicated event
                // This prevents replication loops
                if (taskId != null) {
                    replicationStrategy.send(taskId, item.getEvent());
                }
            }
        }
    }
}