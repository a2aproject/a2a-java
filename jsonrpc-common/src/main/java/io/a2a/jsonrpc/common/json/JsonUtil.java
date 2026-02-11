package io.a2a.jsonrpc.common.json;

import static io.a2a.jsonrpc.common.json.JsonUtil.A2AErrorTypeAdapter.THROWABLE_MARKER_FIELD;
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
import static io.a2a.spec.DataPart.DATA;
import static io.a2a.spec.FilePart.FILE;
import static io.a2a.spec.TextPart.TEXT;
import static java.lang.String.format;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ToNumberPolicy;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import io.a2a.spec.A2AError;
import io.a2a.spec.APIKeySecurityScheme;
import io.a2a.spec.ContentTypeNotSupportedError;
import io.a2a.spec.DataPart;
import io.a2a.spec.FileContent;
import io.a2a.spec.FilePart;
import io.a2a.spec.FileWithBytes;
import io.a2a.spec.FileWithUri;
import io.a2a.spec.HTTPAuthSecurityScheme;
import io.a2a.spec.InvalidAgentResponseError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.Message;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.MutualTLSSecurityScheme;
import io.a2a.spec.OAuth2SecurityScheme;
import io.a2a.spec.OpenIdConnectSecurityScheme;
import io.a2a.spec.Part;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.SecurityScheme;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;

import org.jspecify.annotations.Nullable;

/**
 * Utility class for JSON operations.
 */
public class JsonUtil {

    private static GsonBuilder createBaseGsonBuilder() {
        return new GsonBuilder()
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeTypeAdapter())
                .registerTypeHierarchyAdapter(A2AError.class, new A2AErrorTypeAdapter())
                .registerTypeAdapter(TaskState.class, new TaskStateTypeAdapter())
                .registerTypeAdapter(Message.Role.class, new RoleTypeAdapter())
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
            .registerTypeHierarchyAdapter(SecurityScheme.class, new SecuritySchemeTypeAdapter())
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
     * Gson TypeAdapter for serializing and deserializing {@link A2AError} and its subclasses.
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
     * <li>Other codes: {@link A2AError}</li>
     * </ul>
     *
     * @see A2AError
     */
    static class A2AErrorTypeAdapter extends TypeAdapter<A2AError> {

        private static final ThrowableTypeAdapter THROWABLE_ADAPTER = new ThrowableTypeAdapter();
        static final String THROWABLE_MARKER_FIELD = "__throwable";
        private static final String CODE_FIELD = "code";
        private static final String DATA_FIELD = "data";
        private static final String MESSAGE_FIELD = "message";
        private static final String TYPE_FIELD = "type";

