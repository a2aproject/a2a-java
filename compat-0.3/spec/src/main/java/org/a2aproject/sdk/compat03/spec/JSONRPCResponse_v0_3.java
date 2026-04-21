package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;

import org.a2aproject.sdk.util.Assert;

/**
 * Represents a JSONRPC response.
 */
public abstract sealed class JSONRPCResponse_v0_3<T> implements JSONRPCMessage_v0_3 permits SendStreamingMessageResponse_v0_3,
        GetTaskResponse_v0_3, CancelTaskResponse_v0_3, SetTaskPushNotificationConfigResponse_v0_3, GetTaskPushNotificationConfigResponse_v0_3,
        SendMessageResponse_v0_3, DeleteTaskPushNotificationConfigResponse_v0_3, ListTaskPushNotificationConfigResponse_v0_3, JSONRPCErrorResponse_v0_3,
        GetAuthenticatedExtendedCardResponse_v0_3 {

    protected String jsonrpc;
    protected Object id;
    protected T result;
    protected JSONRPCError_v0_3 error;

    public JSONRPCResponse_v0_3() {
    }

    public JSONRPCResponse_v0_3(String jsonrpc, Object id, T result, JSONRPCError_v0_3 error, Class<T> resultType) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        if (error != null && result != null) {
            throw new IllegalArgumentException("Invalid JSON-RPC error response");
        }
        if (error == null && result == null && ! Void.class.equals(resultType)) {
            throw new IllegalArgumentException("Invalid JSON-RPC success response");
        }
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.result = result;
        this.error = error;
    }

    @Override
    public String getJsonrpc() {
        return this.jsonrpc;
    }

    @Override
    public Object getId() {
        return this.id;
    }

    public T getResult() {
        return this.result;
    }

    public JSONRPCError_v0_3 getError() {
        return this.error;
    }
}
