# A2A Java SDK - Vert.x HTTP Client

This module provides a Vert.x WebClient-based implementation of the `A2AHttpClient` interface for reactive, high-performance HTTP communication in the A2A Java SDK.

## Overview

The A2A SDK uses an `A2AHttpClient` abstraction for all HTTP communication, including fetching agent cards and making REST transport calls. By default, the SDK uses a JDK 11+ HttpClient implementation. This module provides a drop-in replacement using **Vert.x WebClient**, offering:

- **Reactive/Async Architecture**: Built on Vert.x's event loop for non-blocking I/O
- **Better Performance**: Lower resource usage and higher throughput than blocking JDK HttpClient
- **HTTP/2 Support**: Automatic HTTP/2 negotiation via ALPN
- **Seamless Integration**: Automatic discovery via Java SPI - no code changes required

## What It Does

Replaces the default `JdkA2AHttpClient` with `VertxA2AHttpClient`, which uses Vert.x WebClient for all HTTP operations:

- GET requests (synchronous and async SSE streaming)
- POST requests (synchronous and async SSE streaming)
- DELETE requests
- Agent card fetching
- REST transport communication

The implementation maintains the same API as the JDK client but uses Vert.x's reactive architecture under the hood.

## Problem It Solves

### Performance & Scalability
- **JDK HttpClient**: Uses platform threads for blocking I/O operations
- **Vert.x WebClient**: Uses event loop threads with non-blocking I/O
- **Result**: Lower memory footprint, higher concurrency, better throughput

### Reactive Integration
- Applications already using Vert.x can share the same event loop
- Avoids mixing blocking and non-blocking I/O patterns
- Better integration with reactive frameworks (Quarkus, Vert.x, etc.)

### Resource Efficiency
- Fewer threads needed for high-concurrency scenarios
- Better connection pooling and keep-alive management
- Lower latency for streaming operations (SSE)

## When to Use

✅ **Recommended for:**
- Quarkus applications (Vert.x is already included)
- Reactive applications using Vert.x or reactive frameworks
- High-throughput scenarios with many concurrent requests
- Applications requiring efficient SSE streaming
- Cloud-native deployments optimizing for resource usage

❌ **Not needed for:**
- Simple, low-volume applications
- Applications without existing Vert.x dependency
- Environments where JDK HttpClient performs adequately

## Quick Start

### 1. Add Dependency

Add this module to your project's `pom.xml`:

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-sdk-http-client-vertx</artifactId>
    <version>${a2a.version}</version>
</dependency>
```

You also need the Vert.x WebClient dependency (if not already present):

```xml
<dependency>
    <groupId>io.vertx</groupId>
    <artifactId>vertx-web-client</artifactId>
</dependency>
```

**For Quarkus**: Vert.x is already included, so you only need to add the `a2a-java-sdk-http-client-vertx` dependency.

### 2. Automatic Discovery (No Code Changes)

The Vert.x HTTP client is automatically discovered via **Java SPI (Service Provider Interface)**:

```java
// No changes needed - A2A SDK automatically uses VertxA2AHttpClient
A2ACardResolver resolver = new A2ACardResolver("http://localhost:9999");
AgentCard card = resolver.getAgentCard(); // Uses Vert.x under the hood

// Client creation also uses Vert.x automatically
Client client = Client.builder(card)
    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
    .build();
```

The `VertxA2AHttpClientProvider` has **priority 100**, which is higher than the JDK implementation's priority (50). The SDK's `A2AHttpClientFactory` uses `ServiceLoader` to discover and select the highest-priority provider available.

### 3. No Configuration Required

The module works out-of-the-box with sensible defaults:
- HTTP keep-alive enabled
- Automatic redirect following
- Automatic HTTP/2 negotiation

## Usage Examples

### Basic Usage (Automatic Discovery)

```java
// The A2A SDK internally uses A2AHttpClient for all HTTP operations
// With vertx-http-client on the classpath, it automatically uses VertxA2AHttpClient

// Example 1: Fetching agent card
A2ACardResolver resolver = new A2ACardResolver("http://localhost:9999");
AgentCard card = resolver.getAgentCard();

// Example 2: Using REST transport (uses HTTP client internally)
Client client = Client.builder(card)
    .withTransport(RESTTransport.class, new RESTTransportConfig())
    .build();

Message message = A2A.toUserMessage("Hello!");
client.sendMessage(message);
```

### Direct Usage (Advanced)

If you need direct access to the HTTP client (rare):

```java
import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpClientFactory;
import io.a2a.client.http.A2AHttpResponse;

// Get the client via factory (returns VertxA2AHttpClient if available)
try (A2AHttpClient client = A2AHttpClientFactory.create()) {
    // Simple GET request
    A2AHttpResponse response = client.createGet()
        .url("https://api.example.com/data")
        .addHeader("Authorization", "Bearer token")
        .get();

    if (response.success()) {
        System.out.println(response.body());
    }
}
```

### POST Request with JSON Body

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

### Server-Sent Events (SSE) Streaming

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
    future.join(); // Wait for completion if needed
}
```

## Advanced Configuration

### Using an External Vert.x Instance

In Quarkus or other CDI environments, you can inject an existing Vert.x instance:

#### Quarkus Example

```java
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MyService {

    @Inject
    Vertx vertx;

    public void doSomething() {
        // VertxA2AHttpClient will automatically discover and use the CDI-managed Vertx
        try (A2AHttpClient client = A2AHttpClientFactory.create()) {
            // The client internally reuses the injected Vertx instance
            A2AHttpResponse response = client.createGet()
                .url("https://example.com")
                .get();
        }
    }
}
```

