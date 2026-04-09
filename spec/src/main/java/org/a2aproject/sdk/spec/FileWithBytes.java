package org.a2aproject.sdk.spec;

import org.a2aproject.sdk.util.Assert;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Represents file content embedded directly as base64-encoded bytes.
 * <p>
 * FileWithBytes is used when file content needs to be transmitted inline with the message or
 * artifact, rather than requiring a separate download. This is appropriate for:
 * <ul>
 * <li>Small files that fit comfortably in a JSON payload</li>
 * <li>Generated content that doesn't exist as a standalone file</li>
 * <li>Content that must be preserved exactly as created</li>
 * <li>Scenarios where URI accessibility is uncertain</li>
 * </ul>
 * <p>
 * The bytes field contains the base64-encoded file content. Decoders should handle the base64
 * encoding/decoding transparently.
 * <p>
 * This class uses lazy loading with soft-reference caching to reduce memory pressure: the
 * base64-encoded content is computed on-demand and held via a {@link SoftReference}, allowing
 * the JVM to reclaim it under memory pressure. If reclaimed, it is recomputed on next access.
 *
 * @see FileContent
 * @see FilePart
 * @see FileWithUri
 */
public final class FileWithBytes implements FileContent {

    /**
     * Maximum file size that can be loaded (10 MB).
     * Files larger than this will be rejected at construction time.
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final String mimeType;
    private final String name;

    // Source for (re)generating base64 content on-demand
    private final ByteSource source;

    // Soft-reference cache: held in memory but reclaimable by GC under memory pressure
    @Nullable
    private volatile SoftReference<String> cachedBytes;

    /**
     * Creates a {@code FileWithBytes} with pre-encoded base64 content.
     * This is the canonical constructor used by serialization frameworks.
     *
     * @param mimeType the MIME type of the file (e.g., "image/png", "application/pdf")
     * @param name the file name (e.g., "report.pdf", "diagram.png")
     * @param bytes the base64-encoded file content
     */
    public FileWithBytes(String mimeType, String name, String bytes) {
        this.mimeType = Assert.checkNotNullParam("mimeType", mimeType);
        this.name = Assert.checkNotNullParam("name", name);
        this.source = new PreEncodedSource(Assert.checkNotNullParam("bytes", bytes));
        this.cachedBytes = new SoftReference<>(bytes);
    }

    /**
     * Creates a {@code FileWithBytes} by reading the content of the given {@link File}.
     * The file name is derived from {@link File#getName()}.
     * <p>
     * The file is validated at construction time to ensure it exists, is readable, is a regular file,
     * and does not exceed the maximum size limit ({@value #MAX_FILE_SIZE} bytes).
     * <p>
     * The file content is read and base64-encoded on the first call to {@link #bytes()}, then
     * cached via a soft reference. The cache may be cleared by GC under memory pressure, in
     * which case the file is re-read on the next access.
     *
     * @param mimeType the MIME type of the file (e.g., {@code "image/png"})
     * @param file the file whose content will be read and encoded
     * @throws IllegalArgumentException if the file does not exist, is not readable, is not a regular file,
     *                                  or exceeds the maximum size limit
     * @throws RuntimeException if an I/O error occurs while checking the file
     */
    public FileWithBytes(String mimeType, File file) {
        this(mimeType, file.toPath());
    }

    /**
     * Creates a {@code FileWithBytes} by reading the content of the given {@link Path}.
     * The file name is derived from {@link Path#getFileName()}.
     * <p>
     * The file is validated at construction time to ensure it exists, is readable, is a regular file,
     * and does not exceed the maximum size limit ({@value #MAX_FILE_SIZE} bytes).
     * <p>
     * The file content is read and base64-encoded on the first call to {@link #bytes()}, then
     * cached via a soft reference. The cache may be cleared by GC under memory pressure, in
     * which case the file is re-read on the next access.
     *
     * @param mimeType the MIME type of the file (e.g., {@code "image/png"})
     * @param file the path whose content will be read and encoded
     * @throws IllegalArgumentException if the file does not exist, is not readable, is not a regular file,
     *                                  or exceeds the maximum size limit
     * @throws RuntimeException if an I/O error occurs while checking the file
     */
    public FileWithBytes(String mimeType, Path file) {
        this.mimeType = Assert.checkNotNullParam("mimeType", mimeType);
        validateFile(file);
        this.name = file.getFileName().toString();
        this.source = new PathSource(file);
    }

    /**
     * Creates a {@code FileWithBytes} by base64-encoding the given raw byte array.
     * <p>
     * A defensive copy of {@code content} is made at construction time, so subsequent mutations
     * to the caller's array have no effect. The copy is base64-encoded on the first call to
     * {@link #bytes()}, then cached via a soft reference. The cache may be cleared by GC under
     * memory pressure, in which case the encoding is recomputed from the retained copy.
     *
     * @param mimeType the MIME type of the file (e.g., {@code "application/pdf"})
     * @param name the file name (e.g., {@code "report.pdf"})
     * @param content the raw file content to be base64-encoded
     * @throws NullPointerException if {@code content} is null
     */
    public FileWithBytes(String mimeType, String name, byte[] content) {
        this.mimeType = Assert.checkNotNullParam("mimeType", mimeType);
        this.name = Assert.checkNotNullParam("name", name);
        this.source = new ByteArraySource(content);
    }

