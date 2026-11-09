---
title: HTTP Clients
description: Alternative HTTP client implementations for the A2A Java SDK, including Vert.x WebClient for reactive applications and an Android-specific client.
layout: page
---

# HTTP Clients

The A2A SDK uses an `A2AHttpClient` abstraction for all HTTP communication — fetching agent cards, making transport calls, and Server-Sent Events (SSE) streaming. By default, the SDK uses a JDK 11+ HttpClient implementation. These extras provide alternative implementations or a CDI-based extension point for custom clients.

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

The `VertxA2AHttpClientProvider` has **priority 100**. The SDK's `A2AHttpClientFactory` discovers all registered providers via `ServiceLoader`, sorts them by descending priority, and tries each in order, falling back to the next one on failure.

Use `A2AHttpClientFactory.create(A2AHttpClientConfiguration)` for both the default client and
custom provider selection. To force the JDK provider, exclude the other built-in providers:

```java
// Force the JDK client regardless of what else is on the classpath
try (A2AHttpClient client = A2AHttpClientFactory.create(
        A2AHttpClientConfiguration.builder()
            .excludedProviderNames(List.of("cdi", "android", "vertx"))
            .build())) {
    // ...
}
```

Available built-in provider names: `jdk`, `vertx`, `android`, `cdi`.


```java
// No changes needed — A2A SDK automatically uses VertxA2AHttpClient
A2ACardResolver resolver = A2ACardResolver.builder().baseUrl("http://localhost:9999").build();
AgentCard card = resolver.getAgentCard(); // Uses Vert.x under the hood

Client client = Client.builder(card)
    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
    .build();
```

If Vert.x classes are not on the classpath, the provider returns priority `-1` and the SDK falls back to the next available provider.

## Configuring an A2A HTTP Client

Use `A2AHttpClientConfiguration` to select providers and configure optional SSE parsing and
redirect-header settings in one place. Providers that do not support a requested setting are
skipped, so configuration is never silently ignored.

```java
import java.net.URI;
import java.util.List;
import java.util.Set;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientConfiguration;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.client.http.RedirectHeaderPolicy;
import org.a2aproject.sdk.client.http.SSEParserConfig;

SSEParserConfig sseConfig = SSEParserConfig.builder()
    .maxLineLength(64 * 1024)
    .build();

RedirectHeaderPolicy redirectPolicy = new RedirectHeaderPolicy(
    Set.of("X-Trusted-Header"),
    Set.of(URI.create("https://trusted-agent.example")));

A2AHttpClientConfiguration configuration = A2AHttpClientConfiguration.builder()
    .excludedProviderNames(List.of("cdi"))
    .sseParserConfig(sseConfig)
    .redirectHeaderPolicy(redirectPolicy)
    .build();

A2AHttpClient client = A2AHttpClientFactory.create(configuration);
```

The additional header is sent only when the redirect target has the trusted origin's scheme,
host, and port. Other cross-origin redirects receive only the default safe headers. Configure
trusted origins with HTTPS when forwarding credentials or other sensitive headers.

For default provider selection without extra settings, use
`A2AHttpClientFactory.create(A2AHttpClientConfiguration.DEFAULT)`.

### Usage Examples

#### GET Request

```java
try (A2AHttpClient client = A2AHttpClientFactory.create(A2AHttpClientConfiguration.DEFAULT)) {
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
try (A2AHttpClient client = A2AHttpClientFactory.create(A2AHttpClientConfiguration.DEFAULT)) {
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
try (A2AHttpClient client = A2AHttpClientFactory.create(A2AHttpClientConfiguration.DEFAULT)) {
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

### SSE Parser Limits

The Vert.x client uses the shared SSE parser with `SSEParserConfig.DEFAULT`: a 1 MiB byte limit per raw line, at most 1,000 data lines per event, and at most 1,048,576 characters across an event's data values. Large events are parsed incrementally across response buffers.

When a limit is exceeded, the client reports an error through the error callback, discards the affected event, and continues with the next event. Custom SSE limits currently require the JDK HTTP client.

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


HTTP client implementation for Android applications, using `HttpURLConnection` under the hood — no external dependencies required.

### When to Use

- Android applications targeting API level 24+ (Android 7.0+)
- Mobile apps that need to communicate with A2A agents

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-http-client-android</artifactId>
</dependency>
```


### 2. Automatic Discovery (No Code Changes)

The `AndroidA2AHttpClientProvider` has **priority 110** and detects the Android runtime automatically. On non-Android runtimes, the provider returns priority `-1` and the SDK falls back to the next available client.

### Features

- **GET, POST, DELETE** requests with fluent builder API
- **SSE streaming** via `getAsyncSSE()` and `postAsyncSSE()` — async operations run on a dedicated cached thread pool (`A2A-Android-Net`)
- **Timeouts**: 15 s connection, 60 s read
- **Response size limit**: 10 MB

## CDI HTTP Client

Allows providing a fully custom `A2AHttpClient` bean through the CDI container. When this module is on the classpath and a user-provided `A2AHttpClient` bean is registered, it is used in place of any other provider.

### When to Use

- Quarkus or Jakarta EE applications that need a custom HTTP client (specific TLS configuration, connection pooling, tracing, etc.)
- When neither the JDK nor the Vert.x client meets your requirements out of the box

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-http-client-cdi</artifactId>
</dependency>
```

### 2. Produce an A2AHttpClient Bean

Declare a CDI producer in your application. The SDK will discover and use it automatically:

```java
@ApplicationScoped
public class MyHttpClientProducer {

    @Produces
    @ApplicationScoped
    public A2AHttpClient produce(MyConfig config) {
        // return any A2AHttpClient implementation
    }
}
```

> **Scope requirement:** Use a normal scope such as `@ApplicationScoped`. A `@Dependent`-scoped producer will leak a new unmanaged instance on every `create()` call because the `Instance` used to obtain the bean is discarded immediately and CDI cannot destroy the dependent instance.

### How It Works

`CdiA2AHttpClientProvider` has **priority 200** — the highest of all built-in providers (Android: 110, Vert.x: 100, JDK: 0). The `A2AHttpClientFactory` iterates providers in descending priority order and falls back to the next one if a provider is unavailable:

| Priority | Provider | Activates when |
|----------|----------|----------------|
| 200 | CDI | A CDI container is active and an `A2AHttpClient` bean is registered |
| 110 | Android | Android runtime detected |
| 100 | Vert.x | `vertx-web-client` is on the classpath |
| 0 | JDK | Always (fallback) |

If the CDI container is active but no `A2AHttpClient` bean is registered, this provider is skipped and the next available provider is used.

### Compatibility with OpenTelemetry

When the [OpenTelemetry server module](opentelemetry.md) is on the classpath alongside a custom `A2AHttpClient` CDI bean, the OpenTelemetry module wraps your bean with trace-context propagation rather than replacing it. Your custom configuration (mTLS, proxy, authentication, timeouts, etc.) is preserved for push notification delivery while `traceparent` and `tracestate` headers are injected automatically.

This works through the `@Instrumented` CDI qualifier (defined in `server-common`): the OpenTelemetry module produces an `@Instrumented A2AHttpClient` that delegates to your unqualified bean. `BasePushNotificationSender` prefers the `@Instrumented` variant when available, so both your custom configuration and the trace-context headers are present on outbound requests. No changes to your producer code are required.
