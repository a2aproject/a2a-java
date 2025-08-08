package io.a2a.spec;

/**
 * A specialized JSON mapping exception for invalid parameters in JSON-RPC requests.
 * 
 * This exception extends {@link IdJsonMappingException} to provide specific handling
 * for parameter validation errors during JSON deserialization of JSON-RPC requests.
 * It carries the request ID to enable proper error correlation and response generation
 * according to the JSON-RPC 2.0 specification.
 * 
 * This exception is typically thrown when:
 * - Required parameters are missing from the request
 * - Parameter values cannot be deserialized to the expected types
 * - Parameter validation fails during JSON processing
 * 
 * @see IdJsonMappingException
 */
public class InvalidParamsJsonMappingException extends IdJsonMappingException {

    /**
     * Constructs an InvalidParamsJsonMappingException with a message and request ID.
     * 
     * @param msg the detail message explaining the parameter validation error
     * @param id the JSON-RPC request ID associated with this exception
     */
    public InvalidParamsJsonMappingException(String msg, Object id) {
        super(msg, id);
    }

    /**
     * Constructs an InvalidParamsJsonMappingException with a message, cause, and request ID.
     * 
     * @param msg the detail message explaining the parameter validation error
     * @param cause the underlying cause of the mapping failure
     * @param id the JSON-RPC request ID associated with this exception
     */
    public InvalidParamsJsonMappingException(String msg, Throwable cause, Object id) {
        super(msg, cause, id);
    }
}
