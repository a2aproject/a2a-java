package io.a2a.spec;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Sealed interface representing file content in the A2A Protocol.
 * <p>
 * FileContent provides a polymorphic abstraction for file data, allowing files to be
 * represented either as embedded binary content or as URI references. This flexibility
 * enables different strategies for file transmission based on size, security, and
 * accessibility requirements.
 * <p>
 * The sealed interface permits only two implementations:
 * <ul>
 *   <li>{@link FileWithBytes} - File content embedded as base64-encoded bytes (for small files or inline data)</li>
 *   <li>{@link FileWithUri} - File content referenced by URI (for large files or external resources)</li>
 * </ul>
 * <p>
 * Both implementations must provide:
 * <ul>
 *   <li>MIME type - Describes the file format (e.g., "image/png", "application/pdf")</li>
 *   <li>File name - The original or display name for the file</li>
 * </ul>
 *
 * @see FilePart
 * @see FileWithBytes
 * @see FileWithUri
 */
@JsonDeserialize(using = FileContentDeserializer.class)
public sealed interface FileContent permits FileWithBytes, FileWithUri {

    /**
     * Returns the MIME type of the file content.
     *
     * @return the MIME type (e.g., "image/png", "text/plain", "application/json")
     */
    String mimeType();

    /**
     * Returns the file name.
     *
     * @return the file name (e.g., "document.pdf", "image.jpg")
     */
    String name();
}
