package org.a2aproject.sdk.compat03.spec;

public class MethodNotFoundJsonMappingException extends IdJsonMappingException {

    public MethodNotFoundJsonMappingException(String msg, Object id) {
        super(msg, id);
    }

    public MethodNotFoundJsonMappingException(String msg, Throwable cause, Object id) {
        super(msg, cause, id);
    }
}
