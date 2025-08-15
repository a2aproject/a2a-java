package io.a2a.spec;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

/**
 * Custom Jackson deserializer for FileContent objects.
 * This deserializer handles the polymorphic nature of file content, which can be represented
 * either as base64-encoded bytes or as a URI reference. It determines the appropriate
 * FileContent subtype based on the presence of "bytes" or "uri" fields in the JSON.
 */
public class FileContentDeserializer extends StdDeserializer<FileContent> {

    /**
     * Default constructor for the deserializer.
     */
    public FileContentDeserializer() {
        this(null);
    }

    /**
     * Constructor with value class specification.
     * 
     * @param vc the value class being deserialized
     */
    public FileContentDeserializer(Class<?> vc) {
        super(vc);
    }

    /**
     * Deserializes JSON into a FileContent object.
     * The method examines the JSON structure to determine whether to create
     * a FileWithBytes or FileWithUri instance based on the available fields.
     * 
     * @param jsonParser the JSON parser
     * @param context the deserialization context
     * @return a FileContent instance (either FileWithBytes or FileWithUri)
     * @throws IOException if there's an I/O error during parsing
     * @throws JsonProcessingException if the JSON structure is invalid
     */
    @Override
    public FileContent deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException, JsonProcessingException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        JsonNode mimeType = node.get("mimeType");
        JsonNode name = node.get("name");
        JsonNode bytes = node.get("bytes");
        
        // Check if the file content is provided as base64-encoded bytes
        if (bytes != null) {
            return new FileWithBytes(mimeType != null ? mimeType.asText() : null,
                    name != null ? name.asText() : null, bytes.asText());
        } 
        // Check if the file content is provided as a URI reference
        else if (node.has("uri")) {
            return new FileWithUri(mimeType != null ? mimeType.asText() : null,
                    name != null ? name.asText() : null, node.get("uri").asText());
        } 
        // Neither bytes nor uri field is present - invalid format
        else {
            throw new IOException("Invalid file format: missing 'bytes' or 'uri'");
        }
    }
}
