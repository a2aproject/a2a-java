package io.a2a.spec;

import io.a2a.json.JsonMappingException;

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
