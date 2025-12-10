package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.a2a.util.Assert;

import static io.a2a.spec.DataPart.DATA;
import static io.a2a.util.Utils.SPEC_VERSION_1_0;

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
@JsonTypeName(DATA)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DataPart(@JsonProperty("data") Map<String, Object> data,
                       @JsonProperty("metadata") Map<String, Object> metadata) implements Part<Map<String, Object>> {

    public static final String DATA = "data";

    @JsonCreator
    public DataPart {
        Assert.checkNotNullParam("data", data);
        data = Map.copyOf(data);
        metadata = (metadata != null) ? Map.copyOf(metadata) : null;
    }

    public DataPart(Map<String, Object> data) {
        this(data, null);
    }

    @Override
    public Kind kind() {
        return Kind.DATA;
    }

    /**
     * @deprecated Use {@link #data()} instead
     */
    @Deprecated(since = SPEC_VERSION_1_0)
    public Map<String, Object> getData() {
        return data;
    }

}
