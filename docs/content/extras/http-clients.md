---
title: HTTP Clients
description: Alternative HTTP client implementations for the A2A Java SDK, including Vert.x WebClient for reactive applications and an Android-specific client.
layout: page
---

# HTTP Clients

The A2A SDK uses an `A2AHttpClient` abstraction for all HTTP communication — fetching agent cards, making transport calls, and SSE streaming. By default, the SDK uses a JDK 11+ HttpClient implementation. These extras provide alternative implementations.

## Vert.x HTTP Client

Replaces the default `JdkA2AHttpClient` with a [Vert.x WebClient](https://vertx.io/docs/vertx-web-client/java/)-based implementation (`VertxA2AHttpClient`) for reactive, non-blocking HTTP communication.

### When to Use

**Recommended for:**

- Quarkus applications (Vert.x is already included)
- Reactive applications using Vert.x or reactive frameworks
- High-throughput scenarios with many concurrent requests
- Applications requiring efficient SSE streaming
- Cloud-native deployments optimizing for resource usage

**Not needed for** simple, low-volume applications or environments where JDK HttpClient performs adequately.

### What It Solves

| | JDK HttpClient | Vert.x WebClient |
|--|----------------|-------------------|
| **I/O model** | Platform threads, blocking | Event loop, non-blocking |
| **Memory** | Higher (one thread per connection) | Lower (shared event loop) |
| **HTTP/2** | Manual configuration | Automatic ALPN negotiation |
| **Reactive integration** | Requires bridging | Native |

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-http-client-vertx</artifactId>
</dependency>
```

For non-Quarkus environments, also add `vertx-web-client`:

```xml
<dependency>
    <groupId>io.vertx</groupId>
    <artifactId>vertx-web-client</artifactId>
</dependency>
```

### 2. Automatic Discovery (No Code Changes)

The `VertxA2AHttpClientProvider` has **priority 100** (vs. 50 for the JDK client). The SDK's `A2AHttpClientFactory` uses `ServiceLoader` to discover and select the highest-priority provider available:

```java
// No changes needed — A2A SDK automatically uses VertxA2AHttpClient
A2ACardResolver resolver = A2ACardResolver.builder().baseUrl("http://localhost:9999").build();
AgentCard card = resolver.getAgentCard(); // Uses Vert.x under the hood

Client client = Client.builder(card)
    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
    .build();
```

If Vert.x classes are not on the classpath, the provider returns priority `-1` and the SDK falls back to the JDK HttpClient.

### Usage Examples

#### GET Request

```java
try (A2AHttpClient client = A2AHttpClientFactory.create()) {
    A2AHttpResponse response = client.createGet()
        .url("https://api.example.com/data")
        .addHeader("Authorization", "Bearer token")
        .get();

    if (response.success()) {
        System.out.println(response.body());
    }
}
```

#### POST Request with JSON Body

```java
try (A2AHttpClient client = A2AHttpClientFactory.create()) {
    A2AHttpResponse response = client.createPost()
        .url("https://api.example.com/submit")
        .addHeader("Content-Type", "application/json")
        .body("{\"key\":\"value\"}")
        .post();

    System.out.println("Status: " + response.status());
}
```

#### Server-Sent Events (SSE) Streaming

```java
try (A2AHttpClient client = A2AHttpClientFactory.create()) {
    CompletableFuture<Void> future = client.createGet()
        .url("https://api.example.com/stream")
        .getAsyncSSE(
            message -> System.out.println("Received: " + message),
            error -> error.printStackTrace(),
            () -> System.out.println("Stream complete")
        );

    // Do other work while streaming...
    future.join();
}
```

### CDI / Quarkus Integration

In Quarkus or other CDI environments, the `VertxA2AHttpClient` automatically discovers and reuses the CDI-managed `Vertx` instance so that your entire application shares the same event loop.

For non-CDI environments, you can share a Vert.x instance manually:

```java
Vertx vertx = Vertx.vertx();

try {
    try (VertxA2AHttpClient client = new VertxA2AHttpClient(vertx)) {
        A2AHttpResponse response = client.createGet()
            .url("https://example.com")
            .get();
    }
} finally {
    vertx.close();
}
```

### Lifecycle Management

| Usage | Behavior on close |
|-------|--------------------|
| Standalone (`new VertxA2AHttpClient()`) | Both `WebClient` and `Vertx` are closed |
| CDI/Quarkus (`new VertxA2AHttpClient(injectedVertx)`) | Only `WebClient` is closed; `Vertx` remains open |

Always use try-with-resources to avoid leaking `Vertx` instances.

### Thread Safety

- **Client instances** are thread-safe — multiple threads can use the same client
- **Builder instances** are NOT thread-safe — create separate builders per thread
- All I/O operations execute on Vert.x event loop threads

### Performance Notes

Synchronous methods (`.get()`, `.post()`, `.delete()`) block the calling thread despite using Vert.x's async API internally. Vert.x still provides benefits at the network layer: non-blocking I/O, efficient connection pooling, and lower thread usage overall.

Async methods (`.getAsyncSSE()`, `.postAsyncSSE()`) are truly async — they return a `CompletableFuture` immediately.

### Troubleshooting

**Client not being used** — If logs show `JdkA2AHttpClient` instead of `VertxA2AHttpClient`, verify Vert.x WebClient is on the classpath:

```bash
mvn dependency:tree | grep vertx-web-client
```

**ClassNotFoundException for Vert.x** — Add the `vertx-web-client` dependency (see above). For Quarkus, ensure `quarkus-vertx` is present.

**Memory leaks** — Always use try-with-resources when creating `VertxA2AHttpClient` with the no-args constructor.

### Version Compatibility

- **Java**: 17+
- **Vert.x**: 4.x (tested with 4.5.0+)
- **Quarkus**: Any version using Vert.x 4.x
- **Jakarta EE**: 9.0+

## Android HTTP Client

HTTP client implementation for Android applications.

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-http-client-android</artifactId>
</dependency>
```
