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
 * Represents a single message in the conversation between a user and an agent.
 */
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

    public Message(Role role, List<Part<?>> parts, String messageId, String contextId, String taskId,
                   List<String> referenceTaskIds, Map<String, Object> metadata) {
        this(role, parts, messageId, contextId, taskId, referenceTaskIds, metadata, MESSAGE);
    }

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

    @Override
    public String getKind() {
        return kind;
    }

    public enum Role {
        USER("user"),
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

    public static class Builder {

        private Role role;
        private List<Part<?>> parts;
        private String messageId;
        private String contextId;
        private String taskId;
        private List<String> referenceTaskIds;
        private Map<String, Object> metadata;

        public Builder() {
        }

        public Builder(Message message) {
            role = message.role;
            parts = message.parts;
            messageId = message.messageId;
            contextId = message.contextId;
            taskId = message.taskId;
            referenceTaskIds = message.referenceTaskIds;
            metadata = message.metadata;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder parts(List<Part<?>> parts) {
            this.parts = parts;
            return this;
        }

        public Builder parts(Part<?>...parts) {
            this.parts = List.of(parts);
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder referenceTaskIds(List<String> referenceTaskIds) {
            this.referenceTaskIds = referenceTaskIds;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Message build() {
            return new Message(role, parts, messageId, contextId, taskId, referenceTaskIds, metadata);
        }
    }
}
