package io.a2a.client.http;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class A2AHttpClientFactoryTest {

    @Test
    public void testCreateReturnsNonNull() {
        A2AHttpClient client = A2AHttpClientFactory.create();
        assertNotNull(client, "Factory should return a non-null client");
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
    public void testCreateWithJdkProviderName() {
        A2AHttpClient client = A2AHttpClientFactory.create("jdk");
        assertNotNull(client);
        assertInstanceOf(JdkA2AHttpClient.class, client,
            "Factory should return JdkA2AHttpClient when 'jdk' provider is requested");
    }

    @Test
    public void testCreateWithVertxProviderNameThrows() {
        // Vertx provider is not available in the core module
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> A2AHttpClientFactory.create("vertx"),
            "Factory should throw IllegalArgumentException when vertx provider is not found"
        );
        assertTrue(exception.getMessage().contains("vertx"),
            "Exception message should mention the provider name");
    }

    @Test
    public void testCreateWithInvalidProviderNameThrows() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> A2AHttpClientFactory.create("nonexistent"),
            "Factory should throw IllegalArgumentException for unknown provider"
        );
        assertTrue(exception.getMessage().contains("nonexistent"),
            "Exception message should mention the provider name");
    }

    @Test
    public void testCreateWithNullProviderNameThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> A2AHttpClientFactory.create(null),
            "Factory should throw IllegalArgumentException for null provider name"
        );
    }

    @Test
    public void testCreateWithEmptyProviderNameThrows() {
        assertThrows(
            IllegalArgumentException.class,
            () -> A2AHttpClientFactory.create(""),
            "Factory should throw IllegalArgumentException for empty provider name"
        );
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
