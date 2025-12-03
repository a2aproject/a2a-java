package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * JSON-RPC request to cancel an in-progress task.
 * <p>
 * This request instructs the agent to cancel execution of a specific task identified by ID.
 * The agent should stop processing, clean up resources, and transition the task to
 * {@link TaskState#CANCELED} state if cancellation is possible.
 * <p>
 * Not all tasks can be canceled (e.g., already completed tasks), which may result in
 * a {@link TaskNotCancelableError}.
 * <p>
 * This class implements the JSON-RPC {@code tasks/cancel} method as specified in the A2A Protocol.
 *
 * @see CancelTaskResponse for the corresponding response
 * @see TaskIdParams for the parameter structure
 * @see TaskNotCancelableError for the error when cancellation is not possible
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CancelTaskRequest extends NonStreamingJSONRPCRequest<TaskIdParams> {

    public static final String METHOD = "CancelTask";

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

    public CancelTaskRequest(Object id, TaskIdParams params) {
        this(null, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method = METHOD;
        private TaskIdParams params;

        public CancelTaskRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public CancelTaskRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public CancelTaskRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        public CancelTaskRequest.Builder params(TaskIdParams params) {
            this.params = params;
            return this;
        }

        public CancelTaskRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new CancelTaskRequest(jsonrpc, id, method, params);
        }
    }
}
