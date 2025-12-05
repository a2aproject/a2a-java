package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;
import io.a2a.util.Utils;

import java.util.UUID;

/**
 * JSON-RPC request to retrieve push notification configuration for a task.
 * <p>
 * This request retrieves the currently configured push notification endpoint and settings
 * for a specific task, allowing clients to verify or inspect the notification configuration.
 * <p>
 * This class implements the JSON-RPC {@code tasks/pushNotificationConfig/get} method.
 *
 * @see GetTaskPushNotificationConfigResponse for the response
 * @see GetTaskPushNotificationConfigParams for the parameter structure
 * @see TaskPushNotificationConfig for the returned configuration
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GetTaskPushNotificationConfigRequest extends NonStreamingJSONRPCRequest<GetTaskPushNotificationConfigParams> {

    public static final String METHOD = "GetTaskPushNotificationConfig";

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
