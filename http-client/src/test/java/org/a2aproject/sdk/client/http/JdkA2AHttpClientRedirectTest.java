package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.http.HttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

public class JdkA2AHttpClientRedirectTest extends AbstractA2AHttpClientRedirectTest {

    private ClientAndServer server;

    @AfterEach
    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Override
    protected A2AHttpClient createClient() {
        return new JdkA2AHttpClient();
    }

    @Test
    public void getDoesNotFollowNativeRedirectsEvenWithRedirectFollowingClient() throws Exception {
        server = ClientAndServer.startClientAndServer(0);
        server.when(request().withMethod("GET").withPath("/agent"))
                .respond(
                        response()
                                .withStatusCode(302)
                                .withHeader("Location", "http://127.0.0.1:1/elsewhere"));

        JdkA2AHttpClient client =
                new JdkA2AHttpClient(
                        HttpClient.newBuilder()
                                .followRedirects(HttpClient.Redirect.NORMAL)
                                .build());

        A2AHttpResponse result =
                client.createGet()
                        .url("http://127.0.0.1:" + server.getLocalPort() + "/agent")
                        .get();

        assertEquals(
                302,
                result.status(),
                "GET must not follow redirects even when the underlying HttpClient is configured to"
                        + " do so");
    }

    @Test
    public void deleteDoesNotFollowNativeRedirectsEvenWithRedirectFollowingClient()
            throws Exception {
        server = ClientAndServer.startClientAndServer(0);
        server.when(request().withMethod("DELETE").withPath("/task"))
                .respond(
                        response()
                                .withStatusCode(302)
                                .withHeader("Location", "http://127.0.0.1:1/elsewhere"));

        JdkA2AHttpClient client =
                new JdkA2AHttpClient(
                        HttpClient.newBuilder()
                                .followRedirects(HttpClient.Redirect.NORMAL)
                                .build());

        A2AHttpResponse result =
                client.createDelete()
                        .url("http://127.0.0.1:" + server.getLocalPort() + "/task")
                        .delete();

        assertEquals(
                302,
                result.status(),
                "DELETE must not follow redirects even when the underlying HttpClient is configured"
                        + " to do so");
    }

    @Test
    public void crossOriginRedirectUsesSharedHeaderPolicy() throws Exception {
        startRedirectPair();

        A2AHttpResponse result =
                createClient()
                        .createPost()
                        .url("http://127.0.0.1:" + getRedirector().getLocalPort() + "/agent")
                        .addHeader("Authorization", "Bearer secret")
                        .addHeader("X-Custom-Token", "secret")
                        .addHeader("A2A-Version", "1.0")
                        .body("{}")
                        .followRedirects(true)
                        .post();

        assertEquals(200, result.status());
        org.mockserver.model.HttpRequest[] recorded =
                getCollector().retrieveRecordedRequests(request().withPath("/collect"));
        assertEquals(1, recorded.length);
        assertHeaderAbsent(recorded[0], "Authorization");
        assertHeaderAbsent(recorded[0], "X-Custom-Token");
        assertEquals("1.0", recorded[0].getFirstHeader("A2A-Version"));
    }

    @Test
    public void rejectsCallerConfiguredNativeRedirectsWhenFollowingIsEnabled() {
        JdkA2AHttpClient client =
                new JdkA2AHttpClient(
                        HttpClient.newBuilder()
                                .followRedirects(HttpClient.Redirect.ALWAYS)
                                .build());

        assertThrows(
                IOException.class,
                () ->
                        client.createPost()
                                .url("http://127.0.0.1:1/agent")
                                .followRedirects(true)
                                .post());
    }

    private static void assertHeaderAbsent(org.mockserver.model.HttpRequest request, String name) {
        String value = request.getFirstHeader(name);
        assertTrue(value == null || value.isEmpty(), name + " must not be forwarded");
    }
}
