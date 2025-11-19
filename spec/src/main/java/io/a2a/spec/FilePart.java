package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.a2a.util.Assert;

import static io.a2a.spec.FilePart.FILE;

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
public class FilePart extends Part<FileContent> {

    public static final String FILE = "file";
    private final FileContent file;
    private final Map<String, Object> metadata;
    private final Kind kind;

    public FilePart(FileContent file) {
        this(file, null);
    }

    @JsonCreator
    public FilePart(@JsonProperty("file") FileContent file, @JsonProperty("metadata") Map<String, Object> metadata) {
        Assert.checkNotNullParam("file", file);
        this.file = file;
        this.metadata = metadata;
        this.kind = Kind.FILE;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    public FileContent getFile() {
        return file;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

}