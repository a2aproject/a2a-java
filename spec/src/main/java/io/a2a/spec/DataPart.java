package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;


/**
 * Represents a structured data content part within a {@link Message} or {@link Artifact}.
 * <p>
 * DataPart contains structured data (typically JSON objects) for machine-to-machine communication.
 * It is used when content needs to be processed programmatically rather than displayed as text,
 * such as API responses, configuration data, analysis results, or structured metadata.
 * <p>
 * The data is represented as a Map of key-value pairs, which can contain nested structures
 * including lists, maps, and primitive values.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Simple structured data
 * DataPart result = new DataPart(Map.of(
 *     "status", "success",
 *     "count", 42,
 *     "items", List.of("item1", "item2")
 * ));
 *
 * // With metadata
 * DataPart withMeta = new DataPart(
 *     Map.of("temperature", 72.5, "unit", "F"),
 *     Map.of("source", "weather-api", "timestamp", "2024-01-20T12:00:00Z")
 * );
 * }</pre>
 *
 * @see Part
 * @see Message
 * @see Artifact
 */
public class DataPart extends Part<Map<String, Object>> {

    /** The type identifier for data parts in messages and artifacts. */
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

    /**
     * Gets the structured data contained in this part.
     *
     * @return a map of key-value pairs representing the data
     */
    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

}
