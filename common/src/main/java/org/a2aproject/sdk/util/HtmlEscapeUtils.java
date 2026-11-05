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
     * Both lowercase ({@code <}) and uppercase ({@code <}) hex digits are handled,
     * as the JSON specification permits either case for unicode escapes.
     * Escaped backslashes are preserved, so literal Unicode escape text is not
     * mistaken for an encoded HTML character.
     * <p>
     * Gson also escapes U+2028 (line separator) and U+2029 (paragraph separator) in HTML-safe
     * mode, but those are left as-is because they are valid JSON encodings that preserve the
     * original characters without data corruption.
     *
     * @param json the JSON string potentially containing HTML-escaped sequences
     * @return the JSON string with literal characters restored
     */
    public static String removeHtmlEscaping(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        if (!json.contains("\\u00")) {
            return json;
        }
        StringBuilder result = new StringBuilder(json.length());
        for (int index = 0; index < json.length(); index++) {
            char current = json.charAt(index);
            if (current == '\\' && index + 1 < json.length()) {
                if (index + 5 < json.length() && json.startsWith("\\u00", index)) {
                    char replacement = switch (json.substring(index + 4, index + 6)) {
                        case "3c", "3C" -> '<';
                        case "3e", "3E" -> '>';
                        case "26" -> '&';
                        case "3d", "3D" -> '=';
                        case "27" -> '\'';
                        default -> 0;
                    };
                    if (replacement != 0) {
                        result.append(replacement);
                        index += 5;
                        continue;
                    }
                }
                // Consume each escape as a pair, including escaped backslashes.
                result.append(current).append(json.charAt(++index));
            } else {
                result.append(current);
            }
        }
        return result.toString();
    }
}
