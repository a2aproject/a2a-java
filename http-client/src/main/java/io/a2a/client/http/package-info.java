/**
 * HTTP client utilities for A2A protocol communication.
 *
 * <p>This package provides a pluggable HTTP client abstraction for making HTTP requests
 * to A2A agents, including support for fetching agent cards, synchronous requests, and
 * Server-Sent Events (SSE) streaming.
 *
 * <h2>Core Components</h2>
 * <ul>
 *   <li>{@link io.a2a.client.http.A2AHttpClient} - Main HTTP client interface with builder pattern</li>
 *   <li>{@link io.a2a.client.http.A2AHttpClientFactory} - Factory for creating client instances via ServiceLoader</li>
 *   <li>{@link io.a2a.client.http.A2ACardResolver} - Utility for fetching agent cards from standard endpoints</li>
 *   <li>{@link io.a2a.client.http.A2AHttpResponse} - Response wrapper with status and body</li>
 * </ul>
 *
 * <h2>Provider System</h2>
 * <p>The module uses a ServiceLoader-based provider system allowing different HTTP client
 * implementations to be plugged in:
 * <ul>
 *   <li>{@link io.a2a.client.http.JdkA2AHttpClient} - Default implementation using JDK 11+ HttpClient (priority 0)</li>
 *   <li>VertxA2AHttpClient - Vertx-based implementation when available (priority 100)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Fetch an agent card
 * A2ACardResolver resolver = new A2ACardResolver("http://localhost:9999");
 * AgentCard card = resolver.getAgentCard();
 *
 * // Make HTTP requests
 * A2AHttpClient client = A2AHttpClientFactory.create();
 * A2AHttpResponse response = client.createGet()
 *     .url("http://localhost:9999/api/endpoint")
 *     .addHeader("Authorization", "Bearer token")
 *     .get();
 *
 * // Server-Sent Events (SSE) streaming
 * client.createPost()
 *     .url("http://localhost:9999/message:stream")
 *     .body(jsonBody)
 *     .postAsyncSSE(
 *         message -> System.out.println("Received: " + message),
 *         error -> System.err.println("Error: " + error),
 *         () -> System.out.println("Stream complete")
 *     );
 * }</pre>
 *
 * <h2>Agent Card Resolution</h2>
 * <p>Agent cards are fetched from the standard {@code /.well-known/agent-card.json} endpoint
 * by default, with support for tenant-specific paths and custom authentication headers.
 *
 * @see io.a2a.client.http.A2AHttpClient
 * @see io.a2a.client.http.A2ACardResolver
 * @see io.a2a.spec.AgentCard
 */
@NullMarked
package io.a2a.client.http;

import org.jspecify.annotations.NullMarked;

