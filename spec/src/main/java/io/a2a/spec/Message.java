package io.a2a.spec;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.util.Assert;

/**
 * Represents a message in the A2A protocol communication.
 * A message contains content parts, metadata, and context information for agent-to-agent communication.
 * 
 * This class implements both EventKind and StreamingEventKind interfaces to support
 * different types of message processing in the A2A framework.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Message implements EventKind, StreamingEventKind {

    public static final TypeReference<Message> TYPE_REFERENCE = new TypeReference<>() {};

    public static final String MESSAGE = "message";
    /**
     * The role of the entity that created this message (user or agent).
     */
    private final Role role;
    
    /**
     * The content parts that make up this message.
     * Each part can be text, file, data, or other content types.
     */
    private final List<Part<?>> parts;
    
    /**
     * Unique identifier for this message.
     */
    private final String messageId;
    
    /**
     * Identifier for the conversation context this message belongs to.
     */
    private String contextId;
    
    /**
     * Identifier for the task this message is associated with.
     */
    private String taskId;
    
    /**
     * Additional metadata associated with this message.
     * Can contain custom properties specific to the implementation.
     */
    private final Map<String, Object> metadata;
    
    /**
     * The kind/type of this message, always "message" for Message instances.
     */
    private final String kind;
    
    /**
     * List of task IDs that this message references.
     * Used for establishing relationships between tasks.
     */
    private final List<String> referenceTaskIds;

    /**
     * Constructs a new Message with the specified properties.
     *
     * @param role the role of the entity that created this message
     * @param parts the content parts that make up this message
     * @param messageId unique identifier for this message
     * @param contextId identifier for the conversation context
     * @param taskId identifier for the associated task
     * @param referenceTaskIds list of task IDs that this message references
     * @param metadata additional metadata for this message
     */
    public Message(Role role, List<Part<?>> parts, String messageId, String contextId, String taskId,
                   List<String> referenceTaskIds, Map<String, Object> metadata) {
        this(role, parts, messageId, contextId, taskId, referenceTaskIds, metadata, MESSAGE);
    }

    /**
     * JSON constructor for creating Message instances from JSON data.
     *
     * @param role the role of the entity that created this message
     * @param parts the content parts that make up this message
     * @param messageId unique identifier for this message
     * @param contextId identifier for the conversation context
     * @param taskId identifier for the associated task
     * @param referenceTaskIds list of task IDs that this message references
     * @param metadata additional metadata for this message
     * @param kind the kind/type of this message
     */
    @JsonCreator
    public Message(@JsonProperty("role") Role role, @JsonProperty("parts") List<Part<?>> parts,
                   @JsonProperty("messageId") String messageId, @JsonProperty("contextId") String contextId,
                   @JsonProperty("taskId") String taskId, @JsonProperty("referenceTaskIds") List<String> referenceTaskIds,
                   @JsonProperty("metadata") Map<String, Object> metadata,
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
        this.role = role;
        this.parts = parts;
        this.messageId = messageId == null ? UUID.randomUUID().toString() : messageId;
        this.contextId = contextId;
        this.taskId = taskId;
        this.referenceTaskIds = referenceTaskIds;
        this.metadata = metadata;
        this.kind = kind;
    }

    /**
     * Gets the role of the entity that created this message.
     *
     * @return the role (USER or AGENT)
     */
    public Role getRole() {
        return role;
    }

    /**
     * Gets the content parts that make up this message.
     *
     * @return the list of content parts
     */
    public List<Part<?>> getParts() {
        return parts;
    }

    /**
     * Gets the unique identifier for this message.
     *
     * @return the message ID
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Gets the identifier for the conversation context this message belongs to.
     *
     * @return the context ID
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * Gets the identifier for the task this message is associated with.
     *
     * @return the task ID
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Gets the additional metadata associated with this message.
     *
     * @return the metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Sets the identifier for the task this message is associated with.
     *
     * @param taskId the task ID to set
     */
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    /**
     * Sets the identifier for the conversation context this message belongs to.
     *
     * @param contextId the context ID to set
     */
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    /**
     * Gets the list of task IDs that this message references.
     *
     * @return the list of reference task IDs
     */
    public List<String> getReferenceTaskIds() {
        return referenceTaskIds;
    }

    /**
     * Gets the kind/type of this message.
     *
     * @return the message kind, always "message" for Message instances
     */
    @Override
    public String getKind() {
        return kind;
    }

    /**
     * Enumeration representing the role of the entity that created a message.
     */
    public enum Role {
        /**
         * Represents a message created by a user/human.
         */
        USER("user"),
        
        /**
         * Represents a message created by an agent/AI system.
         */
        AGENT("agent");

        private String role;

        Role(String role) {
            this.role = role;
        }

        @JsonValue
        public String asString() {
            return this.role;
        }
    }

    /**
     * Builder class for constructing {@link Message} instances.
     */
    public static class Builder {

        private Role role;
        private List<Part<?>> parts;
        private String messageId;
        private String contextId;
        private String taskId;
        private List<String> referenceTaskIds;
        private Map<String, Object> metadata;

        /**
         * Default constructor for creating a new Builder instance.
         */
        public Builder() {
        }

        /**
         * Constructor for creating a Builder instance from an existing Message.
         *
         * @param message the message to copy properties from
         */
        public Builder(Message message) {
            role = message.role;
            parts = message.parts;
            messageId = message.messageId;
            contextId = message.contextId;
            taskId = message.taskId;
            referenceTaskIds = message.referenceTaskIds;
            metadata = message.metadata;
        }

        /**
         * Sets the role of the entity that created this message.
         *
         * @param role the role (USER or AGENT)
         * @return this builder instance
         */
        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        /**
         * Sets the content parts that make up this message.
         *
         * @param parts the list of content parts
         * @return this builder instance
         */
        public Builder parts(List<Part<?>> parts) {
            this.parts = parts;
            return this;
        }

        /**
         * Sets the content parts that make up this message using varargs.
         *
         * @param parts the content parts as varargs
         * @return this builder instance
         */
        public Builder parts(Part<?>...parts) {
            this.parts = List.of(parts);
            return this;
        }

        /**
         * Sets the unique identifier for this message.
         *
         * @param messageId the message ID
         * @return this builder instance
         */
        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Sets the identifier for the conversation context this message belongs to.
         *
         * @param contextId the context ID
         * @return this builder instance
         */
        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        /**
         * Sets the identifier for the task this message is associated with.
         *
         * @param taskId the task ID
         * @return this builder instance
         */
        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        /**
         * Sets the list of task IDs that this message references.
         *
         * @param referenceTaskIds the list of reference task IDs
         * @return this builder instance
         */
        public Builder referenceTaskIds(List<String> referenceTaskIds) {
            this.referenceTaskIds = referenceTaskIds;
            return this;
        }

        /**
         * Sets the additional metadata associated with this message.
         *
         * @param metadata the metadata map
         * @return this builder instance
         */
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        /**
         * Builds and returns a new {@link Message} instance with the configured properties.
         *
         * @return a new Message instance
         */
        public Message build() {
            return new Message(role, parts, messageId, contextId, taskId, referenceTaskIds, metadata);
        }
    }
}
