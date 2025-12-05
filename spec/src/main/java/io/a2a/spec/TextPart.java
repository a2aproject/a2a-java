package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.a2a.util.Assert;

import static io.a2a.spec.TextPart.TEXT;
import static io.a2a.util.Utils.SPEC_VERSION_1_0;

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
@JsonTypeName(TEXT)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TextPart(@JsonProperty("text") String text,
                       @JsonProperty("metadata")Map<String, Object> metadata) implements Part<String> {

    public static final String TEXT = "text";

    @JsonCreator
    public TextPart {
        Assert.checkNotNullParam("text", text);
        metadata = (metadata != null) ? Map.copyOf(metadata) : null;
    }

    public TextPart(String text) {
        this(text, null);
    }

    @Override
    public Kind kind() {
        return Kind.TEXT;
    }

    /**
     * @deprecated Use {@link #text()} instead
     */
    @Deprecated(since = SPEC_VERSION_1_0)
    public String getText() {
        return text;
    }
}