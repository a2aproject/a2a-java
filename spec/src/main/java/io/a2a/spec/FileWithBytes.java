package io.a2a.spec;

/**
 * Represents file content embedded directly as base64-encoded bytes.
 * <p>
 * FileWithBytes is used when file content needs to be transmitted inline with the message or
 * artifact, rather than requiring a separate download. This is appropriate for:
 * <ul>
 *   <li>Small files that fit comfortably in a JSON payload</li>
 *   <li>Generated content that doesn't exist as a standalone file</li>
 *   <li>Content that must be preserved exactly as created</li>
 *   <li>Scenarios where URI accessibility is uncertain</li>
 * </ul>
 * <p>
 * The bytes field contains the base64-encoded file content. Decoders should handle the base64
 * encoding/decoding transparently.
 * <p>
 * This class is immutable.
 *
 * @param mimeType the MIME type of the file (e.g., "image/png", "application/pdf") (required)
 * @param name the file name (e.g., "report.pdf", "diagram.png") (required)
 * @param bytes the base64-encoded file content (required)
 * @see FileContent
 * @see FilePart
 * @see FileWithUri
 */
public record FileWithBytes(String mimeType, String name, String bytes) implements FileContent {
}
