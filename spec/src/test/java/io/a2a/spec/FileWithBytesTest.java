package io.a2a.spec;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the convenience constructors added to {@link FileWithBytes}.
 * <p>
 * The canonical {@code FileWithBytes(String, String, String)} constructor expects the bytes field
 * to already be base64-encoded. The constructors under test accept raw sources ({@link java.io.File},
 * {@link java.nio.file.Path}, or {@code byte[]}) and handle the base64 encoding internally.
 * <p>
 * Each group of tests verifies:
 * <ul>
 * <li>Correct base64 encoding of the provided content</li>
 * <li>Correct derivation of the file name from the source</li>
 * <li>Correct propagation of the MIME type</li>
 * <li>Edge cases such as empty content</li>
 * </ul>
 * The cross-constructor consistency tests confirm that all three convenience constructors and the
 * canonical constructor produce equivalent {@link FileWithBytes} instances when given the same data.
 */
class FileWithBytesTest {

    private static final String SVG_MIME_TYPE = "image/svg+xml";
    private static final String SVG_RESOURCE = "/a2a-logo-white.svg";

    @TempDir
    Path tempDir;

    private Path svgPath() throws URISyntaxException {
        return Path.of(getClass().getResource(SVG_RESOURCE).toURI());
    }

    private String base64(byte[] content) {
        return Base64.getEncoder().encodeToString(content);
    }

    private Path writeTempFile(String name, byte[] content) throws IOException {
        Path path = tempDir.resolve(name);
        Files.write(path, content);
        return path;
    }

    // ========== File constructor ==========

    @Test
    void testFileConstructor_encodesContentAsBase64() throws IOException {
        byte[] content = "hello world".getBytes();
        File file = writeTempFile("test.txt", content).toFile();

        FileWithBytes fwb = new FileWithBytes("text/plain", file);

        assertEquals("text/plain", fwb.mimeType());
        assertEquals("test.txt", fwb.name());
        assertEquals(base64(content), fwb.bytes());
    }

    @Test
    void testFileConstructor_useFileNameFromPath() throws IOException, URISyntaxException {
        File svgFile = svgPath().toFile();

        FileWithBytes fwb = new FileWithBytes(SVG_MIME_TYPE, svgFile);

        assertEquals(SVG_MIME_TYPE, fwb.mimeType());
        assertEquals("a2a-logo-white.svg", fwb.name());
        assertEquals(base64(Files.readAllBytes(svgFile.toPath())), fwb.bytes());
    }

    @Test
    void testFileConstructor_emptyFile() throws IOException {
        File file = writeTempFile("empty.bin", new byte[0]).toFile();

        FileWithBytes fwb = new FileWithBytes("application/octet-stream", file);

        assertEquals("application/octet-stream", fwb.mimeType());
        assertEquals("empty.bin", fwb.name());
        assertEquals("", fwb.bytes());
    }

    // ========== Path constructor ==========

    @Test
    void testPathConstructor_encodesContentAsBase64() throws IOException {
        byte[] content = "path content".getBytes();
        Path path = writeTempFile("data.txt", content);

        FileWithBytes fwb = new FileWithBytes("text/plain", path);

        assertEquals("text/plain", fwb.mimeType());
        assertEquals("data.txt", fwb.name());
        assertEquals(base64(content), fwb.bytes());
    }

    @Test
    void testPathConstructor_usesFileNameFromPath() throws IOException, URISyntaxException {
        Path path = svgPath();

        FileWithBytes fwb = new FileWithBytes(SVG_MIME_TYPE, path);

        assertEquals(SVG_MIME_TYPE, fwb.mimeType());
        assertEquals("a2a-logo-white.svg", fwb.name());
        assertEquals(base64(Files.readAllBytes(path)), fwb.bytes());
    }

    @Test
    void testPathConstructor_emptyFile() throws IOException {
        Path path = writeTempFile("empty.txt", new byte[0]);

        FileWithBytes fwb = new FileWithBytes("text/plain", path);

        assertEquals("text/plain", fwb.mimeType());
        assertEquals("empty.txt", fwb.name());
        assertEquals("", fwb.bytes());
    }

    // ========== byte[] constructor ==========

    @Test
    void testByteArrayConstructor_encodesContentAsBase64() throws IOException {
        byte[] content = "binary data".getBytes();

        FileWithBytes fwb = new FileWithBytes("application/octet-stream", "data.bin", content);

        assertEquals("application/octet-stream", fwb.mimeType());
        assertEquals("data.bin", fwb.name());
        assertEquals(base64(content), fwb.bytes());
    }

