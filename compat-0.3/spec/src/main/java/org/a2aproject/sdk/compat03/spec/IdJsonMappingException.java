package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.compat03.json.JsonMappingException;

public class IdJsonMappingException extends JsonMappingException {

    Object id;

    public IdJsonMappingException(String msg, Object id) {
        super(msg);
        this.id = id;
    }

    public IdJsonMappingException(String msg, Throwable cause, Object id) {
        super(msg, cause);
        this.id = id;
    }

    public Object getId() {
        return id;
    }
}
