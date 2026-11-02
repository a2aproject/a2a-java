package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;

import java.io.IOException;
import java.net.http.HttpClient;

import org.junit.jupiter.api.Test;

public class JdkA2AHttpClientRedirectTest extends AbstractA2AHttpClientRedirectTest {

    @Override
    protected A2AHttpClient createClient() {
        return new JdkA2AHttpClient();
    }

    @Test
    public void crossOriginRedirectUsesSharedHeaderPolicy() throws Exception {
        startRedirectPair();

        A2AHttpResponse result = createClient().createPost()
                .url("http://127.0.0.1:" + getRedirector().getLocalPort() + "/agent")
                .addHeader("Authorization", "Bearer secret")
                .addHeader("X-Custom-Token", "secret")
                .addHeader("A2A-Version", "1.0")
                .body("{}")
                .followRedirects(true)
                .post();

        assertEquals(200, result.status());
        org.mockserver.model.HttpRequest[] recorded = getCollector().retrieveRecordedRequests(
                request().withPath("/collect"));
        assertEquals(1, recorded.length);
        assertHeaderAbsent(recorded[0], "Authorization");
        assertHeaderAbsent(recorded[0], "X-Custom-Token");
        assertEquals("1.0", recorded[0].getFirstHeader("A2A-Version"));
    }

    @Test
    public void rejectsCallerConfiguredNativeRedirectsWhenFollowingIsEnabled() {
        JdkA2AHttpClient client = new JdkA2AHttpClient(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build());

        assertThrows(IOException.class, () -> client.createPost()
                .url("http://127.0.0.1:1/agent")
                .followRedirects(true)
                .post());
    }

    private static void assertHeaderAbsent(org.mockserver.model.HttpRequest request, String name) {
        String value = request.getFirstHeader(name);
        assertTrue(value == null || value.isEmpty(), name + " must not be forwarded");
    }
}