    @Test
    void testByteArrayConstructor_emptyArray() throws IOException {
        FileWithBytes fwb = new FileWithBytes("text/plain", "empty.txt", new byte[0]);

        assertEquals("text/plain", fwb.mimeType());
        assertEquals("empty.txt", fwb.name());
        assertEquals("", fwb.bytes());
    }

    @Test
    void testByteArrayConstructor_binaryContent() throws IOException {
        byte[] content = new byte[]{0, 1, 2, (byte) 0xFF, (byte) 0xFE};

        FileWithBytes fwb = new FileWithBytes("application/octet-stream", "bin.dat", content);

        byte[] decoded = Base64.getDecoder().decode(fwb.bytes());
        assertArrayEquals(content, decoded);
    }

    // ========== Consistency across constructors ==========

    @Test
    void testFileAndPathConstructorsProduceSameResult() throws IOException {
        Path path = writeTempFile("consistent.txt", "consistent content".getBytes());

        FileWithBytes fromFile = new FileWithBytes("text/plain", path.toFile());
        FileWithBytes fromPath = new FileWithBytes("text/plain", path);

        assertEquals(fromFile.mimeType(), fromPath.mimeType());
        assertEquals(fromFile.name(), fromPath.name());
        assertEquals(fromFile.bytes(), fromPath.bytes());
    }

    @Test
    void testByteArrayConstructorMatchesCanonicalConstructor() throws IOException {
        byte[] content = "test".getBytes();

        FileWithBytes fromCanonical = new FileWithBytes("text/plain", "test.txt", base64(content));
        FileWithBytes fromByteArray = new FileWithBytes("text/plain", "test.txt", content);

        assertEquals(fromCanonical.mimeType(), fromByteArray.mimeType());
        assertEquals(fromCanonical.name(), fromByteArray.name());
        assertEquals(fromCanonical.bytes(), fromByteArray.bytes());
    }

    @Test
    void testFileConstructorMatchesCanonicalConstructor() throws IOException {
        byte[] content = "file content".getBytes();
        Path path = writeTempFile("match.txt", content);

        FileWithBytes fromCanonical = new FileWithBytes("text/plain", "match.txt", base64(content));
        FileWithBytes fromFile = new FileWithBytes("text/plain", path.toFile());

        assertEquals(fromCanonical.mimeType(), fromFile.mimeType());
        assertEquals(fromCanonical.name(), fromFile.name());
        assertEquals(fromCanonical.bytes(), fromFile.bytes());
    }

    // ========== File validation tests ==========

    @Test
    void testPathConstructor_rejectsNonExistentFile() {
        Path nonExistent = tempDir.resolve("does-not-exist.txt");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new FileWithBytes("text/plain", nonExistent));

        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void testPathConstructor_rejectsDirectory() throws IOException {
        Path directory = tempDir.resolve("subdir");
        Files.createDirectory(directory);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new FileWithBytes("text/plain", directory));

        assertTrue(exception.getMessage().contains("Not a regular file"));
    }

    @Test
    void testPathConstructor_rejectsTooLargeFile() throws IOException {
        // Create a file larger than 10MB
        Path largeFile = tempDir.resolve("large.bin");
        byte[] chunk = new byte[1024 * 1024]; // 1MB
        try (var out = Files.newOutputStream(largeFile)) {
            for (int i = 0; i < 11; i++) { // Write 11MB
                out.write(chunk);
            }
        }

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new FileWithBytes("application/octet-stream", largeFile));

        assertTrue(exception.getMessage().contains("too large"));
        assertTrue(exception.getMessage().contains("maximum"));
    }

    // ========== Identity-based equality tests ==========

    @Test
    void testEquals_usesIdentityComparison() throws IOException {
        byte[] content = "test content".getBytes();
        Path path = writeTempFile("test.txt", content);

        FileWithBytes fwb1 = new FileWithBytes("text/plain", path);
        FileWithBytes fwb2 = new FileWithBytes("text/plain", path);

        // Same object should equal itself
        assertEquals(fwb1, fwb1);

        // Different objects with same content should NOT be equal (identity-based)
        assertNotEquals(fwb1, fwb2);
    }

    @Test
    void testHashCode_usesIdentityHashCode() throws IOException {
        byte[] content = "test content".getBytes();
        Path path = writeTempFile("test.txt", content);

        FileWithBytes fwb1 = new FileWithBytes("text/plain", path);
        FileWithBytes fwb2 = new FileWithBytes("text/plain", path);

        // Hash codes should be different for different objects (identity-based)
        assertNotEquals(fwb1.hashCode(), fwb2.hashCode());

        // Hash code should be consistent for same object
        assertEquals(fwb1.hashCode(), fwb1.hashCode());
    }
}
