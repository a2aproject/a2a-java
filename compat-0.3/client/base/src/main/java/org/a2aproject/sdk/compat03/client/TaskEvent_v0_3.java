package org.a2aproject.sdk.compat03.client;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import org.a2aproject.sdk.compat03.spec.Task_v0_3;

/**
 * A task event received by a client.
 */
public final class TaskEvent_v0_3 implements ClientEvent_v0_3 {

    private final Task_v0_3 task;

    /**
     * A client task event.
     *
     * @param task the task received
     */
    public TaskEvent_v0_3(Task_v0_3 task) {
        checkNotNullParam("task", task);
        this.task = task;
    }

    public Task_v0_3 getTask() {
        return task;
    }
}
