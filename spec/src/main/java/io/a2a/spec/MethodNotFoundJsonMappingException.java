package io.a2a.spec;

/**
 * Exception thrown when JSON mapping fails due to a method not found error.
 * <p>
 * This exception is used during JSON-RPC request deserialization when the requested
 * method cannot be found or mapped. It extends {@link IdJsonMappingException} to
 * preserve the request ID for error responses.
 *
 * @see IdJsonMappingException
 * @see MethodNotFoundError
 */
public class MethodNotFoundJsonMappingException extends IdJsonMappingException {

    /**
     * Constructs exception with message and request ID.
     *
     * @param msg the error message
     * @param id the JSON-RPC request ID
     */
    public MethodNotFoundJsonMappingException(String msg, Object id) {
        super(msg, id);
    }

    /**
     * Constructs exception with message, cause, and request ID.
     *
     * @param msg the error message
     * @param cause the underlying cause
     * @param id the JSON-RPC request ID
     */
    public MethodNotFoundJsonMappingException(String msg, Throwable cause, Object id) {
        super(msg, cause, id);
    }
}
