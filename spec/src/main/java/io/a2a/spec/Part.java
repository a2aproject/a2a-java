package io.a2a.spec;

/**
 * Base interface for content parts within {@link Message}s and {@link Artifact}s.
 * <p>
 * Parts represent the fundamental content units in the A2A Protocol, allowing multi-modal
 * communication through different content types. A Part can be:
 * <ul>
 *   <li>{@link TextPart} - Plain text content</li>
 *   <li>{@link FilePart} - File content (as bytes or URI reference)</li>
 *   <li>{@link DataPart} - Structured data (JSON objects)</li>
 * </ul>
 * <p>
 * Parts use polymorphic JSON serialization where the JSON member name itself acts as the
 * type discriminator (e.g., "text", "file", "data"). This aligns with Protocol Buffers'
 * oneof semantics and modern API design practices.
 * <p>
 * Use {@code instanceof} pattern matching to determine the concrete Part type at runtime:
 * <pre>{@code
 * if (part instanceof TextPart textPart) {
 *     String text = textPart.text();
 * } else if (part instanceof FilePart filePart) {
 *     FileContent file = filePart.file();
 * }
 * }</pre>
 *
 * @param <T> the type of content contained in this part
 * @see Message
 * @see Artifact
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public interface Part<T> {
    // No methods - use instanceof for type discrimination
}