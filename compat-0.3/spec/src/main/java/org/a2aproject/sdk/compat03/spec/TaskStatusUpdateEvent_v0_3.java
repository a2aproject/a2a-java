package org.a2aproject.sdk.compat03.spec;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

import org.a2aproject.sdk.util.Assert;

/**
 * An event sent by the agent to notify the client of a change in a task's status.
 * This is typically used in streaming or subscription models.
 */
public final class TaskStatusUpdateEvent_v0_3 implements EventKind_v0_3, StreamingEventKind_v0_3, UpdateEvent_v0_3 {

    public static final String STATUS_UPDATE = "status-update";
    private final String taskId;
    private final TaskStatus_v0_3 status;
    private final String contextId;
    @SerializedName("final")
    private final boolean isFinal;
    private final Map<String, Object> metadata;
    private final String kind;


    public TaskStatusUpdateEvent_v0_3(String taskId, TaskStatus_v0_3 status, String contextId, boolean isFinal,
                                      Map<String, Object> metadata) {
        this(taskId, status, contextId, isFinal, metadata, STATUS_UPDATE);
    }

    public TaskStatusUpdateEvent_v0_3(String taskId, TaskStatus_v0_3 status, String contextId, boolean isFinal, Map<String, Object> metadata, String kind) {
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

    public TaskStatus_v0_3 getStatus() {
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

    public static class Builder {
        private String taskId;
        private TaskStatus_v0_3 status;
        private String contextId;
        private boolean isFinal;
        private Map<String, Object> metadata;

        public Builder() {
        }

        public Builder(TaskStatusUpdateEvent_v0_3 existingTaskStatusUpdateEvent) {
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

        public Builder status(TaskStatus_v0_3 status) {
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

        public TaskStatusUpdateEvent_v0_3 build() {
            return new TaskStatusUpdateEvent_v0_3(taskId, status, contextId, isFinal, metadata);
        }
    }
}
