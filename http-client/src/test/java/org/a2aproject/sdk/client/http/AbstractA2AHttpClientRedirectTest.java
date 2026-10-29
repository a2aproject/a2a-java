package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.integration.ClientAndServer;

public abstract class AbstractA2AHttpClientRedirectTest {

    private ClientAndServer redirector;
    private ClientAndServer collector;

    protected abstract A2AHttpClient createClient();

    /**
     * Creates the client used by {@link #synchronousPostFollowsRedirectWhenExplicitlyEnabled}.
     * Override when the client under test requires additional configuration to honour the
     * {@code followRedirects(true)} flag.
     * Defaults to {@link #createClient()}.
     */
    protected A2AHttpClient createRedirectCapableClient() {
        return createClient();
    }

    protected boolean followsCrossOriginPostToGetRedirects() {
        return true;
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
    public void synchronousGetDoesNotFollowRedirectByDefault() throws Exception {
        redirector = ClientAndServer.startClientAndServer(0);
        redirector.when(request().withMethod("GET").withPath("/agent"))
                .respond(response()
                        .withStatusCode(302)
                        .withHeader("Location", "/target"));
        redirector.when(request().withMethod("GET").withPath("/target"))
                .respond(response().withStatusCode(200).withBody("redirected"));

        A2AHttpResponse result = createClient().createGet()
                .url("http://127.0.0.1:" + redirector.getLocalPort() + "/agent")
                .get();

        assertEquals(302, result.status(),
                "Synchronous GET must not follow redirects by default");
        redirector.verify(request().withPath("/target"), exactly(0));
    }

    @Test
    public void synchronousGetFollowsRedirectWhenExplicitlyEnabled() throws Exception {
        redirector = ClientAndServer.startClientAndServer(0);
        redirector.when(request().withMethod("GET").withPath("/agent"))
                .respond(response()
                        .withStatusCode(302)
                        .withHeader("Location", "/target"));
        redirector.when(request().withMethod("GET").withPath("/target"))
                .respond(response().withStatusCode(200).withBody("redirected"));

        A2AHttpResponse result = createClient().createGet()
                .url("http://127.0.0.1:" + redirector.getLocalPort() + "/agent")
                .followRedirects(true)
                .get();

        assertEquals(200, result.status(),
                "Synchronous GET must follow redirect when explicitly enabled");
        redirector.verify(request().withPath("/target"), exactly(1));
    }

    @ParameterizedTest
    @ValueSource(ints = {307, 308})
    public void synchronousGetDoesNotFollowCrossOriginMethodPreservingRedirect(int redirectStatus)
            throws Exception {
        redirector = ClientAndServer.startClientAndServer(0);
        collector = ClientAndServer.startClientAndServer(0);
        String location = "http://127.0.0.1:" + collector.getLocalPort() + "/collect";
        redirector.when(request().withMethod("GET").withPath("/agent"))
                .respond(response().withStatusCode(redirectStatus).withHeader("Location", location));
        collector.when(request().withMethod("GET").withPath("/collect"))
                .respond(response().withStatusCode(200).withBody("redirected"));

        A2AHttpResponse result = createClient().createGet()
                .url("http://127.0.0.1:" + redirector.getLocalPort() + "/agent")
                .followRedirects(true)
                .get();

        assertEquals(redirectStatus, result.status(),
                "GET 307/308 redirects are deliberately not followed across origins");
        collector.verify(request().withPath("/collect"), exactly(0));
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

    @ParameterizedTest
    @ValueSource(ints = {301, 302, 303})
    public void crossOriginPostToGetRedirectHandlesBodyAndCredentials(int redirectStatus) throws Exception {
        startRedirectPair(redirectStatus);

        A2AHttpResponse result = createRedirectCapableClient().createPost()
                .url("http://127.0.0.1:" + redirector.getLocalPort() + "/agent")
                .addHeader("Authorization", "Bearer secret")
                .addHeader("X-API-Key", "secret")
                .addHeader("X-Custom-Token", "secret")
                .addHeader("A2A-Version", "1.0")
                .body("{\"token\":\"secret\"}")
                .followRedirects(true)
                .post();

        if (!followsCrossOriginPostToGetRedirects()) {
            assertEquals(redirectStatus, result.status());
            collector.verify(request().withPath("/collect"), exactly(0));
            return;
        }

        assertEquals(200, result.status());
        org.mockserver.model.HttpRequest[] recorded = collector.retrieveRecordedRequests(
                request().withPath("/collect"));
        assertEquals(1, recorded.length);
        assertEquals("GET", recorded[0].getMethod().getValue());
        String redirectedBody = recorded[0].getBodyAsString();
        assertTrue(redirectedBody == null || redirectedBody.isEmpty());
        assertHeaderAbsent(recorded[0], "Authorization");
        assertHeaderAbsent(recorded[0], "X-API-Key");
        assertHeaderAbsent(recorded[0], "X-Custom-Token");
        assertEquals("1.0", recorded[0].getFirstHeader("A2A-Version"));
    }

    @ParameterizedTest
    @ValueSource(ints = {307, 308})
    public void synchronousPostDoesNotFollowCrossOriginMethodPreservingRedirect(int redirectStatus)
            throws Exception {
        startRedirectPair(redirectStatus);

        A2AHttpResponse result = createClient().createPost()
                .url("http://127.0.0.1:" + redirector.getLocalPort() + "/agent")
                .body("{\"token\":\"secret\"}")
                .followRedirects(true)
                .post();

        assertEquals(redirectStatus, result.status(),
                "Cross-origin method-preserving redirects must not forward the POST body");
        collector.verify(request().withPath("/collect"), exactly(0));
    }

    @Test
    public void synchronousPostStopsAfterSameOrigin307ThenCrossOrigin302() throws Exception {
        redirector = ClientAndServer.startClientAndServer(0);
        collector = ClientAndServer.startClientAndServer(0);
        String secondLocation = "http://127.0.0.1:" + collector.getLocalPort() + "/collect";
        redirector.when(request().withMethod("POST").withPath("/agent"))
                .respond(response().withStatusCode(307).withHeader("Location", "/same-origin"));
        redirector.when(request().withMethod("POST").withPath("/same-origin"))
                .respond(response().withStatusCode(302).withHeader("Location", secondLocation));
        collector.when(request().withPath("/collect"))
                .respond(response().withStatusCode(200).withBody("redirected"));

        String body = "{\"token\":\"secret\"}";
        A2AHttpResponse result = createRedirectCapableClient().createPost()
                .url("http://127.0.0.1:" + redirector.getLocalPort() + "/agent")
                .body(body)
                .followRedirects(true)
                .post();

        assertEquals(302, result.status());
        assertEquals(secondLocation, result.headers().firstValue("Location"));
        org.mockserver.model.HttpRequest[] secondHop = redirector.retrieveRecordedRequests(
                request().withPath("/same-origin"));
        assertEquals(1, secondHop.length);
        assertEquals("POST", secondHop[0].getMethod().getValue());
        assertEquals(JsonParser.parseString(body), JsonParser.parseString(secondHop[0].getBodyAsString()));
        collector.verify(request().withPath("/collect"), exactly(0));
    }

    protected void startRedirectPair() {
        startRedirectPair(302);
    }

    private void startRedirectPair(int redirectStatus) {
        redirector = ClientAndServer.startClientAndServer(0);
        collector = ClientAndServer.startClientAndServer(0);

        redirector.when(request().withMethod("POST").withPath("/agent"))
                .respond(response()
                        .withStatusCode(redirectStatus)
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

    private static void assertHeaderAbsent(org.mockserver.model.HttpRequest request, String name) {
        String value = request.getFirstHeader(name);
        assertTrue(value == null || value.isEmpty(), name + " must not reach a cross-origin target");
    }
}
