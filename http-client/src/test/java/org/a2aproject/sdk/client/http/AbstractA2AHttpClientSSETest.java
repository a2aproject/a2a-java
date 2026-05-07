package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.a2aproject.sdk.common.A2AErrorMessages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractA2AHttpClientSSETest {

    private ClientAndServer mockServer;
    private A2AHttpClient client;

    protected abstract A2AHttpClient createClient();

    @BeforeEach
    public void setup() {
        mockServer = ClientAndServer.startClientAndServer(0);
        client = createClient();
    }

    @AfterEach
    public void teardown() {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    private String getBaseUrl() {
        return "http://localhost:" + mockServer.getPort();
    }

    @Test
    public void testGetAsyncSSE() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/sse"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("data: event1\n\ndata: event2\n\ndata: event3\n\n"));

        CountDownLatch latch = new CountDownLatch(1);
        List<String> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.createGet()
                .url(getBaseUrl() + "/sse")
                .getAsyncSSE(
                        event -> events.add(event.data()),
                        error::set,
                        latch::countDown
                );

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Expected completion handler to be called");
        assertNull(error.get(), "Expected no errors");
        assertEquals(3, events.size(), "Expected to receive 3 events");
        assertTrue(events.contains("event1"));
        assertTrue(events.contains("event2"));
        assertTrue(events.contains("event3"));
    }

    @Test
    public void testPostAsyncSSE() throws Exception {
        mockServer
                .when(request()
                        .withMethod("POST")
                        .withPath("/sse")
                        .withBody("{\"subscribe\":true}"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("data: message1\n\ndata: message2\n\n"));

        CountDownLatch latch = new CountDownLatch(1);
        List<String> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.createPost()
                .url(getBaseUrl() + "/sse")
                .body("{\"subscribe\":true}")
                .postAsyncSSE(
                        event -> events.add(event.data()),
                        error::set,
                        latch::countDown
                );

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Expected completion handler to be called");
        assertNull(error.get(), "Expected no errors");
        assertEquals(2, events.size(), "Expected to receive 2 events");
        assertTrue(events.contains("message1"));
        assertTrue(events.contains("message2"));
    }

    @Test
    public void testSSEDataPrefixStripping() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/sse"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("data: content here\n\ndata:no space\n\ndata:  extra spaces  \n\n"));

        CountDownLatch latch = new CountDownLatch(1);
        List<String> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.createGet()
                .url(getBaseUrl() + "/sse")
                .getAsyncSSE(
                        event -> events.add(event.data()),
                        error::set,
                        latch::countDown
                );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(error.get());
        assertTrue(events.contains("content here"), "Should have stripped 'data: ' prefix");
        assertTrue(events.contains("no space"), "Should handle 'data:' without space");
        // SSE spec: only first space after colon is removed, rest is preserved
        assertTrue(events.contains(" extra spaces  "), "Should preserve whitespace after first space");
    }

    @Test
    public void testSSEAuthenticationError() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/sse"))
                .respond(response().withStatusCode(401));

        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        client.createGet()
                .url(getBaseUrl() + "/sse")
                .getAsyncSSE(
                        msg -> {},
                        e -> {
                            error.set(e);
                            errorLatch.countDown();
                        },
                        () -> completed.set(true)
                );

        assertTrue(errorLatch.await(5, TimeUnit.SECONDS), "Expected error handler to be called");
        assertNotNull(error.get(), "Expected an error");
        assertTrue(error.get() instanceof IOException, "Expected IOException");
        assertTrue(error.get().getMessage().contains(A2AErrorMessages.AUTHENTICATION_FAILED),
            "Expected authentication error message but got: " + error.get().getMessage());
        assertFalse(completed.get(), "Should not call completion handler on error");
    }

    @Test
    public void testSSEAuthorizationError() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/sse"))
                .respond(response().withStatusCode(403));

        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        client.createGet()
                .url(getBaseUrl() + "/sse")
                .getAsyncSSE(
                        msg -> {},
                        e -> {
                            error.set(e);
                            errorLatch.countDown();
                        },
                        () -> completed.set(true)
                );

        assertTrue(errorLatch.await(5, TimeUnit.SECONDS), "Expected error handler to be called");
        assertNotNull(error.get(), "Expected an error");
        assertTrue(error.get() instanceof IOException, "Expected IOException");
        assertTrue(error.get().getMessage().contains(A2AErrorMessages.AUTHORIZATION_FAILED),
            "Expected authorization error message but got: " + error.get().getMessage());
        assertFalse(completed.get(), "Should not call completion handler on error");
    }

    @Test
    public void testSSEEmptyLinesIgnored() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/sse"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("data: first\n\n\n\ndata: second\n\ndata: \n\ndata: third\n\n"));

        CountDownLatch latch = new CountDownLatch(1);
        List<String> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.createGet()
                .url(getBaseUrl() + "/sse")
                .getAsyncSSE(
                        event -> events.add(event.data()),
                        error::set,
                        latch::countDown
                );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(error.get());
        assertEquals(3, events.size(), "Should have received 3 non-empty events");
        assertTrue(events.contains("first"));
        assertTrue(events.contains("second"));
        assertTrue(events.contains("third"));
    }

    @Test
    public void testSSEHeaderPropagation() throws Exception {
        mockServer
                .when(request()
                        .withMethod("GET")
                        .withPath("/sse")
                        .withHeader("Accept", "text/event-stream")
                        .withHeader("Authorization", "Bearer token"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("data: authenticated\n\n"));

        CountDownLatch latch = new CountDownLatch(1);
        List<String> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.createGet()
                .url(getBaseUrl() + "/sse")
                .addHeader("Authorization", "Bearer token")
                .getAsyncSSE(
                        event -> events.add(event.data()),
                        error::set,
                        latch::countDown
                );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(error.get());
        assertTrue(events.contains("authenticated"));
    }

    @Test
    public void testSSETypedEvents() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/sse"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("event: update\ndata: payload1\n\nevent: delete\ndata: payload2\n\ndata: no-type\n\n"));

        CountDownLatch latch = new CountDownLatch(1);
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.createGet()
                .url(getBaseUrl() + "/sse")
                .getAsyncSSE(events::add, error::set, latch::countDown);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(error.get());
        assertEquals(3, events.size());
        assertEquals("update", events.get(0).eventType());
        assertEquals("payload1", events.get(0).data());
        assertEquals("delete", events.get(1).eventType());
        assertEquals("payload2", events.get(1).data());
        assertEquals("message", events.get(2).eventType(), "Event without 'event:' field should default to 'message'");
    }

    @Test
    public void testSSEEventIdAndLastEventId() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/sse"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("id: 42\ndata: identified\n\ndata: no-id\n\n"));

        CountDownLatch latch = new CountDownLatch(1);
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.createGet()
                .url(getBaseUrl() + "/sse")
                .getAsyncSSE(events::add, error::set, latch::countDown);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(error.get());
        assertEquals(2, events.size());
        assertEquals("42", events.get(0).id());
        assertEquals("42", events.get(1).id(), "Event without 'id:' field inherits last event ID per SSE spec");
    }

    @Test
    public void testSSEMultiLineData() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/sse"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("data: line1\ndata: line2\ndata: line3\n\n"));

        CountDownLatch latch = new CountDownLatch(1);
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.createGet()
                .url(getBaseUrl() + "/sse")
                .getAsyncSSE(events::add, error::set, latch::countDown);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(error.get());
        assertEquals(1, events.size());
        assertEquals("line1\nline2\nline3", events.get(0).data());
    }

    @Test
    public void testSSEStreamEndingWithoutTrailingEmptyLine() throws Exception {
        // Stream ends mid-event with no terminal empty line — flush() must dispatch the buffered data
        mockServer
                .when(request().withMethod("GET").withPath("/sse"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("data: flushed-event"));

        CountDownLatch latch = new CountDownLatch(1);
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.createGet()
                .url(getBaseUrl() + "/sse")
                .getAsyncSSE(events::add, error::set, latch::countDown);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(error.get());
        assertEquals(1, events.size(), "Buffered event must be dispatched on stream end");
        assertEquals("flushed-event", events.get(0).data());
    }

    @Test
    public void testPostSSETypedEvents() throws Exception {
        mockServer
                .when(request().withMethod("POST").withPath("/sse"))
                .respond(response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody("event: result\nid: 99\ndata: done\n\n"));

        CountDownLatch latch = new CountDownLatch(1);
        List<ServerSentEvent> events = new ArrayList<>();
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.createPost()
                .url(getBaseUrl() + "/sse")
                .body("{}")
                .postAsyncSSE(events::add, error::set, latch::countDown);

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(error.get());
        assertEquals(1, events.size());
        assertEquals("result", events.get(0).eventType());
        assertEquals("99", events.get(0).id());
        assertEquals("done", events.get(0).data());
    }
}
