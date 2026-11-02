package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class RedirectHeaderPolicyTest {

    @Test
    public void defaultPolicyStripsCredentialsAndRetainsA2AHeadersCrossOrigin() {
        RedirectHeaderPolicy.RedirectedRequest redirect = RedirectHeaderPolicy.DEFAULT
                .redirectedRequest(
                        URI.create("http://origin.example/agent"),
                        302,
                        "https://target.example/collect",
                        Map.of(
                                "Authorization", "Bearer secret",
                                "X-Custom-Token", "secret",
                                "A2A-Version", "1.0",
                                "A2A-Extensions", "extension-a"))
                .orElseThrow();

        assertTrue(redirect.redirectAsGet());
        assertFalse(redirect.headers().containsKey("Authorization"));
        assertFalse(redirect.headers().containsKey("X-Custom-Token"));
        assertEquals("1.0", redirect.headers().get("A2A-Version"));
        assertEquals("extension-a", redirect.headers().get("A2A-Extensions"));
    }

    @Test
    public void postToGetRedirectRemovesEntityHeadersOnSameOrigin() {
        RedirectHeaderPolicy.RedirectedRequest redirect = RedirectHeaderPolicy.DEFAULT
                .redirectedRequest(
                        URI.create("https://agent.example/send"),
                        302,
                        "/collect",
                        Map.of(
                                "Authorization", "Bearer same-origin",
                                "Content-Length", "2",
                                "Content-Type", "application/json",
                                "Content-Encoding", "identity",
                                "dIgEsT", "sha-256=YWJj",
                                "lAsT-MoDiFiEd", "Wed, 21 Oct 2015 07:28:00 GMT"))
                .orElseThrow();

        assertTrue(redirect.redirectAsGet());
        assertEquals("Bearer same-origin", redirect.headers().get("Authorization"));
        assertFalse(redirect.headers().containsKey("Content-Length"));
        assertFalse(redirect.headers().containsKey("Content-Type"));
        assertFalse(redirect.headers().containsKey("Content-Encoding"));
        assertFalse(redirect.headers().containsKey("dIgEsT"));
        assertFalse(redirect.headers().containsKey("lAsT-MoDiFiEd"));
    }

    @Test
    public void customPolicyAddsHeadersOnlyForTrustedTargetOrigin() {
        RedirectHeaderPolicy policy = new RedirectHeaderPolicy(
                Set.of("Authorization", "X-Trusted-Header"),
                Set.of(URI.create("https://target.example")));
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer secret",
                "A2A-Version", "1.0",
                "X-Trusted-Header", "value");

        RedirectHeaderPolicy.RedirectedRequest trusted = policy
                .redirectedRequest(
                        URI.create("https://origin.example/send"),
                        302,
                        "https://target.example:443/collect",
                        headers)
                .orElseThrow();

        assertEquals("Bearer secret", trusted.headers().get("Authorization"));
        assertEquals("1.0", trusted.headers().get("A2A-Version"));
        assertEquals("value", trusted.headers().get("X-Trusted-Header"));

        for (String target : Set.of(
                "https://other.example/collect",
                "https://target.example:8443/collect")) {
            RedirectHeaderPolicy.RedirectedRequest untrusted = policy
                    .redirectedRequest(URI.create("https://origin.example/send"), 302, target, headers)
                    .orElseThrow();
            assertFalse(untrusted.headers().containsKey("Authorization"), target);
            assertFalse(untrusted.headers().containsKey("X-Trusted-Header"), target);
            assertEquals("1.0", untrusted.headers().get("A2A-Version"), target);
        }
        RedirectHeaderPolicy.RedirectedRequest differentScheme = policy
                .redirectedRequest(
                        URI.create("http://origin.example/send"),
                        302,
                        "http://target.example/collect",
                        headers)
                .orElseThrow();
        assertFalse(differentScheme.headers().containsKey("Authorization"));
        assertFalse(differentScheme.headers().containsKey("X-Trusted-Header"));
    }

    @Test
    public void httpsToHttpRedirectsAreNotFollowed() {
        URI source = URI.create("https://agent.example/send");
        for (int status : new int[] {301, 302, 303, 307, 308}) {
            assertTrue(RedirectHeaderPolicy.DEFAULT
                    .redirectedRequest(source, status, "http://agent.example/collect", Map.of())
                    .isEmpty(), "status " + status);
        }
        assertTrue(RedirectHeaderPolicy.DEFAULT
                .redirectedRequest(
                        URI.create("https://agent.example:8443/send"),
                        302,
                        "http://agent.example:8443/collect",
                        Map.of())
                .isEmpty());
        RedirectHeaderPolicy trustedHttpPolicy = new RedirectHeaderPolicy(
                Set.of("Authorization"), Set.of(URI.create("http://agent.example")));
        assertTrue(trustedHttpPolicy
                .redirectedRequest(
                        source,
                        302,
                        "http://agent.example/collect",
                        Map.of("Authorization", "Bearer secret"))
                .isEmpty());
        assertTrue(RedirectHeaderPolicy.DEFAULT
                .redirectedRequest(
                        URI.create("http://agent.example/send"),
                        302,
                        "https://agent.example/collect",
                        Map.of())
                .isPresent());
    }

    @Test
    public void customPolicyRequiresValidTrustedOrigins() {
        assertThrows(IllegalArgumentException.class,
                () -> new RedirectHeaderPolicy(Set.of("Authorization"), Set.of()));
        for (String origin : Set.of(
                "https://target.example/path",
                "https://target.example?query=1",
                "https://target.example#fragment",
                "https://user@target.example",
                "file:///target")) {
            assertThrows(IllegalArgumentException.class,
                    () -> new RedirectHeaderPolicy(Set.of("Authorization"), Set.of(URI.create(origin))),
                    origin);
        }
    }

    @Test
    public void queryOnlyLocationRetainsSourcePath() {
        RedirectHeaderPolicy.RedirectedRequest redirect = RedirectHeaderPolicy.DEFAULT
                .redirectedRequest(
                        URI.create("http://host/rpc/send?old=1"), 302, "?new=2", Map.of())
                .orElseThrow();

        assertEquals(URI.create("http://host/rpc/send?new=2"), redirect.target());
    }

    @Test
    public void resolvesRelativeAndProtocolRelativeLocations() {
        URI source = URI.create("https://host:3000/rpc/send");

        assertEquals(
                URI.create("https://host:3000/rpc/collect"),
                RedirectHeaderPolicy.DEFAULT.redirectedRequest(source, 302, "collect", Map.of())
                        .orElseThrow()
                        .target());
        assertEquals(
                URI.create("https://elsewhere.example/collect"),
                RedirectHeaderPolicy.DEFAULT.redirectedRequest(source, 302, "//elsewhere.example/collect", Map.of())
                        .orElseThrow()
                        .target());
    }

    @Test
    public void crossOriginMethodPreservingRedirectsAreRejected() {
        assertTrue(RedirectHeaderPolicy.DEFAULT
                .redirectedRequest(
                        URI.create("https://origin.example/send"),
                        307,
                        "https://target.example/collect",
                        Map.of())
                .isEmpty());
        assertTrue(RedirectHeaderPolicy.DEFAULT
                .redirectedRequest(
                        URI.create("https://origin.example/send"),
                        308,
                        "https://target.example/collect",
                        Map.of())
                .isEmpty());
    }

    @Test
    public void normalizesOriginsAndRejectsMalformedOrUnsupportedRedirects() {
        assertTrue(RedirectHeaderPolicy.sameOrigin(
                URI.create("HTTP://HOST:80/rpc"), URI.create("http://host/collect")));
        assertFalse(RedirectHeaderPolicy.sameOrigin(
                URI.create("http://host/rpc"), URI.create("https://host/rpc")));
        assertFalse(RedirectHeaderPolicy.sameOrigin(
                URI.create("http://host/rpc"), URI.create("/collect")));
        assertTrue(RedirectHeaderPolicy.DEFAULT
                .redirectedRequest(URI.create("http://host/rpc"), 302, "/pa th", Map.of())
                .isEmpty());
        assertTrue(RedirectHeaderPolicy.DEFAULT
                .redirectedRequest(URI.create("http://host/rpc"), 302, "file:///tmp/collect", Map.of())
                .isEmpty());
        assertTrue(RedirectHeaderPolicy.DEFAULT
                .redirectedRequest(URI.create("http://host/rpc"), 302, "mailto:agent@example.com", Map.of())
                .isEmpty());
        assertTrue(RedirectHeaderPolicy.DEFAULT
                .redirectedRequest(URI.create("http://host/rpc"), 302, "http:/missing-host", Map.of())
                .isEmpty());
        assertTrue(RedirectHeaderPolicy.DEFAULT
                .redirectedRequest(URI.create("http://host/rpc"), 300, "/collect", Map.of())
                .isEmpty());
    }
}
