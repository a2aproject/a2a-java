package org.a2aproject.sdk.compat03.spec;

import java.util.Map;

import org.a2aproject.sdk.util.Assert;

/**
 * Defines the parameters for a request to send a message to an agent. This can be used
 * to create a new task, continue an existing one, or restart a task.
 */
public record MessageSendParams_v0_3(Message_v0_3 message, MessageSendConfiguration_v0_3 configuration,
                                     Map<String, Object> metadata) {

    public MessageSendParams_v0_3 {
        Assert.checkNotNullParam("message", message);
    }

    public static class Builder {
        Message_v0_3 message;
        MessageSendConfiguration_v0_3 configuration;
        Map<String, Object> metadata;

        public Builder message(Message_v0_3 message) {
            this.message = message;
            return this;
        }

        public Builder configuration(MessageSendConfiguration_v0_3 configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public MessageSendParams_v0_3 build() {
            return new MessageSendParams_v0_3(message, configuration, metadata);
        }
    }
}
