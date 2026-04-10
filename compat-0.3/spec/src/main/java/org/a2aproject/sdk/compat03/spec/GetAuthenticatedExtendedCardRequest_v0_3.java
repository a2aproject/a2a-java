package org.a2aproject.sdk.compat03.spec;

import java.util.UUID;


import org.a2aproject.sdk.util.Assert;
import org.a2aproject.sdk.compat03.util.Utils_v0_3;

/**
 * Represents a JSON-RPC request for the `agent/getAuthenticatedExtendedCard` method.
 */
public final class GetAuthenticatedExtendedCardRequest_v0_3 extends NonStreamingJSONRPCRequest_v0_3<Void> {

    public static final String METHOD = "agent/getAuthenticatedExtendedCard";

    public GetAuthenticatedExtendedCardRequest_v0_3(String jsonrpc, Object id, String method, Void params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid GetAuthenticatedExtendedCardRequest method");
        }
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = Utils_v0_3.defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public GetAuthenticatedExtendedCardRequest_v0_3(String id) {
        this(null, id, METHOD, null);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method;

        public GetAuthenticatedExtendedCardRequest_v0_3.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public GetAuthenticatedExtendedCardRequest_v0_3.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public GetAuthenticatedExtendedCardRequest_v0_3.Builder method(String method) {
            this.method = method;
            return this;
        }

        public GetAuthenticatedExtendedCardRequest_v0_3 build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new GetAuthenticatedExtendedCardRequest_v0_3(jsonrpc, id, method, null);
        }
    }
}
