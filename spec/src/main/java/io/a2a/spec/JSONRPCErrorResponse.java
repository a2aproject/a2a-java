package io.a2a.spec;

import io.a2a.util.Assert;

/**
 * Represents a JSON-RPC 2.0 error response.
 * <p>
 * An error response is returned when a request cannot be processed successfully.
 * According to the JSON-RPC 2.0 specification, an error response must contain:
 * <ul>
 *   <li>{@code jsonrpc} - Always "2.0"</li>
 *   <li>{@code error} - A {@link JSONRPCError} object describing the error</li>
 *   <li>{@code id} - The request ID, or null if the ID could not be determined</li>
 * </ul>
 * <p>
 * The {@code result} field must be absent or null in error responses. This class
 * enforces that constraint by fixing the result type parameter to {@code Void}.
 *
 * @see JSONRPCError
 * @see JSONRPCResponse
 * @see <a href="https://www.jsonrpc.org/specification#response_object">JSON-RPC 2.0 Response Object</a>
 */
public final class JSONRPCErrorResponse extends JSONRPCResponse<Void> {

    /**
     * Constructs a JSON-RPC error response with all fields.
     * <p>
     * This constructor is used for JSON deserialization.
     *
     * @param jsonrpc the JSON-RPC version (must be "2.0")
     * @param id the request ID, or null if the ID could not be determined from the request
     * @param result must be null for error responses
     * @param error the error object describing what went wrong (required)
     * @throws IllegalArgumentException if error is null
     */
    public JSONRPCErrorResponse(String jsonrpc, Object id, Void result, JSONRPCError error) {
        super(jsonrpc, id, result, error, Void.class);
        Assert.checkNotNullParam("error", error);
    }

    /**
     * Constructs a JSON-RPC error response with a request ID and error.
     * <p>
     * The jsonrpc field defaults to "2.0".
     *
     * @param id the request ID
     * @param error the error object (required)
     */
    public JSONRPCErrorResponse(Object id, JSONRPCError error) {
        this(null, id, null, error);
    }

    /**
     * Constructs a JSON-RPC error response without a request ID.
     * <p>
     * Use this constructor when the request ID could not be determined,
     * typically for parse errors or malformed requests.
     *
     * @param error the error object (required)
     */
    public JSONRPCErrorResponse(JSONRPCError error) {
        this(null, null, null, error);
    }
}
