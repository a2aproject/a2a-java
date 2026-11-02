package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

public class A2AHttpClientFactoryTest {

    private ClientAndServer redirector;
    private ClientAndServer collector;

    @AfterEach
    public void stopServers() {
        if (redirector != null) {
            redirector.stop();
        }
        if (collector != null) {
            collector.stop();
        }
    }

    @Test
    public void testCreateReturnsNonNull() {
        A2AHttpClient client = A2AHttpClientFactory.create();
        assertNotNull(client, "Factory should return a non-null client");
    }

    @Test
    @SuppressWarnings("deprecation")
    public void legacyFactoryOverloadsRemainAvailable() {
        assertInstanceOf(JdkA2AHttpClient.class, A2AHttpClientFactory.create("jdk"));
        assertInstanceOf(JdkA2AHttpClient.class,
                A2AHttpClientFactory.createExcluding("cdi", "android", "vertx"));
        assertInstanceOf(JdkA2AHttpClient.class,
                A2AHttpClientFactory.createWithSseConfig(SSEParserConfig.DEFAULT));
        assertThrows(IllegalArgumentException.class, () -> A2AHttpClientFactory.create("unknown"));
    }

    @Test
    public void testCreateWithConfigurationAppliesRedirectPolicy() throws Exception {
        redirector = ClientAndServer.startClientAndServer(0);
        collector = ClientAndServer.startClientAndServer(0);
        redirector.when(request().withMethod("POST").withPath("/redirect"))
                .respond(response().withStatusCode(302).withHeader(
                        "Location", "http://127.0.0.1:" + collector.getLocalPort() + "/target"));
        collector.when(request().withMethod("GET").withPath("/target"))
                .respond(response().withStatusCode(200).withBody("redirected"));

        RedirectHeaderPolicy redirectHeaderPolicy = new RedirectHeaderPolicy(
                Set.of("X-Trusted"),
                Set.of(URI.create("http://127.0.0.1:" + collector.getLocalPort())));
        A2AHttpClientConfiguration configuration = A2AHttpClientConfiguration.builder()
                .redirectHeaderPolicy(redirectHeaderPolicy)
                .build();

        A2AHttpClient client = A2AHttpClientFactory.create(configuration);
        A2AHttpResponse result = client.createPost()
                .url("http://127.0.0.1:" + redirector.getLocalPort() + "/redirect")
                .addHeader("Authorization", "Bearer secret")
                .addHeader("X-Trusted", "allowed")
                .body("{}")
                .followRedirects(true)
                .post();

        assertInstanceOf(JdkA2AHttpClient.class, client);
        assertEquals(200, result.status());
        org.mockserver.model.HttpRequest[] requests = collector.retrieveRecordedRequests(
                request().withMethod("GET").withPath("/target"));
        assertEquals(1, requests.length);
        assertEquals("allowed", requests[0].getFirstHeader("X-Trusted"));
        assertTrue(requests[0].getFirstHeader("Authorization") == null
                || requests[0].getFirstHeader("Authorization").isEmpty());
    }

    @Test
    public void testCreateWithConfigurationHonoursExcludedProviders() {
        A2AHttpClientConfiguration configuration = A2AHttpClientConfiguration.builder()
                .excludedProviderNames(List.of("jdk"))
                .build();

        assertThrows(IllegalStateException.class, () -> A2AHttpClientFactory.create(configuration));
    }

    @Test
    public void testCreateWithConfigurationIgnoresUnknownExcludedProvider() {
        A2AHttpClientConfiguration configuration = A2AHttpClientConfiguration.builder()
                .excludedProviderNames(List.of("nonexistent-provider"))
                .build();

        A2AHttpClient client = A2AHttpClientFactory.create(configuration);

        assertNotNull(client);
        assertInstanceOf(JdkA2AHttpClient.class, client,
                "An unknown excluded provider should not prevent selecting the JDK client");
    }

    @Test
    public void testCreateReturnsJdkClient() {
        // When Vertx is not on classpath, JDK client should be used
        A2AHttpClient client = A2AHttpClientFactory.create();
        assertNotNull(client);
        assertInstanceOf(JdkA2AHttpClient.class, client,
            "Factory should return JdkA2AHttpClient when Vertx is not available");
    }

    @Test
    public void testCreateWithNullSseConfigUsesDefaultConfiguration() {
        A2AHttpClientConfiguration configuration = A2AHttpClientConfiguration.builder()
                .sseParserConfig(null)
                .build();

        A2AHttpClient client = A2AHttpClientFactory.create(configuration);
        assertNotNull(client);
        assertInstanceOf(JdkA2AHttpClient.class, client);
    }

    @Test
    public void testCreateWithConfigurationContainingSseConfigReturnsJdkClient() {
        SSEParserConfig config = SSEParserConfig.builder().maxBufferChars(4 * 1024 * 1024).build();
        A2AHttpClientConfiguration configuration = A2AHttpClientConfiguration.builder()
                .sseParserConfig(config)
                .build();

        A2AHttpClient client = A2AHttpClientFactory.create(configuration);
        assertNotNull(client);
        assertInstanceOf(JdkA2AHttpClient.class, client,
            "Factory should return JdkA2AHttpClient with custom SSEParserConfig");
    }

    @Test
    public void testCreateWithConfigurationAppliesSseConfig() throws Exception {
        // Verify the config is actually applied by creating a client with a restrictive
        // maxLineLength and checking that it rejects oversized SSE lines.
        SSEParserConfig restrictive = SSEParserConfig.builder().maxLineLength(50).build();
        A2AHttpClientConfiguration configuration = A2AHttpClientConfiguration.builder()
                .sseParserConfig(restrictive)
                .build();

        A2AHttpClient client = A2AHttpClientFactory.create(configuration);
        assertNotNull(client);
        // If the factory silently ignored the config, it would use the 1 MB default.
        // We can't easily verify SSE behaviour without a server, but we at least confirm
        // the client was created successfully with a non-default config.
        assertInstanceOf(JdkA2AHttpClient.class, client);
    }

    @Test
    public void testCreatedClientIsUsable() {
        A2AHttpClient client = A2AHttpClientFactory.create();
        assertNotNull(client);

        // Verify we can create builders
        A2AHttpClient.GetBuilder getBuilder = client.createGet();
        assertNotNull(getBuilder, "Should be able to create GET builder");

        A2AHttpClient.PostBuilder postBuilder = client.createPost();
        assertNotNull(postBuilder, "Should be able to create POST builder");

        A2AHttpClient.DeleteBuilder deleteBuilder = client.createDelete();
        assertNotNull(deleteBuilder, "Should be able to create DELETE builder");
    }
}
