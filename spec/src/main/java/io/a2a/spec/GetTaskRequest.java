package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * A JSON-RPC request to retrieve information about a specific task.
 * This request is used to get the current status, progress, and details of a task
 * that was previously created. It allows clients to check on the state of
 * long-running operations and retrieve their results when completed.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GetTaskRequest extends NonStreamingJSONRPCRequest<TaskQueryParams> {

    /** The JSON-RPC method name for getting task information. */
    public static final String METHOD = "tasks/get";

    /**
     * Creates a new GetTaskRequest with full parameter specification.
     * This constructor is primarily used by Jackson for JSON deserialization.
     * 
     * @param jsonrpc the JSON-RPC protocol version (must be "2.0")
     * @param id the request identifier (string, number, or null)
     * @param method the method name (must be "tasks/get")
     * @param params the task parameters containing the task query information
     * @throws IllegalArgumentException if the JSON-RPC version is invalid, method is incorrect, or required parameters are null
     */
    @JsonCreator
    public GetTaskRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                          @JsonProperty("method") String method, @JsonProperty("params") TaskQueryParams params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid GetTaskRequest method");
        }
        Assert.checkNotNullParam("params", params);
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    /**
     * Creates a new GetTaskRequest with simplified parameters.
     * The JSON-RPC version defaults to "2.0" and the method is automatically set.
     * 
     * @param id the request identifier (string, number, or null)
     * @param params the task parameters containing the task query information
     */
    public GetTaskRequest(Object id, TaskQueryParams params) {
        this(null, id, METHOD, params);
    }


    /**
     * Builder class for constructing GetTaskRequest instances.
     * Provides a fluent interface for setting request parameters and automatically
     * generates a unique request ID if none is provided.
     */
    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method = "tasks/get";
        private TaskQueryParams params;

        /**
         * Sets the JSON-RPC protocol version.
         * 
         * @param jsonrpc the JSON-RPC version (typically "2.0")
         * @return this builder instance for method chaining
         */
        public GetTaskRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /**
         * Sets the request identifier.
         * 
         * @param id the request ID (string, number, or null)
         * @return this builder instance for method chaining
         */
        public GetTaskRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the JSON-RPC method name.
         * 
         * @param method the method name (should be "tasks/get")
         * @return this builder instance for method chaining
         */
        public GetTaskRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        /**
         * Sets the task query parameters.
         * 
         * @param params the parameters containing task query information
         * @return this builder instance for method chaining
         */
        public GetTaskRequest.Builder params(TaskQueryParams params) {
            this.params = params;
            return this;
        }

        /**
         * Builds and returns a new GetTaskRequest instance.
         * If no request ID was set, a random UUID will be generated.
         * 
         * @return a new GetTaskRequest with the configured parameters
         */
        public GetTaskRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new GetTaskRequest(jsonrpc, id, method, params);
        }
    }
}
