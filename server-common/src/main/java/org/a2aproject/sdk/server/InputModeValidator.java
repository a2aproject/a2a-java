package org.a2aproject.sdk.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.ContentTypeNotSupportedError;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.FilePart;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.TextPart;
import org.jspecify.annotations.Nullable;

/** Validates declared media types in message parts against an agent card's input modes. */
public final class InputModeValidator {

    private InputModeValidator() {}

    /**
     * Rejects a declared part media type which is unsupported by every advertised skill.
     *
     * <p>An empty set of advertised modes means that the agent has not declared an input-mode
     * restriction. Parts without an explicit media type are not constrained by this check.
     * The aliases {@code text}, {@code audio}, {@code image}, and {@code video} match
     * {@code text/plain}, {@code audio/&#42;}, {@code image/&#42;}, and {@code video/&#42;}, respectively.
     *
     * @param message the incoming message
     * @param agentCard the agent card selected for this request
     * @throws ContentTypeNotSupportedError when a part declares an unsupported media type
     */
    public static void validate(Message message, AgentCard agentCard) {
        Set<String> inputModes = new HashSet<>(agentCard.defaultInputModes());
        agentCard.skills().forEach(skill -> {
            if (skill.inputModes() != null) {
                inputModes.addAll(skill.inputModes());
            }
        });
        if (inputModes.isEmpty()) {
            return;
        }

        List<MediaType> advertisedModes = new ArrayList<>();
        for (String mode : inputModes) {
            MediaType parsed = parseMediaType(mode, true);
            if (parsed != null) {
                advertisedModes.add(parsed);
            }
        }
        for (Part<?> part : message.parts()) {
            String mediaType = mediaType(part);
            if (mediaType != null && !mediaType.isEmpty()) {
                MediaType actual = parseMediaType(mediaType, false);
                if (actual == null || advertisedModes.stream().noneMatch(mode -> matches(mode, actual))) {
                    throw new ContentTypeNotSupportedError(null,
                            "Input media type is not supported: " + mediaType, null);
                }
            }
        }
    }

    private record MediaType(String type, Map<String, String> parameters) {}

    private static @Nullable MediaType parseMediaType(String value, boolean advertised) {
        String[] parts = value.split(";", 2);
        String type = parts[0].trim();
        if (advertised) {
            type = switch (type.toLowerCase(Locale.ROOT)) {
                case "text" -> "text/plain";
                case "audio", "image", "video" -> type + "/*";
                default -> type;
            };
        }
        Map<String, String> parsedParameters = parameters(parts.length == 2 ? parts[1] : "");
        return parsedParameters == null ? null : new MediaType(type, parsedParameters);
    }

    private static boolean matches(MediaType advertised, MediaType actual) {
        String pattern = advertised.type();
        String type = actual.type();
        boolean matchesType = pattern.equalsIgnoreCase(type)
                || (pattern.endsWith("/*") && type.regionMatches(true, 0, pattern, 0, pattern.length() - 1)
                        && type.length() > pattern.length() - 1)
                || pattern.equals("*/*");
        if (!matchesType) {
            return false;
        }
        for (Map.Entry<String, String> entry : advertised.parameters().entrySet()) {
            String value = actual.parameters().get(entry.getKey());
            if (value == null || !(entry.getKey().equals("charset")
                    ? entry.getValue().equalsIgnoreCase(value) : entry.getValue().equals(value))) {
                return false;
            }
        }
        return true;
    }

    /** Parses semicolon-separated MIME parameters, including quoted values and escapes. */
    private static @Nullable Map<String, String> parameters(String input) {
        Map<String, String> values = new HashMap<>();
        int index = 0;
        while (index < input.length()) {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
            int equals = input.indexOf('=', index);
            if (equals < 0) {
                return null;
            }
            String name = input.substring(index, equals).trim().toLowerCase(Locale.ROOT);
            if (name.isEmpty() || name.indexOf(';') >= 0) {
                return null;
            }
            index = equals + 1;
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
            String value;
            if (index < input.length() && input.charAt(index) == '"') {
                index++;
                StringBuilder quoted = new StringBuilder();
                boolean closed = false;
                while (index < input.length()) {
                    char c = input.charAt(index++);
                    if (c == '"') {
                        closed = true;
                        break;
                    }
                    if (c == '\\') {
                        if (index == input.length()) {
                            return null;
                        }
                        c = input.charAt(index++);
                    }
                    quoted.append(c);
                }
                if (!closed) {
                    return null;
                }
                value = quoted.toString();
                while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                    index++;
                }
                if (index < input.length() && input.charAt(index) != ';') {
                    return null;
                }
            } else {
                int end = input.indexOf(';', index);
                if (end < 0) {
                    end = input.length();
                }
                value = input.substring(index, end).trim();
                index = end;
                if (value.isEmpty()) {
                    return null;
                }
            }
            if (values.putIfAbsent(name, value) != null) {
                return null;
            }
            if (index < input.length()) {
                index++;
                if (index == input.length()) {
                    return null;
                }
            }
        }
        return values;
    }

    private static @Nullable String mediaType(Part<?> part) {
        if (part instanceof TextPart textPart) {
            return textPart.mediaType();
        }
        if (part instanceof DataPart dataPart) {
            return dataPart.mediaType();
        }
        if (part instanceof FilePart filePart) {
            return filePart.file().mimeType();
        }
        return null;
    }
}
