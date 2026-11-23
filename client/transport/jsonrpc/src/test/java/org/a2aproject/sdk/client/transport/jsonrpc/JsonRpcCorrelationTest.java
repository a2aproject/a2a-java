package org.a2aproject.sdk.client.transport.jsonrpc;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.a2aproject.sdk.client.http.ServerSentEvent;
import org.a2aproject.sdk.client.transport.jsonrpc.sse.SSEEventListener;
import org.a2aproject.sdk.spec.A2AClientException;
import org.junit.jupiter.api.Test;

class JsonRpcCorrelationTest {
    @Test void validatesResultAndErrorIds() {
        JsonRpcCorrelation.validate("{\"jsonrpc\":\"2.0\",\"id\":\"expected\",\"result\":{}}", "expected");
        JsonRpcCorrelation.validate("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32700}}", "expected");
        for (String body : new String[] {
                "{\"id\":\"other\",\"result\":{}}", "{\"id\":null,\"result\":{}}", "{\"result\":{}}",
                "{\"id\":0,\"error\":{\"code\":-32700}}", "{\"id\":{},\"result\":{}}" }) {
            assertThrows(A2AClientException.class, () -> JsonRpcCorrelation.validate(body, "expected"));
        }
    }

    @Test void extendedCardNullIdErrorIsParsed() throws Exception {
        var response = org.a2aproject.sdk.grpc.utils.JSONRPCUtils.parseResponseBody(
                "{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"invalid\"}}",
                org.a2aproject.sdk.spec.A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD);
        assertNull(response.getId());
        assertNotNull(response.getError());
    }

    @Test void httpMiddlewareErrorsRemainUntouched() {
        JsonRpcCorrelation.validateIfErrorEnvelope("{\"error\":\"proxy denied\"}", "expected");
        JsonRpcCorrelation.validateIfErrorEnvelope("not json", "expected");
        assertThrows(A2AClientException.class, () -> JsonRpcCorrelation.validateIfErrorEnvelope(
                "{\"jsonrpc\":\"2.0\",\"id\":\"wrong\",\"error\":{\"code\":-32600}}", "expected"));
    }

    @Test void serverErrorStopsSubsequentEvents() {
        var errors = new AtomicReference<Throwable>();
        var events = new AtomicInteger();
        var listener = new SSEEventListener(event -> events.incrementAndGet(), errors::set, "expected");
        var future = new CompletableFuture<Void>();
        listener.bindFuture(future);
        listener.onMessage(new ServerSentEvent("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32600,\"message\":\"invalid\"}}"), future);
        assertTrue(future.isCancelled());
        assertInstanceOf(org.a2aproject.sdk.spec.A2AError.class, errors.get());
        listener.onMessage(new ServerSentEvent("{\"jsonrpc\":\"2.0\",\"id\":\"expected\",\"result\":{}}"), future);
        assertEquals(0, events.get());
    }

    @Test void earlyMismatchCancelsFutureAndSuppressesFollowingEvents() {
        var errors = new AtomicReference<Throwable>();
        var events = new AtomicInteger();
        var listener = new SSEEventListener(event -> events.incrementAndGet(), errors::set, "expected");
        listener.onMessage(new ServerSentEvent("{\"jsonrpc\":\"2.0\",\"id\":\"other\",\"result\":{}}"), null);
        var future = new CompletableFuture<Void>();
        listener.bindFuture(future);
        assertTrue(future.isCancelled());
        assertInstanceOf(A2AClientException.class, errors.get());
        listener.onMessage(new ServerSentEvent("{\"jsonrpc\":\"2.0\",\"id\":\"expected\",\"result\":{}}"), future);
        assertEquals(0, events.get());
    }
}
