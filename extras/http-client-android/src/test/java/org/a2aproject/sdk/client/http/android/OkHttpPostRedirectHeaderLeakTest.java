package org.a2aproject.sdk.client.http.android;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

import java.util.stream.Stream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.integration.ClientAndServer;

/**
 * Proves the Android-specific attack vector that motivated the
 * {@code followRedirects = false} default in all PostBuilder implementations.
 *
 * <p>Android's HTTP stack is OkHttp. Unlike the JDK's {@link java.net.HttpURLConnection},
 * OkHttp follows 302 redirects on POST and forwards nonstandard request headers
 * to the redirect target, even across authorities. The standard {@code Authorization}
 * header is stripped on cross-authority redirects, but all other headers survive.
 * This test uses OkHttp directly to validate that assumption without requiring a
 * real Android runtime.
 *
 * <p>The headers tested here are exactly the nonstandard names permitted by
 * {@link org.a2aproject.sdk.client.transport.spi.interceptors.auth.AuthInterceptor}'s
 * {@code SAFE_API_KEY_HEADER_NAMES} allowlist.
 */
public class OkHttpPostRedirectHeaderLeakTest {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private ClientAndServer redirector;
    private ClientAndServer collector;

    @AfterEach
    public void tearDown() {
        if (redirector != null) {
            redirector.stop();
        }
        if (collector != null) {
            collector.stop();
        }
    }

    static Stream<String> authInterceptorNonstandardHeaders() {
        return Stream.of("X-API-Key", "API-Key", "X-Auth-Token", "X-Authentication");
    }

    @ParameterizedTest
    @MethodSource("authInterceptorNonstandardHeaders")
    public void okHttpLeaksNonstandardHeaderOnCrossAuthorityPostRedirect(String headerName)
            throws Exception {
        startRedirectPair();

        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(true)
                .build();

        Request request = new Request.Builder()
                .url("http://127.0.0.1:" + redirector.getLocalPort() + "/agent")
                .post(RequestBody.create("{}", JSON))
                .addHeader(headerName, "FULCRUM-SYNTHETIC-LEAK")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
        }

        collector.verify(request().withPath("/collect"), exactly(1));

        org.mockserver.model.HttpRequest[] recorded = collector.retrieveRecordedRequests(
                request().withPath("/collect"));
        assertEquals(1, recorded.length);
        assertNotEquals("", recorded[0].getFirstHeader(headerName),
                headerName + " must survive cross-authority redirect in OkHttp — "
                        + "this is why PostBuilder must default to followRedirects=false");
        assertEquals("FULCRUM-SYNTHETIC-LEAK", recorded[0].getFirstHeader(headerName));
    }

    @ParameterizedTest
    @MethodSource("authInterceptorNonstandardHeaders")
    public void okHttpDoesNotLeakWhenRedirectsDisabled(String headerName) throws Exception {
        startRedirectPair();

        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(false)
                .build();

        Request request = new Request.Builder()
                .url("http://127.0.0.1:" + redirector.getLocalPort() + "/agent")
                .post(RequestBody.create("{}", JSON))
                .addHeader(headerName, "FULCRUM-SYNTHETIC-NOLEAK")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(302, response.code());
        }

        collector.verify(request().withPath("/collect"), exactly(0));
    }

    @Test
    public void okHttpStripsAuthorizationButKeepsNonstandardHeadersOnRedirect() throws Exception {
        startRedirectPair();

        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(true)
                .build();

        Request request = new Request.Builder()
                .url("http://127.0.0.1:" + redirector.getLocalPort() + "/agent")
                .post(RequestBody.create("{}", JSON))
                .addHeader("Authorization", "Bearer FULCRUM-SYNTHETIC-BEARER")
                .addHeader("X-API-Key", "FULCRUM-SYNTHETIC-APIKEY")
                .addHeader("API-Key", "FULCRUM-SYNTHETIC-APIKEY2")
                .addHeader("X-Auth-Token", "FULCRUM-SYNTHETIC-TOKEN")
                .addHeader("X-Authentication", "FULCRUM-SYNTHETIC-AUTH")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertEquals(200, response.code());
        }

        org.mockserver.model.HttpRequest[] recorded = collector.retrieveRecordedRequests(
                request().withPath("/collect"));
        assertEquals(1, recorded.length);

        assertEquals("", recorded[0].getFirstHeader("Authorization"),
                "OkHttp strips Authorization on cross-authority redirect");

        assertEquals("FULCRUM-SYNTHETIC-APIKEY", recorded[0].getFirstHeader("X-API-Key"));
        assertEquals("FULCRUM-SYNTHETIC-APIKEY2", recorded[0].getFirstHeader("API-Key"));
        assertEquals("FULCRUM-SYNTHETIC-TOKEN", recorded[0].getFirstHeader("X-Auth-Token"));
        assertEquals("FULCRUM-SYNTHETIC-AUTH", recorded[0].getFirstHeader("X-Authentication"));
    }

    private void startRedirectPair() {
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
}
