package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Abstract base class for all content parts in the A2A protocol.
 * A Part represents a unit of content that can be included in messages or artifacts.
 * Different types of parts can contain text, files, data, tool calls, or other content types.
 * 
 * @param <T> the type of content this part contains
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "kind",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextPart.class, name = "text"),
        @JsonSubTypes.Type(value = FilePart.class, name = "file"),
        @JsonSubTypes.Type(value = DataPart.class, name = "data")
})
public abstract class Part<T> {
    /**
     * Enumeration of different part kinds/types supported in the A2A protocol.
     */
    public enum Kind {
        /** Text content part */
        TEXT("text"),
        /** File content part */
        FILE("file"),
        /** Data content part */
        DATA("data");

        /** The string representation of this kind */
        private String kind;

        /**
         * Constructs a Kind with the specified string representation.
         *
         * @param kind the string representation of this kind
         */
        Kind(String kind) {
            this.kind = kind;
        }

        /**
         * Returns the string representation of this kind for JSON serialization.
         *
         * @return the string representation
         */
        @JsonValue
        public String asString() {
            return this.kind;
        }
    }

    /**
     * Returns the kind/type of this part.
     *
     * @return the part kind
     */
    public abstract Kind getKind();

    /**
     * Returns the metadata associated with this part.
     *
     * @return the metadata map
     */
    public abstract Map<String, Object> getMetadata();

}
