package io.a2a.spec;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

import io.a2a.util.Assert;

import static io.a2a.spec.TaskStatusUpdateEvent.STATUS_UPDATE;

/**
 * An event sent by the agent to notify the client of a change in a task's status.
 * This is typically used in streaming or subscription models.
 */
public final class TaskStatusUpdateEvent implements EventKind, StreamingEventKind, UpdateEvent {

    public static final String STATUS_UPDATE = "status-update";
    private final String taskId;
    private final TaskStatus status;
    private final String contextId;
    @SerializedName("final")
    private final boolean isFinal;
    private final Map<String, Object> metadata;
    private final String kind;


    public TaskStatusUpdateEvent(String taskId, TaskStatus status, String contextId, boolean isFinal,
                                 Map<String, Object> metadata) {
        this(taskId, status, contextId, isFinal, metadata, STATUS_UPDATE);
    }

    public TaskStatusUpdateEvent(String taskId, TaskStatus status, String contextId, boolean isFinal, Map<String, Object> metadata, String kind) {
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("status", status);
        Assert.checkNotNullParam("contextId", contextId);
        Assert.checkNotNullParam("kind", kind);
        if (! kind.equals(STATUS_UPDATE)) {
            throw new IllegalArgumentException("Invalid TaskStatusUpdateEvent");
        }
        this.taskId = taskId;
        this.status = status;
        this.contextId = contextId;
        this.isFinal = isFinal;
        this.metadata = metadata;
        this.kind = kind;
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getContextId() {
        return contextId;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String getKind() {
        return kind;
    }

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a new Builder initialized with values from an existing TaskStatusUpdateEvent.
     *
     * @param event the TaskStatusUpdateEvent to copy values from
     * @return the builder
     */
    public static Builder builder(TaskStatusUpdateEvent event) {
        return new Builder(event);
    }

    public static class Builder {
        private String taskId;
        private TaskStatus status;
        private String contextId;
        private boolean isFinal;
        private Map<String, Object> metadata;

        private Builder() {
        }

        private Builder(TaskStatusUpdateEvent existingTaskStatusUpdateEvent) {
            this.taskId = existingTaskStatusUpdateEvent.taskId;
            this.status = existingTaskStatusUpdateEvent.status;
            this.contextId = existingTaskStatusUpdateEvent.contextId;
            this.isFinal = existingTaskStatusUpdateEvent.isFinal;
            this.metadata = existingTaskStatusUpdateEvent.metadata;
        }
        public Builder taskId(String id) {
            this.taskId = id;
            return this;
        }

        public Builder status(TaskStatus status) {
            this.status = status;
            return this;
        }

        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        public Builder isFinal(boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public TaskStatusUpdateEvent build() {
            return new TaskStatusUpdateEvent(taskId, status, contextId, isFinal, metadata);
        }
    }
}
