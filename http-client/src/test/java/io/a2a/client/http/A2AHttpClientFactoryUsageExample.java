package io.a2a.client.http;

import java.io.IOException;

/**
 * Example demonstrating how to use {@link A2AHttpClientFactory} to obtain HTTP client instances.
 *
 * <p>
 * This class shows various usage patterns for the factory-based approach to creating
 * A2AHttpClient instances.
 */
public class A2AHttpClientFactoryUsageExample {

    /**
     * Example 1: Basic usage with automatic selection of best available client.
     */
    public void basicUsage() throws IOException, InterruptedException {
        // The factory automatically selects the best available implementation:
        // - VertxA2AHttpClient (priority 100) if Vert.x is on the classpath
        // - JdkA2AHttpClient (priority 0) as fallback
        A2AHttpClient client = A2AHttpClientFactory.create();

        try {
            A2AHttpResponse response = client.createGet()
                .url("https://api.example.com/data")
                .addHeader("Accept", "application/json")
                .get();

            if (response.success()) {
                System.out.println("Response: " + response.body());
            }
        } finally {
            // Close if the client supports AutoCloseable (Vertx does, JDK doesn't)
            if (client instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) client).close();
                } catch (Exception e) {
                    // Handle close exception
                }
            }
        }
    }

    /**
     * Example 2: Try-with-resources pattern (recommended for AutoCloseable clients).
     */
    public void tryWithResourcesUsage() throws Exception {
        A2AHttpClient client = A2AHttpClientFactory.create();

        // Only use try-with-resources if the client is AutoCloseable
        if (client instanceof AutoCloseable) {
            try (AutoCloseable closeableClient = (AutoCloseable) client) {
                A2AHttpResponse response = client.createPost()
                    .url("https://api.example.com/submit")
                    .addHeader("Content-Type", "application/json")
                    .body("{\"key\":\"value\"}")
                    .post();

                System.out.println("Status: " + response.status());
            }
        } else {
            // Non-closeable client, use normally
            A2AHttpResponse response = client.createPost()
                .url("https://api.example.com/submit")
                .addHeader("Content-Type", "application/json")
                .body("{\"key\":\"value\"}")
                .post();

            System.out.println("Status: " + response.status());
        }
    }

    /**
     * Example 3: Explicitly selecting a specific implementation.
     */
    public void specificProviderUsage() throws IOException, InterruptedException {
        // Force the use of JDK client even if Vert.x is available
        A2AHttpClient jdkClient = A2AHttpClientFactory.create("jdk");
        A2AHttpResponse response = jdkClient.createGet()
            .url("https://api.example.com/data")
            .get();

        System.out.println("Using JDK client: " + response.status());

        // Or explicitly use Vert.x client
        try (AutoCloseable vertxClient = (AutoCloseable) A2AHttpClientFactory.create("vertx")) {
            A2AHttpResponse vertxResponse = ((A2AHttpClient) vertxClient).createGet()
                .url("https://api.example.com/data")
                .get();

            System.out.println("Using Vert.x client: " + vertxResponse.status());
        } catch (Exception e) {
            // Handle exceptions
        }
    }

    /**
     * Example 4: Defensive programming - handling unknown implementations.
     */
    public void defensiveUsage() throws IOException, InterruptedException {
        A2AHttpClient client = null;
        try {
            client = A2AHttpClientFactory.create();

            A2AHttpResponse response = client.createGet()
                .url("https://api.example.com/data")
                .get();

            System.out.println("Response: " + response.body());

        } finally {
            // Safely close if possible
            if (client instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) client).close();
                } catch (Exception e) {
                    System.err.println("Warning: Failed to close client: " + e.getMessage());
                }
            }
        }
    }
}
