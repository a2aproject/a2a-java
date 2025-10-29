package io.a2a.server.util;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.a2a.spec.Artifact;
import io.a2a.spec.DataPart;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;

/**
 * Utility functions for creating A2A Artifact objects.
 */
public final class ArtifactUtils {

    private ArtifactUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a new Artifact object.
     *
     * @param name The human-readable name of the artifact.
     * @param parts The list of {@code Part} objects forming the artifact's content.
     * @param description An optional description of the artifact.
     * @return A new {@code Artifact} object with a generated artifact_id.
     */
    public static Artifact newArtifact(String name, List<Part<?>> parts, String description) {
        return new Artifact(
            UUID.randomUUID().toString(),
            name,
            description,
            parts,
            null,
            null
        );
    }

    /**
     * Creates a new Artifact object with empty description.
     *
     * @param name The human-readable name of the artifact.
     * @param parts The list of {@code Part} objects forming the artifact's content.
     * @return A new {@code Artifact} object with a generated artifact_id.
     */
    public static Artifact newArtifact(String name, List<Part<?>> parts) {
        return newArtifact(name, parts, null);
    }

    /**
     * Creates a new Artifact object containing only a single TextPart.
     *
     * @param name The human-readable name of the artifact.
     * @param text The text content of the artifact.
     * @param description An optional description of the artifact.
     * @return A new {@code Artifact} object with a generated artifact_id.
     */
    public static Artifact newTextArtifact(String name, String text, String description) {
        return newArtifact(
            name,
            List.of(new TextPart(text)),
            description
        );
    }

    /**
     * Creates a new Artifact object containing only a single TextPart with empty description.
     *
     * @param name The human-readable name of the artifact.
     * @param text The text content of the artifact.
     * @return A new {@code Artifact} object with a generated artifact_id.
     */
    public static Artifact newTextArtifact(String name, String text) {
        return newTextArtifact(name, text, null);
    }

    /**
     * Creates a new Artifact object containing only a single DataPart.
     *
     * @param name The human-readable name of the artifact.
     * @param data The structured data content of the artifact.
     * @param description An optional description of the artifact.
     * @return A new {@code Artifact} object with a generated artifact_id.
     */
    public static Artifact newDataArtifact(String name, Map<String, Object> data, String description) {
        return newArtifact(
            name,
            List.of(new DataPart(data)),
            description
        );
    }

    /**
     * Creates a new Artifact object containing only a single DataPart with empty description.
     *
     * @param name The human-readable name of the artifact.
     * @param data The structured data content of the artifact.
     * @return A new {@code Artifact} object with a generated artifact_id.
     */
    public static Artifact newDataArtifact(String name, Map<String, Object> data) {
        return newDataArtifact(name, data, null);
    }
}
