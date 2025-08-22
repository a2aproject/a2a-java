package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;

/**
 * Represents a structured data segment (e.g., JSON) within a message or artifact.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataPart extends Part<Map<String, Object>> {

    private final Map<String, Object> data;
    private final Map<String, Object> metadata;
    private final Kind kind;

    public DataPart(Map<String, Object> data) {
        this(data, null);
    }

    @JsonCreator
    public DataPart(@JsonProperty("data") Map<String, Object> data,
                    @JsonProperty("metadata") Map<String, Object> metadata) {
        Assert.checkNotNullParam("data", data);
        this.data = data;
        this.metadata = metadata;
        this.kind = Kind.DATA;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

}
