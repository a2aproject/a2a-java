package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents file content referenced by a URI in the A2A protocol.
 * <p>
 * This implementation of {@link FileContent} contains a reference to file data
 * via a URI instead of embedding the actual file content. This approach is
 * more efficient for larger files or when bandwidth optimization is important,
 * as it avoids including the file data directly in the message payload.
 * </p>
 * <p>
 * The URI can point to various locations such as HTTP/HTTPS URLs, file system
 * paths, or other accessible resource identifiers. The receiving agent is
 * responsible for retrieving the file content from the specified URI.
 * </p>
 *
 * @param mimeType the MIME type of the file (e.g., "text/plain", "image/jpeg")
 * @param name the name of the file including extension
 * @param uri the URI where the file content can be accessed
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FileWithUri(String mimeType, String name, String uri) implements FileContent {
}

