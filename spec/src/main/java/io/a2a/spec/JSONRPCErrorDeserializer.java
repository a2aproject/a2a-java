package io.a2a.spec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Custom Jackson deserializer for JSON-RPC error objects.
 * 
 * This deserializer maps JSON-RPC error codes to their corresponding specific error classes.
 * It reads the error code from the JSON and instantiates the appropriate error subclass
 * based on a predefined mapping. If no specific error class is found for the code,
 * it falls back to creating a generic {@link JSONRPCError} instance.
 * 
 * The deserializer supports both standard JSON-RPC 2.0 error codes and A2A-specific
 * extension error codes, ensuring proper type safety and error handling throughout
 * the application.
 * 
 * @see JSONRPCError
 * @see StdDeserializer
 */
public class JSONRPCErrorDeserializer extends StdDeserializer<JSONRPCError> {

    /** Map of error codes to their corresponding constructor functions */
    private static final Map<Integer, TriFunction<Integer, String, Object, JSONRPCError>> ERROR_MAP = new HashMap<>();

    // Static initialization block to populate the error code mapping
    static {
        // Standard JSON-RPC 2.0 error codes
        ERROR_MAP.put(JSONParseError.DEFAULT_CODE, JSONParseError::new);
        ERROR_MAP.put(InvalidRequestError.DEFAULT_CODE, InvalidRequestError::new);
        ERROR_MAP.put(MethodNotFoundError.DEFAULT_CODE, MethodNotFoundError::new);
        ERROR_MAP.put(InvalidParamsError.DEFAULT_CODE, InvalidParamsError::new);
        ERROR_MAP.put(InternalError.DEFAULT_CODE, InternalError::new);
        
        // A2A-specific extension error codes
        ERROR_MAP.put(PushNotificationNotSupportedError.DEFAULT_CODE, PushNotificationNotSupportedError::new);
        ERROR_MAP.put(UnsupportedOperationError.DEFAULT_CODE, UnsupportedOperationError::new);
        ERROR_MAP.put(ContentTypeNotSupportedError.DEFAULT_CODE, ContentTypeNotSupportedError::new);
        ERROR_MAP.put(InvalidAgentResponseError.DEFAULT_CODE, InvalidAgentResponseError::new);
        ERROR_MAP.put(TaskNotCancelableError.DEFAULT_CODE, TaskNotCancelableError::new);
        ERROR_MAP.put(TaskNotFoundError.DEFAULT_CODE, TaskNotFoundError::new);
    }

    /**
     * Default constructor for JSONRPCErrorDeserializer.
     */
    public JSONRPCErrorDeserializer() {
        this(null);
    }

    /**
     * Constructor with value class parameter.
     * 
     * @param vc the value class for deserialization
     */
    public JSONRPCErrorDeserializer(Class<?> vc) {
        super(vc);
    }

    /**
     * Deserializes a JSON-RPC error object from JSON.
     * 
     * This method reads the JSON representation of an error and creates the appropriate
     * error instance based on the error code. It extracts the code, message, and optional
     * data fields from the JSON and uses the error code to determine which specific
     * error class to instantiate.
     * 
     * @param jsonParser the JSON parser containing the error data
     * @param context the deserialization context
     * @return the deserialized JSONRPCError instance
     * @throws IOException if an I/O error occurs during parsing
     * @throws JsonProcessingException if JSON processing fails
     */
    @Override
    public JSONRPCError deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException, JsonProcessingException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        int code = node.get("code").asInt();
        String message = node.get("message").asText();
        JsonNode dataNode = node.get("data");
        Object data = dataNode != null ? jsonParser.getCodec().treeToValue(dataNode, Object.class) : null;
        TriFunction<Integer, String, Object, JSONRPCError> constructor = ERROR_MAP.get(code);
        if (constructor != null) {
            return constructor.apply(code, message, data);
        } else {
            return new JSONRPCError(code, message, data);
        }
    }

    /**
     * Functional interface for three-parameter constructor functions.
     * 
     * @param <A> the type of the first parameter
     * @param <B> the type of the second parameter
     * @param <C> the type of the third parameter
     * @param <R> the return type
     */
    @FunctionalInterface
    private interface TriFunction<A, B, C, R> {
        /**
         * Applies this function to the given arguments.
         * 
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         */
        R apply(A a, B b, C c);
    }
}
