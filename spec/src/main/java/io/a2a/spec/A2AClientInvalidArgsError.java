package io.a2a.spec;

/**
 * Client exception indicating invalid arguments provided to a client operation.
 * <p>
 * This exception is thrown when parameters passed to A2A client SDK methods fail
 * validation. This is distinct from {@link InvalidParamsError}, which represents
 * server-side parameter validation errors.
 * <p>
 * Common scenarios:
 * <ul>
 *   <li>Null values for required parameters</li>
 *   <li>Invalid parameter combinations</li>
 *   <li>Parameter values outside acceptable ranges</li>
 *   <li>Malformed URIs or identifiers</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>{@code
 * if (agentUrl == null || agentUrl.isEmpty()) {
 *     throw new A2AClientInvalidArgsError("agentUrl cannot be null or empty");
 * }
 * }</pre>
 *
 * @see A2AClientError for the base client error class
 * @see InvalidParamsError for server-side parameter errors
 */
public class A2AClientInvalidArgsError extends A2AClientError {

    public A2AClientInvalidArgsError() {
    }

    public A2AClientInvalidArgsError(String message) {
        super("Invalid arguments error: " + message);
    }

    public A2AClientInvalidArgsError(String message, Throwable cause) {
        super("Invalid arguments error: " + message, cause);
    }
}
