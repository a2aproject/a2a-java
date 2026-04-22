package io.a2a.spec;

/**
 * Represents a file with its content located at a specific URI.
 *
 * @param mimeType the MIME type of the file content
 * @param name optional name of the file
 * @param uri the URI pointing to the file content
 */
public record FileWithUri(String mimeType, String name, String uri) implements FileContent {
}

