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
 * Parts use polymorphic JSON serialization with the "kind" discriminator property to
 * determine the concrete type during deserialization.
 * <p>
 * Each Part can include optional metadata for additional context about the content.
 *
 * @param <T> the type of content contained in this part
 * @see Message
 * @see Artifact
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public interface Part<T> {
    /**
     * Enum defining the different types of content parts.
     */
    enum Kind {
        /**
         * Plain text content part.
         */
        TEXT("text"),

        /**
         * File content part (bytes or URI).
         */
        FILE("file"),

        /**
         * Structured data content part (JSON).
         */
        DATA("data");

        private final String kind;

        Kind(String kind) {
            this.kind = kind;
        }

        /**
         * Returns the string representation of the kind for JSON serialization.
         *
         * @return the kind as a string
         */
        public String asString() {
            return this.kind;
        }
    }

    /**
     * Returns the kind of this part.
     *
     * @return the Part.Kind indicating the content type
     */
    Kind getKind();
}