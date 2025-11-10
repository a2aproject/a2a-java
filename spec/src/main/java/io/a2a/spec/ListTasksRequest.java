package io.a2a.spec;

import static io.a2a.util.Utils.defaultIfNull;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * A list tasks request.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ListTasksRequest extends NonStreamingJSONRPCRequest<ListTasksParams> {

    public static final String METHOD = "tasks/list";

    @JsonCreator
    public ListTasksRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                            @JsonProperty("method") String method, @JsonProperty("params") ListTasksParams params) {
        if (jsonrpc == null || jsonrpc.isEmpty()) {
            throw new IllegalArgumentException("JSON-RPC protocol version cannot be null or empty");
        }
        if (jsonrpc != null && !jsonrpc.equals(JSONRPC_VERSION)) {
            throw new IllegalArgumentException("Invalid JSON-RPC protocol version");
        }
        Assert.checkNotNullParam("method", method);
        if (!method.equals(METHOD)) {
            throw new IllegalArgumentException("Invalid ListTasksRequest method");
        }
        Assert.checkNotNullParam("params", params);
        Assert.isNullOrStringOrInteger(id);
        this.jsonrpc = defaultIfNull(jsonrpc, JSONRPC_VERSION);
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public ListTasksRequest(Object id, ListTasksParams params) {
        this(JSONRPC_VERSION, id, METHOD, params);
    }

    public static class Builder {
        private String jsonrpc;
        private Object id;
        private String method;
        private ListTasksParams params;

        public Builder jsonrpc(String jsonrpc) {
            this.jsonrpc = jsonrpc;
            return this;
        }

        public Builder id(Object id) {
            this.id = id;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder params(ListTasksParams params) {
            this.params = params;
            return this;
        }

        public ListTasksRequest build() {
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
            return new ListTasksRequest(jsonrpc, id, method, params);
        }
    }
}
