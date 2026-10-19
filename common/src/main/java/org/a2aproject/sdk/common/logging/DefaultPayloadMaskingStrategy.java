package org.a2aproject.sdk.common.logging;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Default masking strategy that redacts sensitive fields by key name.
 * <p>
 * The strategy parses the payload as JSON and recursively traverses the structure,
 * masking values whose keys contain sensitive substrings (case-insensitive).
 * If parsing fails, it falls back to regex-based masking for robustness.
 * </p>
 * <p>
 * Default sensitive keys (case-insensitive substring match):
 * <ul>
 *   <li>token, credentials, password, secret, apikey, api_key</li>
 *   <li>authorization, authentication, privatekey, private_key</li>
 *   <li>accesstoken, access_token, refreshtoken, refresh_token</li>
 *   <li>clientsecret, client_secret</li>
 *   <li>url (push notification URLs are sensitive; can be disabled via builder)</li>
 * </ul>
 * </p>
 */
public class DefaultPayloadMaskingStrategy implements PayloadMaskingStrategy {

    private static final String MASKED_VALUE = "***";

    /**
     * Default sensitive key substrings (lowercase).
     */
    public static final Set<String> DEFAULT_SENSITIVE_KEYS = Set.of(
            "token",
            "credentials",
            "password",
            "secret",
            "apikey",
            "api_key",
            "privatekey",
            "private_key",
            "authorization",
            "authentication",
            "accesstoken",
            "access_token",
            "refreshtoken",
            "refresh_token",
            "clientsecret",
            "client_secret",
            "url"
    );

    private final Set<String> sensitiveKeys;

    /**
     * Creates with default sensitive keys.
     */
    public DefaultPayloadMaskingStrategy() {
        this.sensitiveKeys = normalizeKeys(DEFAULT_SENSITIVE_KEYS);
    }

    /**
     * Creates with custom sensitive keys (case-insensitive substring match).
     *
     * @param sensitiveKeys set of sensitive key substrings (case-insensitive)
     */
    public DefaultPayloadMaskingStrategy(Set<String> sensitiveKeys) {
        if (sensitiveKeys == null || sensitiveKeys.isEmpty()) {
            this.sensitiveKeys = normalizeKeys(DEFAULT_SENSITIVE_KEYS);
        } else {
            this.sensitiveKeys = normalizeKeys(sensitiveKeys);
        }
    }

    private static Set<String> normalizeKeys(Set<String> keys) {
        Set<String> normalized = new HashSet<>();
        for (String k : keys) {
            if (k != null) {
                normalized.add(k.toLowerCase());
            }
        }
        return normalized;
    }

    /**
     * Returns the configured sensitive keys (lowercase).
     */
    public Set<String> getSensitiveKeys() {
        return Set.copyOf(sensitiveKeys);
    }

    @Override
    public String mask(String jsonPayload) {
        if (jsonPayload == null || jsonPayload.isBlank()) {
            return jsonPayload;
        }
        try {
            JsonElement element = JsonParser.parseString(jsonPayload);
            boolean masked = maskJsonElement(element, null);
            // Return original if nothing was masked to avoid unnecessary serialization cost?
            // But we still need to return serialized form if masked; to keep behavior consistent,
            // we serialize only if masking occurred or to normalize. For simplicity, serialize only if masked.
            if (masked) {
                return element.toString();
            }
            return jsonPayload;
        } catch (JsonSyntaxException e) {
            // Fallback to regex masking for non-JSON or malformed payloads
            return maskWithRegex(jsonPayload);
        } catch (Exception e) {
            // On any unexpected error, return original to avoid log loss
            return jsonPayload;
        }
    }

    /**
     * Recursively masks JsonElement if its key is sensitive.
     *
     * @param element current element
     * @param parentKey the key of this element in its parent object, may be null for root
     * @return true if any masking occurred in this subtree
     */
    private boolean maskJsonElement(JsonElement element, String parentKey) {
        if (element == null || element.isJsonNull()) {
            return false;
        }
        if (parentKey != null && isSensitiveKey(parentKey)) {
            // Mask the entire value regardless of type
            // For objects/arrays, we replace with masked string at parent level (handled below)
            // But if this is called recursively for child, parent already masked
            return false; // shouldn't happen because parent masks before recursing
        }

        boolean masked = false;
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            // Collect keys to avoid concurrent modification
            Set<String> keys = new HashSet<>(obj.keySet());
            for (String key : keys) {
                JsonElement child = obj.get(key);
                if (isSensitiveKey(key)) {
                    // Replace value with masked string
                    obj.addProperty(key, MASKED_VALUE);
                    masked = true;
                } else if (child != null && (child.isJsonObject() || child.isJsonArray())) {
                    boolean childMasked = maskJsonElement(child, key);
                    if (childMasked) {
                        masked = true;
                    }
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                JsonElement child = array.get(i);
                if (child != null && (child.isJsonObject() || child.isJsonArray())) {
                    boolean childMasked = maskJsonElement(child, null);
                    if (childMasked) {
                        masked = true;
                    }
                }
            }
        }
        return masked;
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        for (String sensitive : sensitiveKeys) {
            if (lower.contains(sensitive)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fallback regex masking for non-JSON payloads.
     * Masks patterns like "key": "value" where key is sensitive.
     */
    private String maskWithRegex(String payload) {
        String result = payload;
        for (String sensitive : sensitiveKeys) {
            // Regex: "sensitiveKey" optional spaces : optional spaces "value" or 'value' or bare token
            // We use case-insensitive matching for key
            // This is best-effort; avoid catastrophic backtracking
            String regex = "(\"(?i)" + escapeRegex(sensitive) + "[^\"]*\"\\s*:\\s*\")(\"[^\"]*\"|'[^']*'|[^,\\}\\]]+)";
            // Replace value part with "***" preserving quotes if present
            result = result.replaceAll(regex, "$1\"***\"");
        }
        return result;
    }

    private String escapeRegex(String input) {
        return input.replaceAll("([\\\\.*+?\\[^\\]$(){}=!<>|:\\-])", "\\\\$1");
    }
}
