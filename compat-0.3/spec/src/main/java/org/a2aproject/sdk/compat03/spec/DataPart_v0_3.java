package org.a2aproject.sdk.compat03.spec;

import java.util.Map;

import org.a2aproject.sdk.compat03.util.Utils_v0_3;
import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Represents a structured data segment (e.g., JSON) within a message or artifact.
 */
public record DataPart_v0_3(Map<String, Object> data, @Nullable Map<String, Object> metadata, Kind kind) implements Part_v0_3<Map<String, Object>> {

    public static final String DATA = "data";

    public DataPart_v0_3(Map<String, Object> data, @Nullable Map<String, Object> metadata, Kind kind) {
        Assert.checkNotNullParam("data", data);
        if (kind != Kind.DATA) {
            throw new IllegalArgumentException("Invalid DataPart kind: " + kind);
        }
        this.data = Utils_v0_3.copyJsonMap(data);
        this.metadata = metadata == null ? Map.of() : Utils_v0_3.copyJsonMap(metadata);
        this.kind = kind;
    }

    public DataPart_v0_3(Map<String, Object> data) {
        this(data, null, Kind.DATA);
    }

    public DataPart_v0_3(Map<String, Object> data, @Nullable Map<String, Object> metadata) {
        this(data, metadata, Kind.DATA);
    }
}
