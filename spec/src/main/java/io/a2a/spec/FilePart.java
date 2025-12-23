package io.a2a.spec;


import io.a2a.util.Assert;


/**
 * Represents a file content part within a {@link Message} or {@link Artifact}.
 * <p>
 * FilePart contains file data that can be provided in two ways:
 * <ul>
 *   <li>{@link FileWithBytes} - File content embedded as base64-encoded bytes</li>
 *   <li>{@link FileWithUri} - File content referenced by URI</li>
 * </ul>
 * <p>
 * File parts are used to exchange binary data, documents, images, or any file-based content
 * between users and agents. The choice between bytes and URI depends on file size, accessibility,
 * and security requirements.
 * <p>
 * Example usage:
 * <pre>{@code
 * // File with embedded bytes
 * FilePart imageBytes = new FilePart(
 *     new FileWithBytes("image/png", "diagram.png", "iVBORw0KGgoAAAANS...")
 * );
 *
 * // File with URI reference
 * FilePart imageUri = new FilePart(
 *     new FileWithUri("image/png", "photo.png", "https://example.com/photo.png")
 * );
 * }</pre>
 *
 * @param file the file content (required, either FileWithBytes or FileWithUri)
 * @see Part
 * @see FileContent
 * @see FileWithBytes
 * @see FileWithUri
 */
public record FilePart(FileContent file) implements Part<FileContent> {

    /**
     * The JSON member name discriminator for file parts: "file".
     * <p>
     * In protocol v1.0+, this constant defines the JSON member name used for serialization:
     * {@code { "file": { "mediaType": "image/png", "name": "photo.png", ... } }}
     */
    public static final String FILE = "file";

    /**
     * Compact constructor with validation.
     *
     * @param file the file content (required, either FileWithBytes or FileWithUri)
     * @throws IllegalArgumentException if file is null
     */
    public FilePart {
        Assert.checkNotNullParam("file", file);
    }
}
