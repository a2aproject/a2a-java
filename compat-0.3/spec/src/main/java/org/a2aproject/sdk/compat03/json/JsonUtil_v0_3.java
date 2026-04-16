package org.a2aproject.sdk.compat03.json;

import static org.a2aproject.sdk.compat03.spec.A2AErrorCodes_v0_3.CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE;
import static org.a2aproject.sdk.compat03.spec.A2AErrorCodes_v0_3.INTERNAL_ERROR_CODE;
import static org.a2aproject.sdk.compat03.spec.A2AErrorCodes_v0_3.INVALID_AGENT_RESPONSE_ERROR_CODE;
import static org.a2aproject.sdk.compat03.spec.A2AErrorCodes_v0_3.INVALID_PARAMS_ERROR_CODE;
import static org.a2aproject.sdk.compat03.spec.A2AErrorCodes_v0_3.INVALID_REQUEST_ERROR_CODE;
import static org.a2aproject.sdk.compat03.spec.A2AErrorCodes_v0_3.JSON_PARSE_ERROR_CODE;
import static org.a2aproject.sdk.compat03.spec.A2AErrorCodes_v0_3.METHOD_NOT_FOUND_ERROR_CODE;
import static org.a2aproject.sdk.compat03.spec.A2AErrorCodes_v0_3.PUSH_NOTIFICATION_NOT_SUPPORTED_ERROR_CODE;
import static org.a2aproject.sdk.compat03.spec.A2AErrorCodes_v0_3.TASK_NOT_CANCELABLE_ERROR_CODE;
import static org.a2aproject.sdk.compat03.spec.A2AErrorCodes_v0_3.TASK_NOT_FOUND_ERROR_CODE;
import static org.a2aproject.sdk.compat03.spec.A2AErrorCodes_v0_3.UNSUPPORTED_OPERATION_ERROR_CODE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ToNumberPolicy;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.a2aproject.sdk.compat03.spec.APIKeySecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.ContentTypeNotSupportedError_v0_3;
import org.a2aproject.sdk.compat03.spec.DataPart_v0_3;
import org.a2aproject.sdk.compat03.spec.FileContent_v0_3;
import org.a2aproject.sdk.compat03.spec.FilePart_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithBytes_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithUri_v0_3;
import org.a2aproject.sdk.compat03.spec.HTTPAuthSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.InternalError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidAgentResponseError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONParseError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.MethodNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.MutualTLSSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.OAuth2SecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.OpenIdConnectSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError_v0_3;
import org.a2aproject.sdk.compat03.spec.SecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskNotCancelableError_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.a2aproject.sdk.compat03.spec.UnsupportedOperationError_v0_3;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.jspecify.annotations.Nullable;

import static org.a2aproject.sdk.compat03.json.JsonUtil_v0_3.JSONRPCErrorTypeAdapter.THROWABLE_MARKER_FIELD;

public class JsonUtil_v0_3 {

