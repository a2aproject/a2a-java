package org.a2aproject.sdk.compat03.spec;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Represents a single message in the conversation between a user and an agent.
 */
public record Message_v0_3(
        Role role,
        List<Part_v0_3<?>> parts,
        @Nullable String messageId,
        @Nullable String contextId,
        @Nullable String taskId,
        @Nullable List<String> referenceTaskIds,
        @Nullable Map<String, Object> metadata,
        @Nullable List<String> extensions,
        String kind
) implements EventKind_v0_3, StreamingEventKind_v0_3 {

    public static final String KIND = "message";

    public Message_v0_3(Role role, List<Part_v0_3<?>> parts, @Nullable String messageId, @Nullable String contextId,
                        @Nullable String taskId, @Nullable List<String> referenceTaskIds,
                        @Nullable Map<String, Object> metadata, @Nullable List<String> extensions, String kind) {
        Assert.checkNotNullParam("role", role);
        Assert.checkNotNullParam("parts", parts);
        this.role = role;
        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Parts cannot be empty");
        }
        this.parts = List.copyOf(parts);
        this.messageId = messageId;
        this.kind = kind != null ? kind : KIND;
        if (!this.kind.equals(KIND)) {
            throw new IllegalArgumentException("Invalid Message");
        }
        this.contextId = contextId;
        this.taskId = taskId;
        this.referenceTaskIds = referenceTaskIds != null ? List.copyOf(referenceTaskIds) : null;
        this.metadata = metadata != null ? Map.copyOf(metadata) : null;
        this.extensions = extensions != null ? List.copyOf(extensions) : null;
    }

    public Message_v0_3(Role role, List<Part_v0_3<?>> parts, @Nullable String messageId, @Nullable String contextId,
                        @Nullable String taskId, @Nullable List<String> referenceTaskIds,
                        @Nullable Map<String, Object> metadata, @Nullable List<String> extensions) {
        this(role, parts, messageId, contextId, taskId, referenceTaskIds, metadata, extensions, KIND);
    }

    public enum Role {
        USER("user"),
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

    public static class Builder {

        private Role role;
        private List<Part_v0_3<?>> parts;
        private String messageId;
        private String contextId;
        private String taskId;
        private List<String> referenceTaskIds;
        private Map<String, Object> metadata;
        private List<String> extensions;

        public Builder() {
        }

        public Builder(Message_v0_3 message) {
            role = message.role;
            parts = message.parts;
            messageId = message.messageId;
            contextId = message.contextId;
            taskId = message.taskId;
            referenceTaskIds = message.referenceTaskIds;
            metadata = message.metadata;
            extensions = message.extensions;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder parts(List<Part_v0_3<?>> parts) {
            this.parts = parts;
            return this;
        }

        public Builder parts(Part_v0_3<?>... parts) {
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

        public Builder extensions(List<String> extensions) {
            this.extensions = (extensions == null) ? null : List.copyOf(extensions);
            return this;
        }

        public Message_v0_3 build() {
            return new Message_v0_3(role, parts, messageId == null ? UUID.randomUUID().toString() : messageId,
                    contextId, taskId, referenceTaskIds, metadata, extensions);
        }
    }
}
