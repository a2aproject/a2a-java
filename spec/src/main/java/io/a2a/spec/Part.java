package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A fundamental unit with a Message or Artifact.
 * @param <T> the type of unit
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
public abstract class Part<T> {
    public enum Kind {
        TEXT("text"),
        FILE("file"),
        DATA("data");

        private String kind;

        Kind(String kind) {
            this.kind = kind;
        }

        @JsonValue
        public String asString() {
            return this.kind;
        }
    }

    public abstract Kind getKind();

    public abstract Map<String, Object> getMetadata();

}