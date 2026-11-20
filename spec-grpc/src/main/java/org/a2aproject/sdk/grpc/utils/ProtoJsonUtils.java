package org.a2aproject.sdk.grpc.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import org.a2aproject.sdk.util.Assert;
import org.a2aproject.sdk.util.HtmlEscapeUtils;
import org.jspecify.annotations.Nullable;

/**
 * Protobuf-to-JSON serialization without HTML escaping.
 * <p>
 * {@link JsonFormat#printer()} delegates to Gson's {@link com.google.gson.stream.JsonWriter}
 * which HTML-escapes {@code <}, {@code >}, and {@code &} by default. This utility
 * removes those escape sequences via {@link HtmlEscapeUtils#removeHtmlEscaping(String)}.
 */
public final class ProtoJsonUtils {

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
}
