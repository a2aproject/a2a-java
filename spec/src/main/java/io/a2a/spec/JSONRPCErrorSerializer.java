package io.a2a.spec;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * Custom Jackson serializer for JSON-RPC error objects.
 * 
 * This serializer ensures that JSON-RPC errors are serialized according to the
 * JSON-RPC 2.0 specification format. It writes the error as a JSON object with
 * the required "code" and "message" fields, and an optional "data" field if present.
 * 
 * The serializer handles all subclasses of {@link JSONRPCError} uniformly,
 * ensuring consistent error format across the application.
 * 
 * @see JSONRPCError
 * @see StdSerializer
 * @see <a href="https://www.jsonrpc.org/specification#error_object">JSON-RPC 2.0 Error Object</a>
 */
public class JSONRPCErrorSerializer extends StdSerializer<JSONRPCError> {

    /**
     * Default constructor for JSONRPCErrorSerializer.
     */
    public JSONRPCErrorSerializer() {
        this(null);
    }

    /**
     * Constructor with type parameter.
     * 
     * @param t the class type for serialization
     */
    public JSONRPCErrorSerializer(Class<JSONRPCError> t) {
        super(t);
    }

    /**
     * Serializes a JSON-RPC error object to JSON format.
     * 
     * This method writes the error as a JSON object containing:
     * - "code": the numeric error code (required)
     * - "message": the error message string (required)
     * - "data": additional error data (optional, only included if not null)
     * 
     * The output format conforms to the JSON-RPC 2.0 specification for error objects.
     * 
     * @param value the JSONRPCError instance to serialize
     * @param gen the JSON generator to write to
     * @param provider the serializer provider
     * @throws IOException if an I/O error occurs during serialization
     */
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
