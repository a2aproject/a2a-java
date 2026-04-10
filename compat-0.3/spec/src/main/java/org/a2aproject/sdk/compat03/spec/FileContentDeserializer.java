package org.a2aproject.sdk.compat03.spec;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class FileContentDeserializer extends StdDeserializer<FileContent> {

    public FileContentDeserializer() {
        this(null);
    }

    public FileContentDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public FileContent deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException, JsonProcessingException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        JsonNode mimeType = node.get("mimeType");
        JsonNode name = node.get("name");
        JsonNode bytes = node.get("bytes");
        if (bytes != null) {
            return new FileWithBytes(mimeType != null ? mimeType.asText() : null,
                    name != null ? name.asText() : null, bytes.asText());
        } else if (node.has("uri")) {
            return new FileWithUri(mimeType != null ? mimeType.asText() : null,
                    name != null ? name.asText() : null, node.get("uri").asText());
        } else {
            throw new IOException("Invalid file format: missing 'bytes' or 'uri'");
        }
    }
}
