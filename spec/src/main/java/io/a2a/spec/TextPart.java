package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;

/**
 * A text content part that contains string content.
 * This is one of the most common part types used for textual communication
 * in the A2A protocol.
 * 
 * A fundamental text unit of an Artifact or Message.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextPart extends Part<String> {
    /**
     * The text content of this part.
     */
    private final String text;
    
    /**
     * Additional metadata associated with this text part.
     */
    private final Map<String, Object> metadata;
    
    /**
     * The kind of this part, always TEXT for TextPart instances.
     */
    private final Kind kind;

    /**
     * Constructs a new TextPart with the specified text content and no metadata.
     *
     * @param text the text content
     */
    public TextPart(String text) {
        this(text, null);
    }

    /**
     * Constructs a new TextPart with the specified text content and metadata.
     *
     * @param text the text content
     * @param metadata additional metadata for this text part
     */
    @JsonCreator
    public TextPart(@JsonProperty("text") String text, @JsonProperty("metadata") Map<String, Object> metadata) {
        Assert.checkNotNullParam("text", text);
        this.text = text;
        this.metadata = metadata;
        this.kind = Kind.TEXT;
    }

    /**
     * Returns the kind of this part.
     *
     * @return the part kind, always Kind.TEXT
     */
    @Override
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the text content of this part.
     *
     * @return the text content
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the metadata associated with this text part.
     *
     * @return the metadata map
     */
    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }
}