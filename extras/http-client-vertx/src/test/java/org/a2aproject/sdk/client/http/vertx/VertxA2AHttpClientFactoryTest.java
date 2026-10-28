package org.a2aproject.sdk.client.http.vertx;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientConfiguration;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.client.http.JdkA2AHttpClient;
import org.junit.jupiter.api.Test;

public class VertxA2AHttpClientFactoryTest {

    @Test
    public void testCreateReturnsVertxClient() {
        // When both JDK and Vertx are on classpath, Vertx should be preferred due to higher priority
        A2AHttpClient client = A2AHttpClientFactory.create();
        assertNotNull(client);
        assertInstanceOf(VertxA2AHttpClient.class, client,
            "Factory should return VertxA2AHttpClient when Vertx is available");
        // Clean up
        if (client instanceof AutoCloseable) {
            try {
                ((AutoCloseable) client).close();
            } catch (Exception e) {
                fail("Failed to close client: " + e.getMessage());
            }
        }
    }

    @Test
    public void testCreateExcludingVertxFallsBackToJdkClient() {
        A2AHttpClientConfiguration configuration = A2AHttpClientConfiguration.builder()
                .excludedProviderNames(List.of("vertx"))
                .build();

        A2AHttpClient client = A2AHttpClientFactory.create(configuration);

        assertNotNull(client);
        assertInstanceOf(JdkA2AHttpClient.class, client,
                "Excluding Vert.x should select the JDK fallback");
    }
}
