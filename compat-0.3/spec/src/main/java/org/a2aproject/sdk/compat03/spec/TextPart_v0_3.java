package org.a2aproject.sdk.compat03.spec;

import java.util.Map;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Represents a text segment within a message or artifact.
 */
public record TextPart_v0_3(String text, Map<String, Object> metadata, Kind kind) implements Part_v0_3<String> {

    public static final String TEXT = "text";

    public TextPart_v0_3 (String text, @Nullable Map<String, Object> metadata, Kind kind){
        Assert.checkNotNullParam("text", text);
        if (kind != Kind.TEXT) {
            throw new IllegalArgumentException("Invalid TextPart kind: " + kind);
        }
        this.text = text;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        this.kind = kind;
    }

    public TextPart_v0_3(String text) {
        this(text, null, Kind.TEXT);
    }

    public TextPart_v0_3(String text, @Nullable Map<String, Object> metadata) {
        this(text, metadata, Kind.TEXT);
    }
}
