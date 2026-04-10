package org.a2aproject.sdk.compat03.spec;

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
 * } catch (org.a2aproject.sdk.compat03.json.JsonProcessingException e) {
 *     throw new A2AClientJSONError("Failed to parse agent card", e);
 * }
 * }</pre>
 *
 * @see A2AClientError_v0_3 for the base client error class
 * @see JSONParseError_v0_3 for protocol-level JSON errors
 */
public class A2AClientJSONError_v0_3 extends A2AClientError_v0_3 {

    public A2AClientJSONError_v0_3() {
    }

    public A2AClientJSONError_v0_3(String message) {
        super(message);
    }

    public A2AClientJSONError_v0_3(String message, Throwable cause) {
        super(message, cause);
    }
}
