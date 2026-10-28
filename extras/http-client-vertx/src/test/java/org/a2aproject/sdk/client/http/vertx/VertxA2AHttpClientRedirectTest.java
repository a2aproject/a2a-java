package org.a2aproject.sdk.client.http.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientConfiguration;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.http.AbstractA2AHttpClientRedirectTest;
import org.a2aproject.sdk.client.http.RedirectHeaderPolicy;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.integration.ClientAndServer;

public class VertxA2AHttpClientRedirectTest extends AbstractA2AHttpClientRedirectTest {

    private ClientAndServer relativeServer;
    private ClientAndServer redirectCollector;

    @Override
    protected A2AHttpClient createClient() {
        return new VertxA2AHttpClient();
    }

    @AfterEach
    public void stopRelativeServer() {
        if (relativeServer != null) {
            relativeServer.stop();
        }
        if (redirectCollector != null) {
            redirectCollector.stop();
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

    @ParameterizedTest
    @ValueSource(ints = {301, 302, 303})
    public void absoluteCrossOriginRedirectDoesNotForwardSensitiveHeaders(int redirectStatus) throws Exception {
        assertCrossOriginRedirectDoesNotForwardSensitiveHeaders(false, redirectStatus);
    }

    @Test
    public void protocolRelativeCrossOriginRedirectDoesNotForwardSensitiveHeaders()
            throws Exception {
        assertCrossOriginRedirectDoesNotForwardSensitiveHeaders(true, 302);
    }

    @Test
    public void providerConfigurationAppliesCustomRedirectPolicy() throws Exception {
        assertProviderConfigurationAppliesCustomRedirectPolicy(true);
    }

    @Test
    public void providerConfigurationDoesNotForwardCustomHeaderToUntrustedOrigin() throws Exception {
        assertProviderConfigurationAppliesCustomRedirectPolicy(false);
    }

    private void assertProviderConfigurationAppliesCustomRedirectPolicy(boolean trustCollector) throws Exception {
        relativeServer = ClientAndServer.startClientAndServer(0);
        redirectCollector = ClientAndServer.startClientAndServer(0);
        relativeServer.when(request().withMethod("POST").withPath("/agent"))
                .respond(response().withStatusCode(302).withHeader(
                        "Location", "http://127.0.0.1:" + redirectCollector.getLocalPort() + "/collect"));
        redirectCollector.when(request().withPath("/collect"))
                .respond(response().withStatusCode(200).withBody("redirected"));

        int trustedPort = trustCollector ? redirectCollector.getLocalPort() : relativeServer.getLocalPort();
        A2AHttpClient client = new VertxA2AHttpClientProvider().create(
                A2AHttpClientConfiguration.builder()
                        .redirectHeaderPolicy(new RedirectHeaderPolicy(
                                Set.of("X-Trusted"),
                                Set.of(URI.create("http://127.0.0.1:" + trustedPort))))
                        .build());
        try {
            A2AHttpResponse result = client.createPost()
                    .url("http://127.0.0.1:" + relativeServer.getLocalPort() + "/agent")
                    .addHeader("Authorization", "Bearer secret")
                    .addHeader("X-Trusted", "allowed")
                    .body("{}")
                    .followRedirects(true)
                    .post();

            assertEquals(200, result.status());
            org.mockserver.model.HttpRequest[] requests = redirectCollector.retrieveRecordedRequests(
                    request().withPath("/collect"));
            assertEquals(1, requests.length);
            if (trustCollector) {
                assertEquals("allowed", requests[0].getFirstHeader("X-Trusted"));
            } else {
                assertHeadersAbsent(requests[0], Map.of("X-Trusted", "allowed"));
            }
            assertHeadersAbsent(requests[0], Map.of("Authorization", "Bearer secret"));
        } finally {
            ((VertxA2AHttpClient) client).close();
        }
    }

    private void assertCrossOriginRedirectDoesNotForwardSensitiveHeaders(boolean protocolRelative, int redirectStatus)
            throws Exception {
        relativeServer = ClientAndServer.startClientAndServer(0);
        redirectCollector = ClientAndServer.startClientAndServer(0);
        String target = "127.0.0.1:" + redirectCollector.getLocalPort() + "/collect";
        String location = protocolRelative ? "//" + target : "http://" + target;

        relativeServer.when(request().withMethod("POST").withPath("/agent"))
                .respond(response().withStatusCode(redirectStatus).withHeader("Location", location));
        redirectCollector.when(request().withPath("/collect"))
                .respond(response().withStatusCode(200).withBody("redirected"));

        A2AHttpResponse result = createClient().createPost()
                .url("http://127.0.0.1:" + relativeServer.getLocalPort() + "/agent")
                .addHeaders(requestHeaders())
                .body("{}")
                .followRedirects(true)
                .post();

        assertEquals(200, result.status());
        redirectCollector.verify(request().withPath("/collect"), exactly(1));
        org.mockserver.model.HttpRequest[] recorded = redirectCollector.retrieveRecordedRequests(
                request().withPath("/collect"));
        assertEquals(1, recorded.length);
        assertEquals("GET", recorded[0].getMethod().getValue());
        @Nullable String redirectedBody = recorded[0].getBodyAsString();
        assertTrue(redirectedBody == null || redirectedBody.isEmpty());
        assertHeadersAbsent(recorded[0], credentialHeaders());
        assertEntityHeadersRemoved(recorded[0]);
        assertEquals("application/json", recorded[0].getFirstHeader("aCcEpT"));
        assertEquals("1.0", recorded[0].getFirstHeader("A2A-Version"));
        assertEquals("extension-a", recorded[0].getFirstHeader("A2A-Extensions"));
    }

    @Test
    public void sameOriginRedirectRetainsSensitiveHeaders() throws Exception {
        relativeServer = ClientAndServer.startClientAndServer(0);
        relativeServer.when(request().withMethod("POST").withPath("/agent"))
                .respond(response().withStatusCode(307).withHeader("Location", "/collect"));
        relativeServer.when(request().withMethod("POST").withPath("/collect"))
                .respond(response().withStatusCode(200).withBody("redirected"));

        A2AHttpResponse result = createClient().createPost()
                .url("http://127.0.0.1:" + relativeServer.getLocalPort() + "/agent")
                .addHeaders(requestHeaders())
                .body("{}")
                .followRedirects(true)
                .post();

        assertEquals(200, result.status());
        org.mockserver.model.HttpRequest[] recorded = relativeServer.retrieveRecordedRequests(
                request().withMethod("POST").withPath("/collect"));
        assertEquals(1, recorded.length);
        assertHeadersPresent(recorded[0], requestHeaders());
    }

    @Test
    public void sameOriginPostToGetRedirectDropsEntityHeaders() throws Exception {
        relativeServer = ClientAndServer.startClientAndServer(0);
        relativeServer.when(request().withMethod("POST").withPath("/agent"))
                .respond(response().withStatusCode(302).withHeader("Location", "/collect"));
        relativeServer.when(request().withMethod("GET").withPath("/collect"))
                .respond(response().withStatusCode(200).withBody("redirected"));

        A2AHttpResponse result = createClient().createPost()
                .url("http://127.0.0.1:" + relativeServer.getLocalPort() + "/agent")
                .addHeaders(requestHeaders())
                .body("{}")
                .followRedirects(true)
                .post();

        assertEquals(200, result.status());
        org.mockserver.model.HttpRequest[] recorded = relativeServer.retrieveRecordedRequests(
                request().withMethod("GET").withPath("/collect"));
        assertEquals(1, recorded.length);
        assertHeadersPresent(recorded[0], credentialHeaders());
        assertEntityHeadersRemoved(recorded[0]);
    }

    @Test
    public void malformedRedirectLocationReturnsOriginalResponse() throws Exception {
        relativeServer = ClientAndServer.startClientAndServer(0);
        relativeServer.when(request().withMethod("POST").withPath("/agent"))
                .respond(response().withStatusCode(302).withHeader("Location", "/pa th"));

        A2AHttpResponse result = createClient().createPost()
                .url("http://127.0.0.1:" + relativeServer.getLocalPort() + "/agent")
                .body("{}")
                .followRedirects(true)
                .post();

        assertEquals(302, result.status());
    }

    private static Map<String, String> requestHeaders() {
        return Map.ofEntries(
                Map.entry("aUtHoRiZaTiOn", "Bearer redirect-test"),
                Map.entry("pRoXy-AuThOrIzAtIoN", "Basic redirect-test"),
                Map.entry("cOoKiE", "session=redirect-test"),
                Map.entry("x-aPi-kEy", "redirect-test"),
                Map.entry("aPi-kEy", "redirect-test"),
                Map.entry("x-AuTh-ToKeN", "redirect-test"),
                Map.entry("x-aUtHeNtIcAtIoN", "redirect-test"),
                Map.entry("X-Custom-Token", "redirect-test"),
                Map.entry("X-Amz-Security-Token", "redirect-test"),
                Map.entry("X-Goog-Api-Key", "redirect-test"),
                Map.entry("aCcEpT", "application/json"),
                Map.entry("A2A-Version", "1.0"),
                Map.entry("A2A-Extensions", "extension-a"),
                Map.entry("cOnTeNt-LeNgTh", "2"),
                Map.entry("cOnTeNt-TyPe", "application/json"),
                Map.entry("cOnTeNt-EnCoDiNg", "identity"),
                Map.entry("dIgEsT", "sha-256=YWJj"),
                Map.entry("lAsT-MoDiFiEd", "Wed, 21 Oct 2015 07:28:00 GMT"));
    }

    private static Map<String, String> credentialHeaders() {
        return Map.ofEntries(
                Map.entry("aUtHoRiZaTiOn", "Bearer redirect-test"),
                Map.entry("pRoXy-AuThOrIzAtIoN", "Basic redirect-test"),
                Map.entry("cOoKiE", "session=redirect-test"),
                Map.entry("x-aPi-kEy", "redirect-test"),
                Map.entry("aPi-kEy", "redirect-test"),
                Map.entry("x-AuTh-ToKeN", "redirect-test"),
                Map.entry("x-aUtHeNtIcAtIoN", "redirect-test"),
                Map.entry("X-Custom-Token", "redirect-test"),
                Map.entry("X-Amz-Security-Token", "redirect-test"),
                Map.entry("X-Goog-Api-Key", "redirect-test"));
    }

    private static Map<String, String> entityHeaders() {
        return Map.ofEntries(
                Map.entry("cOnTeNt-LeNgTh", "2"),
                Map.entry("cOnTeNt-TyPe", "application/json"),
                Map.entry("cOnTeNt-EnCoDiNg", "identity"),
                Map.entry("dIgEsT", "sha-256=YWJj"),
                Map.entry("lAsT-MoDiFiEd", "Wed, 21 Oct 2015 07:28:00 GMT"));
    }

    private static void assertHeadersAbsent(
            org.mockserver.model.HttpRequest request, Map<String, String> headers) {
        for (String headerName : headers.keySet()) {
            @Nullable String value = request.getFirstHeader(headerName);
            assertTrue(value == null || value.isEmpty(),
                    headerName + " must not be forwarded on this redirect");
        }
    }

    private static void assertHeadersPresent(
            org.mockserver.model.HttpRequest request, Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            assertEquals(header.getValue(), request.getFirstHeader(header.getKey()),
                    header.getKey() + " must be retained on this redirect");
        }
    }

    private static void assertEntityHeadersRemoved(org.mockserver.model.HttpRequest request) {
        @Nullable String contentLength = request.getFirstHeader("cOnTeNt-LeNgTh");
        assertTrue(contentLength == null || !contentLength.equals("2"),
                "Content-Length must not retain the POST body length on a redirected GET");
        for (String headerName : entityHeaders().keySet()) {
            if (!headerName.equalsIgnoreCase("Content-Length")) {
                @Nullable String value = request.getFirstHeader(headerName);
                assertTrue(value == null || value.isEmpty(),
                        headerName + " must not be forwarded on a redirected GET");
            }
        }
    }
}
