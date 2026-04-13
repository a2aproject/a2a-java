package org.a2aproject.sdk.compat03.spec;

import java.util.Map;

import org.a2aproject.sdk.util.Assert;


/**
 * Represents a structured data segment (e.g., JSON) within a message or artifact.
 */
public class DataPart extends Part<Map<String, Object>> {

    public static final String DATA = "data";
    private final Map<String, Object> data;
    private final Map<String, Object> metadata;
    private final Kind kind;

    public DataPart(Map<String, Object> data) {
        this(data, null);
    }

    public DataPart(Map<String, Object> data, Map<String, Object> metadata) {
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
