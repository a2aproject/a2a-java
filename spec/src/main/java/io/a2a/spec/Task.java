package io.a2a.spec;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.util.Assert;

/**
 * Represents a task in the A2A protocol.
 * A task encapsulates a unit of work with its current state, artifacts, message history, and metadata.
 * Tasks are the primary way to track and manage work progress in agent-to-agent communication.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Task implements EventKind, StreamingEventKind {

    public static final TypeReference<Task> TYPE_REFERENCE = new TypeReference<>() {};

    public static final String TASK = "task";
    
    /**
     * Unique identifier for this task.
     */
    private final String id;
    
    /**
     * Identifier for the conversation context this task belongs to.
     */
    private final String contextId;
    
    /**
     * Current status of the task (e.g., pending, in_progress, completed, failed).
     */
    private final TaskStatus status;
    
    /**
     * List of artifacts produced or associated with this task.
     * Artifacts represent the outputs or deliverables of the task.
     */
    private final List<Artifact> artifacts;
    
    /**
     * Message history associated with this task.
     * Contains all messages that are part of this task's execution.
     */
    private final List<Message> history;
    
    /**
     * Additional metadata associated with this task.
     * Can contain custom properties specific to the implementation or task type.
     */
    private final Map<String, Object> metadata;
    
    /**
     * The kind/type of this object, always "task" for Task instances.
     */
    private final String kind;

    /**
     * Constructs a new Task with the specified properties.
     * The kind is automatically set to "task".
     *
     * @param id unique identifier for this task
     * @param contextId identifier for the conversation context
     * @param status current status of the task
     * @param artifacts list of artifacts associated with this task
     * @param history message history for this task
     * @param metadata additional metadata for this task
     */
    public Task(String id, String contextId, TaskStatus status, List<Artifact> artifacts,
                List<Message> history, Map<String, Object> metadata) {
        this(id, contextId, status, artifacts, history, metadata, TASK);
    }

    /**
     * Constructs a new Task with the specified properties and kind.
     *
     * @param id unique identifier for this task
     * @param contextId identifier for the conversation context
     * @param status current status of the task
     * @param artifacts list of artifacts associated with this task
     * @param history message history for this task
     * @param metadata additional metadata for this task
     * @param kind the kind/type of this object, must be "task"
     */
    @JsonCreator
    public Task(@JsonProperty("id") String id, @JsonProperty("contextId") String contextId, @JsonProperty("status") TaskStatus status,
                @JsonProperty("artifacts") List<Artifact> artifacts, @JsonProperty("history") List<Message> history,
                @JsonProperty("metadata") Map<String, Object> metadata, @JsonProperty("kind") String kind) {
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
        this.artifacts = artifacts;
        this.history = history;
        this.metadata = metadata;
        this.kind = kind;
    }

    /**
     * Returns the unique identifier for this task.
     *
     * @return the task ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the identifier for the conversation context this task belongs to.
     *
     * @return the context ID
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * Returns the current status of the task.
     *
     * @return the task status
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * Returns the list of artifacts associated with this task.
     *
     * @return the artifacts list
     */
    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    /**
     * Returns the message history for this task.
     *
     * @return the message history
     */
    public List<Message> getHistory() {
        return history;
    }

    /**
     * Returns the additional metadata associated with this task.
     *
     * @return the metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Returns the kind/type of this object.
     *
     * @return the kind string, always "task" for Task instances
     */
    public String getKind() {
        return kind;
    }

    public static class Builder {
        private String id;
        private String contextId;
        private TaskStatus status;
        private List<Artifact> artifacts;
        private List<Message> history;
        private Map<String, Object> metadata;

        /**
         * Creates a new empty Builder.
         */
        public Builder() {

        }

        /**
         * Creates a new Builder initialized with values from an existing Task.
         *
         * @param task the task to copy values from
         */
        public Builder(Task task) {
            id = task.id;
            contextId = task.contextId;
            status = task.status;
            artifacts = task.artifacts;
            history = task.history;
            metadata = task.metadata;

        }

        /**
         * Sets the task ID.
         *
         * @param id the task ID
         * @return this builder
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the context ID.
         *
         * @param contextId the context ID
         * @return this builder
         */
        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        /**
         * Sets the task status.
         *
         * @param status the task status
         * @return this builder
         */
        public Builder status(TaskStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the artifacts list.
         *
         * @param artifacts the artifacts list
         * @return this builder
         */
        public Builder artifacts(List<Artifact> artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        /**
         * Sets the message history.
         *
         * @param history the message history
         * @return this builder
         */
        public Builder history(List<Message> history) {
            this.history = history;
            return this;
        }

        /**
         * Sets the message history from varargs.
         *
         * @param history the messages
         * @return this builder
         */
        public Builder history(Message... history) {
            this.history = List.of(history);
            return this;
        }

        /**
         * Sets the metadata.
         *
         * @param metadata the metadata map
         * @return this builder
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Builds a new Task instance with the configured properties.
         *
         * @return a new Task instance
         */
        public Task build() {
            return new Task(id, contextId, status, artifacts, history, metadata);
        }
    }
}
