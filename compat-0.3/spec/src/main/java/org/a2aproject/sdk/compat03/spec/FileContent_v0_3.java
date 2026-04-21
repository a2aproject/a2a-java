package org.a2aproject.sdk.compat03.spec;

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
 *   <li>{@link FileWithBytes_v0_3} - File content embedded as base64-encoded bytes (for small files or inline data)</li>
 *   <li>{@link FileWithUri_v0_3} - File content referenced by URI (for large files or external resources)</li>
 * </ul>
 * <p>
 * Both implementations must provide:
 * <ul>
 *   <li>MIME type - Describes the file format (e.g., "image/png", "application/pdf")</li>
 *   <li>File name - The original or display name for the file</li>
 * </ul>
 *
 * @see FilePart_v0_3
 * @see FileWithBytes_v0_3
 * @see FileWithUri_v0_3
 */
public sealed interface FileContent_v0_3 permits FileWithBytes_v0_3, FileWithUri_v0_3 {

    String mimeType();

    String name();
}
