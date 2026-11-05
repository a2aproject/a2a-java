package org.a2aproject.sdk.spec;


import org.a2aproject.sdk.util.Assert;
import org.a2aproject.sdk.spec.util.CollectionCopies;
import java.util.Map;
import org.jspecify.annotations.Nullable;


/**
 * Represents a plain text content part within a {@link Message} or {@link Artifact}.
 * <p>
 * TextPart is the most common part type, containing textual content such as user messages,
 * agent responses, descriptions, or any other human-readable text.
 * <p>
 * The text content is required and must be non-null. Optional metadata can provide additional
 * context about the text (such as language, encoding, or formatting hints).
 * <p>
 * Example usage:
 * <pre>{@code
 * TextPart greeting = new TextPart("Hello, how can I help you?");
 * TextPart withMetadata = new TextPart("Bonjour!", Map.of("language", "fr"));
 * }</pre>
 *
 * @param text the text content (required, must not be null)
 * @param metadata additional metadata for the part
 * @param mediaType the optional MIME type of the content
 * @see Part
 * @see Message
 * @see Artifact
 */
public record TextPart(String text, @Nullable Map<String, Object> metadata, @Nullable String mediaType) implements Part<String> {

    /**
     * The JSON member name discriminator for text parts: "text".
     * <p>
     * In protocol v1.0+, this constant defines the JSON member name used for serialization:
     * {@code { "text": "Hello, world!" }}
     */
    public static final String TEXT = "text";

    /**
     * Compact constructor with validation.
     *
     * @param text the text content (required, must not be null)
     * @throws IllegalArgumentException if text is null
     */
    public TextPart (String text, @Nullable Map<String, Object> metadata, @Nullable String mediaType) {
        Assert.checkNotNullParam("text", text);
        this.metadata = CollectionCopies.unmodifiableNullableShallowMap(metadata);
        this.text = text;
        this.mediaType = mediaType;
    }

    /** Creates a text part with metadata and no media type. */
    public TextPart(String text, @Nullable Map<String, Object> metadata) {
        this(text, metadata, null);
    }

    /**
     * Constructor.
     *
     * @param text the text content (required, must not be null)
     * @throws IllegalArgumentException if data is null
     */
    public TextPart (String text){
        this(text, null);
    }
}
