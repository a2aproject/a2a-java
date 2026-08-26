package org.a2aproject.sdk.spec;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ToNumberPolicy;
import org.a2aproject.sdk.util.Assert;
import org.a2aproject.sdk.spec.util.CollectionCopies;
import java.util.List;
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
 * @param data the structured data (required, supports JSON objects, arrays, and primitives)
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
     * Compact constructor with validation and defensive copying.
     * <p>
     * For mutable data types ({@code Map} and {@code List}), an unmodifiable defensive
     * copy is created. Primitives and other immutable values are stored as-is.
     *
     * @param data the structured data (required, supports JSON objects, arrays, and primitives)
     * @param metadata additional metadata for the part
     * @throws IllegalArgumentException if data is null
     */
    public DataPart (Object data, @Nullable Map<String, Object> metadata) {
        Assert.checkNotNullParam("data", data);
        this.metadata = CollectionCopies.unmodifiableNullableShallowMap(metadata);
        this.data = defensivelyCopy(data);
    }

    /**
     * Constructor.
     *
     * @param data the structured data (required, supports JSON objects, arrays, and primitives)
     * @throws IllegalArgumentException if data is null
     */
    public DataPart(Object data) {
        this(data, null);
    }

    /**
     * Creates a DataPart by parsing a JSON string into its corresponding Java type.
     * <p>
     * The JSON string is parsed using Gson with {@code ToNumberPolicy.LONG_OR_DOUBLE},
     * producing the following mappings:
     * <ul>
     *   <li>JSON objects → {@code Map<String, Object>}</li>
     *   <li>JSON arrays → {@code List<Object>}</li>
     *   <li>JSON strings → {@code String}</li>
     *   <li>JSON integers → {@code Long}</li>
     *   <li>JSON decimals → {@code Double}</li>
     *   <li>JSON booleans → {@code Boolean}</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * DataPart dataPart = DataPart.fromJson("""
     *     {
     *         "temperature": 22.5,
     *         "humidity": 65
     *     }""");
     * }</pre>
     *
     * @param json the JSON string to parse (must not be null or the JSON literal "null")
     * @return a new DataPart containing the parsed data
     * @throws IllegalArgumentException if json is null, parses to null, or is not valid
     */
    public static DataPart fromJson(String json) {
        return fromJson(json, null);
    }

    /**
     * Creates a DataPart by parsing a JSON string into its corresponding Java type,
     * with optional metadata.
     *
     * @param json the JSON string to parse (must not be null or the JSON literal "null")
     * @param metadata additional metadata for the part
     * @return a new DataPart containing the parsed data and metadata
     * @throws IllegalArgumentException if json is null, parses to null, or is not valid
     * @see #fromJson(String)
     */
    public static DataPart fromJson(String json, @Nullable Map<String, Object> metadata) {
        Assert.checkNotNullParam("json", json);
        try {
            Object data = JSON_PARSER.fromJson(json, Object.class);
            return new DataPart(data, metadata);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Invalid JSON: " + json, e);
        }
    }

    private static final Gson JSON_PARSER = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create();

    /**
     * Creates a defensive copy of mutable collection types to ensure immutability.
     * <p>
     * For {@code Map} and {@code List} instances, returns an unmodifiable copy preserving
     * null elements. For all other types (primitives, Strings, immutable objects), returns
     * the value as-is.
     *
     * @param data the data value to potentially copy
     * @return an unmodifiable copy for collections, or the original value for immutable types
     */
    private static Object defensivelyCopy(Object data) {
        if (data instanceof Map<?, ?> map) {
            return CollectionCopies.unmodifiableShallowMap(map);
        }
        if (data instanceof List<?> list) {
            return CollectionCopies.unmodifiableShallowList(list);
        }
        return data;
    }
}