        @Override
        public void write(JsonWriter out, A2AError value) throws java.io.IOException {
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
        A2AError read(JsonReader in) throws java.io.IOException {
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
         * Creates the appropriate A2AError subclass based on the error code.
         */
        private A2AError createErrorInstance(@Nullable Integer code, @Nullable String message, @Nullable Object data) {
            if (code == null) {
                throw new JsonSyntaxException("A2AError must have a code field");
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
                    new A2AError(code, message == null ? "" : message, data);
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
     * Gson TypeAdapter for serializing and deserializing {@link Part} and its subclasses.
     * <p>
     * This adapter handles polymorphic deserialization, creating the
     * appropriate subclass instance (TextPart, FilePart, or DataPart) based on available fields.
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

        private static final Set<String> VALID_KEYS = Set.of(TEXT, FILE, DATA);

        // Create separate Gson instance without the Part adapter to avoid recursion
        private final Gson delegateGson = createBaseGsonBuilder().create();

        @Override
        public void write(JsonWriter out, Part<?> value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            // Write wrapper object with member name as discriminator
            out.beginObject();

            if (value instanceof TextPart textPart) {
                // TextPart: { "text": "value" } - direct string value
                out.name(TEXT);
                out.value(textPart.text());
            } else if (value instanceof FilePart filePart) {
                // FilePart: { "file": {...} }
                out.name(FILE);
                delegateGson.toJson(filePart.file(), FileContent.class, out);
            } else if (value instanceof DataPart dataPart) {
                // DataPart: { "data": <any JSON value> }
                out.name(DATA);
                delegateGson.toJson(dataPart.data(), Object.class, out);
            } else {
                throw new JsonSyntaxException("Unknown Part subclass: " + value.getClass().getName());
            }

            out.endObject();
        }

        @Override
        public @Nullable
        Part<?> read(JsonReader in) throws java.io.IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            // Read the JSON as a tree to inspect the member name discriminator
            com.google.gson.JsonElement jsonElement = com.google.gson.JsonParser.parseReader(in);
            if (!jsonElement.isJsonObject()) {
                throw new JsonSyntaxException("Part must be a JSON object");
            }

            com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();

            // Check for member name discriminators (v1.0 protocol)
            Set<String> keys = jsonObject.keySet();
            if (keys.size() != 1) {
                throw new JsonSyntaxException(format("Part object must have exactly one key, which must be one of: %s (found: %s)", VALID_KEYS, keys));
            }

            String discriminator = keys.iterator().next();

            return switch (discriminator) {
                case TEXT -> new TextPart(jsonObject.get(TEXT).getAsString());
                case FILE -> new FilePart(delegateGson.fromJson(jsonObject.get(FILE), FileContent.class));
                case DATA -> {
                    // DataPart supports any JSON value: object, array, primitive, or null
                    Object data = delegateGson.fromJson(
                            jsonObject.get(DATA),
                            Object.class
                    );
                    yield new DataPart(data);
                }
                default ->
                        throw new JsonSyntaxException(format("Part must have one of: %s (found: %s)", VALID_KEYS, discriminator));
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
            // Write wrapper object with member name as discriminator
            out.beginObject();
            out.name(value.kind());
            delegateGson.toJson(value, value.getClass(), out);
            out.endObject();
        }

        @Override
        public @Nullable
        StreamingEventKind read(JsonReader in) throws java.io.IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            // Read the JSON as a tree to inspect the member name discriminator
            com.google.gson.JsonElement jsonElement = com.google.gson.JsonParser.parseReader(in);
            if (!jsonElement.isJsonObject()) {
                throw new JsonSyntaxException("StreamingEventKind must be a JSON object");
            }

            com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();

            // Check for wrapped member name discriminators (v1.0 protocol - streaming format)
            if (jsonObject.has(Task.STREAMING_EVENT_ID)) {
                return delegateGson.fromJson(jsonObject.get(Task.STREAMING_EVENT_ID), Task.class);
            } else if (jsonObject.has(Message.STREAMING_EVENT_ID)) {
                return delegateGson.fromJson(jsonObject.get(Message.STREAMING_EVENT_ID), Message.class);
            } else if (jsonObject.has(TaskStatusUpdateEvent.STREAMING_EVENT_ID)) {
                return delegateGson.fromJson(
                        jsonObject.get(TaskStatusUpdateEvent.STREAMING_EVENT_ID), TaskStatusUpdateEvent.class);
            } else if (jsonObject.has(TaskArtifactUpdateEvent.STREAMING_EVENT_ID)) {
                return delegateGson.fromJson(
                        jsonObject.get(TaskArtifactUpdateEvent.STREAMING_EVENT_ID), TaskArtifactUpdateEvent.class);
            }

            // Check for unwrapped format (direct Task/Message deserialization)
            // Task objects have "id" and "contextId" fields
            // Message objects have "role" and "messageId" fields
            if (jsonObject.has("role") && jsonObject.has("messageId")) {
                // This is an unwrapped Message
                return delegateGson.fromJson(jsonObject, Message.class);
            } else if (jsonObject.has("id") && jsonObject.has("contextId")) {
                // This is an unwrapped Task
                return delegateGson.fromJson(jsonObject, Task.class);
            } else if (jsonObject.has("taskId") && jsonObject.has("status")) {
                // This is an unwrapped TaskStatusUpdateEvent
                return delegateGson.fromJson(jsonObject, TaskStatusUpdateEvent.class);
            } else if (jsonObject.has("taskId") && jsonObject.has("artifact")) {
                // This is an unwrapped TaskArtifactUpdateEvent
                return delegateGson.fromJson(jsonObject, TaskArtifactUpdateEvent.class);
            } else {
                throw new JsonSyntaxException("StreamingEventKind must have wrapper (task/message/statusUpdate/artifactUpdate) or recognizable unwrapped fields (found: " + jsonObject.keySet() + ")");
            }
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
        private final Gson delegateGson = new GsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeTypeAdapter())
                .create();

        @Override
        public void write(JsonWriter out, FileContent value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            // Delegate to Gson's default serialization for the concrete type
            delegateGson.toJson(value, value.getClass(), out);
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

    /**
     * Gson TypeAdapter for serializing and deserializing {@link APIKeySecurityScheme.Location} enum.
     * <p>
     * This adapter ensures that Location enum values are serialized using their
     * wire format string representation (e.g., "header") rather than
     * the Java enum constant name (e.g., "HEADER").
     * <p>
     * For serialization, it uses {@link APIKeySecurityScheme.Location#asString()} to get the wire format.
     * For deserialization, it uses {@link APIKeySecurityScheme.Location#fromString(String)} to parse the
     * wire format back to the enum constant.
     *
     * @see APIKeySecurityScheme.Location
     */
    static class APIKeyLocationTypeAdapter extends TypeAdapter<APIKeySecurityScheme.Location> {

        @Override
        public void write(JsonWriter out, APIKeySecurityScheme.Location value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.value(value.asString());
        }

        @Override
        public APIKeySecurityScheme.@Nullable Location read(JsonReader in) throws java.io.IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String locationString = in.nextString();
            try {
                return APIKeySecurityScheme.Location.fromString(locationString);
            } catch (IllegalArgumentException e) {
                throw new JsonSyntaxException("Invalid APIKeySecurityScheme.Location: " + locationString, e);
            }
        }
    }

    /**
     * Gson TypeAdapter for serializing and deserializing {@link SecurityScheme} and its implementations.
     * <p>
     * This adapter handles polymorphic deserialization for the sealed SecurityScheme interface,
     * which permits five implementations:
     * <ul>
     * <li>{@link APIKeySecurityScheme} - API key authentication</li>
     * <li>{@link HTTPAuthSecurityScheme} - HTTP authentication (basic or bearer)</li>
     * <li>{@link OAuth2SecurityScheme} - OAuth 2.0 flows</li>
     * <li>{@link OpenIdConnectSecurityScheme} - OpenID Connect discovery</li>
     * <li>{@link MutualTLSSecurityScheme} - Client certificate authentication</li>
     * </ul>
     * <p>
     * The adapter uses a wrapper object with the security scheme type as the discriminator field.
     * Each SecurityScheme is serialized as a JSON object with a single field whose name identifies
     * the security scheme type.
     * <p>
     * Serialization format examples:
     * <pre>{@code
     * // HTTPAuthSecurityScheme
     * {
     *   "httpAuthSecurityScheme": {
     *     "scheme": "bearer",
     *     "bearerFormat": "JWT",
     *     "description": "..."
     *   }
     * }
     *
     * // APIKeySecurityScheme
     * {
     *   "apiKeySecurityScheme": {
     *     "location": "header",
     *     "name": "X-API-Key",
     *     "description": "..."
     *   }
     * }
     * }</pre>
     *
     * @see SecurityScheme
     * @see APIKeySecurityScheme
     * @see HTTPAuthSecurityScheme
     * @see OAuth2SecurityScheme
     * @see OpenIdConnectSecurityScheme
     * @see MutualTLSSecurityScheme
     */
    static class SecuritySchemeTypeAdapter extends TypeAdapter<SecurityScheme> {

        private static final Set<String> VALID_KEYS = Set.of(APIKeySecurityScheme.TYPE,
                HTTPAuthSecurityScheme.TYPE,
                OAuth2SecurityScheme.TYPE,
                OpenIdConnectSecurityScheme.TYPE,
                MutualTLSSecurityScheme.TYPE);

        // Create separate Gson instance without the SecurityScheme adapter to avoid recursion
        // Register custom adapter for APIKeySecurityScheme.Location enum
        private final Gson delegateGson = createBaseGsonBuilder()
                .registerTypeAdapter(APIKeySecurityScheme.Location.class, new APIKeyLocationTypeAdapter())
                .create();

        @Override
        public void write(JsonWriter out, SecurityScheme value) throws java.io.IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            // Write wrapper object with member name as discriminator
            out.beginObject();
            out.name(value.type());
            delegateGson.toJson(value, value.getClass(), out);
            out.endObject();
        }

        @Override
        public @Nullable
        SecurityScheme read(JsonReader in) throws java.io.IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            // Read the JSON as a tree to inspect the member name discriminator
            com.google.gson.JsonElement jsonElement = com.google.gson.JsonParser.parseReader(in);
            if (!jsonElement.isJsonObject()) {
                throw new JsonSyntaxException("SecurityScheme must be a JSON object");
            }

            com.google.gson.JsonObject jsonObject = jsonElement.getAsJsonObject();

            // Check for member name discriminators
            Set<String> keys = jsonObject.keySet();
            if (keys.size() != 1) {
                throw new JsonSyntaxException(format("A SecurityScheme object must have exactly one key, which must be one of: %s (found: %s)", VALID_KEYS, keys));
            }

            String discriminator = keys.iterator().next();
            com.google.gson.JsonElement nestedObject = jsonObject.get(discriminator);

            return switch (discriminator) {
                case APIKeySecurityScheme.TYPE -> delegateGson.fromJson(nestedObject, APIKeySecurityScheme.class);
                case HTTPAuthSecurityScheme.TYPE -> delegateGson.fromJson(nestedObject, HTTPAuthSecurityScheme.class);
                case OAuth2SecurityScheme.TYPE -> delegateGson.fromJson(nestedObject, OAuth2SecurityScheme.class);
                case OpenIdConnectSecurityScheme.TYPE -> delegateGson.fromJson(nestedObject, OpenIdConnectSecurityScheme.class);
                case MutualTLSSecurityScheme.TYPE -> delegateGson.fromJson(nestedObject, MutualTLSSecurityScheme.class);
                default -> throw new JsonSyntaxException(format("Unknown SecurityScheme type. Must be one of: %s (found: %s)", VALID_KEYS, discriminator));
            };
        }
    }
}
