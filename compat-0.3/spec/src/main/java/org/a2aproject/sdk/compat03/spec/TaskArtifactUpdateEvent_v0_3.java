package org.a2aproject.sdk.compat03.spec;

import java.util.Map;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * An event sent by the agent to notify the client that an artifact has been
 * generated or updated. This is typically used in streaming models.
 */
public record TaskArtifactUpdateEvent_v0_3(
        String taskId,
        @Nullable Boolean append,
        @Nullable Boolean lastChunk,
        Artifact_v0_3 artifact,
        String contextId,
        Map<String, Object> metadata,
        String kind
) implements EventKind_v0_3, StreamingEventKind_v0_3, UpdateEvent_v0_3 {

    public static final String KIND = "artifact-update";

    public TaskArtifactUpdateEvent_v0_3 (String taskId, @Nullable Boolean append, @Nullable Boolean lastChunk,
                                         Artifact_v0_3 artifact, String contextId,@Nullable Map<String, Object> metadata, String kind){
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("artifact", artifact);
        Assert.checkNotNullParam("contextId", contextId);
        this.kind = kind != null ? kind : KIND;
        if (!KIND.equals(this.kind)) {
            throw new IllegalArgumentException("Invalid TaskArtifactUpdateEvent");
        }
        this.taskId = taskId;
        this.append = append;
        this.lastChunk = lastChunk;
        this.artifact = artifact;
        this.contextId = contextId;
        this.metadata = metadata != null ? Map.copyOf(metadata) : null;
    }

    public TaskArtifactUpdateEvent_v0_3(String taskId, Artifact_v0_3 artifact, String contextId,
                                         @Nullable Boolean append, @Nullable Boolean lastChunk,
                                         @Nullable Map<String, Object> metadata) {
        this(taskId, append, lastChunk, artifact, contextId, metadata, KIND);
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

        public Builder(TaskArtifactUpdateEvent_v0_3 existing) {
            this.taskId = existing.taskId;
            this.artifact = existing.artifact;
            this.contextId = existing.contextId;
            this.append = existing.append;
            this.lastChunk = existing.lastChunk;
            this.metadata = existing.metadata;
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
            this.lastChunk = lastChunk;
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
