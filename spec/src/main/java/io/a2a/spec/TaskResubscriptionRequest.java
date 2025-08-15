package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;

import java.util.UUID;

/**
 * Represents a streaming JSON-RPC 2.0 request to resubscribe to task updates.
 * This request allows clients to re-establish a subscription to receive real-time
 * updates about a specific task's status, progress, and completion events.
 *
 * <p>Task resubscription is typically used in scenarios such as:</p>
 * <ul>
 *   <li>Reconnecting after a network disconnection</li>
 *   <li>Resuming monitoring of long-running tasks</li>
 *   <li>Re-establishing event streams after client restart</li>
 *   <li>Switching between different monitoring configurations</li>
 * </ul>
 *
 * <p>As a streaming request, this operation establishes a persistent connection
 * that delivers continuous updates until the task completes or the subscription
 * is explicitly canceled. The request follows the JSON-RPC 2.0 specification
 * and uses the method "tasks/resubscribe".</p>
 *
 * @see StreamingJSONRPCRequest
 * @see TaskIdParams
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TaskResubscriptionRequest extends StreamingJSONRPCRequest<TaskIdParams> {

    /** The JSON-RPC method name for task resubscription requests */
    public static final String METHOD = "tasks/resubscribe";

    /**
     * Creates a new TaskResubscriptionRequest with all JSON-RPC 2.0 fields.
     * This constructor is primarily used by JSON deserialization.
     *
     * @param jsonrpc the JSON-RPC protocol version (must be "2.0")
     * @param id the request identifier (string, number, or null; auto-generated if null)
     * @param method the method name (must be "tasks/resubscribe")
     * @param params the task ID parameters specifying which task to resubscribe to
     * @throws IllegalArgumentException if jsonrpc version is invalid, method is incorrect, or params is null
     */
    @JsonCreator
    public TaskResubscriptionRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                     @JsonProperty("method") String method, @JsonProperty("params") TaskIdParams params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid TaskResubscriptionRequest method");
        }
        Assert.checkNotNullParam("params", params);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.method = method;
        this.params = params;
    }

    /**
     * Creates a new TaskResubscriptionRequest with simplified parameters.
     * The JSON-RPC version defaults to "2.0" and the method is automatically set.
     *
     * @param id the request identifier
     * @param params the task ID parameters specifying which task to resubscribe to
     */
    public TaskResubscriptionRequest(Object id, TaskIdParams params) {
        this(null, id, METHOD, params);
    }

    /**
     * Builder class for constructing TaskResubscriptionRequest instances.
     * Provides a fluent API for setting request parameters with validation.
     */
    public static class Builder {
        /** The JSON-RPC protocol version */
        private String jsonrpc;
        
        /** The request identifier */
        private Object id;
        
        /** The JSON-RPC method name */
        private String method = METHOD;
        
        /** The task ID parameters */
        private TaskIdParams params;

        /**
         * Sets the JSON-RPC protocol version.
         *
         * @param jsonrpc the JSON-RPC version (should be "2.0")
         * @return this builder instance for method chaining
         */
        public TaskResubscriptionRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /**
         * Sets the request identifier.
         *
         * @param id the request identifier (string, number, or null)
         * @return this builder instance for method chaining
         */
        public TaskResubscriptionRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the JSON-RPC method name.
         *
         * @param method the method name (should be "tasks/resubscribe")
         * @return this builder instance for method chaining
         */
        public TaskResubscriptionRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        /**
         * Sets the task ID parameters.
         *
         * @param params the task ID parameters specifying which task to resubscribe to
         * @return this builder instance for method chaining
         */
        public TaskResubscriptionRequest.Builder params(TaskIdParams params) {
            this.params = params;
            return this;
        }

        /**
         * Builds and returns a new TaskResubscriptionRequest instance.
         * If no ID is provided, a random UUID will be generated.
         *
         * @return a new TaskResubscriptionRequest with the configured parameters
         */
        public TaskResubscriptionRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new TaskResubscriptionRequest(jsonrpc, id, method, params);
        }
    }
}
