package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;
import io.a2a.util.Utils;

import java.util.UUID;

/**
 * A JSON-RPC request to retrieve the push notification configuration for a specific task.
 * This request is used to get the current webhook configuration that the server uses
 * to send asynchronous task updates to the client. Push notifications are useful for
 * long-running tasks where the client may not maintain a persistent connection.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GetTaskPushNotificationConfigRequest extends NonStreamingJSONRPCRequest<GetTaskPushNotificationConfigParams> {

    /** The JSON-RPC method name for getting push notification configuration. */
    public static final String METHOD = "tasks/pushNotificationConfig/get";

    /**
     * Creates a new GetTaskPushNotificationConfigRequest with full parameter specification.
     * This constructor is primarily used by Jackson for JSON deserialization.
     * 
     * @param jsonrpc the JSON-RPC protocol version (must be "2.0")
     * @param id the request identifier (string, number, or null)
     * @param method the method name (must be "tasks/pushNotificationConfig/get")
     * @param params the task parameters containing the task ID
     * @throws IllegalArgumentException if the JSON-RPC version is invalid, method is incorrect, or required parameters are null
     */
    @JsonCreator
    public GetTaskPushNotificationConfigRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                                @JsonProperty("method") String method, @JsonProperty("params") GetTaskPushNotificationConfigParams params) {
        if (jsonrpc != null && ! jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (! method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid GetTaskPushNotificationRequest method");
        }
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = Utils.defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public GetTaskPushNotificationConfigRequest(String id, GetTaskPushNotificationConfigParams params) {
        this(null, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method;
        private GetTaskPushNotificationConfigParams params;

        public GetTaskPushNotificationConfigRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public GetTaskPushNotificationConfigRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public GetTaskPushNotificationConfigRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        public GetTaskPushNotificationConfigRequest.Builder params(GetTaskPushNotificationConfigParams params) {
            this.params = params;
            return this;
        }

        public GetTaskPushNotificationConfigRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new GetTaskPushNotificationConfigRequest(jsonrpc, id, method, params);
        }
    }
}
