package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils.defaultIfNull;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.a2aproject.sdk.util.Assert;

/**
 * Used to send a message request.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SendMessageRequest extends NonStreamingJSONRPCRequest<MessageSendParams> {

    public static final String METHOD = "message/send";

    @JsonCreator
    public SendMessageRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                              @JsonProperty("method") String method, @JsonProperty("params") MessageSendParams params) {
        if (jsonrpc == null || jsonrpc.isEmpty()) {
            throw new IllegalArgumentException("JSON-RPC protocol version cannot be null or empty");
        }
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid SendMessageRequest method");
        }
        Assert.checkNotNullParam("params", params);
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public SendMessageRequest(Object id, MessageSendParams params) {
        this(JSONRPC_VERSION, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method;
        private MessageSendParams params;

        public Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public Builder id(Object id) {
            this.id = id;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder params(MessageSendParams params) {
            this.params = params;
            return this;
        }

        public SendMessageRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new SendMessageRequest(jsonrpc, id, method, params);
        }
    }
}
