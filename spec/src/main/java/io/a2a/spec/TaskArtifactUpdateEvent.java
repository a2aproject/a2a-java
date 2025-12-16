package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;

import static io.a2a.spec.TaskArtifactUpdateEvent.ARTIFACT_UPDATE;

/**
 * Event notifying that a task artifact has been created, modified, or appended to.
 * <p>
 * TaskArtifactUpdateEvent is emitted during streaming operations to deliver partial or complete
 * artifacts as they become available. This enables progressive result delivery and real-time
 * feedback for long-running operations.
 * <p>
 * The event supports two primary patterns:
 * <ul>
 *   <li><b>Complete artifacts</b> - New artifacts added to the task (append=false or null)</li>
 *   <li><b>Incremental chunks</b> - Content appended to existing artifacts (append=true)</li>
 * </ul>
 * <p>
 * Use cases include:
 * <ul>
 *   <li>Streaming text generation (progressive LLM responses)</li>
 *   <li>Incremental file generation (large documents built over time)</li>
 *   <li>Partial results (early outputs before complete analysis)</li>
 * </ul>
 * <p>
 * The {@code lastChunk} flag indicates whether this is the final update for an artifact,
 * allowing clients to distinguish between intermediate and final states.
 *
 * @see UpdateEvent
 * @see StreamingEventKind
 * @see Artifact
 * @see Task
 */
public final class TaskArtifactUpdateEvent implements EventKind, StreamingEventKind, UpdateEvent {

    /**
     * The kind identifier for artifact update events: "artifact-update".
     */
    public static final String ARTIFACT_UPDATE = "artifact-update";
    private final String taskId;
    private final Boolean append;
    private final Boolean lastChunk;
    private final Artifact artifact;
    private final String contextId;
    private final Map<String, Object> metadata;
    private final String kind;

    /**
     * Constructs a TaskArtifactUpdateEvent with default kind.
     *
     * @param taskId the task identifier (required)
     * @param artifact the artifact being updated (required)
     * @param contextId the context identifier (required)
     * @param append whether to append to existing artifact (optional)
     * @param lastChunk whether this is the final chunk (optional)
     * @param metadata additional metadata (optional)
     */
    public TaskArtifactUpdateEvent(String taskId, Artifact artifact, String contextId, Boolean append, Boolean lastChunk, Map<String, Object> metadata) {
        this(taskId, artifact, contextId, append, lastChunk, metadata, ARTIFACT_UPDATE);
    }

    /**
     * Constructs a TaskArtifactUpdateEvent with all parameters.
     *
     * @param taskId the task identifier (required)
     * @param artifact the artifact being updated (required)
     * @param contextId the context identifier (required)
     * @param append whether to append to existing artifact (optional)
     * @param lastChunk whether this is the final chunk (optional)
     * @param metadata additional metadata (optional)
     * @param kind the event kind (must be "artifact-update")
     */
    public TaskArtifactUpdateEvent(String taskId, Artifact artifact, String contextId, Boolean append, Boolean lastChunk, Map<String, Object> metadata, String kind) {
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
     * Returns the task identifier.
     *
     * @return the task ID
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Returns the artifact being updated.
     *
     * @return the artifact
     */
    public Artifact getArtifact() {
        return artifact;
    }

    /**
     * Returns the context identifier.
     *
     * @return the context ID
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * Returns whether this update should append to an existing artifact.
     *
     * @return true if appending, false or null for new artifact
     */
    public Boolean isAppend() {
        return append;
    }

    /**
     * Returns whether this is the final chunk for the artifact.
     *
     * @return true if final chunk, false or null otherwise
     */
    public Boolean isLastChunk() {
        return lastChunk;
    }

    /**
     * Returns the metadata associated with this event.
     *
     * @return a map of metadata key-value pairs, or null if not set
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String getKind() {
        return kind;
    }

    /**
     * Creates a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new Builder initialized with values from an existing TaskArtifactUpdateEvent.
     *
     * @param event the TaskArtifactUpdateEvent to copy values from
     * @return the builder
     */
    public static Builder builder(TaskArtifactUpdateEvent event) {
        return new Builder(event);
    }

    /**
     * Builder for constructing {@link TaskArtifactUpdateEvent} instances.
     * <p>
     * Example for complete artifact:
     * <pre>{@code
     * TaskArtifactUpdateEvent event = TaskArtifactUpdateEvent.builder()
     *     .taskId("task-123")
     *     .contextId("ctx-456")
     *     .artifact(new Artifact.Builder()
     *         .artifactId("artifact-789")
     *         .parts(List.of(new TextPart("Analysis complete")))
     *         .build())
     *     .build();
     * }</pre>
     * <p>
     * Example for incremental chunk:
     * <pre>{@code
     * TaskArtifactUpdateEvent chunk = TaskArtifactUpdateEvent.builder()
     *     .taskId("task-123")
     *     .contextId("ctx-456")
     *     .artifact(new Artifact.Builder()
     *         .artifactId("artifact-789")
     *         .parts(List.of(new TextPart("more text...")))
     *         .build())
     *     .append(true)
     *     .lastChunk(false)
     *     .build();
     * }</pre>
     */
    public static class Builder {

        private String taskId;
        private Artifact artifact;
        private String contextId;
        private Boolean append;
        private Boolean lastChunk;
        private Map<String, Object> metadata;

        private Builder() {
        }

        private Builder(TaskArtifactUpdateEvent existingTaskArtifactUpdateEvent) {
            this.taskId = existingTaskArtifactUpdateEvent.taskId;
            this.artifact = existingTaskArtifactUpdateEvent.artifact;
            this.contextId = existingTaskArtifactUpdateEvent.contextId;
            this.append = existingTaskArtifactUpdateEvent.append;
            this.lastChunk = existingTaskArtifactUpdateEvent.lastChunk;
            this.metadata = existingTaskArtifactUpdateEvent.metadata;
        }

        /**
         * Sets the task identifier.
         *
         * @param taskId the task ID (required)
         * @return this builder for method chaining
         */
        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        /**
         * Sets the artifact being updated.
         *
         * @param artifact the artifact (required)
         * @return this builder for method chaining
         */
        public Builder artifact(Artifact artifact) {
            this.artifact = artifact;
            return this;
        }

        /**
         * Sets the context identifier.
         *
         * @param contextId the context ID (required)
         * @return this builder for method chaining
         */
        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        /**
         * Sets whether this update should append to an existing artifact.
         *
         * @param append true to append, false or null for new artifact
         * @return this builder for method chaining
         */
        public Builder append(Boolean append) {
            this.append = append;
            return this;
        }

        /**
         * Sets whether this is the final chunk for the artifact.
         *
         * @param lastChunk true if final chunk, false or null otherwise
         * @return this builder for method chaining
         */
        public Builder lastChunk(Boolean lastChunk) {
            this.lastChunk  = lastChunk;
            return this;
        }

        /**
         * Sets the metadata for this event.
         *
         * @param metadata map of metadata key-value pairs (optional)
         * @return this builder for method chaining
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Builds the TaskArtifactUpdateEvent.
         *
         * @return a new TaskArtifactUpdateEvent instance
         * @throws IllegalArgumentException if required fields are missing
         */
        public TaskArtifactUpdateEvent build() {
            return new TaskArtifactUpdateEvent(taskId, artifact, contextId, append, lastChunk, metadata);
        }
    }
}
