package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.a2a.util.Assert;

import static io.a2a.spec.FilePart.FILE;
import static io.a2a.util.Utils.SPEC_VERSION_1_0;

/**
 * Represents a file content part within a {@link Message} or {@link Artifact}.
 * <p>
 * FilePart contains file data that can be provided in two ways:
 * <ul>
 *   <li>{@link FileWithBytes} - File content embedded as base64-encoded bytes</li>
 *   <li>{@link FileWithUri} - File content referenced by URI</li>
 * </ul>
 * <p>
 * File parts are used to exchange binary data, documents, images, or any file-based content
 * between users and agents. The choice between bytes and URI depends on file size, accessibility,
 * and security requirements.
 * <p>
 * Example usage:
 * <pre>{@code
 * // File with embedded bytes
 * FilePart imageBytes = new FilePart(
 *     new FileWithBytes("image/png", "diagram.png", "iVBORw0KGgoAAAANS...")
 * );
 *
 * // File with URI reference
 * FilePart imageUri = new FilePart(
 *     new FileWithUri("image/png", "photo.png", "https://example.com/photo.png")
 * );
 * }</pre>
 *
 * @see Part
 * @see FileContent
 * @see FileWithBytes
 * @see FileWithUri
 */
@JsonTypeName(FILE)
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record FilePart(@JsonProperty("file") FileContent file,
                       @JsonProperty("metadata") Map<String, Object> metadata) implements Part<FileContent> {

    public static final String FILE = "file";


    @JsonCreator
    public FilePart {
        Assert.checkNotNullParam("file", file);
        metadata = (metadata != null) ? Map.copyOf(metadata) : null;
    }

    public FilePart(FileContent file) {
        this(file, null);
    }

    @Override
    public Kind kind() {
        return Kind.FILE;
    }

    /**
     * @deprecated Use {@link #file()} instead
     */
    @Deprecated(since = SPEC_VERSION_1_0)
    public FileContent getFile() {
        return file;
    }
}