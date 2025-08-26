package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Defines the parameters for a request to send a message to an agent. This can be used
 * to create a new task, continue an existing one, or restart a task.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record MessageSendParams(Message message, MessageSendConfiguration configuration,
                                Map<String, Object> metadata) {

    public MessageSendParams {
        Assert.checkNotNullParam("message", message);
    }

    public static class Builder {
        Message message;
        MessageSendConfiguration configuration;
        Map<String, Object> metadata;

        public Builder message(Message message) {
            this.message = message;
            return this;
        }

        public Builder configuration(MessageSendConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public MessageSendParams build() {
            return new MessageSendParams(message, configuration, metadata);
        }
    }
}
