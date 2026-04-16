package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.compat03.json.JsonMappingException_v0_3;

public class IdJsonMappingException_v0_3 extends JsonMappingException_v0_3 {

    Object id;

    public IdJsonMappingException_v0_3(String msg, Object id) {
        super(msg);
        this.id = id;
    }

    public IdJsonMappingException_v0_3(String msg, Throwable cause, Object id) {
        super(msg, cause);
        this.id = id;
    }

    public Object getId() {
        return id;
    }
}
