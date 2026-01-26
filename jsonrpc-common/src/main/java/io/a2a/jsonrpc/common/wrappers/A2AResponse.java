package io.a2a.jsonrpc.common.wrappers;

import static io.a2a.util.Utils.defaultIfNull;

import io.a2a.spec.A2AError;
import io.a2a.util.Assert;

/**
 * Represents a JSONRPC response.
 *
 * @param <T> the type of the result value returned in successful responses
 */
public abstract sealed class A2AResponse<T> implements A2AMessage permits CancelTaskResponse, DeleteTaskPushNotificationConfigResponse, GetExtendedAgentCardResponse, GetTaskPushNotificationConfigResponse, GetTaskResponse, A2AErrorResponse, ListTaskPushNotificationConfigResponse, ListTasksResponse, SendMessageResponse, SendStreamingMessageResponse, CreateTaskPushNotificationConfigResponse {

    /** The JSON-RPC protocol version. */
    protected String jsonrpc;
    /** The request identifier this response corresponds to. */
    protected Object id;
    /** The result of the method invocation. */
    protected T result;
    /** The error object if the invocation failed. */
    protected A2AError error;

    /**
     * Default constructor for deserialization.
     */
    public A2AResponse() {
    }

    /**
     * Constructs a JSON-RPC response.
     *
     * @param jsonrpc the JSON-RPC version
     * @param id the request ID
     * @param result the result value
     * @param error the error if any
     * @param resultType the result type class
     */
    public A2AResponse(String jsonrpc, Object id, T result, A2AError error, Class<T> resultType) {
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

    /**
     * Gets the result value.
     *
     * @return the result
     */
    public T getResult() {
        return this.result;
    }

    /**
     * Gets the error object.
     *
     * @return the error if any
     */
    public A2AError getError() {
        return this.error;
    }
}
