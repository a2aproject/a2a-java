package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.a2a.util.Assert;

/**
 * Represents a JSON-RPC 2.0 error response.
 * 
 * This class extends {@link JSONRPCResponse} to provide a complete JSON-RPC error response
 * structure according to the JSON-RPC 2.0 specification. An error response contains
 * the standard JSON-RPC fields (jsonrpc, id) along with an error object that includes
 * the error code, message, and optional data.
 * 
 * The response is used when a JSON-RPC request cannot be processed successfully,
 * providing detailed error information to the client.
 * 
 * @see JSONRPCResponse
 * @see JSONRPCError
 * @see <a href="https://www.jsonrpc.org/specification#response_object">JSON-RPC 2.0 Response Object</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class JSONRPCErrorResponse extends JSONRPCResponse<Void> {

    /**
     * Creates a new JSONRPCErrorResponse with all parameters.
     * 
     * @param jsonrpc the JSON-RPC version (should be "2.0")
     * @param id the request identifier that this response corresponds to
     * @param result the result field (always null for error responses)
     * @param error the error object containing error details
     * @throws IllegalArgumentException if error is null
     */
    @JsonCreator
    public JSONRPCErrorResponse(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") Object id,
                                @JsonProperty("result") Void result, @JsonProperty("error") JSONRPCError error) {
        super(jsonrpc, id, result, error, Void.class);
        Assert.checkNotNullParam("error", error);
    }

    /**
     * Creates a new JSONRPCErrorResponse with the specified id and error.
     * The jsonrpc version will be set to null and result will be null.
     * 
     * @param id the request identifier that this response corresponds to
     * @param error the error object containing error details
     */
    public JSONRPCErrorResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    /**
     * Creates a new JSONRPCErrorResponse with only the error object.
     * The jsonrpc version, id, and result will all be set to null.
     * 
     * @param error the error object containing error details
     */
    public JSONRPCErrorResponse(JSONRPCError error) {
        this(null, null, null, error);
    }
}
