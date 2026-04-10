package org.a2aproject.sdk.compat03.spec;

import static org.a2aproject.sdk.compat03.spec.FilePart.FILE;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.a2aproject.sdk.util.Assert;

/**
 * Represents a file segment within a message or artifact. The file content can be
 * provided either directly as bytes or as a URI.
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