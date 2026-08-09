package org.a2aproject.sdk.server.events;

/**
 * Handle for closing all active streams (ChildQueues) for a task.
 * Passed to {@link TaskStreamLifecycleHook} callbacks to allow
 * user-controlled stream lifecycle management.
 */
public interface StreamCloseHandle {

    /**
     * Gracefully closes all active ChildQueues for this task.
     * Events already in ChildQueue deques will be drained before the stream terminates.
     * The MainQueue stays alive for non-finalized tasks — new clients can resubscribe,
     * and the agent can keep emitting events. If the task is already finalized, the
     * MainQueue will also close after the last child is removed (existing lifecycle behavior).
     */
    void closeStreams();

    /**
     * Returns the number of active ChildQueues (subscribers) for this task.
     *
     * @return the active subscriber count
     */
    int getActiveSubscriberCount();
}
