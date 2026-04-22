package io.a2a.spec;

/**
 * Represents a file with its content provided directly as a base64-encoded string.
 *
 * @param mimeType the MIME type of the file content
 * @param name optional name of the file
 * @param bytes the base64-encoded file content
 */
public record FileWithBytes(String mimeType, String name, String bytes) implements FileContent {
}
