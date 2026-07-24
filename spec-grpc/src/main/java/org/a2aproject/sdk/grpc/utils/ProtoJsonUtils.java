package org.a2aproject.sdk.grpc.utils;

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
}
