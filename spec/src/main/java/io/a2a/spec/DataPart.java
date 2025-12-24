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
 * @param data the structured data map (required, defensively copied for immutability)
 * @see Part
 * @see Message
 * @see Artifact
 */
public record DataPart(Map<String, Object> data) implements Part<Map<String, Object>> {

    /**
     * The JSON member name discriminator for data parts: "data".
     * <p>
     * In protocol v1.0+, this constant defines the JSON member name used for serialization:
     * {@code { "data": { "data": { "temperature": 22.5, "unit": "C" } } }}
     */
    public static final String DATA = "data";

    /**
     * Compact constructor with validation and defensive copying.
     *
     * @param data the structured data map (required, defensively copied for immutability)
     * @throws IllegalArgumentException if data is null
     */
    public DataPart {
        Assert.checkNotNullParam("data", data);
        data = Map.copyOf(data);
    }

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing immutable {@link DataPart} instances.
     */
    public static class Builder {
        private Map<String, Object> data;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the structured data map.
         *
         * @param data the structured data (required)
         * @return this builder for method chaining
         */
        public Builder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        /**
         * Builds a new immutable {@link DataPart} from the current builder state.
         *
         * @return a new DataPart instance
         * @throws IllegalArgumentException if data is null
         */
        public DataPart build() {
            return new DataPart(data);
        }
    }
}
