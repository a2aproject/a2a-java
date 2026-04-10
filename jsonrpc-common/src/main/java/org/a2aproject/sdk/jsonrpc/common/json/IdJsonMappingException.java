package org.a2aproject.sdk.jsonrpc.common.json;

/**
 * JSON mapping exception that includes request ID for error tracking.
 */
public class IdJsonMappingException extends JsonMappingException {

    /**
     * The JSON-RPC request ID associated with this exception.
     */
    private Object id;

    /**
     * Constructs exception with message and ID.
     *
     * @param msg the error message
     * @param id the request ID
     */
    public IdJsonMappingException(String msg, Object id) {
        super(msg);
        this.id = id;
    }

    /**
     * Constructs exception with message, cause, and ID.
     *
     * @param msg the error message
     * @param cause the cause
     * @param id the request ID
     */
    public IdJsonMappingException(String msg, Throwable cause, Object id) {
        super(msg, cause);
        this.id = id;
    }

    /**
     * Gets the request ID associated with this exception.
     *
     * @return the request ID
     */
    public Object getId() {
        return id;
    }
}
