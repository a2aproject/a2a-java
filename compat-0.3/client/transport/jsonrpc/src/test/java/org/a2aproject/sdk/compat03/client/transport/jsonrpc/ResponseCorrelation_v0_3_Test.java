package org.a2aproject.sdk.compat03.client.transport.jsonrpc;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.sse.SSEEventListener_v0_3;
import org.junit.jupiter.api.Test;

class ResponseCorrelation_v0_3_Test {
    @Test void distinguishesNullErrorFromMissingAndWrongId() {
        var expected = JsonParser.parseString("\"id-1\"");
        for (String json : new String[] {
                "{\"id\":\"id-1\",\"result\":{}}",
                "{\"id\":null,\"error\":{\"code\":-32700}}" }) {
            assertDoesNotThrow(() -> ResponseCorrelation_v0_3.validate(JsonParser.parseString(json).getAsJsonObject(), expected));
        }
        for (String json : new String[] {
                "{\"result\":{}}", "{\"error\":{\"code\":-32700}}", "{\"id\":null,\"result\":{}}",
                "{\"id\":1,\"error\":{\"code\":-32700}}", "{\"id\":\"wrong\",\"result\":{}}" }) {
            assertThrows(IllegalArgumentException.class,
                    () -> ResponseCorrelation_v0_3.validate(JsonParser.parseString(json).getAsJsonObject(), expected));
        }
    }
    @Test void streamMismatchSignalsOnceAndClosesBeforeLateEvents() {
        var events = new AtomicInteger();
        var errors = new AtomicReference<Throwable>();
        var future = new CompletableFuture<Void>();
        var listener = new SSEEventListener_v0_3(e -> events.incrementAndGet(), errors::set,
                JsonParser.parseString("\"expected\""));
        listener.onMessage("{\"jsonrpc\":\"2.0\",\"id\":\"wrong\",\"result\":{}}", future);
        listener.onMessage("{\"jsonrpc\":\"2.0\",\"id\":\"expected\",\"result\":{}}", future);
        assertInstanceOf(IllegalArgumentException.class, errors.get());
        assertEquals(0, events.get());
        assertTrue(future.isCancelled());
    }
    @Test void nullIdErrorAlsoClosesTheStream() {
        var error = new AtomicReference<Throwable>();
        var future = new CompletableFuture<Void>();
        var listener = new SSEEventListener_v0_3(e -> fail("unexpected event"), error::set,
                JsonParser.parseString("\"expected\""));
        listener.onMessage("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"rejected\"}}", future);
        assertNotNull(error.get());
        assertTrue(future.isCancelled());
    }
}
