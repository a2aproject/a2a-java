package org.a2aproject.sdk.grpc.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import org.a2aproject.sdk.util.Assert;
import org.a2aproject.sdk.util.HtmlEscapeUtils;
import org.jspecify.annotations.Nullable;

/**
 * Protobuf JSON helpers.
 * <p>
 * {@link JsonFormat#printer()} delegates to Gson's {@link com.google.gson.stream.JsonWriter}
 * which HTML-escapes {@code <}, {@code >}, and {@code &} by default. {@link #toJson} removes
 * those escape sequences via {@link HtmlEscapeUtils#removeHtmlEscaping(String)}.
 * <p>
 * {@link #mergeIgnoringUnknownFields} parses JSON while ignoring unrecognized field names,
 * as A2A Section 5.7 asks, without accepting unrecognized values for recognized fields.
 */
public final class ProtoJsonUtils {

    /**
     * Well-known types with a special JSON mapping. Their JSON is not an object of their
     * declared fields, so it is passed to the parser unchanged.
     */
    private static final Set<String> SPECIAL_JSON_TYPES = Set.of(
            "google.protobuf.Any",
            "google.protobuf.Struct",
            "google.protobuf.Value",
            "google.protobuf.ListValue",
            "google.protobuf.Timestamp",
            "google.protobuf.Duration",
            "google.protobuf.FieldMask",
            "google.protobuf.DoubleValue",
            "google.protobuf.FloatValue",
            "google.protobuf.Int64Value",
            "google.protobuf.UInt64Value",
            "google.protobuf.Int32Value",
            "google.protobuf.UInt32Value",
            "google.protobuf.BoolValue",
            "google.protobuf.StringValue",
            "google.protobuf.BytesValue");

    private static final Map<Descriptor, Map<String, FieldDescriptor>> FIELDS_BY_JSON_KEY = new ConcurrentHashMap<>();

    private ProtoJsonUtils() {
    }

    /** Removes unknown top-level fields while preserving nested objects for strict parsing. */
    public static JsonElement retainKnownFields(JsonElement json, Descriptor descriptor) {
        if (!json.isJsonObject()) {
            return json;
        }
        JsonObject result = json.getAsJsonObject().deepCopy();
        result.keySet().removeIf(name -> descriptor.findFieldByName(name) == null
                && descriptor.getFields().stream().noneMatch(field -> field.getJsonName().equals(name)));
        return result;
    }

    /**
     * Serializes a protobuf message to JSON using the supplied printer,
     * then removes HTML escaping.
     *
     * @param printer the configured {@link JsonFormat.Printer} (callers choose options
     *                such as {@code alwaysPrintFieldsWithNoPresence()} or
     *                {@code omittingInsignificantWhitespace()})
     * @param proto   the protobuf message to serialize, or {@code null}
     * @return JSON string without HTML-escaped characters, or empty string if proto is null
     */
    public static String toJson(JsonFormat.Printer printer, @Nullable MessageOrBuilder proto) throws InvalidProtocolBufferException {
        Assert.checkNotNullParam("printer", printer);
        if (proto == null) {
            return "";
        }
        return HtmlEscapeUtils.removeHtmlEscaping(printer.print(proto));
    }

    /**
     * Removes empty {@code artifacts} arrays from the tasks in a ListTasks response while
     * preserving all other default-valued response fields.
     *
     * @param json a serialized ListTasks response
     * @return the response with empty task artifact arrays omitted
     */
    public static String omitEmptyTaskArtifacts(String json) {
        JsonObject response = JsonParser.parseString(json).getAsJsonObject();
        JsonElement tasksElement = response.get("tasks");
        if (tasksElement == null || !tasksElement.isJsonArray()) {
            return json;
        }
        JsonArray tasks = tasksElement.getAsJsonArray();
        for (JsonElement taskElement : tasks) {
            if (!taskElement.isJsonObject()) {
                continue;
            }
            JsonObject task = taskElement.getAsJsonObject();
            JsonElement artifacts = task.get("artifacts");
            if (artifacts != null && artifacts.isJsonArray() && artifacts.getAsJsonArray().isEmpty()) {
                task.remove("artifacts");
            }
        }
        return HtmlEscapeUtils.removeHtmlEscaping(response.toString());
    }

