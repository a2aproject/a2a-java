package org.a2aproject.sdk.compat03.spec;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.util.Assert;

/**
 * Represents a single, stateful operation or conversation between a client and an agent.
 */
public final class Task_v0_3 implements EventKind_v0_3, StreamingEventKind_v0_3 {

    public static final String TASK = "task";
    private final String id;
    private final String contextId;
    private final TaskStatus_v0_3 status;
    private final List<Artifact_v0_3> artifacts;
    private final List<Message_v0_3> history;
    private final Map<String, Object> metadata;
    private final String kind;

    public Task_v0_3(String id, String contextId, TaskStatus_v0_3 status, List<Artifact_v0_3> artifacts,
                     List<Message_v0_3> history, Map<String, Object> metadata) {
        this(id, contextId, status, artifacts, history, metadata, TASK);
    }

    public Task_v0_3(String id, String contextId, TaskStatus_v0_3 status,
                     List<Artifact_v0_3> artifacts, List<Message_v0_3> history,
                     Map<String, Object> metadata, String kind) {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("contextId", contextId);
        Assert.checkNotNullParam("status", status);
        Assert.checkNotNullParam("kind", kind);
        if (! kind.equals(TASK)) {
            throw new IllegalArgumentException("Invalid Task");
        }
        this.id = id;
        this.contextId = contextId;
        this.status = status;
        this.artifacts = artifacts != null ? List.copyOf(artifacts) : List.of();
        this.history = history != null ? List.copyOf(history) : List.of();
        this.metadata = metadata;
        this.kind = kind;
    }

    public String getId() {
        return id;
    }

    public String getContextId() {
        return contextId;
    }

    public TaskStatus_v0_3 getStatus() {
        return status;
    }

    public List<Artifact_v0_3> getArtifacts() {
        return artifacts;
    }

    public List<Message_v0_3> getHistory() {
        return history;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String getKind() {
        return kind;
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
