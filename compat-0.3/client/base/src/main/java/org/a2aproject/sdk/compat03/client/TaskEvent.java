package org.a2aproject.sdk.compat03.client;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import org.a2aproject.sdk.compat03.spec.Task;

/**
 * A task event received by a client.
 */
public final class TaskEvent implements ClientEvent {

    private final Task task;

    /**
     * A client task event.
     *
     * @param task the task received
     */
    public TaskEvent(Task task) {
        checkNotNullParam("task", task);
        this.task = task;
    }

    public Task getTask() {
        return task;
    }
}
