package io.a2a.spec;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;
import io.a2a.util.Utils;

/**
 * Represents a JSON-RPC request for the `agent/getAuthenticatedExtendedCard` method.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GetAuthenticatedExtendedCardRequest extends NonStreamingJSONRPCRequest<Void> {

    public static final String METHOD = "agent/getAuthenticatedExtendedCard";

    @JsonCreator
    public GetAuthenticatedExtendedCardRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                               @JsonProperty("method") String method, @JsonProperty("params") Void params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid GetAuthenticatedExtendedCardRequest method");
        }
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = Utils.defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public GetAuthenticatedExtendedCardRequest(String id) {
        this(null, id, METHOD, null);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method;

        public GetAuthenticatedExtendedCardRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public GetAuthenticatedExtendedCardRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public GetAuthenticatedExtendedCardRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        public GetAuthenticatedExtendedCardRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new GetAuthenticatedExtendedCardRequest(jsonrpc, id, method, null);
        }
    }
}
