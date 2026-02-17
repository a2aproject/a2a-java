package io.a2a.spec;


import io.a2a.util.Assert;
import java.util.Map;
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
 * @param metadata additional metadata for the part
 * @see Part
 * @see Message
 * @see Artifact
 */
public record DataPart(Object data, @Nullable Map<String, Object> metadata) implements Part<Object> {

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
    public DataPart (Object data, @Nullable Map<String, Object> metadata) {
        Assert.checkNotNullParam("data", data);
        this.metadata = metadata == null ? null : Map.copyOf(metadata);
        this.data = data;
    }

    /**
     * Constructor.
     *
     * @param data the structured data (supports JSON objects, arrays, primitives, and not null)
     * @throws IllegalArgumentException if data is null
     */
    public DataPart(Object data) {
        this(data, null);
    }
}
