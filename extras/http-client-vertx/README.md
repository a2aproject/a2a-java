# A2A Java SDK - Vertx HTTP Client

This module provides an HTTP client implementation of the `HttpClient` interface that relies on Vertx for the HTTP transport communication.

By default, the A2A client is relying on the default JDK HttpClient implementation. While this one is convenient for most of use-cases, it may still
be relevant to switch to the Vertx based implementation, especially when your current code is already relying on Vertx or if your A2A server is based on Quarkus which, itself, heavily relies on Vertx.

## Quick Start

This section will get you up and running quickly with a `Client` using the `VertxHttpClient` implementation.

### 1. Add Dependency

Add this module to your project's `pom.xml`:

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-extras-http-client-vertx</artifactId>
    <version>${a2a.version}</version>
</dependency>
```

### 2. Configure Client

##### JSON-RPC Transport Configuration

For the JSON-RPC transport, to use the default `JdkHttpClient`, provide a `JSONRPCTransportConfig` created with its default constructor.

To use a custom HTTP client implementation, simply create a `JSONRPCTransportConfig` as follows:

```java
import io.a2a.client.http.vertx.VertxHttpClientBuilder;

// Create a Vertx HTTP client
HttpClientBuilder vertxHttpClientBuilder = new VertxHttpClientBuilder();

// Configure the client settings
ClientConfig clientConfig = new ClientConfig.Builder()
        .setAcceptedOutputModes(List.of("text"))
        .build();

Client client = Client
        .builder(agentCard)
        .clientConfig(clientConfig)
        .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig(vertxHttpClientBuilder))
        .build();
```

## Configuration Options

This implementation allows to pass the Vertx context you want to rely on, but also the HTTPClientOptions, in case
you want / need to provide some extended configuration's properties such as a better of management of SSL Context, or an HTTP proxy.

```java
import io.a2a.client.http.vertx.VertxHttpClientBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.ProxyOptions;

// Create a Vertx HTTP client
HttpClientBuilder vertxHttpClientBuilder = new VertxHttpClientBuilder()
        .vertx(Vertx.vertx())
        .options(new HttpClientOptions().setProxyOptions(new ProxyOptions().setHost("host").setPort("1234")));

        // Configure the client settings
        ClientConfig clientConfig = new ClientConfig.Builder()
                .setAcceptedOutputModes(List.of("text"))
                .build();

        Client client = Client
                .builder(agentCard)
                .clientConfig(clientConfig)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig(vertxHttpClientBuilder))
                .build();
```
