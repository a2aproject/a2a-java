package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

public abstract class AbstractA2AHttpClientRedirectTest {

    private ClientAndServer redirector;
    private ClientAndServer collector;

    protected abstract A2AHttpClient createClient();

    /**
     * Creates the client used by {@link #synchronousPostFollowsRedirectWhenExplicitlyEnabled}.
     * Override when the client under test requires additional configuration to honour the
     * {@code followRedirects(true)} flag (e.g. JDK client needs a redirect-capable HttpClient).
     * Defaults to {@link #createClient()}.
     */
    protected A2AHttpClient createRedirectCapableClient() {
        return createClient();
    }

    @AfterEach
    public void tearDown() {
        if (redirector != null) {
            redirector.stop();
        }
        if (collector != null) {
            collector.stop();
        }
    }

    @Test
    public void synchronousPostDoesNotFollowRedirectByDefault() throws Exception {
        startRedirectPair();

        A2AHttpResponse result = createClient().createPost()
                .url("http://127.0.0.1:" + redirector.getLocalPort() + "/agent")
                .addHeader("X-API-Key", "FULCRUM-SYNTHETIC-DEFAULT")
                .body("{}")
                .post();

        assertEquals(302, result.status(),
                "Synchronous POST must not follow redirects by default");
        collector.verify(request().withPath("/collect"), exactly(0));
    }

    @Test
    public void synchronousPostFollowsRedirectWhenExplicitlyEnabled() throws Exception {
        startRedirectPair();

        A2AHttpResponse result = createRedirectCapableClient().createPost()
                .url("http://127.0.0.1:" + redirector.getLocalPort() + "/agent")
                .addHeader("X-API-Key", "FULCRUM-SYNTHETIC-EXPLICIT")
                .body("{}")
                .followRedirects(true)
                .post();

        assertEquals(200, result.status(),
                "Synchronous POST must follow redirect when explicitly enabled");
        collector.verify(request().withPath("/collect"), exactly(1));
    }

    protected void startRedirectPair() {
        redirector = ClientAndServer.startClientAndServer(0);
        collector = ClientAndServer.startClientAndServer(0);

        redirector.when(request().withMethod("POST").withPath("/agent"))
                .respond(response()
                        .withStatusCode(302)
                        .withHeader("Location",
                                "http://127.0.0.1:" + collector.getLocalPort() + "/collect"));
        collector.when(request().withPath("/collect"))
                .respond(response().withStatusCode(200).withBody("redirected"));
    }

    protected ClientAndServer getCollector() {
        return collector;
    }

    protected ClientAndServer getRedirector() {
        return redirector;
    }
}
