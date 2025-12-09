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

    /**
     * Creates a new Builder for constructing TaskArtifactUpdateEvent instances.
     *
     * @return a new TaskArtifactUpdateEvent.Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing {@link TaskArtifactUpdateEvent} instances.
     * <p>
     * Example for complete artifact:
     * <pre>{@code
     * TaskArtifactUpdateEvent event = new TaskArtifactUpdateEvent.Builder()
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
     * TaskArtifactUpdateEvent chunk = new TaskArtifactUpdateEvent.Builder()
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

        public Builder() {
        }

        public Builder(TaskArtifactUpdateEvent existingTaskArtifactUpdateEvent) {
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
