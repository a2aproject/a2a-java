package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;

/**
 * Represents a data part within a Message or Artifact in the A2A protocol.
 * <p>
 * A DataPart contains structured data in the form of a map of key-value pairs.
 * This type of part is used to transmit arbitrary structured data between
 * agents, such as JSON objects, configuration data, or any other structured
 * information that can be represented as a map.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataPart extends Part<Map<String, Object>> {

    private final Map<String, Object> data;
    private final Map<String, Object> metadata;
    private final Kind kind;

    /**
     * Constructs a new DataPart with the specified data and no metadata.
     *
     * @param data the structured data to be contained in this part
     */
    public DataPart(Map<String, Object> data) {
        this(data, null);
    }

    /**
     * Constructs a new DataPart with the specified data and metadata.
     * This constructor is used for JSON deserialization.
     *
     * @param data the structured data to be contained in this part
     * @param metadata optional metadata associated with this data part
     * @throws IllegalArgumentException if data is null
     */
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

    /**
     * Gets the structured data contained in this part.
     *
     * @return the map containing the structured data
     */
    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

}
