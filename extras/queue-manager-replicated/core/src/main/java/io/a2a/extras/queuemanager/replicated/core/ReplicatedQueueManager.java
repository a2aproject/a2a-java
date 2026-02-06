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
        return delegate.createOrTap(taskId);
    }

    @Override
    public void awaitQueuePollerStart(EventQueue eventQueue) throws InterruptedException {
        delegate.awaitQueuePollerStart(eventQueue);
    }

    public void onReplicatedEvent(@Observes ReplicatedEventQueueItem replicatedEvent) {
        // Check if task is still active before processing replicated event
        // Always allow QueueClosedEvent and Task events (they carry final state)
        // Skip other event types for inactive tasks to prevent queue creation for expired tasks
        if (!replicatedEvent.isClosedEvent()
                && !replicatedEvent.isTaskEvent()
                && !taskStateProvider.isTaskActive(replicatedEvent.getTaskId())) {
            // Task is no longer active - skip processing this replicated event
            // This prevents creating queues for tasks that have been finalized beyond the grace period
            LOGGER.debug("Skipping replicated event for inactive task {}", replicatedEvent.getTaskId());
            return;
        }

        // Get the MainQueue to enqueue the replicated event item
        // We must use enqueueItem (not enqueueEvent) to preserve the isReplicated() flag
        // and avoid triggering the replication hook again (which would cause a replication loop)
        //
        // IMPORTANT: We must NOT create a ChildQueue here! Creating and immediately closing
        // a ChildQueue means there are zero children when MainEventBusProcessor distributes
        // the event. Existing ChildQueues (from active client subscriptions) will receive
        // the event when MainEventBusProcessor distributes it to all children.
        //
        // If MainQueue doesn't exist, create it. This handles late-arriving replicated events
        // for tasks that were created on another instance.
        EventQueue childQueue = null;  // Track ChildQueue we might create
        EventQueue mainQueue = delegate.get(replicatedEvent.getTaskId());
        try {
            if (mainQueue == null) {
                LOGGER.debug("Creating MainQueue for replicated event on task {}", replicatedEvent.getTaskId());
                childQueue = delegate.createOrTap(replicatedEvent.getTaskId());  // Creates MainQueue + returns ChildQueue
                mainQueue = delegate.get(replicatedEvent.getTaskId());          // Get MainQueue from map
            }

            if (mainQueue != null) {
                mainQueue.enqueueItem(replicatedEvent);
            } else {
                LOGGER.warn(
                        "MainQueue not found for task {}, cannot enqueue replicated event. This may happen if the queue was already cleaned up.",
                        replicatedEvent.getTaskId());
            }
        } finally {
            if (childQueue != null) {
                try {
                    childQueue.close();  // Close the ChildQueue we created (not MainQueue!)
                } catch (Exception ignore) {
                    // The close is safe, but print a stacktrace just in case
                    if (LOGGER.isDebugEnabled()) {
                        ignore.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Observes task finalization events fired AFTER database transaction commits.
     * This guarantees the task's final state is durably stored before replication.
     *
     * Sends TaskStatusUpdateEvent (not full Task) FIRST, then the poison pill (QueueClosedEvent),
     * ensuring correct event ordering across instances and eliminating race conditions where
     * the poison pill arrives before the final task state.
     *
     * IMPORTANT: We send TaskStatusUpdateEvent instead of full Task to maintain consistency
     * with local event distribution. Clients expect TaskStatusUpdateEvent for status changes,
     * and sending the full Task causes issues in remote instances where clients don't handle
     * bare Task objects the same way they handle TaskStatusUpdateEvent.
     *
     * @param event the task finalized event containing the task ID and final Task
     */
    public void onTaskFinalized(@Observes(during = TransactionPhase.AFTER_SUCCESS) TaskFinalizedEvent event) {
        String taskId = event.getTaskId();
        io.a2a.spec.Task finalTask = (io.a2a.spec.Task) event.getTask();  // Cast from Object

        LOGGER.debug("Task {} finalized - sending TaskStatusUpdateEvent then poison pill (QueueClosedEvent) after transaction commit", taskId);

        // Convert final Task to TaskStatusUpdateEvent to match local event distribution
        // This ensures remote instances receive the same event type as local instances
        io.a2a.spec.TaskStatusUpdateEvent finalStatusEvent = io.a2a.spec.TaskStatusUpdateEvent.builder()
                .taskId(taskId)
                .contextId(finalTask.contextId())
                .status(finalTask.status())
                .build();

        // Send TaskStatusUpdateEvent FIRST to ensure it arrives before poison pill
        replicationStrategy.send(taskId, finalStatusEvent);

        // Then send poison pill
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