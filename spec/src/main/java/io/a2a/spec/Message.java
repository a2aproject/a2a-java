package io.a2a.spec;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.a2a.util.Assert;

import static io.a2a.spec.Message.MESSAGE;

/**
 * Represents a single message in the conversation between a user and an agent in the A2A Protocol.
 * <p>
 * A Message encapsulates communication content exchanged during agent interactions. It contains the
 * message role (user or agent), content parts (text, files, or data), and contextual metadata for
 * message threading and correlation.
 * <p>
 * Messages are fundamental to the A2A Protocol's conversational model, enabling rich multi-modal
 * communication between users and agents. Each message has a unique identifier and can reference
 * related tasks and contexts.
 * <p>
 * Messages implement both {@link EventKind} and {@link StreamingEventKind}, meaning they can be
 * sent as standalone events or as part of a streaming response sequence.
 * <p>
 * This class is mutable (allows setting taskId and contextId) to support post-construction correlation
 * with tasks and conversation contexts. Use the {@link Builder} for construction.
 *
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public final class Message implements EventKind, StreamingEventKind {

    /**
     * The kind identifier for Message events: "message".
     */
    public static final String MESSAGE = "message";
    private final Role role;
    private final List<Part<?>> parts;
    private final String messageId;
    private String contextId;
    private String taskId;
    private final Map<String, Object> metadata;
    private final String kind;
    private final List<String> referenceTaskIds;
    private final List<String> extensions;

    /**
     * Constructs a Message with default kind.
     *
     * @param role the message role (required)
     * @param parts the message content parts (required)
     * @param messageId the message identifier (required)
     * @param contextId the context identifier (optional)
     * @param taskId the task identifier (optional)
     * @param referenceTaskIds list of related task IDs (optional)
     * @param metadata additional metadata (optional)
     * @param extensions list of protocol extensions (optional)
     */
    public Message(Role role, List<Part<?>> parts, String messageId, String contextId, String taskId,
                   List<String> referenceTaskIds, Map<String, Object> metadata, List<String> extensions) {
        this(role, parts, messageId, contextId, taskId, referenceTaskIds, metadata, extensions, MESSAGE);
    }

    /**
     * Constructs a Message with all parameters.
     *
     * @param role the message role (required)
     * @param parts the message content parts (required)
     * @param messageId the message identifier (required)
     * @param contextId the context identifier (optional)
     * @param taskId the task identifier (optional)
     * @param referenceTaskIds list of related task IDs (optional)
     * @param metadata additional metadata (optional)
     * @param extensions list of protocol extensions (optional)
     * @param kind the event kind (must be "message")
     */
    public Message(Role role, List<Part<?>> parts,
                   String messageId, String contextId,
                   String taskId, List<String> referenceTaskIds,
                   Map<String, Object> metadata, List<String> extensions,
                   String kind) {
        Assert.checkNotNullParam("kind", kind);
        Assert.checkNotNullParam("parts", parts);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Parts cannot be empty");
        }
        Assert.checkNotNullParam("role", role);
        if (! kind.equals(MESSAGE)) {
            throw new IllegalArgumentException("Invalid Message");
        }
        Assert.checkNotNullParam("messageId", messageId);
        this.role = role;
        this.parts = parts;
        this.messageId = messageId;
        this.contextId = contextId;
        this.taskId = taskId;
        this.referenceTaskIds = referenceTaskIds;
        this.metadata = metadata;
        this.extensions = extensions;
        this.kind = kind;
    }

    /**
     * Returns the role of the message sender.
     *
     * @return the message role (USER or AGENT)
     */
    public Role getRole() {
        return role;
    }

    /**
     * Returns the content parts of this message.
     *
     * @return an immutable list of message parts
     */
    public List<Part<?>> getParts() {
        return parts;
    }

    /**
     * Returns the unique identifier for this message.
     *
     * @return the message ID
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Returns the conversation context identifier.
     *
     * @return the context ID, or null if not set
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * Returns the task identifier this message is associated with.
     *
     * @return the task ID, or null if not set
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Returns the metadata associated with this message.
     *
     * @return a map of metadata key-value pairs, or null if not set
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Sets the task identifier for this message.
     * <p>
     * This method allows associating the message with a task after construction.
     *
     * @param taskId the task ID to set
     */
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    /**
     * Sets the conversation context identifier for this message.
     * <p>
     * This method allows associating the message with a context after construction.
     *
     * @param contextId the context ID to set
     */
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    /**
     * Returns the list of reference task identifiers this message relates to.
     *
     * @return a list of task IDs, or null if not set
     */
    public List<String> getReferenceTaskIds() {
        return referenceTaskIds;
    }

    /**
     * Returns the list of protocol extensions used in this message.
     *
     * @return a list of extension identifiers, or null if not set
     */
    public List<String> getExtensions() {
        return extensions;
    }

    @Override
    public String getKind() {
        return kind;
    }

    /**
     * Creates a new Builder for constructing Message instances.
     *
     * @return a new Message.Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new Builder initialized with values from an existing Message.
     *
     * @param message the Message to copy values from
     * @return the builder
     */
    public static Builder builder(Message message) {
        return new Builder(message);
    }

    /**
     * Defines the role of the message sender in the conversation.
     * <p>
     * The role determines who originated the message and how it should be processed
     * within the conversational context.
     */
    public enum Role {
        /**
         * Message originated from the user (client side).
         */
        USER("user"),

        /**
         * Message originated from the agent (server side).
         */
        AGENT("agent");

        private final String role;

        Role(String role) {
            this.role = role;
        }

        /**
         * Returns the string representation of the role for JSON serialization.
         *
         * @return the role as a string ("user" or "agent")
         */
        public String asString() {
            return this.role;
        }
    }

    /**
     * Builder for constructing {@link Message} instances with fluent API.
     * <p>
     * The Builder provides a convenient way to construct messages with required and optional fields.
     * If messageId is not provided, a random UUID will be generated automatically.
     * <p>
     * Example usage:
     * <pre>{@code
     * Message userMessage = Message.builder()
     *     .role(Message.Role.USER)
     *     .parts(List.of(new TextPart("Hello, agent!", null)))
     *     .contextId("conv-123")
     *     .build();
     * }</pre>
     */
    public static class Builder {

        private Role role;
        private List<Part<?>> parts;
        private String messageId;
        private String contextId;
        private String taskId;
        private List<String> referenceTaskIds;
        private Map<String, Object> metadata;
        private List<String> extensions;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Creates a new Builder initialized with values from an existing Message.
         *
         * @param message the Message to copy values from
         */
        private Builder(Message message) {
            role = message.role;
            parts = message.parts;
            messageId = message.messageId;
            contextId = message.contextId;
            taskId = message.taskId;
            referenceTaskIds = message.referenceTaskIds;
            metadata = message.metadata;
            extensions = message.extensions;
        }

        /**
         * Sets the role of the message sender.
         *
         * @param role the message role (required)
         * @return this builder for method chaining
         */
        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        /**
         * Sets the content parts of the message.
         *
         * @param parts the list of message parts (required, must not be empty)
         * @return this builder for method chaining
         */
        public Builder parts(List<Part<?>> parts) {
            this.parts = parts;
            return this;
        }

        /**
         * Sets the content parts of the message from varargs.
         *
         * @param parts the message parts (required, must not be empty)
         * @return this builder for method chaining
         */
        public Builder parts(Part<?>...parts) {
            this.parts = List.of(parts);
            return this;
        }

        /**
         * Sets the unique identifier for this message.
         * <p>
         * If not provided, a random UUID will be generated when {@link #build()} is called.
         *
         * @param messageId the message identifier (optional)
         * @return this builder for method chaining
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Sets the conversation context identifier.
         *
         * @param contextId the context identifier (optional)
         * @return this builder for method chaining
         */
        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        /**
         * Sets the task identifier this message is associated with.
         *
         * @param taskId the task identifier (optional)
         * @return this builder for method chaining
         */
        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        /**
         * Sets the list of reference task identifiers this message relates to.
         *
         * @param referenceTaskIds the list of reference task IDs (optional)
         * @return this builder for method chaining
         */
        public Builder referenceTaskIds(List<String> referenceTaskIds) {
            this.referenceTaskIds = referenceTaskIds;
            return this;
        }

        /**
         * Sets additional metadata for the message.
         *
         * @param metadata map of metadata key-value pairs (optional)
         * @return this builder for method chaining
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Sets the list of protocol extensions used in this message.
         *
         * @param extensions the list of extension identifiers (optional)
         * @return this builder for method chaining
         */
        public Builder extensions(List<String> extensions) {
            this.extensions = (extensions == null) ? null : List.copyOf(extensions);
            return this;
        }

        /**
         * Builds a new {@link Message} from the current builder state.
         * <p>
         * If messageId was not set, a random UUID will be generated.
         *
         * @return a new Message instance
         * @throws IllegalArgumentException if required fields are missing or invalid
         */
        public Message build() {
            return new Message(role, parts, messageId == null ? UUID.randomUUID().toString() : messageId,
                    contextId, taskId, referenceTaskIds, metadata, extensions);
        }
    }
}
