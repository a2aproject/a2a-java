package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.spec.FileWithBytes_v0_3;
import org.a2aproject.sdk.compat03.spec.FileWithUri_v0_3;
import org.a2aproject.sdk.spec.FileContent;
import org.a2aproject.sdk.spec.FileWithBytes;
import org.a2aproject.sdk.spec.FileWithUri;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FileContentMapper_v0_3_Test {

    @Test
    void fileWithBytesToV10PreservesName() {
        FileWithBytes_v0_3 v03 = new FileWithBytes_v0_3("image/png", "photo.png", "aGVsbG8=");
        FileContent v10 = FileContentMapper_v0_3.INSTANCE.toV10(v03);
        FileWithBytes result = assertInstanceOf(FileWithBytes.class, v10);
        assertEquals("image/png", result.mimeType());
        assertEquals("photo.png", result.name());
        assertEquals("aGVsbG8=", result.bytes());
    }

    @Test
    void fileWithUriToV10PreservesName() {
        FileWithUri_v0_3 v03 = new FileWithUri_v0_3("text/plain", "readme.txt", "file:///path/to/readme.txt");
        FileContent v10 = FileContentMapper_v0_3.INSTANCE.toV10(v03);
        FileWithUri result = assertInstanceOf(FileWithUri.class, v10);
        assertEquals("text/plain", result.mimeType());
        assertEquals("readme.txt", result.name());
        assertEquals("file:///path/to/readme.txt", result.uri());
    }

    @Test
    void fileWithBytesFromV10PreservesName() {
        FileWithBytes v10 = new FileWithBytes("image/png", "photo.png", "aGVsbG8=");
        FileWithBytes_v0_3 result = assertInstanceOf(FileWithBytes_v0_3.class, FileContentMapper_v0_3.INSTANCE.fromV10(v10));
        assertEquals("image/png", result.mimeType());
        assertEquals("photo.png", result.name());
        assertEquals("aGVsbG8=", result.bytes());
    }

    @Test
    void fileWithUriFromV10PreservesName() {
        FileWithUri v10 = new FileWithUri("text/plain", "readme.txt", "file:///path/to/readme.txt");
        FileWithUri_v0_3 result = assertInstanceOf(FileWithUri_v0_3.class, FileContentMapper_v0_3.INSTANCE.fromV10(v10));
        assertEquals("text/plain", result.mimeType());
        assertEquals("readme.txt", result.name());
        assertEquals("file:///path/to/readme.txt", result.uri());
    }

    @Test
    void nullNameInV03BecomesEmptyStringInV10() {
        FileWithBytes_v0_3 v03 = new FileWithBytes_v0_3("image/png", null, "aGVsbG8=");
        FileWithBytes result = assertInstanceOf(FileWithBytes.class, FileContentMapper_v0_3.INSTANCE.toV10(v03));
        assertEquals("", result.name());
    }
}
