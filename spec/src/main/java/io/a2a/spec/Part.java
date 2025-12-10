package io.a2a.spec;

import static io.a2a.util.Utils.SPEC_VERSION_1_0;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Base class for content parts within {@link Message}s and {@link Artifact}s.
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
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "kind",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextPart.class, name = TextPart.TEXT),
        @JsonSubTypes.Type(value = FilePart.class, name = FilePart.FILE),
        @JsonSubTypes.Type(value = DataPart.class, name = DataPart.DATA)
})
public sealed interface Part<T> permits DataPart, FilePart, TextPart {
    /**
     * Enum defining the different types of content parts.
     */
    enum Kind {
        /**
         * Plain text content part.
         */
        TEXT(TextPart.TEXT),

        /**
         * File content part (bytes or URI).
         */
        FILE(FilePart.FILE),

        /**
         * Structured data content part (JSON).
         */
        DATA(DataPart.DATA);

        private final String kind;

        Kind(String kind) {
            this.kind = kind;
        }

        /**
         * Returns the string representation of the kind for JSON serialization.
         *
         * @return the kind as a string
         */
        @JsonValue
        public String asString() {
            return this.kind;
        }
    }

    /**
     * @deprecated Use {@link #metadata()} instead
     */
    @Deprecated(since = SPEC_VERSION_1_0)
    default Kind getKind() {
        return kind();
    }

    /**
     * Returns the kind of this part.
     *
     * @return the Part.Kind indicating the content type
     */
    Kind kind();

    /**
     * @deprecated Use {@link #metadata()} instead
     */
    @Deprecated(since = SPEC_VERSION_1_0)
    default Map<String, Object> getMetadata() {
        return metadata();
    }

    /**
     * Returns optional metadata associated with this part.
     *
     * @return map of metadata key-value pairs, or null if no metadata
     */
    Map<String, Object> metadata();
}