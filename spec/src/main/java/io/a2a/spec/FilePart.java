package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;

import static io.a2a.spec.FilePart.FILE;

/**
 * Represents a file segment within a message or artifact. The file content can be
 * provided either directly as bytes or as a URI.
 */
public class FilePart extends Part<FileContent> {

    public static final String FILE = "file";
    private final FileContent file;
    private final Map<String, Object> metadata;
    private final Kind kind;

    public FilePart(FileContent file) {
        this(file, null);
    }

    public FilePart(FileContent file, Map<String, Object> metadata) {
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