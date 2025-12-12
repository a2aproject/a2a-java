package io.a2a.server.extensions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentExtension;

public class A2AExtensions {

    public static Set<String> getRequestedExtensions(List<String> values) {
        Set<String> extensions = new HashSet<>();
        if (values == null) {
            return extensions;
        }
        
        for (String value : values) {
            if (value != null) {
                // Split by comma and trim whitespace
                String[] parts = value.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        extensions.add(trimmed);
                    }
                }
            }
        }
        
        return extensions;
    }

    public static @Nullable AgentExtension findExtensionByUri(AgentCard card, String uri) {
        if (card.capabilities() == null || card.capabilities().extensions() == null) {
            return null;
        }
        for (AgentExtension extension : card.capabilities().extensions()) {
            if (extension.uri().equals(uri)) {
                return extension;
            }
        }
        return null;
    }
}
