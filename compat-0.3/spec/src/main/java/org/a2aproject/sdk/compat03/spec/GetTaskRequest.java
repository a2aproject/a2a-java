package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.util.Utils.defaultIfNull;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.a2aproject.sdk.util.Assert;

/**
 * A get task request.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GetTaskRequest extends NonStreamingJSONRPCRequest<TaskQueryParams> {

    public static final String METHOD = "tasks/get";

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

    public GetTaskRequest(Object id, TaskQueryParams params) {
        this(null, id, METHOD, params);
    }


    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method = "tasks/get";
        private TaskQueryParams params;

        public GetTaskRequest.Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public GetTaskRequest.Builder id(Object id) {
            this.id = id;
            return this;
        }

        public GetTaskRequest.Builder method(String method) {
            this.method = method;
            return this;
        }

        public GetTaskRequest.Builder params(TaskQueryParams params) {
            this.params = params;
            return this;
        }

        public GetTaskRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new GetTaskRequest(jsonrpc, id, method, params);
        }
    }
}
