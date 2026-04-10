package org.a2aproject.sdk.compat03.client;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.UpdateEvent_v0_3;

/**
 * A task update event received by a client.
 */
public final class TaskUpdateEvent_v0_3 implements ClientEvent_v0_3 {

    private final Task_v0_3 task;
    private final UpdateEvent_v0_3 updateEvent;

    /**
     * A task update event.
     *
     * @param task the current task
     * @param updateEvent the update event received for the current task
     */
    public TaskUpdateEvent_v0_3(Task_v0_3 task, UpdateEvent_v0_3 updateEvent) {
        checkNotNullParam("task", task);
        checkNotNullParam("updateEvent", updateEvent);
        this.task = task;
        this.updateEvent = updateEvent;
    }

    public Task_v0_3 getTask() {
        return task;
    }

    public UpdateEvent_v0_3 getUpdateEvent() {
        return updateEvent;
    }

}
