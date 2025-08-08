package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * Represents a JSON-RPC 2.0 request to configure push notification settings for a specific task.
 * This request allows clients to set up push notification configurations that will be used
 * to notify about task status changes, progress updates, or completion events.
 *
 * <p>Push notification configurations enable real-time communication between the agent
 * and external systems, allowing for immediate notification delivery when task events occur.
 * This is particularly useful for long-running tasks where clients need to be notified
 * of progress or completion without polling.</p>
 *
 * <p>The request follows the JSON-RPC 2.0 specification and uses the method
 * "tasks/pushNotificationConfig/set" to identify this operation type.</p>
 *
 * @see TaskPushNotificationConfig
 * @see NonStreamingJSONRPCRequest
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SetTaskPushNotificationConfigRequest extends NonStreamingJSONRPCRequest<TaskPushNotificationConfig> {

    /** The JSON-RPC method name for setting task push notification configuration */
    public static final String METHOD = "tasks/pushNotificationConfig/set";

    /**
     * Creates a new SetTaskPushNotificationConfigRequest with all JSON-RPC 2.0 fields.
     * This constructor is primarily used by JSON deserialization.
     *
     * @param jsonrpc the JSON-RPC protocol version (must be "2.0")
     * @param id the request identifier (string, number, or null)
     * @param method the method name (must be "tasks/pushNotificationConfig/set")
     * @param params the task push notification configuration parameters
     * @throws IllegalArgumentException if jsonrpc version is invalid, method is incorrect, or params is null
     */
    @JsonCreator
    public SetTaskPushNotificationConfigRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                                @JsonProperty("method") String method, @JsonProperty("params") TaskPushNotificationConfig params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid SetTaskPushNotificationRequest method");
        }
        Assert.checkNotNullParam("params", params);
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    /**
     * Creates a new SetTaskPushNotificationConfigRequest with simplified parameters.
     * The JSON-RPC version defaults to "2.0" and the method is automatically set.
     *
     * @param id the request identifier
     * @param taskPushConfig the task push notification configuration
     */
    public SetTaskPushNotificationConfigRequest(String id, TaskPushNotificationConfig taskPushConfig) {
        this(null, id, METHOD, taskPushConfig);
    }

    /**
     * Builder class for constructing SetTaskPushNotificationConfigRequest instances.
     * Provides a fluent API for setting request parameters with validation.
     */
    public static class Builder {
        /** The JSON-RPC protocol version */
        private String jsonrpc;
        
        /** The request identifier */
        private Object id;
        
        /** The JSON-RPC method name */
        private String method = METHOD;
        
        /** The task push notification configuration parameters */
        private TaskPushNotificationConfig params;

        /**
         * Sets the JSON-RPC protocol version.
         *
         * @param jsonrpc the JSON-RPC version (should be "2.0")
         * @return this builder instance for method chaining
         */
        public SetTaskPushNotificationConfigRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        /**
         * Sets the request identifier.
         *
         * @param id the request identifier (string, number, or null)
         * @return this builder instance for method chaining
         */
        public SetTaskPushNotificationConfigRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the JSON-RPC method name.
         *
         * @param method the method name (should be "tasks/pushNotificationConfig/set")
         * @return this builder instance for method chaining
         */
        public SetTaskPushNotificationConfigRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        /**
         * Sets the task push notification configuration parameters.
         *
         * @param params the task push notification configuration
         * @return this builder instance for method chaining
         */
        public SetTaskPushNotificationConfigRequest.Builder params(TaskPushNotificationConfig params) {
            this.params = params;
            return this;
        }

        /**
         * Builds and returns a new SetTaskPushNotificationConfigRequest instance.
         * If no ID is provided, a random UUID will be generated.
         *
         * @return a new SetTaskPushNotificationConfigRequest with the configured parameters
         */
        public SetTaskPushNotificationConfigRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new SetTaskPushNotificationConfigRequest(jsonrpc, id, method, params);
        }
    }
}
