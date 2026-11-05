package org.a2aproject.sdk.util;

/**
 * Utilities for removing HTML escaping applied by Gson's {@code JsonWriter}
 * when {@code htmlSafe} is enabled (the default).
 */
public final class HtmlEscapeUtils {

    /** Length of a unicode escape sequence (backslash, {@code u}, four hex digits), in chars. */
    private static final int UNICODE_ESCAPE_LENGTH = 6;

    private HtmlEscapeUtils() {
    }

    /**
     * Removes HTML escaping applied by Gson's {@code JsonWriter} when
     * {@code htmlSafe} is enabled (the default). Restores literal
     * {@code <}, {@code >}, {@code &}, {@code =}, and {@code '}.
     * <p>
     * Lowercase and uppercase hex digits are both handled, as the JSON specification permits
     * either case for unicode escapes.
     * <p>
     * Only the six characters of an unescaped escape sequence are replaced, so a value that
     * already contains a backslash survives unchanged: an escaped backslash is copied as is, and
     * the text after it is not mistaken for an escape. Replacing the sequences blindly would
     * collapse the escaped backslash, leaving a backslash in front of the restored character,
     * which is not valid JSON.
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

        StringBuilder unescaped = new StringBuilder(json.length());
        int index = 0;
        while (index < json.length()) {
            char character = json.charAt(index);
            if (character != '\\' || index == json.length() - 1) {
                unescaped.append(character);
                index++;
                continue;
            }

            if (json.charAt(index + 1) == '\\') {
                // An escaped backslash is part of the value, not the start of an escape.
                unescaped.append('\\').append('\\');
                index += 2;
                continue;
            }

            int restored = unescapedHtmlCharacter(json, index);
            if (restored >= 0) {
                unescaped.append((char) restored);
                index += UNICODE_ESCAPE_LENGTH;
                continue;
            }

            unescaped.append(character);
            index++;
        }
        return unescaped.toString();
    }

    /**
     * Returns the character a unicode escape sequence stands for, when that sequence is one of
     * the HTML escapes removed by {@link #removeHtmlEscaping(String)}.
     *
     * @param json the JSON string
     * @param backslashIndex index of the backslash that may start the sequence
     * @return the restored character, or {@code -1} when the sequence is absent or is not an
     *         HTML escape
     */
    private static int unescapedHtmlCharacter(String json, int backslashIndex) {
        if (backslashIndex + UNICODE_ESCAPE_LENGTH > json.length()
                || json.charAt(backslashIndex + 1) != 'u') {
            return -1;
        }

        String code = json.substring(backslashIndex + 2, backslashIndex + UNICODE_ESCAPE_LENGTH);
        if (code.equalsIgnoreCase("003c")) {
            return '<';
        }
        if (code.equalsIgnoreCase("003e")) {
            return '>';
        }
        if (code.equalsIgnoreCase("0026")) {
            return '&';
        }
        if (code.equalsIgnoreCase("003d")) {
            return '=';
        }
        if (code.equalsIgnoreCase("0027")) {
            return '\'';
        }
        return -1;
    }
}
