package org.a2aproject.sdk.client.http.android;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.verify.VerificationTimes.exactly;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.http.AbstractA2AHttpClientRedirectTest;
import org.junit.jupiter.api.Test;

public class AndroidA2AHttpClientRedirectTest extends AbstractA2AHttpClientRedirectTest {

    private CookieHandler originalCookieHandler;
    private boolean cookieHandlerReplaced;
    private Authenticator originalAuthenticator;
    private boolean authenticatorReplaced;

    @org.junit.jupiter.api.AfterEach
    public void restoreProcessWideCredentials() {
        if (cookieHandlerReplaced) {
            CookieHandler.setDefault(originalCookieHandler);
        }
        if (authenticatorReplaced) {
            Authenticator.setDefault(originalAuthenticator);
        }
    }

    @Override
    protected A2AHttpClient createClient() {
        return new AndroidA2AHttpClient();
    }

    @Override
    protected boolean followsCrossOriginPostToGetRedirects() {
        return false;
    }

    @Test
    public void synchronousPostFollowsRedirectWhenExplicitlyEnabled() throws Exception {
        startRedirectPair();

        A2AHttpResponse result = createClient().createPost()
                .url("http://127.0.0.1:" + getRedirector().getLocalPort() + "/agent")
                .addHeader("X-API-Key", "FULCRUM-SYNTHETIC-EXPLICIT")
                .body("{}")
                .followRedirects(true)
                .post();

        assertEquals(302, result.status(),
                "Android client must not follow cross-origin redirects");
        getCollector().verify(request().withPath("/collect"), exactly(0));
    }

    @Test
    public void crossOriginRedirectDoesNotContactTargetEvenWithSafeHeaders() throws Exception {
        startRedirectPair();

        A2AHttpResponse result = createClient().createPost()
                .url("http://127.0.0.1:" + getRedirector().getLocalPort() + "/agent")
                .addHeader("Authorization", "Bearer secret")
                .addHeader("X-Custom-Token", "secret")
                .addHeader("A2A-Version", "1.0")
                .body("{}")
                .followRedirects(true)
                .post();

        assertEquals(302, result.status());
        getCollector().verify(request().withPath("/collect"), exactly(0));
    }

    @Test
    public void crossOriginRedirectDoesNotUseProcessWideCookieHandler() throws Exception {
        startRedirectPair();
        originalCookieHandler = CookieHandler.getDefault();
        CookieManager cookieManager = new CookieManager();
        cookieManager.put(
                URI.create("http://127.0.0.1:" + getRedirector().getLocalPort() + "/agent"),
                Map.of("Set-Cookie", List.of("session=secret; Path=/")));
        URI targetUri = URI.create("http://127.0.0.1:" + getCollector().getLocalPort() + "/collect");
        assertEquals(List.of("session=secret"), cookieManager.get(targetUri, Map.of()).get("Cookie"),
                "The process-wide CookieHandler must be capable of injecting the cross-port cookie");
        CookieHandler.setDefault(cookieManager);
        cookieHandlerReplaced = true;

        A2AHttpResponse result = createClient().createPost()
                .url("http://127.0.0.1:" + getRedirector().getLocalPort() + "/agent")
                .body("{}")
                .followRedirects(true)
                .post();

        assertEquals(302, result.status());
        getCollector().verify(request().withPath("/collect"), exactly(0));
    }

    @Test
    public void crossOriginRedirectDoesNotUseProcessWideAuthenticator() throws Exception {
        startRedirectPair();
        originalAuthenticator = Authenticator.getDefault();
        Authenticator.setDefault(new Authenticator() {});
        authenticatorReplaced = true;

        A2AHttpResponse result = createClient().createPost()
                .url("http://127.0.0.1:" + getRedirector().getLocalPort() + "/agent")
                .body("{}")
                .followRedirects(true)
                .post();

        assertEquals(302, result.status());
        getCollector().verify(request().withPath("/collect"), exactly(0));
    }
}
