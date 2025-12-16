package io.a2a.spec;


import io.a2a.util.Assert;


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
public class FilePart extends Part<FileContent> {

    /** The type identifier for file parts in messages and artifacts. */
    public static final String FILE = "file";
    private final FileContent file;
    private final Kind kind;


    /**
     * Constructs a FilePart with file content.
     *
     * @param file the file content (required, either FileWithBytes or FileWithUri)
     * @throws IllegalArgumentException if file is null
     */
    public FilePart(FileContent file) {
        Assert.checkNotNullParam("file", file);
        this.file = file;
        this.kind = Kind.FILE;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    /**
     * Gets the file content contained in this part.
     *
     * @return the file content (FileWithBytes or FileWithUri)
     */
    public FileContent getFile() {
        return file;
    }

}