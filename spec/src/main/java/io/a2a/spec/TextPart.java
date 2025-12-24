package io.a2a.spec;


import io.a2a.util.Assert;


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
 * @see Part
 * @see Message
 * @see Artifact
 */
public record TextPart(String text) implements Part<String> {

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
    public TextPart {
        Assert.checkNotNullParam("text", text);
    }
}
