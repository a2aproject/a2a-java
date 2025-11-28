package io.a2a.spec;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.util.Assert;

import static io.a2a.spec.Task.TASK;

/**
 * Represents a single, stateful operation or conversation between a client and an agent in the A2A Protocol.
 * <p>
 * A Task encapsulates the complete lifecycle of an agent interaction, from submission through completion,
 * cancellation, or failure. It maintains the current state, accumulated artifacts (responses), conversation
 * history, and metadata associated with the operation.
 * <p>
 * Tasks are the fundamental unit of work in the A2A Protocol. When a client sends a message to an agent,
 * a Task is created to track the operation. The agent updates the Task's state as it processes the request,
 * and may add artifacts containing partial or final responses. The Task's status transitions through
 * various states (SUBMITTED, WORKING, COMPLETED, etc.) until reaching a final state.
 * <p>
 * Tasks support both blocking and streaming patterns:
 * <ul>
 *   <li><b>Blocking:</b> Client sends a message and waits for the Task to reach a final state</li>
 *   <li><b>Streaming:</b> Client subscribes to Task updates and receives incremental artifacts as they are produced</li>
 * </ul>
 * <p>
 * Tasks are immutable once created and use the Builder pattern for construction. Updates to a Task's
 * state are communicated via new Task instances or TaskStatusUpdateEvent/TaskArtifactUpdateEvent objects.
 * <p>
 * This class implements {@link EventKind} and {@link StreamingEventKind}, allowing Task instances to
 * be transmitted as events in both blocking and streaming scenarios.
 *
 * @see TaskStatus
 * @see TaskState
 * @see Artifact
 * @see Message
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonTypeName(TASK)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Task implements EventKind, StreamingEventKind {

    public static final TypeReference<Task> TYPE_REFERENCE = new TypeReference<>() {};

    public static final String TASK = "task";
    private final String id;
    private final String contextId;
    private final TaskStatus status;
    private final List<Artifact> artifacts;
    private final List<Message> history;
    private final Map<String, Object> metadata;
    private final String kind;

    public Task(String id, String contextId, TaskStatus status, List<Artifact> artifacts,
                List<Message> history, Map<String, Object> metadata) {
        this(id, contextId, status, artifacts, history, metadata, TASK);
    }

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

    public TaskStatus getStatus() {
        return status;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public List<Message> getHistory() {
        return history;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String getKind() {
        return kind;
    }

    /**
     * Creates a new Builder for constructing Task instances.
     *
     * @return a new Task.Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing immutable {@link Task} instances.
     * <p>
     * The Builder pattern is used to enforce immutability of Task objects while providing
     * a fluent API for setting required and optional fields. This approach ensures that once
     * a Task is created, its state cannot be modified directly, which is important for
     * thread-safety and protocol correctness.
     * <p>
     * Example usage:
     * <pre>{@code
     * Task task = new Task.Builder()
     *     .id("task-123")
     *     .contextId("context-456")
     *     .status(new TaskStatus(TaskState.WORKING))
     *     .artifacts(List.of(new Artifact(...)))
     *     .history(List.of(userMessage))
     *     .metadata(Map.of("key", "value"))
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private String id;
        private String contextId;
        private TaskStatus status;
        private List<Artifact> artifacts;
        private List<Message> history;
        private Map<String, Object> metadata;

        /**
         * Creates a new Builder with all fields unset.
         */
        public Builder() {

        }

        /**
         * Creates a new Builder initialized with values from an existing Task.
         * <p>
         * This constructor allows for creating a modified copy of an existing Task
         * by copying all fields and then selectively updating specific values.
         *
         * @param task the Task to copy values from
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
         * Sets the unique identifier for this task.
         *
         * @param id the task ID (required)
         * @return this builder for method chaining
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the context identifier associating this task with a conversation or session.
         * <p>
         * Multiple tasks may share the same contextId if they are part of a multi-turn
         * conversation or related workflow.
         *
         * @param contextId the context ID (required)
         * @return this builder for method chaining
         */
        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        /**
         * Sets the current status of the task.
         * <p>
         * The status includes the state (SUBMITTED, WORKING, COMPLETED, etc.),
         * an optional message, and a timestamp.
         *
         * @param status the task status (required)
         * @return this builder for method chaining
         * @see TaskStatus
         */
        public Builder status(TaskStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the list of artifacts produced by the agent during task execution.
         * <p>
         * Artifacts represent the agent's responses or output, which may include
         * text, files, data, or other content types. Artifacts accumulate over the
         * lifetime of the task, especially in streaming scenarios.
         *
         * @param artifacts the list of artifacts (optional)
         * @return this builder for method chaining
         * @see Artifact
         */
        public Builder artifacts(List<Artifact> artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        /**
         * Sets the conversation history for this task.
         * <p>
         * The history contains all messages exchanged between the client and agent
         * as part of this task, providing context for multi-turn interactions.
         *
         * @param history the list of messages (optional)
         * @return this builder for method chaining
         * @see Message
         */
        public Builder history(List<Message> history) {
            this.history = history;
            return this;
        }

        /**
         * Sets the conversation history using a varargs array of messages.
         * <p>
         * This is a convenience method for setting history without creating a List explicitly.
         *
         * @param history the messages to include in the history
         * @return this builder for method chaining
         * @see Message
         */
        public Builder history(Message... history) {
            this.history = List.of(history);
            return this;
        }

        /**
         * Sets arbitrary metadata associated with the task.
         * <p>
         * Metadata can be used to store custom information about the task,
         * such as client identifiers, routing information, or application-specific data.
         *
         * @param metadata map of metadata key-value pairs (optional)
         * @return this builder for method chaining
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Builds an immutable {@link Task} from the current builder state.
         *
         * @return a new Task instance
         * @throws IllegalArgumentException if any required field (id, contextId, status) is null
         */
        public Task build() {
            return new Task(id, contextId, status, artifacts, history, metadata);
        }
    }
}
