package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpEndpointTest {
    @ParameterizedTest
    @ValueSource(strings = {"http://my_host:8080/agent", "https://bücher.example/"})
    void rejectsHostnamesThatHttpClientsCannotParse(String endpoint) {
        var error = assertThrows(IllegalArgumentException.class, () -> HttpEndpoint.normalize(endpoint));
        assertEquals("HTTP endpoint must have a URI-compatible hostname", error.getMessage());
    }

    @Test
    void normalizesOriginAndPreservesPath() {
        assertEquals("https://example.com:443/agent", HttpEndpoint.normalize("HTTPS://EXAMPLE.COM/agent"));
        assertEquals("http://example.com:80", HttpEndpoint.normalize("http://example.com"));
        assertEquals("http://[::1]:80/agent", HttpEndpoint.normalize("http://[::1]/agent"));
        assertThrows(IllegalArgumentException.class, () -> HttpEndpoint.normalize("http://EXAMPLE.COM.:0/"));
        assertEquals(HttpEndpoint.normalize("https://example.com/agent"),
                HttpEndpoint.normalize("https://EXAMPLE.COM./agent"));
        assertEquals("https://xn--bcher-kva.example:443/", HttpEndpoint.normalize("https://xn--bcher-kva.example/"));
        assertEquals("https://[2001:db8::1]:8443/a", HttpEndpoint.normalize("https://[2001:DB8::1]:8443/a"));
        assertNotEquals(HttpEndpoint.normalize("https://example.com/agent"),
                HttpEndpoint.normalize("https://example.com/agent/"));
        assertEquals(HttpEndpoint.normalizeRest("https://example.com/agent"),
                HttpEndpoint.normalizeRest("https://example.com/agent/"));
        assertEquals("https://example.com:443", HttpEndpoint.normalizeRest("https://example.com/"));
        assertEquals(HttpEndpoint.normalizeJsonRpc("https://example.com"),
                HttpEndpoint.normalizeJsonRpc("https://example.com/"));
        assertNotEquals(HttpEndpoint.normalizeJsonRpc("https://example.com/agent"),
                HttpEndpoint.normalizeJsonRpc("https://example.com/agent/"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"job@east", "a!$&'()*+,=b", "task-1"})
    void acceptsSafePathIdentifiers(String value) {
        assertEquals(value, HttpEndpoint.pathSegment(value));
    }

    @Test
    void encodesPathIdentifiers() {
        assertEquals("urn%3Auuid%3Aabc", HttpEndpoint.pathSegment("urn:uuid:abc"));
        assertEquals("caf%C3%A9%25", HttpEndpoint.pathSegment("café%"));
        assertEquals("%252e%252E", HttpEndpoint.pathSegment("%2e%2E"));
    }

    @ParameterizedTest
    @ValueSource(strings = {".", "..", "a/b", "a\\b", "a?b", "a#b"})
    void rejectsUnsafePathIdentifiers(String value) {
        assertThrows(IllegalArgumentException.class, () -> HttpEndpoint.pathSegment(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"relative", "ftp://example.com", "http://user@example.com", "http://example.com:",
            "http://example.com:65536", "http://example.com/a/./b", "http://example.com/%2E",
            "http://example.com/a?x=1", "http://example.com/a#x", "http://example.com/a/../b",
            "http://example.com/a/%2e%2e/b", "http://example.com/a%2fb", "http://example.com/a%5cb",
            "http://example.com/a%252fb", "http://example.com/a//b", "http://example.com/%xx"})
    void rejectsMalformedOrAmbiguousEndpoints(String endpoint) {
        assertThrows(IllegalArgumentException.class, () -> HttpEndpoint.normalize(endpoint));
    }
}