    /** Removes the unset task ID from a direct SendMessage response. */
    public static String omitEmptyMessageTaskId(String json) {
        JsonObject response = JsonParser.parseString(json).getAsJsonObject();
        JsonElement element = response.get("message");
        if (element != null && element.isJsonObject()) {
            JsonObject message = element.getAsJsonObject();
            JsonElement taskId = message.get("taskId");
            if (taskId != null && taskId.isJsonPrimitive() && taskId.getAsString().isEmpty()) {
                message.remove("taskId");
                return HtmlEscapeUtils.removeHtmlEscaping(response.toString());
            }
        }
        return json;
    }

    /**
     * Merges JSON into a builder, ignoring object keys that name no field of the target message.
     * <p>
     * Unlike {@link JsonFormat.Parser#ignoringUnknownFields()}, this keeps the strict parser's
     * checks on the values of recognized fields: an unrecognized enum value is still rejected
     * instead of being dropped. Unknown keys are removed at every level of nesting, using the
     * descriptors of the target message. Well-known types with a special JSON mapping, such as
     * {@code google.protobuf.Struct}, are passed through unchanged, since their JSON is not an
     * object of their declared fields.
     *
     * @param json    the JSON to parse
     * @param builder the builder to merge into
     * @throws InvalidProtocolBufferException if the JSON is malformed or a recognized field has an invalid value
     */
    public static void mergeIgnoringUnknownFields(String json, Message.Builder builder) throws InvalidProtocolBufferException {
        Assert.checkNotNullParam("json", json);
        Assert.checkNotNullParam("builder", builder);
        JsonElement element;
        try {
            element = JsonParser.parseString(json);
        } catch (JsonParseException e) {
            // Let the protobuf parser report malformed JSON the way it always has.
            JsonFormat.parser().merge(json, builder);
            return;
        }
        JsonFormat.parser().merge(removeUnknownFields(element, builder.getDescriptorForType()).toString(), builder);
    }

    /**
     * Returns a copy of {@code json} without the object keys that name no field of
     * {@code descriptor}, recursively. The input is not modified. A value that does not have
     * the shape the field expects is kept as it is, so that the protobuf parser reports it.
     *
     * @param json       the JSON value for a message of type {@code descriptor}
     * @param descriptor the message type
     * @return the JSON without unknown keys
     */
    public static JsonElement removeUnknownFields(JsonElement json, Descriptor descriptor) {
        Assert.checkNotNullParam("json", json);
        Assert.checkNotNullParam("descriptor", descriptor);
        if (!json.isJsonObject() || SPECIAL_JSON_TYPES.contains(descriptor.getFullName())) {
            return json;
        }
        Map<String, FieldDescriptor> fields = fieldsByJsonKey(descriptor);
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
            FieldDescriptor field = fields.get(entry.getKey());
            if (field != null) {
                result.add(entry.getKey(), removeUnknownFieldsFromValue(entry.getValue(), field));
            }
        }
        return result;
    }

    private static JsonElement removeUnknownFieldsFromValue(JsonElement value, FieldDescriptor field) {
        if (field.getJavaType() != FieldDescriptor.JavaType.MESSAGE) {
            return value;
        }
        if (field.isMapField()) {
            FieldDescriptor valueField = field.getMessageType().findFieldByName("value");
            if (!value.isJsonObject() || valueField == null
                    || valueField.getJavaType() != FieldDescriptor.JavaType.MESSAGE) {
                return value;
            }
            JsonObject result = new JsonObject();
            for (Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                result.add(entry.getKey(), removeUnknownFields(entry.getValue(), valueField.getMessageType()));
            }
            return result;
        }
        if (field.isRepeated()) {
            if (!value.isJsonArray()) {
                return value;
            }
            JsonArray result = new JsonArray();
            for (JsonElement element : value.getAsJsonArray()) {
                result.add(removeUnknownFields(element, field.getMessageType()));
            }
            return result;
        }
        return removeUnknownFields(value, field.getMessageType());
    }

    private static Map<String, FieldDescriptor> fieldsByJsonKey(Descriptor descriptor) {
        return FIELDS_BY_JSON_KEY.computeIfAbsent(descriptor, d -> {
            // JsonFormat accepts both the JSON name and the original proto name of a field.
            Map<String, FieldDescriptor> fields = new HashMap<>();
            for (FieldDescriptor field : d.getFields()) {
                fields.put(field.getJsonName(), field);
                fields.put(field.getName(), field);
            }
            return fields;
        });
    }
}
