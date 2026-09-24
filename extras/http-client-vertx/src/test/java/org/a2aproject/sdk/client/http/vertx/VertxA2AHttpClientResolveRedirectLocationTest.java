package org.a2aproject.sdk.client.http.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link VertxA2AHttpClient#resolveRedirectLocation(String, String)}, the RFC 3986
 * section 5 reference resolution applied to a redirect {@code Location} before the redirected
 * request is built.
 *
 * <p>Covers a2aproject/a2a-java#1142: a relative {@code Location} must resolve against the original
 * request URI rather than be handed verbatim to an absolute request.
 */
public class VertxA2AHttpClientResolveRedirectLocationTest {

    @Test
    public void absolutePathResolvesAgainstOrigin() {
        assertEquals(
                "http://host:3000/collect",
                VertxA2AHttpClient.resolveRedirectLocation("http://host:3000/rpc/send", "/collect"));
    }

    @Test
    public void relativePathResolvesAgainstCurrentDirectory() {
        assertEquals(
                "http://host:3000/rpc/collect",
                VertxA2AHttpClient.resolveRedirectLocation("http://host:3000/rpc/send", "collect"));
    }

    @Test
    public void relativePathWithQueryIsResolved() {
        assertEquals(
                "http://host:3000/a/c?x=1",
                VertxA2AHttpClient.resolveRedirectLocation("http://host:3000/a/b", "c?x=1"));
    }

    @Test
    public void dotSegmentsAreResolved() {
        assertEquals(
                "http://host:3000/a/d",
                VertxA2AHttpClient.resolveRedirectLocation("http://host:3000/a/b/c", "../d"));
    }

    @Test
    public void absoluteLocationIsReturnedUnchanged() {
        assertEquals(
                "https://elsewhere.example/xyz",
                VertxA2AHttpClient.resolveRedirectLocation(
                        "http://host:3000/rpc", "https://elsewhere.example/xyz"));
    }

    @Test
    public void protocolRelativeLocationKeepsScheme() {
        assertEquals(
                "https://elsewhere.example/p",
                VertxA2AHttpClient.resolveRedirectLocation(
                        "https://host/rpc", "//elsewhere.example/p"));
    }

    @Test
    public void malformedLocationFallsBackToRawValue() {
        // A space is illegal in a URI reference, so URI.create throws and the raw
        // Location is returned so any existing downstream handling still applies.
        String raw = "/pa th";
        assertEquals(raw, VertxA2AHttpClient.resolveRedirectLocation("http://host/rpc", raw));
    }

    @Test
    public void malformedBaseFallsBackToRawLocation() {
        String location = "/collect";
        assertEquals(
                location, VertxA2AHttpClient.resolveRedirectLocation("ht tp://host/rpc", location));
    }
}