    @Override
    public String mimeType() {
        return mimeType;
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Returns the base64-encoded file content.
     * <p>
     * The content is computed on the first call and cached via a soft reference. Subsequent calls
     * return the cached value. If the JVM reclaims the cache under memory pressure, the content is
     * recomputed transparently on the next access.
     * <p>
     * For instances created from a {@link File} or {@link Path}, recomputation involves reading
     * the file from disk. Callers in performance-sensitive paths should retain the returned value
     * rather than calling this method repeatedly.
     *
     * @return the base64-encoded file content
     * @throws RuntimeException if an I/O error occurs while reading a file-backed source
     */
    public String bytes() {
        // First check: fast path without locking
        SoftReference<String> ref = cachedBytes;
        if (ref != null) {
            String cached = ref.get();
            if (cached != null) {
                return cached;
            }
        }
        // Second check: slow path, synchronized to prevent redundant computation
        // (especially costly for file-backed sources, which would re-read from disk)
        synchronized (this) {
            ref = cachedBytes;
            if (ref != null) {
                String cached = ref.get();
                if (cached != null) {
                    return cached;
                }
            }
            try {
                String computed = source.getBase64();
                cachedBytes = new SoftReference<>(computed);
                return computed;
            } catch (IOException e) {
                throw new RuntimeException("Failed to load file content", e);
            }
        }
    }

    /**
     * Compares this FileWithBytes to another object for equality.
     * <p>
     * <strong>Important:</strong> This method uses identity-based comparison to avoid triggering
     * potentially expensive I/O operations. Two FileWithBytes instances are considered equal only
     * if they are the same object (reference equality).
     * <p>
     * This design choice prevents:
     * <ul>
     * <li>Unexpected file I/O during collection operations (HashMap, HashSet, etc.)</li>
     * <li>Performance issues when comparing file-backed instances</li>
     * <li>RuntimeExceptions from I/O errors during equality checks</li>
     * </ul>
     * <p>
     * If you need to compare the actual content of two FileWithBytes instances, use a separate
     * method or compare the results of {@link #bytes()} explicitly.
     *
     * @param o the object to compare with
     * @return true if this is the same object as o, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    /**
     * Returns the identity hash code for this FileWithBytes.
     * <p>
     * This method uses {@link System#identityHashCode(Object)} to avoid triggering I/O operations
     * that would be required to compute a content-based hash code. This ensures that using
     * FileWithBytes instances as keys in HashMap or elements in HashSet remains safe and efficient.
     *
     * @return the identity hash code
     */
    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "FileWithBytes[mimeType=" + mimeType + ", name=" + name + "]";
    }

    /**
     * Validates that a file exists, is readable, is a regular file, and does not exceed the maximum size.
     *
     * @param file the file to validate
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException if an I/O error occurs during validation
     */
    private static void validateFile(Path file) {
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("File does not exist: " + file);
        }
        if (!Files.isReadable(file)) {
            throw new IllegalArgumentException("File is not readable: " + file);
        }
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Not a regular file: " + file);
        }
        try {
            long size = Files.size(file);
            if (size > MAX_FILE_SIZE) {
                throw new IllegalArgumentException(
                    String.format("File too large: %d bytes (maximum: %d bytes)", size, MAX_FILE_SIZE)
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to check file size: " + file, e);
        }
    }

    /**
     * Internal interface for different byte sources.
     */
    private interface ByteSource {
        String getBase64() throws IOException;
    }

    /**
     * Source for pre-encoded base64 content.
     */
    private static final class PreEncodedSource implements ByteSource {
        private final String base64;

        PreEncodedSource(String base64) {
            this.base64 = base64;
        }

        @Override
        public String getBase64() {
            return base64;
        }
    }

    /**
     * Source for file path that needs to be read and encoded.
     */
    private static final class PathSource implements ByteSource {
        private final Path path;

        PathSource(Path path) {
            this.path = path;
        }

        @Override
        public String getBase64() throws IOException {
            return encodeFileToBase64(path);
        }
    }

    /**
     * Source for byte array that needs to be encoded.
     */
    private static final class ByteArraySource implements ByteSource {
        private final byte[] content;

        ByteArraySource(byte[] content) {
            this.content = Objects.requireNonNull(content, "content must not be null").clone();
        }

        @Override
        public String getBase64() {
            return Base64.getEncoder().encodeToString(content);
        }
    }

    /**
     * Encodes a file to base64 by streaming its content in chunks.
     * This avoids loading the entire file into memory at once by using
     * a wrapping output stream that encodes data as it's written.
     *
     * @param path the path to the file to encode
     * @return the base64-encoded content
     * @throws IOException if an I/O error occurs reading the file
     */
    private static String encodeFileToBase64(Path path) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(path));
             OutputStream base64OutputStream = Base64.getEncoder().wrap(outputStream)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                base64OutputStream.write(buffer, 0, bytesRead);
            }
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }
}
