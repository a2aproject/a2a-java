package io.a2a.spec;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

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
 * @param role the role of the message sender (user or agent)
 * @param parts the content parts of the message (text, file, or data)
 * @param messageId the unique identifier for this message
 * @param contextId the conversation context identifier
 * @param taskId the task identifier this message is associated with
 * @param referenceTaskIds list of reference task identifiers
 * @param metadata additional metadata for the message
 * @param extensions list of protocol extensions used in this message
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record Message(Role role, List<Part<?>> parts,
        String messageId, @Nullable
        String contextId,
        @Nullable
        String taskId, @Nullable
        List<String> referenceTaskIds,
        @Nullable
        Map<String, Object> metadata, @Nullable
        List<String> extensions
        ) implements EventKind, StreamingEventKind {

    /**
     * The identifier when used in streaming responses
     */
    public static final String STREAMING_EVENT_ID = "message";

    /**
     * Compact constructor with validation and defensive copying.
     *
     * @param role the role of the message sender (user or agent)
     * @param parts the content parts of the message (text, file, or data)
     * @param messageId the unique identifier for this message
     * @param contextId the conversation context identifier
     * @param taskId the task identifier this message is associated with
     * @param referenceTaskIds list of reference task identifiers
     * @param metadata additional metadata for the message
     * @param extensions list of protocol extensions used in this message
     * @throws IllegalArgumentException if role, parts, or messageId is null, or if parts is empty
     */
    public Message {
        Assert.checkNotNullParam("role", role);
        Assert.checkNotNullParam("parts", parts);
        Assert.checkNotNullParam("messageId", messageId);
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Parts cannot be empty");
        }
        parts = List.copyOf(parts);
        referenceTaskIds = referenceTaskIds != null ? List.copyOf(referenceTaskIds) : null;
        metadata = (metadata != null) ? Map.copyOf(metadata) : null;
        extensions = extensions != null ? List.copyOf(extensions) : null;
    }

    @Override
    public String kind() {
        return STREAMING_EVENT_ID;
    }

    /**
     * Creates a new Builder for constructing Message instances.
     *
     * @return a Message.builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new Builder initialized with values from an existing Message.
     *
     * @param message the Message to copy values from
     * @return a new Builder instance initialized with the message's values
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
     *     .parts(List.of(new TextPart("Hello, agent!")))
     *     .contextId("conv-123")
     *     .build();
     * }</pre>
     */
    public static class Builder {

        private @Nullable
        Role role;
        private @Nullable
        List<Part<?>> parts;
        private @Nullable
        String messageId;
        private @Nullable
        String contextId;
        private @Nullable
        String taskId;
        private @Nullable
        List<String> referenceTaskIds;
        private @Nullable
        Map<String, Object> metadata;
        private @Nullable
        List<String> extensions;

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
            role = message.role();
            parts = message.parts();
            messageId = message.messageId();
            contextId = message.contextId();
            taskId = message.taskId();
            referenceTaskIds = message.referenceTaskIds();
            metadata = message.metadata();
            extensions = message.extensions();
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
        public Builder parts(Part<?>... parts) {
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
        public Builder metadata(@Nullable Map<String, Object> metadata) {
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
            return new Message(
                    Assert.checkNotNullParam("role", role),
                    Assert.checkNotNullParam("parts", parts),
                    messageId == null ? UUID.randomUUID().toString() : messageId,
                    contextId,
                    taskId,
                    referenceTaskIds,
                    metadata,
                    extensions);
        }
    }
}
