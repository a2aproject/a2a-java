package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * A JSON-RPC request that can be used to cancel a running or pending task.
 * This request follows the A2A protocol specification for task cancellation.
 * When a task is successfully cancelled, it transitions to the "canceled" state.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CancelTaskRequest extends NonStreamingJSONRPCRequest<TaskIdParams> {

    /** The JSON-RPC method name for canceling tasks. */
    public static final String METHOD = "tasks/cancel";

    /**
     * Creates a new CancelTaskRequest with full parameter specification.
     * This constructor is primarily used by Jackson for JSON deserialization.
     * 
     * @param jsonrpc the JSON-RPC protocol version (must be "2.0")
     * @param id the request identifier (string, number, or null)
     * @param method the method name (must be "tasks/cancel")
     * @param params the task parameters containing the task ID to cancel
     * @throws IllegalArgumentException if the JSON-RPC version is invalid, method is incorrect, or required parameters are null
     */
    @JsonCreator
    public CancelTaskRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                             @JsonProperty("method") String method, @JsonProperty("params") TaskIdParams params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid CancelTaskRequest method");
        }
        Assert.checkNotNullParam("params", params);
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    /**
     * Creates a new CancelTaskRequest with simplified parameters.
     * The JSON-RPC version defaults to "2.0" and the method is automatically set to "tasks/cancel".
     * 
     * @param id the request identifier (string, number, or null)
     * @param params the task parameters containing the task ID to cancel
     */
    public CancelTaskRequest(Object id, TaskIdParams params) {
        this(null, id, METHOD, params);
    }

    /**
     * Builder class for constructing CancelTaskRequest instances.
     * Provides a fluent API for setting request parameters with sensible defaults.
     */
    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method = METHOD;
        private TaskIdParams params;

        /**
         * Sets the JSON-RPC protocol version.
         * 
         * @param jsonrpc the JSON-RPC version (typically "2.0")
         * @return this builder instance for method chaining
         */
        public CancelTaskRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /**
         * Sets the request identifier.
         * 
         * @param id the request identifier (string, number, or null)
         * @return this builder instance for method chaining
         */
        public CancelTaskRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the JSON-RPC method name.
         * 
         * @param method the method name (should be "tasks/cancel")
         * @return this builder instance for method chaining
         */
        public CancelTaskRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        /**
         * Sets the task parameters containing the task ID to cancel.
         * 
         * @param params the task parameters
         * @return this builder instance for method chaining
         */
        public CancelTaskRequest.Builder params(TaskIdParams params) {
            this.params = params;
            return this;
        }

        /**
         * Builds the CancelTaskRequest instance.
         * If no ID is specified, a random UUID will be generated.
         * 
         * @return a new CancelTaskRequest instance
         */
        public CancelTaskRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new CancelTaskRequest(jsonrpc, id, method, params);
        }
    }
}
