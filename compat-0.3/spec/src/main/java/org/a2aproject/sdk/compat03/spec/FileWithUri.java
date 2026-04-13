package org.a2aproject.sdk.compat03.spec;

/**
 * Represents a file with its content located at a specific URI.
 */
public record FileWithUri(String mimeType, String name, String uri) implements FileContent {
}

