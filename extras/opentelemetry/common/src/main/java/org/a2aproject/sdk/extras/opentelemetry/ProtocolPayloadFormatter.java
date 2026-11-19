package org.a2aproject.sdk.extras.opentelemetry;

import com.google.gson.Gson;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.a2aproject.sdk.spec.FilePart;
import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/** Builds a bounded, redacted projection without serializing the original object. */
public final class ProtocolPayloadFormatter {
    /** Default maximum UTF-8 size of a formatted payload. */
    public static final int DEFAULT_MAX_BYTES = 4096;

    /** Package prefix that contains the A2A spec record types eligible for reflective projection. */
    private static final String SPEC_PACKAGE = "org.a2aproject.sdk.spec";
    private static final String MARKER = " [truncated]";
    private static final String REDACTED = "[redacted]";
    private static final int MAX_DEPTH = 12;
    private static final int MAX_ELEMENTS = 100;
    private static final int MAX_VISITED = 1024;
    private static final Gson GSON = new Gson();
    private final int maxBytes;
    private final ProtocolPayloadSanitizer sanitizer;

    /** Uses the default 4 KiB limit. */
    public ProtocolPayloadFormatter(ProtocolPayloadSanitizer sanitizer) {
        this(DEFAULT_MAX_BYTES, sanitizer);
    }

    /**
     * Creates a formatter with a maximum UTF-8 output size.
     *
     * @param maxBytes maximum payload size including a truncation marker
     * @param sanitizer application-specific sanitizer applied in addition to built-in redaction
     * @throws IllegalArgumentException if {@code maxBytes} cannot contain the truncation marker or
     *         {@code sanitizer} is null
     */
    public ProtocolPayloadFormatter(int maxBytes, ProtocolPayloadSanitizer sanitizer) {
        if (maxBytes < utf8Length(MARKER)) {
            throw new IllegalArgumentException("maxBytes must fit the truncation marker");
        }
        this.maxBytes = maxBytes;
        this.sanitizer = Assert.checkNotNullParam("sanitizer", sanitizer);
    }

    /**
     * Formats a bounded, redacted projection of a payload.
     *
     * <p>A truncated result is plain text and is not necessarily valid JSON.
     *
     * @param payload payload to project
     * @return formatted payload and size metadata
     * @throws IllegalArgumentException if payload projection or sanitization fails
     */
    public Result format(Object payload) {
        Budget budget = new Budget(maxBytes);
        String json = GSON.toJson(project(payload, 0, budget));
        int size = json.getBytes(StandardCharsets.UTF_8).length;
        if (!budget.truncated && size <= maxBytes) {
            return new Result(json, false, size);
        }
        int limit = maxBytes - utf8Length(MARKER);
        int end = utf8PrefixEnd(json, limit);
        // The full projected size is unknown when traversal stopped early.
        return new Result(json.substring(0, end) + MARKER, true, budget.truncated ? -1 : size);
    }

