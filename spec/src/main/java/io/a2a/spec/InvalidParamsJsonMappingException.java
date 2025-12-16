package io.a2a.spec;

/**
 * Exception for invalid parameters during JSON mapping.
 */
public class InvalidParamsJsonMappingException extends IdJsonMappingException {

    /**
     * Constructs exception with message and ID.
     *
     * @param msg the error message
     * @param id the request ID
     */
    public InvalidParamsJsonMappingException(String msg, Object id) {
        super(msg, id);
    }

    /**
     * Constructs exception with message, cause, and ID.
     *
     * @param msg the error message
     * @param cause the cause
     * @param id the request ID
     */
    public InvalidParamsJsonMappingException(String msg, Throwable cause, Object id) {
        super(msg, cause, id);
    }
}
