package io.a2a.client;

import static io.a2a.util.Assert.checkNotNullParam;

import io.a2a.spec.Task;
import io.a2a.spec.UpdateEvent;

/**
 * A task update event received by a client.
 */
public final class TaskUpdateEvent implements ClientEvent {

    private final Task task;
    private final UpdateEvent updateEvent;

    /**
     * A task update event.
     *
     * @param task the current task
     * @param updateEvent the update event received for the current task
     */
    public TaskUpdateEvent(Task task, UpdateEvent updateEvent) {
        checkNotNullParam("task", task);
        checkNotNullParam("updateEvent", updateEvent);
        this.task = task;
        this.updateEvent = updateEvent;
    }

    public Task getTask() {
        return task;
    }

    public UpdateEvent getUpdateEvent() {
        return updateEvent;
    }

}
