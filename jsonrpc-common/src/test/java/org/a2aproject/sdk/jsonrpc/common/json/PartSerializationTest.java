package org.a2aproject.sdk.jsonrpc.common.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.FilePart;
import org.a2aproject.sdk.spec.FileWithBytes;
import org.a2aproject.sdk.spec.FileWithUri;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Decoding of the flat Part oneOf when a producer emits more than one content key.
 */
public class PartSerializationTest {

    @Test
    void testPopulatedDataWinsOverEmptyTextPlaceholder() throws JsonProcessingException {
        String json = """
            {"text": "", "data": {"answer": "42"}}
            """;
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(DataPart.class, part);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) ((DataPart) part).data();
        assertEquals("42", data.get("answer"));
    }

    @Test
    void testDataPartDecodesRegardlessOfContentKeyOrder() throws JsonProcessingException {
        String json = """
            {"data": {"answer": "42"}, "text": ""}
            """;
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(DataPart.class, part);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) ((DataPart) part).data();
        assertEquals("42", data.get("answer"));
    }

    @Test
    void testPopulatedDataWinsOverAllEmptyPlaceholders() throws JsonProcessingException {
        String json = """
            {"text": "", "raw": "", "url": "", "data": {"answer": "42"}}
            """;
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(DataPart.class, part);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) ((DataPart) part).data();
        assertEquals("42", data.get("answer"));
    }

    @Test
    void testPopulatedUrlWinsOverEmptyTextPlaceholder() throws JsonProcessingException {
        String json = """
            {"text": "", "url": "https://example.org/report.pdf", "mediaType": "application/pdf"}
            """;
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(FilePart.class, part);
        FileWithUri file = (FileWithUri) ((FilePart) part).file();
        assertEquals("https://example.org/report.pdf", file.uri());
        assertEquals("application/pdf", file.mimeType());
    }

    @Test
    void testPopulatedRawWinsOverEmptyTextPlaceholder() throws JsonProcessingException {
        String json = """
            {"text": "", "raw": "abc12w==", "filename": "diagram.png", "mediaType": "image/png"}
            """;
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(FilePart.class, part);
        FileWithBytes file = (FileWithBytes) ((FilePart) part).file();
        assertEquals("abc12w==", file.bytes());
        assertEquals("diagram.png", file.name());
        assertEquals("image/png", file.mimeType());
    }

    /** Emptiness is decided on the JSON string only, so a falsy data payload still outranks a placeholder. */
    @ParameterizedTest
    @ValueSource(strings = {"0", "false", "{}", "[]"})
    void testFalsyDataPayloadWinsOverEmptyTextPlaceholder(String payload) throws JsonProcessingException {
        Part<?> part = JsonUtil.fromJson("{\"text\": \"\", \"data\": " + payload + "}", Part.class);
        assertInstanceOf(DataPart.class, part);
    }

    @Test
    void testJsonNullContentKeyIsSkipped() throws JsonProcessingException {
        String json = """
            {"text": "hello", "data": null}
            """;
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(TextPart.class, part);
        assertEquals("hello", ((TextPart) part).text());
    }

    @Test
    void testPopulatedDataWinsOverJsonNullText() throws JsonProcessingException {
        String json = """
            {"text": null, "data": {"answer": "42"}}
            """;
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(DataPart.class, part);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) ((DataPart) part).data();
        assertEquals("42", data.get("answer"));
    }

    @Test
    void testDeliberatelyEmptyTextPartDecodes() throws JsonProcessingException {
        String json = """
            {"text": ""}
            """;
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(TextPart.class, part);
        assertEquals("", ((TextPart) part).text());
    }

    @Test
    void testEmptyTextPartRoundTrips() throws JsonProcessingException {
        String json = JsonUtil.toJson(new TextPart(""));
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(TextPart.class, part);
        assertEquals("", ((TextPart) part).text());
    }

    @Test
    void testDataPartWithEmptyStringPayloadDecodes() throws JsonProcessingException {
        String json = """
            {"data": ""}
            """;
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(DataPart.class, part);
        assertEquals("", ((DataPart) part).data());
    }

    /** Nothing distinguishes the intended content when every key is an empty string, so order decides. */
    @Test
    void testAllEmptyContentKeysFallBackToTheFirst() throws JsonProcessingException {
        String json = """
            {"text": "", "data": ""}
            """;
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(TextPart.class, part);
        assertEquals("", ((TextPart) part).text());
    }

    @Test
    void testMultiplePopulatedContentKeysAreRejected() {
        String json = """
            {"text": "hello", "data": {"answer": "42"}}
            """;
        assertThrows(JsonProcessingException.class, () -> JsonUtil.fromJson(json, Part.class));
    }

    @Test
    void testNonStringContentKeyIsRejected() {
        String json = """
            {"text": "", "raw": {"nested": 1}}
            """;
        assertThrows(JsonProcessingException.class, () -> JsonUtil.fromJson(json, Part.class));
    }
}
