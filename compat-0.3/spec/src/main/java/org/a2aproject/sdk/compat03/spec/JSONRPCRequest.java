package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils.defaultIfNull;

import org.a2aproject.sdk.util.Assert;

/**
 * Represents a JSONRPC request.
 */
public abstract sealed class JSONRPCRequest<T> implements JSONRPCMessage permits NonStreamingJSONRPCRequest, StreamingJSONRPCRequest {

    protected String jsonrpc;
    protected Object id;
    protected String method;
    protected T params;

    public JSONRPCRequest() {
    }

    public JSONRPCRequest(String jsonrpc, Object id, String method, T params) {
        Assert.checkNotNullParam("jsonrpc", jsonrpc);
        Assert.checkNotNullParam("method", method);
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    @Override
    public String getJsonrpc() {
        return this.jsonrpc;
    }

    @Override
    public Object getId() {
        return this.id;
    }

    public String getMethod() {
        return this.method;
    }

    public T getParams() {
        return this.params;
    }
}