The `VertxA2AHttpClient` constructor automatically checks for a CDI-managed `Vertx` instance and reuses it if available. This ensures that your entire application shares the same Vert.x event loop.

#### Manual Vertx Instance (Non-CDI)

If you're not using CDI but want to share a Vert.x instance:

```java
import io.a2a.client.http.VertxA2AHttpClient;
import io.vertx.core.Vertx;

// Create Vertx instance once
Vertx vertx = Vertx.vertx();

try {
    // Create client with shared Vertx instance
    try (VertxA2AHttpClient client = new VertxA2AHttpClient(vertx)) {
        A2AHttpResponse response = client.createGet()
            .url("https://example.com")
            .get();
    }
    // Client is closed, but Vertx instance remains open
} finally {
    // Close Vertx when application shuts down
    vertx.close();
}
```

### Custom WebClient Configuration

For advanced use cases requiring custom Vert.x WebClient configuration, you can create your own provider:

```java
import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.VertxA2AHttpClient;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

// Create custom Vertx instance with specific options
Vertx vertx = Vertx.vertx();

// Note: VertxA2AHttpClient doesn't expose WebClient customization directly
// For custom WebClient options, you would need to extend VertxA2AHttpClient
// or configure Vert.x-level options
```

## How It Works

### Service Provider Interface (SPI)

The module uses Java's `ServiceLoader` mechanism for automatic discovery:

1. **Provider Registration**: `META-INF/services/io.a2a.client.http.A2AHttpClientProvider` contains:
   ```
   io.a2a.client.http.VertxA2AHttpClientProvider
   ```

2. **Priority System**: Each provider has a priority:
   - `VertxA2AHttpClientProvider`: **100** (when Vert.x is available)
   - `JdkA2AHttpClientProvider`: **50** (always available)

3. **Automatic Selection**: `A2AHttpClientFactory.create()` uses the highest-priority available provider

4. **Graceful Fallback**: If Vert.x classes are not on the classpath, the provider returns priority `-1` and the SDK falls back to JDK HttpClient

### Lifecycle Management

#### Standalone Usage
```java
// Client owns Vertx instance
try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
    // Use client
} // Both WebClient and Vertx are closed
```

#### CDI/Quarkus Usage
```java
// Client uses externally-managed Vertx
try (VertxA2AHttpClient client = new VertxA2AHttpClient(injectedVertx)) {
    // Use client
} // Only WebClient is closed, Vertx remains open
```

### Thread Safety

- **Client Instance**: Thread-safe - multiple threads can use the same client
- **Builder Instances**: NOT thread-safe - create separate builders per thread
- **Vertx Event Loop**: All I/O operations execute on Vert.x event loop threads

## Performance Characteristics

### Synchronous Methods (`.get()`, `.post()`, `.delete()`)

Despite using Vert.x's async API internally, these methods block the calling thread:

```java
A2AHttpResponse response = client.createGet()
    .url("https://example.com")
    .get(); // ← Blocks until response received
```

**Why block?** The `A2AHttpClient` interface is designed for synchronous operations to simplify SDK usage. Vert.x's async execution still provides benefits:
- Non-blocking I/O at the network layer
- Efficient connection pooling
- Lower thread usage overall

### Async Methods (`.getAsyncSSE()`, `.postAsyncSSE()`)

True async operation - returns immediately with a `CompletableFuture`:

```java
CompletableFuture<Void> future = client.createGet()
    .url("https://example.com/stream")
    .getAsyncSSE(
        message -> handleMessage(message),
        error -> handleError(error),
        () -> handleComplete()
    ); // ← Returns immediately

// Do other work
future.join(); // Optional: wait for completion
```

## Troubleshooting

### Client Not Being Used

**Symptom**: Logs show `JdkA2AHttpClient` instead of `VertxA2AHttpClient`

**Cause**: Vert.x WebClient not on classpath or version incompatibility

**Solution**:
1. Verify dependency is present:
   ```bash
   mvn dependency:tree | grep vertx-web-client
   ```

2. For Quarkus, ensure Vert.x version matches:
   ```xml
   <dependency>
       <groupId>io.quarkus</groupId>
       <artifactId>quarkus-vertx</artifactId>
   </dependency>
   ```

### ClassNotFoundException for Vert.x

**Symptom**: `ClassNotFoundException: io.vertx.core.Vertx`

**Solution**: Add Vert.x WebClient dependency:
```xml
<dependency>
    <groupId>io.vertx</groupId>
    <artifactId>vertx-web-client</artifactId>
    <version>4.x.x</version> <!-- Use version compatible with your framework -->
</dependency>
```

### Memory Leaks

**Symptom**: `Vertx` instances not being closed

**Cause**: Not closing `VertxA2AHttpClient` when created with no-args constructor

**Solution**: Always use try-with-resources:
```java
try (VertxA2AHttpClient client = new VertxA2AHttpClient()) {
    // Use client
} // Automatically closed
```

## Version Compatibility

- **Java**: 17+ (same as A2A SDK)
- **Vert.x**: 4.x (tested with 4.5.0+)
- **Quarkus**: Any version using Vert.x 4.x
- **Jakarta EE**: 9.0+ (for CDI discovery)

## Additional Resources

- [Vert.x WebClient Documentation](https://vertx.io/docs/vertx-web-client/java/)
- [A2A Protocol Specification](https://a2a-protocol.org/)
- [Quarkus Vert.x Guide](https://quarkus.io/guides/vertx)

---

*This module is part of the A2A Java SDK extras and provides production-ready reactive HTTP support for high-performance A2A applications.*
