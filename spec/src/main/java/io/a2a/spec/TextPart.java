package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;

/**
 * Represents a text segment within a message or artifact.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextPart extends Part<String> {
    private final String text;
    private final Map<String, Object> metadata;
    private final Kind kind;

    public TextPart(String text) {
        this(text, null);
    }

    @JsonCreator
    public TextPart(@JsonProperty("text") String text, @JsonProperty("metadata") Map<String, Object> metadata) {
        Assert.checkNotNullParam("text", text);
        this.text = text;
        this.metadata = metadata;
        this.kind = Kind.TEXT;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    public String getText() {
        return text;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }
}