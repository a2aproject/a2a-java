package org.a2aproject.sdk.compat03.spec;

import java.util.Map;

import org.a2aproject.sdk.util.Assert;

/**
 * Represents a file segment within a message or artifact. The file content can be
 * provided either directly as bytes or as a URI.
 */
public class FilePart_v0_3 extends Part_v0_3<FileContent_v0_3> {

    public static final String FILE = "file";
    private final FileContent_v0_3 file;
    private final Map<String, Object> metadata;
    private final Kind kind;

    public FilePart_v0_3(FileContent_v0_3 file) {
        this(file, null);
    }

    public FilePart_v0_3(FileContent_v0_3 file, Map<String, Object> metadata) {
        Assert.checkNotNullParam("file", file);
        this.file = file;
        this.metadata = metadata;
        this.kind = Kind.FILE;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    public FileContent_v0_3 getFile() {
        return file;
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadata;
    }

}