package org.a2aproject.sdk.compat03.spec;

import java.util.Map;

import org.a2aproject.sdk.util.Assert;

import static org.a2aproject.sdk.compat03.spec.TextPart.TEXT;

/**
 * Represents a text segment within a message or artifact.
 */
public class TextPart extends Part<String> {

    public static final String TEXT = "text";
    private final String text;
    private final Map<String, Object> metadata;
    private final Kind kind;

    public TextPart(String text) {
        this(text, null);
    }

    public TextPart(String text, Map<String, Object> metadata) {
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