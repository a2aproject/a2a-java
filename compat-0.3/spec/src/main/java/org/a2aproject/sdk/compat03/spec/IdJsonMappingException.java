package org.a2aproject.sdk.compat03.spec;

import com.fasterxml.jackson.databind.JsonMappingException;

public class IdJsonMappingException extends JsonMappingException {

    Object id;

    public IdJsonMappingException(String msg, Object id) {
        super(null, msg);
        this.id = id;
    }

    public IdJsonMappingException(String msg, Throwable cause, Object id) {
        super(null, msg, cause);
        this.id = id;
    }

    public Object getId() {
        return id;
    }
}
