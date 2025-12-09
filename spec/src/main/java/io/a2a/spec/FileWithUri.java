package io.a2a.spec;

/**
 * Represents file content referenced by a URI location.
 * <p>
 * FileWithUri is used when file content is stored externally and accessed via a URI reference.
 * This is appropriate for:
 * <ul>
 *   <li>Large files that would be impractical to embed in JSON</li>
 *   <li>Publicly accessible resources (HTTP/HTTPS URLs)</li>
 *   <li>Files stored in object storage (S3, Azure Blob, etc.)</li>
 *   <li>Content that may be accessed multiple times or by multiple clients</li>
 * </ul>
 * <p>
 * The URI should be accessible to the receiving party. Considerations include:
 * <ul>
 *   <li>Authentication requirements for private resources</li>
 *   <li>URI expiration for temporary access (signed URLs)</li>
 *   <li>Network accessibility and firewall rules</li>
 * </ul>
 * <p>
 * This class is immutable.
 *
 * @param mimeType the MIME type of the file (e.g., "image/png", "application/pdf") (required)
 * @param name the file name (e.g., "report.pdf", "photo.jpg") (required)
 * @param uri the URI where the file content can be accessed (e.g., "https://example.com/file.pdf") (required)
 * @see FileContent
 * @see FilePart
 * @see FileWithBytes
 */
public record FileWithUri(String mimeType, String name, String uri) implements FileContent {
}

