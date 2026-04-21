package org.a2aproject.sdk.compat03.spec;

/**
 * Represents a file with its content provided directly as a base64-encoded string.
 */
public record FileWithBytes_v0_3(String mimeType, String name, String bytes) implements FileContent_v0_3 {
}
