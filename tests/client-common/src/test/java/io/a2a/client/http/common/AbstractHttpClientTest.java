package io.a2a.client.http.common;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.a2a.client.http.HttpClientBuilder;
import io.a2a.client.http.HttpResponse;
import io.a2a.client.http.sse.Event;
import org.junit.jupiter.api.*;

import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractHttpClientTest {

    private static final String AGENT_CARD_PATH = "/.well-known/agent-card.json";

    private WireMockServer server;

    @BeforeEach
    public void setUp() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();

        configureFor("localhost", server.port());
    }

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    protected abstract HttpClientBuilder getHttpClientBuilder();

    private String getServerUrl() {
        return "http://localhost:" + server.port();
    }

    /**
     * This test is disabled until we can make the http-client layer fully async
     */
    @Test
    @Disabled
    public void testGetWithBodyResponse() throws Exception {
        givenThat(get(urlPathEqualTo(AGENT_CARD_PATH))
                .willReturn(okForContentType("application/json", JsonMessages.AGENT_CARD)));

        CountDownLatch latch = new CountDownLatch(1);
        getHttpClientBuilder()
                .create(getServerUrl())
                .get(AGENT_CARD_PATH)
                .send()
                .thenAccept(new Consumer<HttpResponse>() {
                    @Override
                    public void accept(HttpResponse httpResponse) {
                        String body = httpResponse.body();

                        Assertions.assertEquals(JsonMessages.AGENT_CARD, body);
                        latch.countDown();
                    }
                });

        boolean dataReceived = latch.await(5, TimeUnit.SECONDS);
        assertTrue(dataReceived);

    }

    @Test
    public void testA2AClientSendStreamingMessage() throws Exception {
        String eventStream =
                JsonStreamingMessages.SEND_MESSAGE_STREAMING_TEST_RESPONSE +
                        JsonStreamingMessages.TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE;

        givenThat(post(urlPathEqualTo("/"))
                .willReturn(okForContentType("text/event-stream", eventStream)));

        CountDownLatch latch = new CountDownLatch(2);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        getHttpClientBuilder()
                .create(getServerUrl())
                .post("/")
                .send()
                .thenAccept(new Consumer<HttpResponse>() {
                    @Override
                    public void accept(HttpResponse httpResponse) {
                        httpResponse.bodyAsSse(new Consumer<Event>() {
                            @Override
                            public void accept(Event event) {
                                System.out.println(event);
                                latch.countDown();
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) {
                                errorRef.set(throwable);
                                latch.countDown();
                            }
                        });
                    }
                });

        boolean dataReceived = latch.await(5, TimeUnit.SECONDS);
        assertTrue(dataReceived);
        assertNull(errorRef.get(), "Should not receive errors during SSE stream");
    }

    @Test
    public void testUnauthorizedClient_post() throws Exception {
        givenThat(post(urlPathEqualTo("/"))
                .willReturn(aResponse().withStatus(HttpURLConnection.HTTP_UNAUTHORIZED)));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicReference<HttpResponse> responseRef = new AtomicReference<>();

        getHttpClientBuilder()
                // Enforce that the client will be receiving the SSE stream into multiple chunks
                // .options(new HttpClientOptions().setMaxChunkSize(24))
                .create(getServerUrl())
                .post("/")
                .send()
                .whenComplete(new BiConsumer<HttpResponse, Throwable>() {
                    @Override
                    public void accept(HttpResponse httpResponse, Throwable throwable) {
                        if (throwable != null) {
                            errorRef.set(throwable);
                        }

                        if (httpResponse != null) {
                            responseRef.set(httpResponse);
                        }

                        latch.countDown();
                    }
                });

        boolean callCompleted = latch.await(5, TimeUnit.SECONDS);
        assertTrue(callCompleted);
        assertNull(responseRef.get(), "Should not receive response when unauthorized");
        assertNotNull(errorRef.get(), "Should not receive errors during SSE stream");
    }

    @Test
    public void testUnauthorizedClient_get() throws Exception {
        givenThat(get(urlPathEqualTo("/"))
                .willReturn(aResponse().withStatus(HttpURLConnection.HTTP_UNAUTHORIZED)));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicReference<HttpResponse> responseRef = new AtomicReference<>();

        getHttpClientBuilder()
                // Enforce that the client will be receiving the SSE stream into multiple chunks
                // .options(new HttpClientOptions().setMaxChunkSize(24))
                .create(getServerUrl())
                .get("/")
                .send()
                .whenComplete(new BiConsumer<HttpResponse, Throwable>() {
                    @Override
                    public void accept(HttpResponse httpResponse, Throwable throwable) {
                        if (throwable != null) {
                            errorRef.set(throwable);
                        }

                        if (httpResponse != null) {
                            responseRef.set(httpResponse);
                        }

                        latch.countDown();
                    }
                });

        boolean callCompleted = latch.await(5, TimeUnit.SECONDS);
        assertTrue(callCompleted);
        assertNull(responseRef.get(), "Should not receive response when unauthorized");
        assertNotNull(errorRef.get(), "Should not receive errors during SSE stream");
    }
}
