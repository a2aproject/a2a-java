package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;

import java.util.UUID;

/**
 * JSON-RPC request to resubscribe to an ongoing or completed task's event stream.
 * <p>
 * This request allows clients to reconnect to a task and receive its events, enabling
 * recovery from disconnections or retrieval of missed updates. The agent will stream
 * events for the task starting from its current state.
 * <p>
 * Resubscription is particularly useful for:
 * <ul>
 *   <li>Recovering from network interruptions without losing task context</li>
 *   <li>Multiple clients observing the same task</li>
 *   <li>Retrieving final results for completed tasks</li>
 * </ul>
 * <p>
 * This class implements the JSON-RPC {@code SubscribeToTask} method as specified in the A2A Protocol.
 *
 * @see TaskIdParams for the parameter structure
 * @see StreamingEventKind for the types of events that can be streamed
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SubscribeToTaskRequest extends StreamingJSONRPCRequest<TaskIdParams> {

    public static final String METHOD = "SubscribeToTask";

    @JsonCreator
    public SubscribeToTaskRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
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

    public SubscribeToTaskRequest(Object id, TaskIdParams params) {
        this(null, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method = METHOD;
        private TaskIdParams params;

        public SubscribeToTaskRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public SubscribeToTaskRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public SubscribeToTaskRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        public SubscribeToTaskRequest.Builder params(TaskIdParams params) {
            this.params = params;
            return this;
        }

        public SubscribeToTaskRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new SubscribeToTaskRequest(jsonrpc, id, method, params);
        }
    }
}
