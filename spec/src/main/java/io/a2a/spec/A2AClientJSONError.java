package io.a2a.spec;

/**
 * Client exception indicating a JSON serialization or deserialization error.
 * <p>
 * This exception is thrown when the A2A client SDK encounters errors while
 * parsing JSON responses from agents or serializing requests. This typically
 * indicates:
 * <ul>
 *   <li>Malformed JSON in agent responses</li>
 *   <li>Unexpected JSON structure or field types</li>
 *   <li>Missing required JSON fields</li>
 *   <li>JSON encoding/decoding errors</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>{@code
 * try {
 *     AgentCard card = objectMapper.readValue(json, AgentCard.class);
 * } catch (JsonProcessingException e) {
 *     throw new A2AClientJSONError("Failed to parse agent card", e);
 * }
 * }</pre>
 *
 * @see A2AClientError for the base client error class
 * @see JSONParseError for protocol-level JSON errors
 */
public class A2AClientJSONError extends A2AClientError {

    public A2AClientJSONError() {
    }

    public A2AClientJSONError(String message) {
        super(message);
    }

    public A2AClientJSONError(String message, Throwable cause) {
        super(message, cause);
    }
}
