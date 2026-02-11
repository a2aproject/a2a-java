package io.a2a.spec;


import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;


/**
 * Represents a structured data content part within a {@link Message} or {@link Artifact}.
 * <p>
 * DataPart contains arbitrary JSON data for machine-to-machine communication.
 * It is used when content needs to be processed programmatically rather than displayed as text,
 * such as API responses, configuration data, analysis results, or structured metadata.
 * <p>
 * The data can be any valid JSON value:
 * <ul>
 *   <li>JSON objects: {@code Map<String, Object>}</li>
 *   <li>JSON arrays: {@code List<Object>}</li>
 *   <li>Primitives: {@code String}, {@code Number}, {@code Boolean}</li>
 *   <li>Null values: {@code null}</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * // JSON object
 * DataPart obj = new DataPart(Map.of(
 *     "status", "success",
 *     "count", 42,
 *     "items", List.of("item1", "item2")
 * ));
 *
 * // JSON array
 * DataPart array = new DataPart(List.of("item1", "item2", "item3"));
 *
 * // Primitive value
 * DataPart primitive = new DataPart(42);
 * }</pre>
 *
 * @param data the structured data (required, supports JSON objects, arrays, primitives, and null)
 * @see Part
 * @see Message
 * @see Artifact
 */
public record DataPart(Object data) implements Part<Object> {

    /**
     * The JSON member name discriminator for data parts: "data".
     * <p>
     * In protocol v1.0+, this constant defines the JSON member name used for serialization:
     * {@code { "data": { "data": { "temperature": 22.5, "unit": "C" } } }}
     */
    public static final String DATA = "data";

    /**
     * Compact constructor with validation.
     * <p>
     * Note: For mutable data types (Map, List), callers should ensure immutability
     * by using {@code Map.copyOf()} or {@code List.copyOf()} before passing to this constructor.
     *
     * @param data the structured data (supports JSON objects, arrays, primitives, and null)
     * @throws IllegalArgumentException if data is null
     */
    public DataPart {
        Assert.checkNotNullParam("data", data);
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
     * Builder for constructing {@link DataPart} instances.
     */
    public static class Builder {
        private @Nullable Object data;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the structured data.
         *
         * @param data the structured data (required, supports JSON objects, arrays, primitives, and null)
         * @return this builder for method chaining
         */
        public Builder data(Object data) {
            this.data = data;
            return this;
        }

        /**
         * Builds a new {@link DataPart} from the current builder state.
         *
         * @return a new DataPart instance
         * @throws IllegalArgumentException if data is null
         */
        public DataPart build() {
            return new DataPart(Assert.checkNotNullParam("data", data));
        }
    }
}
