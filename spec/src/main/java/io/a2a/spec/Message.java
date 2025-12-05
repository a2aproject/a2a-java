package io.a2a.spec;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
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
@JsonTypeName(MESSAGE)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Message implements EventKind, StreamingEventKind {

    public static final TypeReference<Message> TYPE_REFERENCE = new TypeReference<>() {};

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

    public Message(Role role, List<Part<?>> parts, String messageId, String contextId, String taskId,
                   List<String> referenceTaskIds, Map<String, Object> metadata, List<String> extensions) {
        this(role, parts, messageId, contextId, taskId, referenceTaskIds, metadata, extensions, MESSAGE);
    }

    @JsonCreator
    public Message(@JsonProperty("role") Role role, @JsonProperty("parts") List<Part<?>> parts,
                   @JsonProperty("messageId") String messageId, @JsonProperty("contextId") String contextId,
                   @JsonProperty("taskId") String taskId, @JsonProperty("referenceTaskIds") List<String> referenceTaskIds,
                   @JsonProperty("metadata") Map<String, Object> metadata, @JsonProperty("extensions") List<String> extensions,
                   @JsonProperty("kind") String kind) {
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

    public Role getRole() {
        return role;
    }

    public List<Part<?>> getParts() {
        return parts;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getContextId() {
        return contextId;
    }

    public String getTaskId() {
        return taskId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public List<String> getReferenceTaskIds() {
        return referenceTaskIds;
    }

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
        @JsonValue
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
     * Message userMessage = new Message.Builder()
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
        public Builder() {
        }

        /**
         * Creates a new Builder initialized with values from an existing Message.
         *
         * @param message the Message to copy values from
         */
        public Builder(Message message) {
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
