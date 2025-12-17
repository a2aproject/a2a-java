package io.a2a.json;

import static io.a2a.spec.A2AErrorCodes.CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.INTERNAL_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.INVALID_AGENT_RESPONSE_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.INVALID_PARAMS_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.INVALID_REQUEST_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.JSON_PARSE_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.METHOD_NOT_FOUND_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.PUSH_NOTIFICATION_NOT_SUPPORTED_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.TASK_NOT_CANCELABLE_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.TASK_NOT_FOUND_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.UNSUPPORTED_OPERATION_ERROR_CODE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ToNumberPolicy;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.a2a.spec.ContentTypeNotSupportedError;
import io.a2a.spec.DataPart;
import io.a2a.spec.FileContent;
import io.a2a.spec.FilePart;
import io.a2a.spec.FileWithBytes;
import io.a2a.spec.FileWithUri;
import io.a2a.spec.InvalidAgentResponseError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.Part;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.jspecify.annotations.Nullable;

import static io.a2a.json.JsonUtil.JSONRPCErrorTypeAdapter.THROWABLE_MARKER_FIELD;

/**
 * Utility class for JSON operations.
 */
public class JsonUtil {

    private static GsonBuilder createBaseGsonBuilder() {
        return new GsonBuilder()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeTypeAdapter())
                .registerTypeHierarchyAdapter(JSONRPCError.class, new JSONRPCErrorTypeAdapter())
                .registerTypeAdapter(TaskState.class, new TaskStateTypeAdapter())
                .registerTypeAdapter(Message.Role.class, new RoleTypeAdapter())
                .registerTypeAdapter(Part.Kind.class, new PartKindTypeAdapter())
                .registerTypeHierarchyAdapter(FileContent.class, new FileContentTypeAdapter());
    }

    /**
     * Pre-configured {@link Gson} instance for JSON operations.
     * <p>
     * This mapper is configured with strict parsing mode and all necessary custom TypeAdapters
     * for A2A Protocol types including polymorphic types, enums, and date/time types.
     * <p>
     * Used throughout the SDK for consistent JSON serialization and deserialization.
     *
     * @see JsonUtil#createBaseGsonBuilder()
     */
    public static final Gson OBJECT_MAPPER = createBaseGsonBuilder()
            .registerTypeHierarchyAdapter(Part.class, new PartTypeAdapter())
            .registerTypeHierarchyAdapter(StreamingEventKind.class, new StreamingEventKindTypeAdapter())
            .create();

    /**
     * Deserializes JSON string to an object of the specified class.
     *
     * @param <T> the type of the object to deserialize to
     * @param json the JSON string to parse
     * @param classOfT the class of the object to deserialize to
     * @return the deserialized object
     * @throws JsonProcessingException if JSON parsing fails
     */
    public static <T> T fromJson(String json, Class<T> classOfT) throws JsonProcessingException {
        try {
            return OBJECT_MAPPER.fromJson(json, classOfT);
        } catch (JsonSyntaxException e) {
            throw new JsonProcessingException("Failed to parse JSON", e);
        }
    }

    /**
     * Deserializes JSON string to an object of the specified type.
     *
     * @param <T> the type of the object to deserialize to
     * @param json the JSON string to parse
     * @param type the type of the object to deserialize to (supports generics)
     * @return the deserialized object
     * @throws JsonProcessingException if JSON parsing fails
     */
    public static <T> T fromJson(String json, Type type) throws JsonProcessingException {
        try {
            return OBJECT_MAPPER.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            throw new JsonProcessingException("Failed to parse JSON", e);
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
     * @throws JsonProcessingException if conversion fails
     */
    public static String toJson(Object data) throws JsonProcessingException {
        try {
            return OBJECT_MAPPER.toJson(data);
        } catch (JsonSyntaxException e) {
            throw new JsonProcessingException("Failed to generate JSON", e);
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
     * Gson TypeAdapter for serializing and deserializing {@link JSONRPCError} and its subclasses.
     * <p>
     * This adapter handles polymorphic deserialization based on the error code, creating the
     * appropriate subclass instance.
     * <p>
     * The adapter maps error codes to their corresponding error classes:
     * <ul>
     * <li>-32700: {@link JSONParseError}</li>
     * <li>-32600: {@link InvalidRequestError}</li>
     * <li>-32601: {@link MethodNotFoundError}</li>
     * <li>-32602: {@link InvalidParamsError}</li>
     * <li>-32603: {@link InternalError}</li>
     * <li>-32001: {@link TaskNotFoundError}</li>
     * <li>-32002: {@link TaskNotCancelableError}</li>
     * <li>-32003: {@link PushNotificationNotSupportedError}</li>
     * <li>-32004: {@link UnsupportedOperationError}</li>
     * <li>-32005: {@link ContentTypeNotSupportedError}</li>
     * <li>-32006: {@link InvalidAgentResponseError}</li>
     * <li>Other codes: {@link JSONRPCError}</li>
     * </ul>
     *
     * @see JSONRPCError
     */
    static class JSONRPCErrorTypeAdapter extends TypeAdapter<JSONRPCError> {

        private static final ThrowableTypeAdapter THROWABLE_ADAPTER = new ThrowableTypeAdapter();
        static final String THROWABLE_MARKER_FIELD = "__throwable";
        private static final String CODE_FIELD = "code";
        private static final String DATA_FIELD = "data";
        private static final String MESSAGE_FIELD = "message";
        private static final String TYPE_FIELD = "type";

        @Override
        public void write(JsonWriter out, JSONRPCError value) throws java.io.IOException {
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
        JSONRPCError read(JsonReader in) throws java.io.IOException {
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
        private JSONRPCError createErrorInstance(@Nullable Integer code, @Nullable String message, @Nullable Object data) {
            if (code == null) {
                throw new JsonSyntaxException("JSONRPCError must have a code field");
            }

            return switch (code) {
                case JSON_PARSE_ERROR_CODE ->
                    new JSONParseError(code, message, data);
                case INVALID_REQUEST_ERROR_CODE ->
                    new InvalidRequestError(code, message, data);
                case METHOD_NOT_FOUND_ERROR_CODE ->
                    new MethodNotFoundError(code, message, data);
                case INVALID_PARAMS_ERROR_CODE ->
                    new InvalidParamsError(code, message, data);
                case INTERNAL_ERROR_CODE ->
                    new io.a2a.spec.InternalError(code, message, data);
                case TASK_NOT_FOUND_ERROR_CODE ->
                    new TaskNotFoundError(code, message, data);
                case TASK_NOT_CANCELABLE_ERROR_CODE ->
                    new TaskNotCancelableError(code, message, data);
                case PUSH_NOTIFICATION_NOT_SUPPORTED_ERROR_CODE ->
                    new PushNotificationNotSupportedError(code, message, data);
                case UNSUPPORTED_OPERATION_ERROR_CODE ->
                    new UnsupportedOperationError(code, message, data);
                case CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE ->
                    new ContentTypeNotSupportedError(code, message, data);
                case INVALID_AGENT_RESPONSE_ERROR_CODE ->
                    new InvalidAgentResponseError(code, message, data);
                default ->
                    new JSONRPCError(code, message, data);
            };
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link TaskState} enum.
     * <p>
     * This adapter ensures that TaskState enum values are serialized using their
     * wire format string representation (e.g., "completed", "working") rather than
     * the Java enum constant name (e.g., "COMPLETED", "WORKING").
     * <p>
     * For serialization, it uses {@link TaskState#asString()} to get the wire format.
     * For deserialization, it uses {@link TaskState#fromString(String)} to parse the
     * wire format back to the enum constant.
     *
     * @see TaskState
     * @see TaskState#asString()
     * @see TaskState#fromString(String)
     */
    static class TaskStateTypeAdapter extends TypeAdapter<TaskState> {

        @Override
        public void write(JsonWriter out, TaskState value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.asString());
            }
        }

        @Override
        public @Nullable
        TaskState read(JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String stateString = in.nextString();
            try {
                return TaskState.fromString(stateString);
            } catch (IllegalArgumentException e) {
                throw new JsonSyntaxException("Invalid TaskState: " + stateString, e);
            }
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link Message.Role} enum.
     * <p>
     * This adapter ensures that Message.Role enum values are serialized using their
     * wire format string representation (e.g., "user", "agent") rather than the Java
     * enum constant name (e.g., "USER", "AGENT").
     * <p>
     * For serialization, it uses {@link Message.Role#asString()} to get the wire format.
     * For deserialization, it parses the string to the enum constant.
     *
     * @see Message.Role
     * @see Message.Role#asString()
     */
    static class RoleTypeAdapter extends TypeAdapter<Message.Role> {

        @Override
        public void write(JsonWriter out, Message.Role value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.asString());
            }
        }

        @Override
        public Message.@Nullable Role read(JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String roleString = in.nextString();
            try {
                return switch (roleString) {
                    case "user" ->
                        Message.Role.USER;
                    case "agent" ->
                        Message.Role.AGENT;
                    default ->
                        throw new IllegalArgumentException("Invalid Role: " + roleString);
                };
            } catch (IllegalArgumentException e) {
                throw new JsonSyntaxException("Invalid Message.Role: " + roleString, e);
            }
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link Part.Kind} enum.
     * <p>
     * This adapter ensures that Part.Kind enum values are serialized using their
     * wire format string representation (e.g., "text", "file", "data") rather than
     * the Java enum constant name (e.g., "TEXT", "FILE", "DATA").
     * <p>
     * For serialization, it uses {@link Part.Kind#asString()} to get the wire format.
     * For deserialization, it parses the string to the enum constant.
     *
     * @see Part.Kind
     * @see Part.Kind#asString()
     */
    static class PartKindTypeAdapter extends TypeAdapter<Part.Kind> {

        @Override
        public void write(JsonWriter out, Part.Kind value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.asString());
            }
        }

        @Override
        public Part.@Nullable Kind read(JsonReader in) throws java.io.IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String kindString = in.nextString();
            try {
                return switch (kindString) {
                    case "text" ->
                        Part.Kind.TEXT;
                    case "file" ->
                        Part.Kind.FILE;
                    case "data" ->
                        Part.Kind.DATA;
                    default ->
                        throw new IllegalArgumentException("Invalid Part.Kind: " + kindString);
                };
            } catch (IllegalArgumentException e) {
                throw new JsonSyntaxException("Invalid Part.Kind: " + kindString, e);
            }
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link Part} and its subclasses.
     * <p>
     * This adapter handles polymorphic deserialization based on the "kind" field, creating the
     * appropriate subclass instance (TextPart, FilePart, or DataPart).
     * <p>
     * The adapter uses a two-pass approach: first reads the JSON as a tree to inspect the "kind"
     * field, then deserializes to the appropriate concrete type.
     *
     * @see Part
     * @see TextPart
     * @see FilePart
     * @see DataPart
     */
    static class PartTypeAdapter extends TypeAdapter<Part<?>> {

        // Create separate Gson instance without the Part adapter to avoid recursion
        private final Gson delegateGson = createBaseGsonBuilder().create();

        @Override
        public void write(JsonWriter out, Part<?> value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            // Serialize the concrete type to a JsonElement
            com.google.gson.JsonElement jsonElement;
            if (value instanceof TextPart textPart) {
                jsonElement = delegateGson.toJsonTree(textPart, TextPart.class);
            } else if (value instanceof FilePart filePart) {
                jsonElement = delegateGson.toJsonTree(filePart, FilePart.class);
            } else if (value instanceof DataPart dataPart) {
                jsonElement = delegateGson.toJsonTree(dataPart, DataPart.class);
            } else {
                throw new JsonSyntaxException("Unknown Part subclass: " + value.getClass().getName());
            }


            // TODO temorary workaround to be fixed in https://github.com/a2aproject/a2a-java/issues/544
            // Add the "kind" field from getKind() method
            if (jsonElement.isJsonObject()) {
                jsonElement.getAsJsonObject().addProperty("kind", value.getKind().asString());
            }

            // Write the modified JSON
            delegateGson.toJson(jsonElement, out);
        }

        @Override
        public @Nullable
        Part<?> read(JsonReader in) throws java.io.IOException {
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
                    delegateGson.fromJson(jsonElement, TextPart.class);
                case "file" ->
                    delegateGson.fromJson(jsonElement, FilePart.class);
                case "data" ->
                    delegateGson.fromJson(jsonElement, DataPart.class);
                default ->
                    throw new JsonSyntaxException("Unknown Part kind: " + kind);
            };
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link StreamingEventKind} and its implementations.
     * <p>
     * This adapter handles polymorphic deserialization based on the "kind" field, creating the
     * appropriate implementation instance (Task, Message, TaskStatusUpdateEvent, or TaskArtifactUpdateEvent).
     * <p>
     * The adapter uses a two-pass approach: first reads the JSON as a tree to inspect the "kind"
     * field, then deserializes to the appropriate concrete type.
     *
     * @see StreamingEventKind
     * @see Task
     * @see Message
     * @see TaskStatusUpdateEvent
     * @see TaskArtifactUpdateEvent
     */
    static class StreamingEventKindTypeAdapter extends TypeAdapter<StreamingEventKind> {

        // Create separate Gson instance without the StreamingEventKind adapter to avoid recursion
        private final Gson delegateGson = createBaseGsonBuilder()
                .registerTypeHierarchyAdapter(Part.class, new PartTypeAdapter())
                .create();

        @Override
        public void write(JsonWriter out, StreamingEventKind value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            // Serialize the concrete type to a JsonElement
            com.google.gson.JsonElement jsonElement;
            if (value instanceof Task task) {
                jsonElement = delegateGson.toJsonTree(task, Task.class);
            } else if (value instanceof Message message) {
                jsonElement = delegateGson.toJsonTree(message, Message.class);
            } else if (value instanceof TaskStatusUpdateEvent event) {
                jsonElement = delegateGson.toJsonTree(event, TaskStatusUpdateEvent.class);
            } else if (value instanceof TaskArtifactUpdateEvent event) {
                jsonElement = delegateGson.toJsonTree(event, TaskArtifactUpdateEvent.class);
            } else {
                throw new JsonSyntaxException("Unknown StreamingEventKind implementation: " + value.getClass().getName());
            }

            // TODO temorary workaround to be fixed in https://github.com/a2aproject/a2a-java/issues/544
            // Add the "kind" field from getKind() method
            if (jsonElement.isJsonObject()) {
                jsonElement.getAsJsonObject().addProperty("kind", value.kind());
            }

            // Write the modified JSON
            delegateGson.toJson(jsonElement, out);
        }

        @Override
        public @Nullable
        StreamingEventKind read(JsonReader in) throws java.io.IOException {
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
                    delegateGson.fromJson(jsonElement, Task.class);
                case "message" ->
                    delegateGson.fromJson(jsonElement, Message.class);
                case "status-update" ->
                    delegateGson.fromJson(jsonElement, TaskStatusUpdateEvent.class);
                case "artifact-update" ->
                    delegateGson.fromJson(jsonElement, TaskArtifactUpdateEvent.class);
                default ->
                    throw new JsonSyntaxException("Unknown StreamingEventKind kind: " + kind);
            };
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link FileContent} and its implementations.
     * <p>
     * This adapter handles polymorphic deserialization for the sealed FileContent interface,
     * which permits two implementations:
     * <ul>
     * <li>{@link FileWithBytes} - File content embedded as base64-encoded bytes</li>
     * <li>{@link FileWithUri} - File content referenced by URI</li>
     * </ul>
     * <p>
     * The adapter distinguishes between the two types by checking for the presence of
     * "bytes" or "uri" fields in the JSON object.
     *
     * @see FileContent
     * @see FileWithBytes
     * @see FileWithUri
     */
    static class FileContentTypeAdapter extends TypeAdapter<FileContent> {

        // Create separate Gson instance without the FileContent adapter to avoid recursion
        private final Gson delegateGson = new Gson();

        @Override
        public void write(JsonWriter out, FileContent value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            // Delegate to Gson's default serialization for the concrete type
            if (value instanceof FileWithBytes fileWithBytes) {
                delegateGson.toJson(fileWithBytes, FileWithBytes.class, out);
            } else if (value instanceof FileWithUri fileWithUri) {
                delegateGson.toJson(fileWithUri, FileWithUri.class, out);
            } else {
                throw new JsonSyntaxException("Unknown FileContent implementation: " + value.getClass().getName());
            }
        }

        @Override
        public @Nullable
        FileContent read(JsonReader in) throws java.io.IOException {
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
                return delegateGson.fromJson(jsonElement, FileWithBytes.class);
            } else if (jsonObject.has("uri")) {
                return delegateGson.fromJson(jsonElement, FileWithUri.class);
            } else {
                throw new JsonSyntaxException("FileContent must have either 'bytes' or 'uri' field");
            }
        }
    }

}
