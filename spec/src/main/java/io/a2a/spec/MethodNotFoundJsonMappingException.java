package io.a2a.spec;

/**
 * Exception thrown during JSON deserialization when a requested method is not found.
 * This exception is specifically used in the context of JSON-RPC request deserialization
 * when the method field contains an unrecognized or unsupported method name.
 *
 * <p>This exception extends {@code IdJsonMappingException} to preserve the request ID
 * from the original JSON-RPC request, which is essential for proper error response
 * generation according to the JSON-RPC 2.0 specification.</p>
 *
 * <p>Common scenarios where this exception is thrown:</p>
 * <ul>
 *   <li>The method name in the JSON-RPC request is not recognized</li>
 *   <li>The method is not supported by the current deserializer</li>
 *   <li>The method name is malformed or invalid</li>
 * </ul>
 */
public class MethodNotFoundJsonMappingException extends IdJsonMappingException {

    /**
     * Constructs a new MethodNotFoundJsonMappingException with the specified message and request ID.
     *
     * @param msg the detail message explaining the method not found error
     * @param id the request ID from the original JSON-RPC request (can be null)
     */
    public MethodNotFoundJsonMappingException(String msg, Object id) {
        super(msg, id);
    }

    /**
     * Constructs a new MethodNotFoundJsonMappingException with the specified message, cause, and request ID.
     *
     * @param msg the detail message explaining the method not found error
     * @param cause the underlying cause of the exception (can be null)
     * @param id the request ID from the original JSON-RPC request (can be null)
     */
    public MethodNotFoundJsonMappingException(String msg, Throwable cause, Object id) {
        super(msg, cause, id);
    }
}
