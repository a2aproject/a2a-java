package org.a2aproject.sdk.client.http.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.http.AbstractA2AHttpClientRedirectTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

public class VertxA2AHttpClientRedirectTest extends AbstractA2AHttpClientRedirectTest {

    private ClientAndServer relativeServer;

    @Override
    protected A2AHttpClient createClient() {
        return new VertxA2AHttpClient();
    }

    @AfterEach
    public void stopRelativeServer() {
        if (relativeServer != null) {
            relativeServer.stop();
        }
    }

    /**
     * A relative {@code Location} value (permitted by RFC 9110 10.2.2, resolved per RFC 3986 5)
     * must be resolved against the original request URI and followed, rather than handed verbatim
     * to an absolute request where it fails. The redirect target is served by the same origin so
     * that {@code /collect} only resolves correctly when the base URI is applied.
     *
     * <p>Regression test for a2aproject/a2a-java#1142.
     */
    @Test
    public void synchronousPostFollowsRelativeRedirectLocation() throws Exception {
        relativeServer = ClientAndServer.startClientAndServer(0);
        relativeServer.when(request().withMethod("POST").withPath("/agent"))
                .respond(response().withStatusCode(302).withHeader("Location", "/collect"));
        relativeServer.when(request().withPath("/collect"))
                .respond(response().withStatusCode(200).withBody("redirected"));

        A2AHttpResponse result = createClient().createPost()
                .url("http://127.0.0.1:" + relativeServer.getLocalPort() + "/agent")
                .addHeader("X-API-Key", "FULCRUM-SYNTHETIC-RELATIVE")
                .body("{}")
                .followRedirects(true)
                .post();

        assertEquals(200, result.status(),
                "Synchronous POST must resolve and follow a relative Location against the request URI");
        relativeServer.verify(request().withPath("/collect"), exactly(1));
    }
}
