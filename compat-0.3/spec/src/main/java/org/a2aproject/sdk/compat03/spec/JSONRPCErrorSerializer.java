package org.a2aproject.sdk.compat03.spec;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class JSONRPCErrorSerializer extends StdSerializer<JSONRPCError> {

    public JSONRPCErrorSerializer() {
        this(null);
    }

    public JSONRPCErrorSerializer(Class<JSONRPCError> vc) {
        super(vc);
    }

    @Override
    public void serialize(JSONRPCError value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeNumberField("code", value.getCode());
        gen.writeStringField("message", value.getMessage());
        if (value.getData() != null) {
            gen.writeObjectField("data", value.getData());
        }
        gen.writeEndObject();
    }
}
