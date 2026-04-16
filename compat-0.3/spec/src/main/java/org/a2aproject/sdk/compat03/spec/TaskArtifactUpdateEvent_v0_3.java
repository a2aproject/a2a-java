package org.a2aproject.sdk.compat03.spec;

import java.util.Map;

import org.a2aproject.sdk.util.Assert;

/**
 * An event sent by the agent to notify the client that an artifact has been
 * generated or updated. This is typically used in streaming models.
 */
public final class TaskArtifactUpdateEvent_v0_3 implements EventKind_v0_3, StreamingEventKind_v0_3, UpdateEvent_v0_3 {

    public static final String ARTIFACT_UPDATE = "artifact-update";
    private final String taskId;
    private final Boolean append;
    private final Boolean lastChunk;
    private final Artifact_v0_3 artifact;
    private final String contextId;
    private final Map<String, Object> metadata;
    private final String kind;

    public TaskArtifactUpdateEvent_v0_3(String taskId, Artifact_v0_3 artifact, String contextId, Boolean append, Boolean lastChunk, Map<String, Object> metadata) {
        this(taskId, artifact, contextId, append, lastChunk, metadata, ARTIFACT_UPDATE);
    }

    public TaskArtifactUpdateEvent_v0_3(String taskId, Artifact_v0_3 artifact, String contextId, Boolean append, Boolean lastChunk, Map<String, Object> metadata, String kind) {
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("artifact", artifact);
        Assert.checkNotNullParam("contextId", contextId);
        Assert.checkNotNullParam("kind", kind);
        if (! kind.equals(ARTIFACT_UPDATE)) {
            throw new IllegalArgumentException("Invalid TaskArtifactUpdateEvent");
        }
        this.taskId = taskId;
        this.artifact = artifact;
        this.contextId = contextId;
        this.append = append;
        this.lastChunk = lastChunk;
        this.metadata = metadata;
        this.kind = kind;
    }

    public String getTaskId() {
        return taskId;
    }

    public Artifact_v0_3 getArtifact() {
        return artifact;
    }

    public String getContextId() {
        return contextId;
    }

    public Boolean isAppend() {
        return append;
    }

    public Boolean isLastChunk() {
        return lastChunk;
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
        private Artifact_v0_3 artifact;
        private String contextId;
        private Boolean append;
        private Boolean lastChunk;
        private Map<String, Object> metadata;

        public Builder() {
        }

        public Builder(TaskArtifactUpdateEvent_v0_3 existingTaskArtifactUpdateEvent) {
            this.taskId = existingTaskArtifactUpdateEvent.taskId;
            this.artifact = existingTaskArtifactUpdateEvent.artifact;
            this.contextId = existingTaskArtifactUpdateEvent.contextId;
            this.append = existingTaskArtifactUpdateEvent.append;
            this.lastChunk = existingTaskArtifactUpdateEvent.lastChunk;
            this.metadata = existingTaskArtifactUpdateEvent.metadata;
        }

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder artifact(Artifact_v0_3 artifact) {
            this.artifact = artifact;
            return this;
        }

        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        public Builder append(Boolean append) {
            this.append = append;
            return this;
        }

        public Builder lastChunk(Boolean lastChunk) {
            this.lastChunk  = lastChunk;
            return this;
        }


        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public TaskArtifactUpdateEvent_v0_3 build() {
            return new TaskArtifactUpdateEvent_v0_3(taskId, artifact, contextId, append, lastChunk, metadata);
        }
    }
}