    private static GsonBuilder createBaseGsonBuilder() {
        return new GsonBuilder()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeTypeAdapter())
                // Register JSONRPCError hierarchy adapter for all error subclasses
                .registerTypeHierarchyAdapter(JSONRPCError_v0_3.class, new JSONRPCErrorTypeAdapter())
                // Register Throwable adapter for EXACT Throwable.class only (not subclasses)
                // This prevents it from being used for JSONRPCError which extends Throwable
                .registerTypeAdapter(Throwable.class, new ThrowableTypeAdapter())
                .registerTypeAdapter(TaskState_v0_3.class, new TaskStateTypeAdapter())
                .registerTypeAdapter(Message_v0_3.Role.class, new RoleTypeAdapter())
                .registerTypeAdapter(Part_v0_3.Kind.class, new PartKindTypeAdapter())
                .registerTypeHierarchyAdapter(FileContent_v0_3.class, new FileContentTypeAdapter())
                .registerTypeHierarchyAdapter(SecurityScheme_v0_3.class, new SecuritySchemeTypeAdapter())
                .registerTypeAdapter(void.class, new VoidTypeAdapter())
                .registerTypeAdapter(Void.class, new VoidTypeAdapter())
                .registerTypeAdapterFactory(new JSONRPCResponseTypeAdapterFactory());
    }

    /**
     * Pre-configured {@link Gson} instance for JSON operations.
     * <p>
     * This mapper is configured with strict parsing mode and all necessary custom TypeAdapters
     * for A2A Protocol types including polymorphic types, enums, and date/time types.
     * <p>
     * Used throughout the SDK for consistent JSON serialization and deserialization.
     */
    public static final Gson OBJECT_MAPPER = createBaseGsonBuilder()
            .registerTypeHierarchyAdapter(Part_v0_3.class, new PartTypeAdapter())
            .registerTypeHierarchyAdapter(StreamingEventKind_v0_3.class, new StreamingEventKindTypeAdapter())
            .registerTypeAdapter(EventKind_v0_3.class, new EventKindTypeAdapter())
            .create();

    public static <T> T fromJson(String json, Class<T> classOfT) throws JsonProcessingException_v0_3 {
        try {
            return OBJECT_MAPPER.fromJson(json, classOfT);
        } catch (JsonSyntaxException e) {
            throw new JsonProcessingException_v0_3("Failed to parse JSON", e);
        }
    }

    public static <T> T fromJson(String json, Type type) throws JsonProcessingException_v0_3 {
        try {
            return OBJECT_MAPPER.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            throw new JsonProcessingException_v0_3("Failed to parse JSON", e);
        }
    }

    /**
     * Serializes an object to a JSON string using Gson.
     * <p>
     * This method uses the pre-configured {@link #OBJECT_MAPPER} to produce
     * JSON representation of the provided object.
     *
     * @param data the object to serialize
     * @return JSON string representation of the object
     */
    public static String toJson(Object data) throws JsonProcessingException_v0_3 {
        try {
            return OBJECT_MAPPER.toJson(data);
        } catch (JsonSyntaxException e) {
            throw new JsonProcessingException_v0_3("Failed to generate JSON", e);
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link OffsetDateTime} to/from ISO-8601 format.
     * <p>
     * This adapter ensures that OffsetDateTime instances are serialized to ISO-8601 formatted strings
     * (e.g., "2023-10-01T12:00:00.234-05:00") and deserialized from the same format.
     * This is necessary because Gson cannot access private fields of java.time classes via reflection
     * in Java 17+ due to module system restrictions.
     * <p>
     * The adapter uses {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME} for both serialization and
     * deserialization, which ensures proper handling of timezone offsets.
     *
     * @see OffsetDateTime
     * @see DateTimeFormatter#ISO_OFFSET_DATE_TIME
     */
    static class OffsetDateTimeTypeAdapter extends TypeAdapter<OffsetDateTime> {

        @Override
        public void write(JsonWriter out, OffsetDateTime value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
        }

        @Override
        public @Nullable
        OffsetDateTime read(JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String dateTimeString = in.nextString();
            try {
                return OffsetDateTime.parse(dateTimeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new JsonSyntaxException("Failed to parse OffsetDateTime: " + dateTimeString, e);
            }
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link Throwable} and its subclasses.
     * <p>
     * This adapter avoids reflection into {@link Throwable}'s private fields, which is not allowed
     * in Java 17+ due to module system restrictions. Instead, it serializes Throwables as simple
     * objects containing only the type (fully qualified class name) and message.
     * <p>
     * <b>Serialization:</b> Converts a Throwable to a JSON object with:
     * <ul>
     * <li>"type": The fully qualified class name (e.g., "java.lang.IllegalArgumentException")</li>
     * <li>"message": The exception message</li>
     * </ul>
     * <p>
     * <b>Deserialization:</b> Reads the JSON and reconstructs the Throwable using reflection to find
     * a constructor that accepts a String message parameter. If no such constructor exists or if
     * instantiation fails, returns a generic {@link RuntimeException} with the message.
     *
     * @see Throwable
     */
    static class ThrowableTypeAdapter extends TypeAdapter<Throwable> {

        @Override
        public void write(JsonWriter out, Throwable value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            out.name("type").value(value.getClass().getName());
            out.name("message").value(value.getMessage());
            out.name(THROWABLE_MARKER_FIELD).value(true);
            out.endObject();
        }

        @Override
        public @Nullable
        Throwable read(JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            String type = null;
            String message = null;

            in.beginObject();
            while (in.hasNext()) {
                String fieldName = in.nextName();
                switch (fieldName) {
                    case "type" ->
                        type = in.nextString();
                    case "message" ->
                        message = in.nextString();
                    default ->
                        in.skipValue();
                }
            }
            in.endObject();

            // Try to reconstruct the Throwable
            if (type != null) {
                try {
                    Class<?> throwableClass = Class.forName(type);
                    if (Throwable.class.isAssignableFrom(throwableClass)) {
                        // Try to find a constructor that takes a String message
                        try {
                            var constructor = throwableClass.getConstructor(String.class);
                            return (Throwable) constructor.newInstance(message);
                        } catch (NoSuchMethodException e) {
                            // No String constructor, return a generic RuntimeException
                            return new RuntimeException(message);
                        }
                    }
                } catch (Exception e) {
                    // If we can't reconstruct the exact type, return a generic RuntimeException
                    return new RuntimeException(message);
                }
            }
            return new RuntimeException(message);
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link JSONRPCError_v0_3} and its subclasses.
     * <p>
     * This adapter handles polymorphic deserialization based on the error code, creating the
     * appropriate subclass instance.
     * <p>
     * The adapter maps error codes to their corresponding error classes:
     * <ul>
     * <li>-32700: {@link JSONParseError_v0_3}</li>
     * <li>-32600: {@link InvalidRequestError_v0_3}</li>
     * <li>-32601: {@link MethodNotFoundError_v0_3}</li>
     * <li>-32602: {@link InvalidParamsError_v0_3}</li>
     * <li>-32603: {@link InternalError}</li>
     * <li>-32001: {@link TaskNotFoundError_v0_3}</li>
     * <li>-32002: {@link TaskNotCancelableError_v0_3}</li>
     * <li>-32003: {@link PushNotificationNotSupportedError_v0_3}</li>
     * <li>-32004: {@link UnsupportedOperationError_v0_3}</li>
     * <li>-32005: {@link ContentTypeNotSupportedError_v0_3}</li>
     * <li>-32006: {@link InvalidAgentResponseError_v0_3}</li>
     * <li>Other codes: {@link JSONRPCError_v0_3}</li>
     * </ul>
     *
     * @see JSONRPCError_v0_3
     */
    static class JSONRPCErrorTypeAdapter extends TypeAdapter<JSONRPCError_v0_3> {

        private static final ThrowableTypeAdapter THROWABLE_ADAPTER = new ThrowableTypeAdapter();
        static final String THROWABLE_MARKER_FIELD = "__throwable";
        private static final String CODE_FIELD = "code";
        private static final String DATA_FIELD = "data";
        private static final String MESSAGE_FIELD = "message";
        private static final String TYPE_FIELD = "type";

        @Override
        public void write(JsonWriter out, JSONRPCError_v0_3 value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            out.name(CODE_FIELD).value(value.getCode());
            out.name(MESSAGE_FIELD).value(value.getMessage());
            if (value.getData() != null) {
                out.name(DATA_FIELD);
                // If data is a Throwable, use ThrowableTypeAdapter to avoid reflection issues
                if (value.getData() instanceof Throwable throwable) {
                    THROWABLE_ADAPTER.write(out, throwable);
                } else {
                    // Use Gson to serialize the data field for non-Throwable types
                    OBJECT_MAPPER.toJson(value.getData(), Object.class, out);
                }
            }
            out.endObject();
        }

        @Override
        public @Nullable
        JSONRPCError_v0_3 read(JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            Integer code = null;
            String message = null;
            Object data = null;

            in.beginObject();
            while (in.hasNext()) {
                String fieldName = in.nextName();
                switch (fieldName) {
                    case CODE_FIELD ->
                        code = in.nextInt();
                    case MESSAGE_FIELD ->
                        message = in.nextString();
                    case DATA_FIELD -> {
                        // Read data as a generic object (could be string, number, object, etc.)
                        data = readDataValue(in);
                    }
                    default ->
                        in.skipValue();
                }
            }
            in.endObject();

            // Create the appropriate subclass based on the error code
            return createErrorInstance(code, message, data);
        }

        /**
         * Reads the data field value, which can be of any JSON type.
         */
        private @Nullable
        Object readDataValue(JsonReader in) throws java.io.IOException {
            return switch (in.peek()) {
                case STRING ->
                    in.nextString();
                case NUMBER ->
                    in.nextDouble();
                case BOOLEAN ->
                    in.nextBoolean();
                case NULL -> {
                    in.nextNull();
                    yield null;
                }
                case BEGIN_OBJECT -> {
                    // Parse as JsonElement to check if it's a Throwable
                    com.google.gson.JsonElement element = com.google.gson.JsonParser.parseReader(in);
                    if (element.isJsonObject()) {
                        com.google.gson.JsonObject obj = element.getAsJsonObject();
                        // Check if it has the structure of a serialized Throwable (type + message)
                        if (obj.has(TYPE_FIELD) && obj.has(MESSAGE_FIELD) && obj.has(THROWABLE_MARKER_FIELD)) {
                            // Deserialize as Throwable using ThrowableTypeAdapter
                            yield THROWABLE_ADAPTER.read(new JsonReader(new StringReader(element.toString())));
                        }
                    }
                    // Otherwise, deserialize as generic object
                    yield OBJECT_MAPPER.fromJson(element, Object.class);
                }
                case BEGIN_ARRAY ->
                    // For arrays, read as raw JSON using Gson
                    OBJECT_MAPPER.fromJson(in, Object.class);
                default -> {
                    in.skipValue();
                    yield null;
                }
            };
        }

        /**
         * Creates the appropriate JSONRPCError subclass based on the error code.
         */
        private JSONRPCError_v0_3 createErrorInstance(@Nullable Integer code, @Nullable String message, @Nullable Object data) {
            if (code == null) {
                throw new JsonSyntaxException("JSONRPCError must have a code field");
            }

            return switch (code) {
                case JSON_PARSE_ERROR_CODE ->
                    new JSONParseError_v0_3(code, message, data);
                case INVALID_REQUEST_ERROR_CODE ->
                    new InvalidRequestError_v0_3(code, message, data);
                case METHOD_NOT_FOUND_ERROR_CODE ->
                    new MethodNotFoundError_v0_3(code, message, data);
                case INVALID_PARAMS_ERROR_CODE ->
                    new InvalidParamsError_v0_3(code, message, data);
                case INTERNAL_ERROR_CODE ->
                    new InternalError_v0_3(code, message, data);
                case TASK_NOT_FOUND_ERROR_CODE ->
                    new TaskNotFoundError_v0_3(code, message, data);
                case TASK_NOT_CANCELABLE_ERROR_CODE ->
                    new TaskNotCancelableError_v0_3(code, message, data);
                case PUSH_NOTIFICATION_NOT_SUPPORTED_ERROR_CODE ->
                    new PushNotificationNotSupportedError_v0_3(code, message, data);
                case UNSUPPORTED_OPERATION_ERROR_CODE ->
                    new UnsupportedOperationError_v0_3(code, message, data);
                case CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE ->
                    new ContentTypeNotSupportedError_v0_3(code, message, data);
                case INVALID_AGENT_RESPONSE_ERROR_CODE ->
                    new InvalidAgentResponseError_v0_3(code, message, data);
                default ->
                    new JSONRPCError_v0_3(code, message, data);
            };
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link TaskState_v0_3} enum.
     * <p>
     * This adapter ensures that TaskState enum values are serialized using their
     * wire format string representation (e.g., "completed", "working") rather than
     * the Java enum constant name (e.g., "COMPLETED", "WORKING").
     * <p>
     * For serialization, it uses {@link TaskState_v0_3#asString()} to get the wire format.
     * For deserialization, it uses {@link TaskState_v0_3#fromString(String)} to parse the
     * wire format back to the enum constant.
     *
     * @see TaskState_v0_3
     * @see TaskState_v0_3#asString()
     * @see TaskState_v0_3#fromString(String)
     */
    static class TaskStateTypeAdapter extends TypeAdapter<TaskState_v0_3> {

        @Override
        public void write(JsonWriter out, TaskState_v0_3 value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.asString());
            }
        }

        @Override
        public @Nullable
        TaskState_v0_3 read(JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String stateString = in.nextString();
            try {
                return TaskState_v0_3.fromString(stateString);
            } catch (IllegalArgumentException e) {
                throw new JsonSyntaxException("Invalid TaskState: " + stateString, e);
            }
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link Message_v0_3.Role} enum.
     * <p>
     * This adapter ensures that Message.Role enum values are serialized using their
     * wire format string representation (e.g., "user", "agent") rather than the Java
     * enum constant name (e.g., "USER", "AGENT").
     * <p>
     * For serialization, it uses {@link Message_v0_3.Role#asString()} to get the wire format.
     * For deserialization, it parses the string to the enum constant.
     *
     * @see Message_v0_3.Role
     * @see Message_v0_3.Role#asString()
     */
    static class RoleTypeAdapter extends TypeAdapter<Message_v0_3.Role> {

        @Override
        public void write(JsonWriter out, Message_v0_3.Role value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.asString());
            }
        }

        @Override
        public Message_v0_3.@Nullable Role read(JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String roleString = in.nextString();
            try {
                return switch (roleString) {
                    case "user" ->
                        Message_v0_3.Role.USER;
                    case "agent" ->
                        Message_v0_3.Role.AGENT;
                    default ->
                        throw new IllegalArgumentException("Invalid Role: " + roleString);
                };
            } catch (IllegalArgumentException e) {
                throw new JsonSyntaxException("Invalid Message.Role: " + roleString, e);
            }
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link Part_v0_3.Kind} enum.
     * <p>
     * This adapter ensures that Part.Kind enum values are serialized using their
     * wire format string representation (e.g., "text", "file", "data") rather than
     * the Java enum constant name (e.g., "TEXT", "FILE", "DATA").
     * <p>
     * For serialization, it uses {@link Part_v0_3.Kind#asString()} to get the wire format.
     * For deserialization, it parses the string to the enum constant.
     *
     * @see Part_v0_3.Kind
     * @see Part_v0_3.Kind#asString()
     */
    static class PartKindTypeAdapter extends TypeAdapter<Part_v0_3.Kind> {

        @Override
        public void write(JsonWriter out, Part_v0_3.Kind value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.asString());
            }
        }

        @Override
        public Part_v0_3.@Nullable Kind read(JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String kindString = in.nextString();
            try {
                return switch (kindString) {
                    case "text" ->
                        Part_v0_3.Kind.TEXT;
                    case "file" ->
                        Part_v0_3.Kind.FILE;
                    case "data" ->
                        Part_v0_3.Kind.DATA;
                    default ->
                        throw new IllegalArgumentException("Invalid Part.Kind: " + kindString);
                };
            } catch (IllegalArgumentException e) {
                throw new JsonSyntaxException("Invalid Part.Kind: " + kindString, e);
            }
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link Part_v0_3} and its subclasses.
     * <p>
     * This adapter handles polymorphic deserialization based on the "kind" field, creating the
     * appropriate subclass instance (TextPart, FilePart, or DataPart).
     * <p>
     * The adapter uses a two-pass approach: first reads the JSON as a tree to inspect the "kind"
     * field, then deserializes to the appropriate concrete type.
     *
     * @see Part_v0_3
     * @see TextPart_v0_3
     * @see FilePart_v0_3
     * @see DataPart_v0_3
     */
    static class PartTypeAdapter extends TypeAdapter<Part_v0_3<?>> {

        // Create separate Gson instance without the Part adapter to avoid recursion
        private final Gson delegateGson = createBaseGsonBuilder().create();

        @Override
        public void write(JsonWriter out, Part_v0_3<?> value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            // Delegate to Gson's default serialization for the concrete type
            if (value instanceof TextPart_v0_3 textPart) {
                delegateGson.toJson(textPart, TextPart_v0_3.class, out);
            } else if (value instanceof FilePart_v0_3 filePart) {
                delegateGson.toJson(filePart, FilePart_v0_3.class, out);
            } else if (value instanceof DataPart_v0_3 dataPart) {
                delegateGson.toJson(dataPart, DataPart_v0_3.class, out);
            } else {
                throw new JsonSyntaxException("Unknown Part subclass: " + value.getClass().getName());
            }
        }

        @Override
        public @Nullable
        Part_v0_3<?> read(JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            // Read the JSON as a tree so we can inspect the "kind" field
            com.google.gson.JsonElement jsonElement = com.google.gson.JsonParser.parseReader(in);
            if (!jsonElement.isJsonObject()) {
                throw new JsonSyntaxException("Part must be a JSON object");
            }

            com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();
            com.google.gson.JsonElement kindElement = jsonObject.get("kind");
            if (kindElement == null || !kindElement.isJsonPrimitive()) {
                throw new JsonSyntaxException("Part must have a 'kind' field");
            }

            String kind = kindElement.getAsString();
            // Use the delegate Gson to deserialize to the concrete type
            return switch (kind) {
                case "text" ->
                    delegateGson.fromJson(jsonElement, TextPart_v0_3.class);
                case "file" ->
                    delegateGson.fromJson(jsonElement, FilePart_v0_3.class);
                case "data" ->
                    delegateGson.fromJson(jsonElement, DataPart_v0_3.class);
                default ->
                    throw new JsonSyntaxException("Unknown Part kind: " + kind);
            };
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link EventKind_v0_3} and its implementations.
     * <p>
     * Discriminates based on the {@code "kind"} field:
     * <ul>
     * <li>{@code "task"} → {@link Task_v0_3}</li>
     * <li>{@code "message"} → {@link Message_v0_3}</li>
     * </ul>
     */
    static class EventKindTypeAdapter extends TypeAdapter<EventKind_v0_3> {

        private final Gson delegateGson = createBaseGsonBuilder()
                .registerTypeHierarchyAdapter(Part_v0_3.class, new PartTypeAdapter())
                .create();

        @Override
        public void write(JsonWriter out, EventKind_v0_3 value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            if (value instanceof Task_v0_3 task) {
                delegateGson.toJson(task, Task_v0_3.class, out);
            } else if (value instanceof Message_v0_3 message) {
                delegateGson.toJson(message, Message_v0_3.class, out);
            } else {
                throw new JsonSyntaxException("Unknown EventKind implementation: " + value.getClass().getName());
            }
        }

        @Override
        public @Nullable EventKind_v0_3 read(JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            com.google.gson.JsonElement jsonElement = com.google.gson.JsonParser.parseReader(in);
            if (!jsonElement.isJsonObject()) {
                throw new JsonSyntaxException("EventKind must be a JSON object");
            }

            com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();
            com.google.gson.JsonElement kindElement = jsonObject.get("kind");
            if (kindElement == null || !kindElement.isJsonPrimitive()) {
                throw new JsonSyntaxException("EventKind must have a 'kind' field");
            }

            String kind = kindElement.getAsString();
            return switch (kind) {
                case Task_v0_3.TASK -> delegateGson.fromJson(jsonElement, Task_v0_3.class);
                case Message_v0_3.MESSAGE -> delegateGson.fromJson(jsonElement, Message_v0_3.class);
                default -> throw new JsonSyntaxException("Unknown EventKind kind: " + kind);
            };
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link StreamingEventKind_v0_3} and its implementations.
     * <p>
     * This adapter handles polymorphic deserialization based on the "kind" field, creating the
     * appropriate implementation instance (Task, Message, TaskStatusUpdateEvent, or TaskArtifactUpdateEvent).
     * <p>
     * The adapter uses a two-pass approach: first reads the JSON as a tree to inspect the "kind"
     * field, then deserializes to the appropriate concrete type.
     *
     * @see StreamingEventKind_v0_3
     * @see Task_v0_3
     * @see Message_v0_3
     * @see TaskStatusUpdateEvent_v0_3
     * @see TaskArtifactUpdateEvent_v0_3
     */
    static class StreamingEventKindTypeAdapter extends TypeAdapter<StreamingEventKind_v0_3> {

        // Create separate Gson instance without the StreamingEventKind adapter to avoid recursion
        private final Gson delegateGson = createBaseGsonBuilder()
                .registerTypeHierarchyAdapter(Part_v0_3.class, new PartTypeAdapter())
                .create();

        @Override
        public void write(JsonWriter out, StreamingEventKind_v0_3 value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            // Delegate to Gson's default serialization for the concrete type
            if (value instanceof Task_v0_3 task) {
                delegateGson.toJson(task, Task_v0_3.class, out);
            } else if (value instanceof Message_v0_3 message) {
                delegateGson.toJson(message, Message_v0_3.class, out);
            } else if (value instanceof TaskStatusUpdateEvent_v0_3 event) {
                delegateGson.toJson(event, TaskStatusUpdateEvent_v0_3.class, out);
            } else if (value instanceof TaskArtifactUpdateEvent_v0_3 event) {
                delegateGson.toJson(event, TaskArtifactUpdateEvent_v0_3.class, out);
            } else {
                throw new JsonSyntaxException("Unknown StreamingEventKind implementation: " + value.getClass().getName());
            }
        }

        @Override
        public @Nullable
        StreamingEventKind_v0_3 read(JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            // Read the JSON as a tree so we can inspect the "kind" field
            com.google.gson.JsonElement jsonElement = com.google.gson.JsonParser.parseReader(in);
            if (!jsonElement.isJsonObject()) {
                throw new JsonSyntaxException("StreamingEventKind must be a JSON object");
            }

            com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();
            com.google.gson.JsonElement kindElement = jsonObject.get("kind");
            if (kindElement == null || !kindElement.isJsonPrimitive()) {
                throw new JsonSyntaxException("StreamingEventKind must have a 'kind' field");
            }

            String kind = kindElement.getAsString();
            // Use the delegate Gson to deserialize to the concrete type
            return switch (kind) {
                case "task" ->
                    delegateGson.fromJson(jsonElement, Task_v0_3.class);
                case "message" ->
                    delegateGson.fromJson(jsonElement, Message_v0_3.class);
                case "status-update" ->
                    delegateGson.fromJson(jsonElement, TaskStatusUpdateEvent_v0_3.class);
                case "artifact-update" ->
                    delegateGson.fromJson(jsonElement, TaskArtifactUpdateEvent_v0_3.class);
                default ->
                    throw new JsonSyntaxException("Unknown StreamingEventKind kind: " + kind);
            };
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link SecurityScheme_v0_3} and its implementations.
     * <p>
     * Discriminates based on the {@code "type"} field:
     * <ul>
     * <li>{@code "apiKey"} → {@link APIKeySecurityScheme_v0_3}</li>
     * <li>{@code "http"} → {@link HTTPAuthSecurityScheme_v0_3}</li>
     * <li>{@code "oauth2"} → {@link OAuth2SecurityScheme_v0_3}</li>
     * <li>{@code "openIdConnect"} → {@link OpenIdConnectSecurityScheme_v0_3}</li>
     * <li>{@code "mutualTLS"} → {@link MutualTLSSecurityScheme_v0_3}</li>
     * </ul>
     */
    static class SecuritySchemeTypeAdapter extends TypeAdapter<SecurityScheme_v0_3> {

        // Use a plain Gson to avoid circular initialization — SecurityScheme concrete types
        // contain only simple fields (Strings, OAuthFlows) that need no custom adapters.
        private final Gson delegateGson = new Gson();

        @Override
        public void write(JsonWriter out, SecurityScheme_v0_3 value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            if (value instanceof APIKeySecurityScheme_v0_3 v) {
                delegateGson.toJson(v, APIKeySecurityScheme_v0_3.class, out);
            } else if (value instanceof HTTPAuthSecurityScheme_v0_3 v) {
                delegateGson.toJson(v, HTTPAuthSecurityScheme_v0_3.class, out);
            } else if (value instanceof OAuth2SecurityScheme_v0_3 v) {
                delegateGson.toJson(v, OAuth2SecurityScheme_v0_3.class, out);
            } else if (value instanceof OpenIdConnectSecurityScheme_v0_3 v) {
                delegateGson.toJson(v, OpenIdConnectSecurityScheme_v0_3.class, out);
            } else if (value instanceof MutualTLSSecurityScheme_v0_3 v) {
                delegateGson.toJson(v, MutualTLSSecurityScheme_v0_3.class, out);
            } else {
                throw new JsonSyntaxException("Unknown SecurityScheme implementation: " + value.getClass().getName());
            }
        }

        @Override
        public @Nullable SecurityScheme_v0_3 read(JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            com.google.gson.JsonElement jsonElement = com.google.gson.JsonParser.parseReader(in);
            if (!jsonElement.isJsonObject()) {
                throw new JsonSyntaxException("SecurityScheme must be a JSON object");
            }

            com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();
            com.google.gson.JsonElement typeElement = jsonObject.get("type");
            if (typeElement == null || !typeElement.isJsonPrimitive()) {
                throw new JsonSyntaxException("SecurityScheme must have a 'type' field");
            }

            String type = typeElement.getAsString();
            return switch (type) {
                case APIKeySecurityScheme_v0_3.API_KEY ->
                    delegateGson.fromJson(jsonElement, APIKeySecurityScheme_v0_3.class);
                case HTTPAuthSecurityScheme_v0_3.HTTP ->
                    delegateGson.fromJson(jsonElement, HTTPAuthSecurityScheme_v0_3.class);
                case OAuth2SecurityScheme_v0_3.OAUTH2 ->
                    delegateGson.fromJson(jsonElement, OAuth2SecurityScheme_v0_3.class);
                case OpenIdConnectSecurityScheme_v0_3.OPENID_CONNECT ->
                    delegateGson.fromJson(jsonElement, OpenIdConnectSecurityScheme_v0_3.class);
                case MutualTLSSecurityScheme_v0_3.MUTUAL_TLS ->
                    delegateGson.fromJson(jsonElement, MutualTLSSecurityScheme_v0_3.class);
                default ->
                    throw new JsonSyntaxException("Unknown SecurityScheme type: " + type);
            };
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link FileContent_v0_3} and its implementations.
     * <p>
     * This adapter handles polymorphic deserialization for the sealed FileContent interface,
     * which permits two implementations:
     * <ul>
     * <li>{@link FileWithBytes_v0_3} - File content embedded as base64-encoded bytes</li>
     * <li>{@link FileWithUri_v0_3} - File content referenced by URI</li>
     * </ul>
     * <p>
     * The adapter distinguishes between the two types by checking for the presence of
     * "bytes" or "uri" fields in the JSON object.
     *
     * @see FileContent_v0_3
     * @see FileWithBytes_v0_3
     * @see FileWithUri_v0_3
     */
    static class FileContentTypeAdapter extends TypeAdapter<FileContent_v0_3> {

        // Create separate Gson instance without the FileContent adapter to avoid recursion
        private final Gson delegateGson = new Gson();

        @Override
        public void write(JsonWriter out, FileContent_v0_3 value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            // Delegate to Gson's default serialization for the concrete type
            if (value instanceof FileWithBytes_v0_3 fileWithBytes) {
                delegateGson.toJson(fileWithBytes, FileWithBytes_v0_3.class, out);
            } else if (value instanceof FileWithUri_v0_3 fileWithUri) {
                delegateGson.toJson(fileWithUri, FileWithUri_v0_3.class, out);
            } else {
                throw new JsonSyntaxException("Unknown FileContent implementation: " + value.getClass().getName());
            }
        }

        @Override
        public @Nullable
        FileContent_v0_3 read(JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            // Read the JSON as a tree to inspect the fields
            com.google.gson.JsonElement jsonElement = com.google.gson.JsonParser.parseReader(in);
            if (!jsonElement.isJsonObject()) {
                throw new JsonSyntaxException("FileContent must be a JSON object");
            }

            com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();

            // Distinguish between FileWithBytes and FileWithUri by checking for "bytes" or "uri" field
            if (jsonObject.has("bytes")) {
                return delegateGson.fromJson(jsonElement, FileWithBytes_v0_3.class);
            } else if (jsonObject.has("uri")) {
                return delegateGson.fromJson(jsonElement, FileWithUri_v0_3.class);
            } else {
                throw new JsonSyntaxException("FileContent must have either 'bytes' or 'uri' field");
            }
        }
    }

    static class VoidTypeAdapter extends TypeAdapter<Void> {


        @Override
        @SuppressWarnings("resource")
        public void write(final JsonWriter out, final Void value) throws java.io.IOException {
            out.nullValue();
        }

        @Override
        public @Nullable Void read(final JsonReader in) throws java.io.IOException {
            in.skipValue();
            return null;
        }

    }

    /**
     * Gson TypeAdapterFactory for serializing {@link JSONRPCResponse_v0_3} subclasses.
     * <p>
     * JSON-RPC 2.0 requires that:
     * <ul>
     * <li>{@code result} MUST be present (possibly null) on success, and MUST NOT be present on error</li>
     * <li>{@code error} MUST be present on error, and MUST NOT be present on success</li>
     * </ul>
     * Gson's default null-field-skipping behavior would omit {@code "result": null} for Void responses,
     * so this factory writes the fields explicitly to comply with the spec.
     */
    static class JSONRPCResponseTypeAdapterFactory implements TypeAdapterFactory {

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public @Nullable <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!JSONRPCResponse_v0_3.class.isAssignableFrom(type.getRawType())) {
                return null;
            }

            TypeAdapter<T> delegateAdapter = gson.getDelegateAdapter(this, type);
            TypeAdapter<JSONRPCError_v0_3> errorAdapter = gson.getAdapter(JSONRPCError_v0_3.class);

            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter out, T value) throws java.io.IOException {
                    if (value == null) {
                        out.nullValue();
                        return;
                    }

                    JSONRPCResponse_v0_3<?> response = (JSONRPCResponse_v0_3<?>) value;

                    out.beginObject();
                    out.name("jsonrpc").value(response.getJsonrpc());

                    Object id = response.getId();
                    out.name("id");
                    if (id == null) {
                        out.nullValue();
                    } else if (id instanceof Number n) {
                        out.value(n.longValue());
                    } else {
                        out.value(id.toString());
                    }

                    JSONRPCError_v0_3 error = response.getError();
                    if (error != null) {
                        out.name("error");
                        errorAdapter.write(out, error);
                    } else {
                        out.name("result");
                        Object result = response.getResult();
                        if (result == null) {
                            // JsonWriter.nullValue() skips both name+value when serializeNulls=false,
                            // so we must temporarily enable it to write "result":null as required
                            // by JSON-RPC 2.0.
                            boolean prev = out.getSerializeNulls();
                            out.setSerializeNulls(true);
                            out.nullValue();
                            out.setSerializeNulls(prev);
                        } else {
                            TypeAdapter resultAdapter = gson.getAdapter(result.getClass());
                            resultAdapter.write(out, result);
                        }
                    }

                    out.endObject();
                }

                @Override
                public T read(JsonReader in) throws java.io.IOException {
                    return delegateAdapter.read(in);
                }
            };
        }
    }

}
