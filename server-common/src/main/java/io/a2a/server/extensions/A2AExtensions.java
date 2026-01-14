package io.a2a.server.extensions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.a2a.server.ServerCallContext;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentExtension;
import io.a2a.spec.ExtensionSupportRequiredError;
import org.jspecify.annotations.Nullable;

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

    /**
     * Validates that all required extensions declared in the AgentCard are requested by the client.
     *
     * @param agentCard the agent card containing extension declarations
     * @param context the server call context containing requested extensions
     * @throws ExtensionSupportRequiredError if a required extension is not requested
     */
    public static void validateRequiredExtensions(AgentCard agentCard, ServerCallContext context)
            throws ExtensionSupportRequiredError {
        if (agentCard.capabilities() == null || agentCard.capabilities().extensions() == null) {
            return;
        }

        for (AgentExtension extension : agentCard.capabilities().extensions()) {
            if (extension.required() && !context.isExtensionRequested(extension.uri())) {
                throw new ExtensionSupportRequiredError(
                    null,
                    "Required extension '" + extension.uri() + "' was not requested by the client",
                    null);
            }
        }
    }
}
