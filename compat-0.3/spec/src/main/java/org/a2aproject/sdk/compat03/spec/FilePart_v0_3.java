package org.a2aproject.sdk.compat03.spec;

import java.util.Map;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Represents a file segment within a message or artifact. The file content can be
 * provided either directly as bytes or as a URI.
 */
public record FilePart_v0_3(FileContent_v0_3 file, Map<String, Object> metadata, Kind kind) implements Part_v0_3<FileContent_v0_3> {

    public static final String FILE = "file";

    public FilePart_v0_3 (FileContent_v0_3 file, @Nullable Map<String, Object> metadata, Kind kind){
        Assert.checkNotNullParam("file", file);
        if (kind != Kind.FILE) {
            throw new IllegalArgumentException("Invalid FilePart kind: " + kind);
        }
        this.file = file;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        this.kind = kind;
    }

    public FilePart_v0_3(FileContent_v0_3 file) {
        this(file, null, Kind.FILE);
    }

    public FilePart_v0_3(FileContent_v0_3 file, @Nullable Map<String, Object> metadata) {
        this(file, metadata, Kind.FILE);
    }
}
