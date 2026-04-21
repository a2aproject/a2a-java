package org.a2aproject.sdk.compat03.spec;


import static org.a2aproject.sdk.compat03.util.Utils_v0_3.defaultIfNull;

/**
 * JSON-RPC error indicating that the server received invalid JSON that could not be parsed.
 * <p>
 * This error is returned when the request payload is not valid JSON, such as malformed syntax,
 * unexpected tokens, or encoding issues. This is distinct from {@link InvalidRequestError_v0_3},
 * which indicates structurally valid JSON that doesn't conform to the JSON-RPC specification.
 * <p>
 * Corresponds to JSON-RPC 2.0 error code {@code -32700}.
 * <p>
 * Usage example:
 * <pre>{@code
 * try {
 *     objectMapper.readValue(payload, JSONRPCRequest.class);
 * } catch (org.a2aproject.sdk.compat03.json.JsonProcessingException e) {
 *     throw new JSONParseError("Malformed JSON: " + e.getMessage());
 * }
 * }</pre>
 *
 * @see JSONRPCError_v0_3 for the base error class
 * @see A2AError_v0_3 for the error marker interface
 * @see InvalidRequestError_v0_3 for structurally valid but invalid requests
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Codes</a>
 */
public class JSONParseError_v0_3 extends JSONRPCError_v0_3 implements A2AError_v0_3 {

    public final static Integer DEFAULT_CODE = -32700;

    public JSONParseError_v0_3() {
        this(null, null, null);
    }

    public JSONParseError_v0_3(String message) {
        this(null, message, null);
    }

    public JSONParseError_v0_3(
            Integer code,
            String message,
            Object data) {
        super(
                defaultIfNull(code, DEFAULT_CODE),
                defaultIfNull(message, "Invalid JSON payload"),
                data);
    }
}
