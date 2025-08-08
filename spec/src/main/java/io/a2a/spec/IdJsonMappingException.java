package io.a2a.spec;

import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * A specialized JsonMappingException that carries a JSON-RPC request ID.
 * This exception is used when JSON mapping errors occur during processing
 * of JSON-RPC requests, allowing the error response to include the original
 * request ID for proper correlation.
 * 
 * According to JSON-RPC 2.0 specification, error responses should include
 * the same ID as the request that caused the error, or null if the ID
 * could not be determined.
 */
public class IdJsonMappingException extends JsonMappingException {

    /** The JSON-RPC request ID associated with this mapping error */
    Object id;

    /**
     * Constructs a new IdJsonMappingException with the specified message and request ID.
     * 
     * @param msg the detail message explaining the mapping error
     * @param id the JSON-RPC request ID (can be String, Number, or null)
     */
    public IdJsonMappingException(String msg, Object id) {
        super(null, msg);
        this.id = id;
    }

    /**
     * Constructs a new IdJsonMappingException with the specified message, cause, and request ID.
     * 
     * @param msg the detail message explaining the mapping error
     * @param cause the underlying cause of the mapping error
     * @param id the JSON-RPC request ID (can be String, Number, or null)
     */
    public IdJsonMappingException(String msg, Throwable cause, Object id) {
        super(null, msg, cause);
        this.id = id;
    }

    /**
     * Returns the JSON-RPC request ID associated with this mapping error.
     * 
     * @return the request ID, or null if not available
     */
    public Object getId() {
        return id;
    }
}
