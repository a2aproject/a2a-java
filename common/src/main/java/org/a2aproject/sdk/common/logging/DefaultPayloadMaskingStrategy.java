package org.a2aproject.sdk.common.logging;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class DefaultPayloadMaskingStrategy implements PayloadMaskingStrategy {

    private static final Set<String> DEFAULT_SENSITIVE_KEYS = Set.of(
            "token", "credentials", "password", "secret", "apikey", "api_key",
            "privatekey", "private_key", "authorization", "authentication",
            "accesstoken", "access_token", "refreshtoken", "refresh_token",
            "clientsecret", "client_secret", "url");

    private final Set<String> sensitiveKeysLower;

    public DefaultPayloadMaskingStrategy() { this(DEFAULT_SENSITIVE_KEYS); }

    public DefaultPayloadMaskingStrategy(Set<String> sensitiveKeys) {
        Set<String> lower = new HashSet<>();
        for (String k : sensitiveKeys) lower.add(k.toLowerCase(Locale.ROOT));
        this.sensitiveKeysLower = Set.copyOf(lower);
    }

    private boolean isSensitive(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        for (String s : sensitiveKeysLower) if (lower.contains(s)) return true;
        return false;
    }

    @Override
    public String mask(String payload) {
        if (payload == null || payload.isEmpty()) return payload;
        try {
            JsonElement el = JsonParser.parseString(payload);
            if (el.isJsonObject() || el.isJsonArray()) {
                maskElement(el);
                return el.toString();
            }
        } catch (JsonSyntaxException ignored) {}
        String result = payload;
        for (String key : sensitiveKeysLower) {
            Pattern p = Pattern.compile("(\"" + Pattern.quote(key) + "\"\\s*:\\s*\")([^\"]+)(\")", Pattern.CASE_INSENSITIVE);
            result = p.matcher(result).replaceAll("$1***$3");
        }
        return result;
    }

    private void maskElement(JsonElement el) {
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            for (String key : Set.copyOf(obj.keySet())) {
                JsonElement val = obj.get(key);
                if (isSensitive(key)) {
                    obj.addProperty(key, MASKED);
                } else {
                    maskElement(val);
                }
            }
        } else if (el.isJsonArray()) {
            for (JsonElement item : el.getAsJsonArray()) maskElement(item);
        }
    }
}
