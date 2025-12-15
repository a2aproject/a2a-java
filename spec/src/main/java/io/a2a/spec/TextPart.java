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
 * @see Part
 * @see Message
 * @see Artifact
 */
public class TextPart extends Part<String> {

    public static final String TEXT = "text";
    private final String text;
    private final Kind kind;

    public TextPart(String text) {
        Assert.checkNotNullParam("text", text);
        this.text = text;
        this.kind = Kind.TEXT;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    public String getText() {
        return text;
    }
}