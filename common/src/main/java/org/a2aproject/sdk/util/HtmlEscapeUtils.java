package org.a2aproject.sdk.util;

/**
 * Utilities for removing HTML escaping applied by Gson's {@code JsonWriter}
 * when {@code htmlSafe} is enabled (the default).
 */
public final class HtmlEscapeUtils {

    private HtmlEscapeUtils() {
    }

    /**
     * Removes HTML escaping applied by Gson's {@code JsonWriter} when
     * {@code htmlSafe} is enabled (the default). Restores literal
     * {@code <}, {@code >}, {@code &}, {@code =}, and {@code '}.
     * <p>
     * Gson also escapes U+2028 (line separator) and U+2029 (paragraph separator) in HTML-safe
     * mode, but those are left as-is because they are valid JSON encodings that preserve the
     * original characters without data corruption.
     *
     * @param json the JSON string potentially containing HTML-escaped sequences
     * @return the JSON string with literal characters restored
     */
    public static String removeHtmlEscaping(String json) {
        return json.replace("\\u003c", "<")
                .replace("\\u003e", ">")
                .replace("\\u0026", "&")
                .replace("\\u003d", "=")
                .replace("\\u0027", "'");
    }
}
