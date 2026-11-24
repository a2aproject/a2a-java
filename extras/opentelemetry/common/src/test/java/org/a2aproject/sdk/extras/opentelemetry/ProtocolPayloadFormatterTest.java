package org.a2aproject.sdk.extras.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.AbstractList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.FilePart;
import org.a2aproject.sdk.spec.FileWithBytes;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;

class ProtocolPayloadFormatterTest {
    @Test
    void redactsNestedSecretsAndAppliesCustomSanitizer() {
        ProtocolPayloadFormatter formatter =
                new ProtocolPayloadFormatter(4096, value -> value.replace("private", "[custom]"));
        Message message =
                Message.builder()
                        .messageId("id")
                        .role(Message.Role.ROLE_USER)
                        .parts(
                                List.of(
                                        new DataPart(
                                                Map.of(
                                                        "nested",
                                                        Map.of(
                                                                "API_KEY",
                                                                "secret-value",
                                                                "callbackUrl",
                                                                "https://example.test/?secret=hidden"))),
                                        new TextPart("private Bearer abc123")))
                        .build();
        String text = formatter.format(message).text();
        assertFalse(text.contains("secret-value"));
        assertFalse(text.contains("abc123"));
        assertFalse(text.contains("private"));
        assertFalse(text.contains("https://example.test"));
        assertTrue(text.contains("[redacted]"));
        assertTrue(text.contains("[custom]"));
    }

    @Test
    void truncatesOnUtf8BoundaryIncludingMarker() {
        ProtocolPayloadFormatter.Result result =
                new ProtocolPayloadFormatter(30, value -> value).format("😀".repeat(40));
        assertTrue(result.truncated());
        assertTrue(result.text().endsWith(" [truncated]"));
        assertTrue(result.text().getBytes(StandardCharsets.UTF_8).length <= 30);
        assertFalse(result.text().contains("�"));
    }

    @Test
    void omitsEmbeddedFileContent() {
        Message message =
                Message.builder()
                        .messageId("id")
                        .role(Message.Role.ROLE_USER)
                        .parts(
                                List.of(
                                        new FilePart(
                                                new FileWithBytes(
                                                        "text/plain", "file", "c2VjcmV0"))))
                        .build();
        String text = new ProtocolPayloadFormatter(4096, value -> value).format(message).text();
        assertFalse(text.contains("c2VjcmV0"));
        assertTrue(text.contains("[omitted]"));
    }

    @Test
    void largeTextIsNotPassedToSanitizer() {
        AtomicInteger largestInput = new AtomicInteger();
        ProtocolPayloadFormatter formatter =
                new ProtocolPayloadFormatter(
                        128,
                        value -> {
                            largestInput.accumulateAndGet(value.length(), Math::max);
                            return value;
                        });
        ProtocolPayloadFormatter.Result result =
                formatter.format(new TextPart("x".repeat(1_000_000)));
        assertTrue(result.truncated());
        assertEquals(-1, result.projectedBytes());
        assertTrue(result.text().contains("x"));
        assertTrue(result.text().getBytes(StandardCharsets.UTF_8).length <= 128);
        assertTrue(largestInput.get() <= 128);
    }

    @Test
    void nestedDataTraversalHasGlobalLimit() {
        AtomicInteger visited = new AtomicInteger();
        List<Object> many =
                new AbstractList<>() {
                    @Override
                    public Object get(int index) {
                        visited.incrementAndGet();
                        return Map.of("children", List.of("value", "value", "value"));
                    }

                    @Override
                    public int size() {
                        return 100;
                    }
                };
        DataPart part = new DataPart(Map.of("items", many));
        visited.set(0);
        ProtocolPayloadFormatter.Result result =
                new ProtocolPayloadFormatter(128, value -> value).format(part);
        assertTrue(result.truncated());
        assertEquals(-1, result.projectedBytes());
        assertTrue(visited.get() < 100);
        assertTrue(result.text().getBytes(StandardCharsets.UTF_8).length <= 128);
    }

    @Test
    void chargesIdentitySanitizerOnlyOnceUsingUtf8Bytes() {
        ProtocolPayloadFormatter.Result result =
                new ProtocolPayloadFormatter(4096, value -> value).format("é".repeat(1500));
        assertFalse(result.truncated());
        assertEquals(
                result.text().getBytes(StandardCharsets.UTF_8).length, result.projectedBytes());
    }

    @Test
    void redactsCredentialsEmbeddedInQueryStrings() {
        String text =
                new ProtocolPayloadFormatter(4096, value -> value)
                        .format(
                                new DataPart(
                                        Map.of(
                                                "endpoint",
                                                "https://example.test/path?token=abc&key=def")))
                        .text();
        assertFalse(text.contains("abc"));
        assertFalse(text.contains("def"));
        assertTrue(text.contains("[redacted]"));
    }

    @Test
    void rejectsNullSanitizerResult() {
        // Objects.requireNonNull throws NullPointerException for a null sanitizer return;
        // this is more precise than IllegalArgumentException, which implies a bad caller argument.
        ProtocolPayloadFormatter formatter = new ProtocolPayloadFormatter(4096, value -> null);
        assertThrows(NullPointerException.class, () -> formatter.format("value"));
    }
}
