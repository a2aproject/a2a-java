package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.a2a.util.Assert;

/**
 * Represents a JSON-RPC 2.0 response object as defined in the A2A protocol specification.
 * This is an abstract sealed class that serves as the base for all JSON-RPC response types.
 * 
 * <p>A JSON-RPC response must contain either a result (for success) or an error (for failure),
 * but never both. The response also includes the JSON-RPC version and an optional ID that
 * matches the corresponding request.</p>
 * 
 * <p>This class enforces the JSON-RPC 2.0 specification constraints and provides common
 * functionality for all response types in the A2A protocol.</p>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract sealed class JSONRPCResponse<T> implements JSONRPCMessage permits SendStreamingMessageResponse,
        GetTaskResponse, CancelTaskResponse, SetTaskPushNotificationConfigResponse, GetTaskPushNotificationConfigResponse,
        SendMessageResponse, DeleteTaskPushNotificationConfigResponse, ListTaskPushNotificationConfigResponse, JSONRPCErrorResponse {

    /** The JSON-RPC protocol version, must be "2.0" */
    protected String jsonrpc;
    
    /** The request identifier, can be a string, number, or null */
    protected Object id;
    
    /** The result object for successful responses, null for error responses */
    protected T result;
    
    /** The error object for failed responses, null for successful responses */
    protected JSONRPCError error;

    /**
     * Default constructor for JSON deserialization.
     */
    public JSONRPCResponse() {
    }

    public JSONRPCResponse(String jsonrpc, Object id, T result, JSONRPCError error, Class<T> resultType) {
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

    /**
     * Gets the JSON-RPC protocol version.
     * 
     * @return the protocol version ("2.0")
     */
    public String getJsonrpc() {
        return this.jsonrpc;
    }

    /**
     * Gets the request identifier.
     * 
     * @return the request ID (string, number, or null)
     */
    public Object getId() {
        return this.id;
    }

    /**
     * Gets the result object for successful responses.
     * 
     * @return the result object, or null if this is an error response
     */
    public T getResult() {
        return this.result;
    }

    /**
     * Gets the error object for failed responses.
     * 
     * @return the error object, or null if this is a successful response
     */
    public JSONRPCError getError() {
        return this.error;
    }
}
