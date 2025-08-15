package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents file content with embedded byte data in the A2A protocol.
 * <p>
 * This implementation of {@link FileContent} contains the actual file data
 * as a base64-encoded string. This approach is suitable for smaller files
 * that can be efficiently transmitted as part of the message payload.
 * </p>
 * <p>
 * For larger files or when bandwidth is a concern, consider using
 * {@link FileWithUri} instead, which references the file by URI.
 * </p>
 *
 * @param mimeType the MIME type of the file (e.g., "text/plain", "image/jpeg")
 * @param name the name of the file including extension
 * @param bytes the file content encoded as a base64 string
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FileWithBytes(String mimeType, String name, String bytes) implements FileContent {
}
