package io.a2a.client.http;

import static org.junit.jupiter.api.Assertions.*;

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
    public void testCreateWithVertxProviderName() {
        A2AHttpClient client = A2AHttpClientFactory.create("vertx");
        assertNotNull(client);
        assertInstanceOf(VertxA2AHttpClient.class, client,
            "Factory should return VertxA2AHttpClient when 'vertx' provider is requested");
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
    public void testVertxClientIsUsable() {
        A2AHttpClient client = A2AHttpClientFactory.create("vertx");
        assertNotNull(client);

        // Verify we can create builders
        A2AHttpClient.GetBuilder getBuilder = client.createGet();
        assertNotNull(getBuilder, "Should be able to create GET builder");

        A2AHttpClient.PostBuilder postBuilder = client.createPost();
        assertNotNull(postBuilder, "Should be able to create POST builder");

        A2AHttpClient.DeleteBuilder deleteBuilder = client.createDelete();
        assertNotNull(deleteBuilder, "Should be able to create DELETE builder");

        // Clean up
        if (client instanceof AutoCloseable) {
            try {
                ((AutoCloseable) client).close();
            } catch (Exception e) {
                fail("Failed to close client: " + e.getMessage());
            }
        }
    }
}
