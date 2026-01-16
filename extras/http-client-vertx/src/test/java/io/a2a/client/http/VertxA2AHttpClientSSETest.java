package io.a2a.client.http;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import io.a2a.common.A2AErrorMessages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class VertxA2AHttpClientSSETest {

    private ClientAndServer mockServer;
    private VertxA2AHttpClient client;

    @BeforeEach
    public void setup() {
        mockServer = ClientAndServer.startClientAndServer(0);  // Use random port
        client = new VertxA2AHttpClient();
    }

    @AfterEach
    public void teardown() {
        if (client != null) {
            client.close();
        }
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

        CompletableFuture<Void> future = client.createGet()
                .url(getBaseUrl() + "/sse")
                .getAsyncSSE(
                        events::add,
                        error::set,
                        latch::countDown
                );

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Expected completion handler to be called");
        assertNull(error.get(), "Expected no errors");
        assertFalse(events.isEmpty(), "Expected to receive events");
        assertTrue(events.contains("event1"), "Expected event1");
        assertTrue(events.contains("event2"), "Expected event2");
        assertTrue(events.contains("event3"), "Expected event3");
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

        CompletableFuture<Void> future = client.createPost()
                .url(getBaseUrl() + "/sse")
                .body("{\"subscribe\":true}")
                .postAsyncSSE(
                        events::add,
                        error::set,
                        latch::countDown
                );

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Expected completion handler to be called");
        assertNull(error.get(), "Expected no errors");
        assertFalse(events.isEmpty(), "Expected to receive events");
        assertTrue(events.contains("message1"), "Expected message1");
        assertTrue(events.contains("message2"), "Expected message2");
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

        CompletableFuture<Void> future = client.createGet()
                .url(getBaseUrl() + "/sse")
                .getAsyncSSE(
                        events::add,
                        error::set,
                        latch::countDown
                );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(error.get());
        assertTrue(events.contains("content here"), "Should have stripped 'data: ' prefix");
        assertTrue(events.contains("no space"), "Should handle 'data:' without space");
        assertTrue(events.contains("extra spaces"), "Should trim whitespace");
    }

    @Test
    public void testSSEAuthenticationError() throws Exception {
        mockServer
                .when(request().withMethod("GET").withPath("/sse"))
                .respond(response().withStatusCode(401));

        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        CompletableFuture<Void> future = client.createGet()
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
        assertTrue(error.get().getMessage().contains("Authentication failed"),
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

        CompletableFuture<Void> future = client.createGet()
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
        assertTrue(error.get().getMessage().contains("Authorization failed"),
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

        CompletableFuture<Void> future = client.createGet()
                .url(getBaseUrl() + "/sse")
                .getAsyncSSE(
                        events::add,
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

        CompletableFuture<Void> future = client.createGet()
                .url(getBaseUrl() + "/sse")
                .addHeader("Authorization", "Bearer token")
                .getAsyncSSE(
                        events::add,
                        error::set,
                        latch::countDown
                );

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(error.get());
        assertTrue(events.contains("authenticated"));
    }
}