    private @Nullable Object project(@Nullable Object value, int depth, Budget budget) {
        if (!budget.visit()) {
            return "[limit]";
        }
        if (value == null) {
            return null;
        }
        if (depth >= MAX_DEPTH) {
            return "[depth limit]";
        }
        if (value instanceof FilePart || value instanceof byte[]) {
            return "[omitted]";
        }
        if (value instanceof String text) {
            return sanitize(text, budget);
        }
        if (value instanceof Boolean
                || value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Float
                || value instanceof Double) {
            return value;
        }
        if (value instanceof Enum<?> e) {
            return e.name();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (budget.truncated) {
                    break;
                }
                if (++count > MAX_ELEMENTS) {
                    result.put("[limit]", "[omitted]");
                    break;
                }
                if (!(entry.getKey() instanceof String key)) {
                    continue;
                }
                String sanitizedKey = sanitize(key, budget);
                if (budget.truncated) {
                    break;
                }
                result.put(
                        sanitizedKey,
                        sensitive(key) ? REDACTED : project(entry.getValue(), depth + 1, budget));
            }
            return result;
        }
        if (value instanceof List<?> list) {
            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                if (budget.truncated) {
                    break;
                }
                if (result.size() == MAX_ELEMENTS) {
                    result.add("[limit]");
                    break;
                }
                result.add(project(item, depth + 1, budget));
            }
            return result;
        }
        Class<?> type = value.getClass();
        if (type.isRecord() && (type.getPackageName().equals(SPEC_PACKAGE)
                || type.getPackageName().startsWith(SPEC_PACKAGE + "."))) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (RecordComponent component : type.getRecordComponents()) {
                if (budget.truncated) {
                    break;
                }
                String name = component.getName();
                if (sensitive(name)) {
                    result.put(name, REDACTED);
                } else {
                    try {
                        result.put(
                                name,
                                project(component.getAccessor().invoke(value), depth + 1, budget));
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new IllegalArgumentException("Cannot project payload", e);
                    }
                }
            }
            return result;
        }
        return "[omitted]";
    }

    private static boolean sensitive(String name) {
        String key = name.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        return key.contains("token")
                || key.contains("secret")
                || key.contains("password")
                || key.contains("credential")
                || key.contains("authorization")
                || key.contains("apikey")
                || key.contains("cookie")
                || key.contains("privatekey")
                || key.equals("bytes")
                || key.endsWith("url")
                || key.endsWith("uri");
    }

    private String sanitize(String value, Budget budget) {
        int inputBytes = utf8Length(value);
        String input = value;
        if (inputBytes > budget.remaining()) {
            input = value.substring(0, utf8PrefixEnd(value, budget.remaining()));
            inputBytes = utf8Length(input);
            budget.markTruncated();
        }
        if (!budget.consume(inputBytes)) {
            return "";
        }
        String sanitized =
                Objects.requireNonNull(sanitizer.sanitize(input),
                        "ProtocolPayloadSanitizer.sanitize() must not return null")
                        .replaceAll("(?i)\\bBearer\\s+[^\\s,;]+", "Bearer [redacted]")
                        .replaceAll(
                                "(?i)([?&](?:access_?token|api_?key|token|key)=)[^&#\\s]*",
                                "$1[redacted]");
        int sanitizedBytes = utf8Length(sanitized);
        int extraBytes = Math.max(0, sanitizedBytes - inputBytes);
        if (extraBytes <= budget.remaining()) {
            budget.consume(extraBytes);
            return sanitized;
        }
        budget.markTruncated();
        return sanitized.substring(0, utf8PrefixEnd(sanitized, inputBytes + budget.remaining()));
    }

    private static int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private static int utf8PrefixEnd(String value, int maxBytes) {
        int end = 0;
        int bytes = 0;
        while (end < value.length()) {
            int codePoint = value.codePointAt(end);
            int width = utf8Length(new String(Character.toChars(codePoint)));
            if (bytes + width > maxBytes) {
                break;
            }
            bytes += width;
            end += Character.charCount(codePoint);
        }
        return end;
    }

    private static final class Budget {
        private int remaining;
        private int visited;
        private boolean truncated;

        private Budget(int maxBytes) {
            remaining = maxBytes;
        }

        private boolean visit() {
            if (++visited > MAX_VISITED || !consume(1)) {
                truncated = true;
                return false;
            }
            return true;
        }

        private boolean consume(int amount) {
            if (amount > remaining) {
                truncated = true;
                return false;
            }
            remaining -= amount;
            return true;
        }

        private int remaining() {
            return remaining;
        }

        private void markTruncated() {
            truncated = true;
        }
    }

    /** {@code projectedBytes} is -1 when traversal prevented computing the full projected size. */
    public record Result(String text, boolean truncated, int projectedBytes) {
        public Result {
            Assert.checkNotNullParam("text", text);
        }
    }
}
