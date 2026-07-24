package org.a2aproject.sdk.compat03.spec;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * An event sent by the agent to notify the client of a change in a task's status.
 * This is typically used in streaming or subscription models.
 */
public record TaskStatusUpdateEvent_v0_3(
        String taskId,
        TaskStatus_v0_3 status,
        String contextId,
        @SerializedName("final") boolean isFinal,
        Map<String, Object> metadata,
        String kind
) implements EventKind_v0_3, StreamingEventKind_v0_3, UpdateEvent_v0_3 {

    public static final String KIND = "status-update";

    public TaskStatusUpdateEvent_v0_3 (String taskId, TaskStatus_v0_3 status, String contextId, boolean isFinal,
                                      @Nullable Map<String, Object> metadata, String kind) {
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("status", status);
        Assert.checkNotNullParam("contextId", contextId);
        this.kind = kind != null ? kind : KIND;
        if (!KIND.equals(this.kind)) {
            throw new IllegalArgumentException("Invalid TaskStatusUpdateEvent");
        }
        this.taskId = taskId;
        this.status = status;
        this.contextId = contextId;
        this.isFinal = isFinal;
        this.metadata = metadata != null ? Map.copyOf(metadata) : null;
    }

    public TaskStatusUpdateEvent_v0_3(String taskId, TaskStatus_v0_3 status, String contextId, boolean isFinal,
                                      @Nullable Map<String, Object> metadata) {
        this(taskId, status, contextId, isFinal, metadata, KIND);
    }

    public static class Builder {
        private String taskId;
        private TaskStatus_v0_3 status;
        private String contextId;
        private boolean isFinal;
        private Map<String, Object> metadata;

        public Builder() {
        }

        public Builder(TaskStatusUpdateEvent_v0_3 existing) {
            this.taskId = existing.taskId;
            this.status = existing.status;
            this.contextId = existing.contextId;
            this.isFinal = existing.isFinal;
            this.metadata = existing.metadata;
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
