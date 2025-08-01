package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;

/**
 * An event sent by the agent to notify the client that an artifact has been
 * generated or updated. This is typically used in streaming models.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TaskArtifactUpdateEvent implements EventKind, StreamingEventKind {

    public static final String ARTIFACT_UPDATE = "artifact-update";
    private final String taskId;
    private final Boolean append;
    private final Boolean lastChunk;
    private final Artifact artifact;
    private final String contextId;
    private final Map<String, Object> metadata;
    private final String kind;

    public TaskArtifactUpdateEvent(String taskId, Artifact artifact, String contextId, Boolean append, Boolean lastChunk, Map<String, Object> metadata) {
        this(taskId, artifact, contextId, append, lastChunk, metadata, ARTIFACT_UPDATE);
    }

    @JsonCreator
    public TaskArtifactUpdateEvent(@JsonProperty("taskId") String taskId, @JsonProperty("artifact") Artifact artifact,
                                   @JsonProperty("contextId") String contextId,
                                   @JsonProperty("append") Boolean append,
                                   @JsonProperty("lastChunk") Boolean lastChunk,
                                   @JsonProperty("metadata") Map<String, Object> metadata,
                                   @JsonProperty("kind") String kind) {
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

    public Artifact getArtifact() {
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
        private Artifact artifact;
        private String contextId;
        private Boolean append;
        private Boolean lastChunk;
        private Map<String, Object> metadata;

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder artifact(Artifact artifact) {
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

        public TaskArtifactUpdateEvent build() {
            return new TaskArtifactUpdateEvent(taskId, artifact, contextId, append, lastChunk, metadata);
        }
    }
}
