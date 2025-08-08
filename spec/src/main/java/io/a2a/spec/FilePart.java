package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.a2a.util.Assert;

/**
 * Represents a file part within a Message or Artifact in the A2A protocol.
 * <p>
 * A FilePart contains file content that can be transmitted between agents.
 * The file content can be either embedded as bytes ({@link FileWithBytes})
 * or referenced by a URI ({@link FileWithUri}). This part type is used
 * for sharing documents, images, or any other file-based content.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilePart extends Part<FileContent> {

    private final FileContent file;
    private final Map<String, Object> metadata;
    private final Kind kind;

    /**
     * Constructs a new FilePart with the specified file content and no metadata.
     *
     * @param file the file content to be contained in this part
     */
    public FilePart(FileContent file) {
        this(file, null);
    }

    /**
     * Constructs a new FilePart with the specified file content and metadata.
     * This constructor is used for JSON deserialization.
     *
     * @param file the file content to be contained in this part
     * @param metadata optional metadata associated with this file part
     * @throws IllegalArgumentException if file is null
     */
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

    /**
     * Gets the file content contained in this part.
     *
     * @return the file content
     */
    public FileContent getFile() {
        return file;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

}