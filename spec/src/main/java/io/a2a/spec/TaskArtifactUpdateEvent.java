package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;

/**
 * Represents a task artifact update event in the A2A protocol.
 * 
 * This event is emitted when a task produces or updates an artifact during execution,
 * providing real-time notifications about artifact creation, modification, or completion.
 * It implements both {@link EventKind} and {@link StreamingEventKind} to support
 * different event processing patterns in the A2A framework.
 * 
 * The event contains information about which task produced the artifact, the artifact
 * itself, the context it belongs to, whether this is an append operation, whether this
 * is the last chunk of a streaming artifact, and any additional metadata.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TaskArtifactUpdateEvent implements EventKind, StreamingEventKind {

    public static final String ARTIFACT_UPDATE = "artifact-update";
    /**
     * The unique identifier of the task that produced or updated the artifact.
     */
    private final String taskId;
    
    /**
     * Indicates whether this artifact update should be appended to existing content.
     * If true, the artifact content should be appended; if false or null, it should replace existing content.
     */
    private final Boolean append;
    
    /**
     * Indicates whether this is the last chunk of a streaming artifact.
     * Used for streaming scenarios where artifacts are sent in multiple chunks.
     */
    private final Boolean lastChunk;
    
    /**
     * The artifact that was produced or updated by the task.
     */
    private final Artifact artifact;
    
    /**
     * The identifier of the context/conversation this task belongs to.
     */
    private final String contextId;
    
    /**
     * Additional metadata associated with this artifact update.
     * Can contain implementation-specific information about the artifact update.
     */
    private final Map<String, Object> metadata;
    
    /**
     * The kind/type of this event, always "artifact-update" for TaskArtifactUpdateEvent instances.
     */
    private final String kind;

    /**
     * Constructs a new TaskArtifactUpdateEvent with the specified properties.
     *
     * @param taskId the unique identifier of the task that produced the artifact
     * @param artifact the artifact that was produced or updated
     * @param contextId the identifier of the context/conversation this task belongs to
     * @param append whether this artifact update should be appended to existing content
     * @param lastChunk whether this is the last chunk of a streaming artifact
     * @param metadata additional metadata associated with this artifact update
     */
    public TaskArtifactUpdateEvent(String taskId, Artifact artifact, String contextId, Boolean append, Boolean lastChunk, Map<String, Object> metadata) {
        this(taskId, artifact, contextId, append, lastChunk, metadata, ARTIFACT_UPDATE);
    }

    /**
     * JSON constructor for creating TaskArtifactUpdateEvent instances from JSON data.
     *
     * @param taskId the unique identifier of the task that produced the artifact
     * @param artifact the artifact that was produced or updated
     * @param contextId the identifier of the context/conversation this task belongs to
     * @param append whether this artifact update should be appended to existing content
     * @param lastChunk whether this is the last chunk of a streaming artifact
     * @param metadata additional metadata associated with this artifact update
     * @param kind the kind/type of this event
     */
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

    /**
     * Gets the unique identifier of the task that produced or updated the artifact.
     *
     * @return the task ID
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Gets the artifact that was produced or updated by the task.
     *
     * @return the artifact
     */
    public Artifact getArtifact() {
        return artifact;
    }

    /**
     * Gets the identifier of the context/conversation this task belongs to.
     *
     * @return the context ID
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * Indicates whether this artifact update should be appended to existing content.
     *
     * @return true if content should be appended, false if it should replace, null if not specified
     */
    public Boolean isAppend() {
        return append;
    }

    /**
     * Indicates whether this is the last chunk of a streaming artifact.
     *
     * @return true if this is the last chunk, false if more chunks are expected, null if not applicable
     */
    public Boolean isLastChunk() {
        return lastChunk;
    }

    /**
     * Gets the additional metadata associated with this artifact update.
     *
     * @return the metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Gets the kind/type of this event.
     *
     * @return the event kind, always "artifact-update" for TaskArtifactUpdateEvent instances
     */
    @Override
    public String getKind() {
        return kind;
    }

    /**
     * Builder class for constructing {@link TaskArtifactUpdateEvent} instances.
     */
    public static class Builder {

        private String taskId;
        private Artifact artifact;
        private String contextId;
        private Boolean append;
        private Boolean lastChunk;
        private Map<String, Object> metadata;

        /**
         * Sets the unique identifier of the task that produced or updated the artifact.
         *
         * @param taskId the task ID
         * @return this builder instance
         */
        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        /**
         * Sets the artifact that was produced or updated by the task.
         *
         * @param artifact the artifact
         * @return this builder instance
         */
        public Builder artifact(Artifact artifact) {
            this.artifact = artifact;
            return this;
        }

        /**
         * Sets the identifier of the context/conversation this task belongs to.
         *
         * @param contextId the context ID
         * @return this builder instance
         */
        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        /**
         * Sets whether this artifact update should be appended to existing content.
         *
         * @param append true if content should be appended, false if it should replace, null if not specified
         * @return this builder instance
         */
        public Builder append(Boolean append) {
            this.append = append;
            return this;
        }

        /**
         * Sets whether this is the last chunk of a streaming artifact.
         *
         * @param lastChunk true if this is the last chunk, false if more chunks are expected, null if not applicable
         * @return this builder instance
         */
        public Builder lastChunk(Boolean lastChunk) {
            this.lastChunk  = lastChunk;
            return this;
        }

        /**
         * Sets the additional metadata associated with this artifact update.
         *
         * @param metadata the metadata map
         * @return this builder instance
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Builds and returns a new {@link TaskArtifactUpdateEvent} instance with the configured properties.
         *
         * @return a new TaskArtifactUpdateEvent instance
         */
        public TaskArtifactUpdateEvent build() {
            return new TaskArtifactUpdateEvent(taskId, artifact, contextId, append, lastChunk, metadata);
        }
    }
}
