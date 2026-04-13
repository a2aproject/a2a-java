package io.a2a.spec;

/**
 * Represents a file with its content provided directly as a base64-encoded string.
 */
public record FileWithBytes(String mimeType, String name, String bytes) implements FileContent {
}
