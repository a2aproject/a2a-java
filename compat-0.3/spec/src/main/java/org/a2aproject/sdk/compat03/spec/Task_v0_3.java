package org.a2aproject.sdk.compat03.spec;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Represents a single, stateful operation or conversation between a client and an agent.
 */
public record Task_v0_3(
        String id,
        String contextId,
        TaskStatus_v0_3 status,
        List<Artifact_v0_3> artifacts,
        List<Message_v0_3> history,
        @Nullable Map<String, Object> metadata,
        String kind
) implements EventKind_v0_3, StreamingEventKind_v0_3 {

    public static final String KIND = "task";

    public Task_v0_3 (String id, String contextId, TaskStatus_v0_3 status, List<Artifact_v0_3> artifacts,
                     List<Message_v0_3> history, @Nullable Map<String, Object> metadata, String kind){
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("contextId", contextId);
        Assert.checkNotNullParam("status", status);
        this.kind = kind != null ? kind : KIND;
        if (!KIND.equals(this.kind)) {
            throw new IllegalArgumentException("Invalid Task");
        }
        this.id = id;
        this.contextId = contextId;
        this.status = status;
        this.artifacts = artifacts != null ? List.copyOf(artifacts) : List.of();
        this.history = history != null ? List.copyOf(history) : List.of();
        this.metadata = metadata != null ? Map.copyOf(metadata) : null;
    }

    public Task_v0_3(String id, String contextId, TaskStatus_v0_3 status, List<Artifact_v0_3> artifacts,
                     List<Message_v0_3> history, @Nullable Map<String, Object> metadata) {
        this(id, contextId, status, artifacts, history, metadata, KIND);
    }

    public static class Builder {
        private String id;
        private String contextId;
        private TaskStatus_v0_3 status;
        private List<Artifact_v0_3> artifacts;
        private List<Message_v0_3> history;
        private Map<String, Object> metadata;

        public Builder() {
        }

        public Builder(Task_v0_3 task) {
            id = task.id;
            contextId = task.contextId;
            status = task.status;
            artifacts = task.artifacts;
            history = task.history;
            metadata = task.metadata;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        public Builder status(TaskStatus_v0_3 status) {
            this.status = status;
            return this;
        }

        public Builder artifacts(List<Artifact_v0_3> artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        public Builder history(List<Message_v0_3> history) {
            this.history = history;
            return this;
        }

        public Builder history(Message_v0_3... history) {
            this.history = List.of(history);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Task_v0_3 build() {
            return new Task_v0_3(id, contextId, status, artifacts, history, metadata);
        }
    }
}
