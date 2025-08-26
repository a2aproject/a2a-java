package io.a2a.client;

import static io.a2a.util.Assert.checkNotNullParam;

import io.a2a.spec.Task;

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
