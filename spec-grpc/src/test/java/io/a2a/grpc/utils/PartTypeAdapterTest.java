package io.a2a.grpc.utils;

import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.JsonUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.util.Structs;
import io.a2a.grpc.mapper.A2ACommonFieldMapper;
import java.util.List;
import java.util.Map;

import io.a2a.spec.DataPart;
import io.a2a.spec.FilePart;
import io.a2a.spec.FileWithBytes;
import io.a2a.spec.FileWithUri;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


public class PartTypeAdapterTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // TextPart
    // -------------------------------------------------------------------------

    @Test
    public void shouldSerializeTextPart() throws JsonProcessingException {
        TextPart part = new TextPart("Hello, world!");
        String json = JsonUtil.toJson(part);
        assertEquals("{\"text\":\"Hello, world!\"}", json);
    }

    @Test
    public void shouldSerializeTextPartWithMetadata() throws JsonProcessingException {
        TextPart part = new TextPart("Bonjour!", Map.of("language", "fr"));
        String json = JsonUtil.toJson(part);
        // Verify the round-trip to avoid ordering issues
        Part<?> deserialized = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(TextPart.class, deserialized);
        TextPart result = (TextPart) deserialized;
        assertEquals("Bonjour!", result.text());
        assertEquals("fr", result.metadata().get("language"));
    }

    @Test
    public void shouldDeserializeTextPart() throws JsonProcessingException, InvalidProtocolBufferException {
        io.a2a.grpc.Part.Builder builder = io.a2a.grpc.Part.newBuilder();
        builder.setText("Hello, world!");
        String json = JsonFormat.printer().alwaysPrintFieldsWithNoPresence().omittingInsignificantWhitespace().print(builder);
        Part<?>part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(TextPart.class, part);
        TextPart textPart = (TextPart) part;
        assertEquals("Hello, world!", textPart.text());
        assertNotNull(textPart.metadata());
        assertEquals(0, textPart.metadata().size());
    }

    @Test
    public void shouldDeserializeTextPartWithMetadata() throws JsonProcessingException, InvalidProtocolBufferException {
        io.a2a.grpc.Part.Builder builder = io.a2a.grpc.Part.newBuilder();
        builder.setText("Hi");
        builder.setMetadata(A2ACommonFieldMapper.INSTANCE.metadataToProto(Map.of("key", "value")));
        String json = JsonFormat.printer().alwaysPrintFieldsWithNoPresence().omittingInsignificantWhitespace().print(builder);
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(TextPart.class, part);
        TextPart textPart = (TextPart) part;
        assertEquals("Hi", textPart.text());
        assertEquals("value", textPart.metadata().get("key"));
    }

    @Test
    public void shouldRoundTripTextPart() throws JsonProcessingException {
        TextPart original = new TextPart("round-trip");
        String json = JsonUtil.toJson(original);
        Part<?> deserialized = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(TextPart.class, deserialized);
        assertEquals(original.text(), ((TextPart) deserialized).text());
    }

    // -------------------------------------------------------------------------
    // FilePart – FileWithBytes
    // -------------------------------------------------------------------------

    @Test
    public void shouldSerializeFilePartWithBytes() throws JsonProcessingException {
        FilePart part = new FilePart(new FileWithBytes("image/png", "diagram.png", "abc12w=="));
        String json = JsonUtil.toJson(part);
        Part<?> deserialized = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(FilePart.class, deserialized);
        FileWithBytes result = (FileWithBytes) ((FilePart) deserialized).file();
        assertEquals("image/png", result.mimeType());
        assertEquals("diagram.png", result.name());
        assertEquals("abc12w==", result.bytes());
    }

    @Test
    public void shouldDeserializeFilePartWithBytes() throws JsonProcessingException, InvalidProtocolBufferException {
        io.a2a.grpc.Part.Builder builder = io.a2a.grpc.Part.newBuilder();
        builder.setFilename("diagram.png").setMediaType("image/png").setRaw(ByteString.copyFrom(Base64.getDecoder().decode("abc12w==")));
        builder.setMetadata(A2ACommonFieldMapper.INSTANCE.metadataToProto(Map.of("key", "value")));
        String json = JsonFormat.printer().alwaysPrintFieldsWithNoPresence().omittingInsignificantWhitespace().print(builder);
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(FilePart.class, part);
        FilePart filePart = (FilePart) part;
        assertInstanceOf(FileWithBytes.class, filePart.file());
        FileWithBytes fileWithBytes = (FileWithBytes) filePart.file();
        assertEquals("image/png", fileWithBytes.mimeType());
        assertEquals("diagram.png", fileWithBytes.name());
        assertEquals("abc12w==", fileWithBytes.bytes());
        assertEquals("value", filePart.metadata().get("key"));

    }

    @Test
    public void shouldRoundTripFilePartWithBytes() throws JsonProcessingException {
        FilePart original = new FilePart(new FileWithBytes("application/pdf", "report.pdf", "AAEC"));
        String json = JsonUtil.toJson(original);
        Part<?> deserialized = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(FilePart.class, deserialized);
        FilePart result = (FilePart) deserialized;
        assertInstanceOf(FileWithBytes.class, result.file());
        FileWithBytes bytes = (FileWithBytes) result.file();
        assertEquals("application/pdf", bytes.mimeType());
        assertEquals("report.pdf", bytes.name());
        assertEquals("AAEC", bytes.bytes());
    }

    @Test
    public void shouldRoundTripFilePartWithBytesFromRealFile() throws JsonProcessingException, IOException {
        // Create a temporary file with some content
        Path testFile = tempDir.resolve("test-file.txt");
        String fileContent = "This is test content for lazy loading verification";
        Files.writeString(testFile, fileContent);
        
        // Create FileWithBytes from the file path (lazy loading)
        FileWithBytes fileWithBytes = new FileWithBytes("text/plain", testFile);
        FilePart original = new FilePart(fileWithBytes);
        
        // Serialize to JSON (this triggers lazy loading)
        String json = JsonUtil.toJson(original);
        
        // Deserialize and verify
        Part<?> deserialized = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(FilePart.class, deserialized);
        FilePart result = (FilePart) deserialized;
        assertInstanceOf(FileWithBytes.class, result.file());
        FileWithBytes bytes = (FileWithBytes) result.file();
        
        assertEquals("text/plain", bytes.mimeType());
        assertEquals("test-file.txt", bytes.name());
        
        // Verify the content by decoding the base64
        byte[] decodedBytes = Base64.getDecoder().decode(bytes.bytes());
        String decodedContent = new String(decodedBytes);
        assertEquals(fileContent, decodedContent);
    }

    // -------------------------------------------------------------------------
    // FilePart – FileWithUri
    // -------------------------------------------------------------------------

    @Test
    public void shouldSerializeFilePartWithUri() throws JsonProcessingException {
        FilePart part = new FilePart(new FileWithUri("image/png", "photo.png", "https://example.com/photo.png"));
        String json = JsonUtil.toJson(part);
        // Verify the serialized JSON can be deserialized correctly (round-trip)
        Part<?> deserialized = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(FilePart.class, deserialized);
        FileWithUri result = (FileWithUri) ((FilePart) deserialized).file();
        assertEquals("image/png", result.mimeType());
        assertEquals("photo.png", result.name());
        assertEquals("https://example.com/photo.png", result.uri());
    }

    @Test
    public void shouldDeserializeFilePartWithUri() throws JsonProcessingException, InvalidProtocolBufferException {
        io.a2a.grpc.Part.Builder builder = io.a2a.grpc.Part.newBuilder();
        builder.setFilename("photo.png").setMediaType("image/png").setUrl("https://example.com/photo.png");
        builder.setMetadata(A2ACommonFieldMapper.INSTANCE.metadataToProto(Map.of("key", "value")));
        String json = JsonFormat.printer().alwaysPrintFieldsWithNoPresence().omittingInsignificantWhitespace().print(builder);
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(FilePart.class, part);
        FilePart filePart = (FilePart) part;
        assertInstanceOf(FileWithUri.class, filePart.file());
        FileWithUri fileWithUri = (FileWithUri) filePart.file();
        assertEquals("image/png", fileWithUri.mimeType());
        assertEquals("photo.png", fileWithUri.name());
        assertEquals("https://example.com/photo.png", fileWithUri.uri());
        assertEquals("value", filePart.metadata().get("key"));
    }

    @Test
    public void shouldRoundTripFilePartWithUri() throws JsonProcessingException {
        FilePart original = new FilePart(new FileWithUri("text/plain", "notes.txt", "https://example.com/notes.txt"));
        String json = JsonUtil.toJson(original);
        Part<?> deserialized = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(FilePart.class, deserialized);
        FilePart result = (FilePart) deserialized;
        assertInstanceOf(FileWithUri.class, result.file());
        FileWithUri uri = (FileWithUri) result.file();
        assertEquals("text/plain", uri.mimeType());
        assertEquals("notes.txt", uri.name());
        assertEquals("https://example.com/notes.txt", uri.uri());
    }

    @Test
    public void shouldRoundTripFilePartWithMetadata() throws JsonProcessingException {
        FilePart original = new FilePart(
                new FileWithUri("image/jpeg", "pic.jpg", "https://example.com/pic.jpg"),
                Map.of("source", "camera"));
        String json = JsonUtil.toJson(original);
        Part<?> deserialized = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(FilePart.class, deserialized);
        FilePart result = (FilePart) deserialized;
        assertEquals("camera", result.metadata().get("source"));
    }

    // -------------------------------------------------------------------------
    // DataPart
    // -------------------------------------------------------------------------

    @Test
    public void shouldSerializeDataPartWithObject() throws JsonProcessingException {
        DataPart part = new DataPart(Map.of("status", "ok"));
        String json = JsonUtil.toJson(part);
        // Verify round-trip to avoid ordering issues with map serialization
        Part<?> deserialized = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(DataPart.class, deserialized);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) ((DataPart) deserialized).data();
        assertEquals("ok", data.get("status"));
    }

    @Test
    public void shouldDeserializeDataPartWithObject() throws JsonProcessingException, InvalidProtocolBufferException {
        io.a2a.grpc.Part.Builder builder = io.a2a.grpc.Part.newBuilder();
        builder.setData(Value.newBuilder().setStructValue(Structs.of("count", Value.newBuilder().setNumberValue(42).build(), "label", Value.newBuilder().setStringValue("items").build())));
        builder.setMetadata(A2ACommonFieldMapper.INSTANCE.metadataToProto(Map.of("key", "value")));
        String json = JsonFormat.printer().alwaysPrintFieldsWithNoPresence().omittingInsignificantWhitespace().print(builder);
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(DataPart.class, part);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) ((DataPart) part).data();
        assertEquals(42.0, data.get("count"));
        assertEquals("items", data.get("label"));
    }

    @Test
    public void shouldSerializeDataPartWithArray() throws JsonProcessingException {
        DataPart part = new DataPart(List.of("a", "b", "c"));
        String json = JsonUtil.toJson(part);
        assertEquals("{\"data\":[\"a\",\"b\",\"c\"]}", json);
    }

    @Test
    public void shouldDeserializeDataPartWithArray() throws JsonProcessingException {
        String json = "{\"data\":[\"a\",\"b\",\"c\"]}";
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(DataPart.class, part);
        @SuppressWarnings("unchecked")
        List<Object> data = (List<Object>) ((DataPart) part).data();
        assertEquals(List.of("a", "b", "c"), data);
    }

    @Test
    public void shouldSerializeDataPartWithString() throws JsonProcessingException {
        DataPart part = new DataPart("hello");
        String json = JsonUtil.toJson(part);
        assertEquals("{\"data\":\"hello\"}", json);
    }

    @Test
    public void shouldDeserializeDataPartWithString() throws JsonProcessingException {
        String json = "{\"data\":\"hello\"}";
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(DataPart.class, part);
        assertEquals("hello", ((DataPart) part).data());
    }

    @Test
    public void shouldSerializeDataPartWithNumber() throws JsonProcessingException {
        DataPart part = new DataPart(42L);
        String json = JsonUtil.toJson(part);
        assertEquals("{\"data\":42}", json);
    }

    @Test
    public void shouldDeserializeDataPartWithNumber() throws JsonProcessingException {
        String json = "{\"data\":42}";
        Part<?> part = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(DataPart.class, part);
        assertEquals(42L, ((DataPart) part).data());
    }

    @Test
    public void shouldRoundTripDataPartWithMetadata() throws JsonProcessingException {
        DataPart original = new DataPart(Map.of("key", "val"), Map.of("version", "1"));
        String json = JsonUtil.toJson(original);
        Part<?> deserialized = JsonUtil.fromJson(json, Part.class);
        assertInstanceOf(DataPart.class, deserialized);
        DataPart result = (DataPart) deserialized;
        assertEquals("1", result.metadata().get("version"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.data();
        assertEquals("val", data.get("key"));
    }
}
