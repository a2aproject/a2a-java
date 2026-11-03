---
title: Backward Compatibility
description: Serve v1.0 and v0.3 A2A protocol versions simultaneously — multi-version modules, version routing, and v0.3 client support.
layout: page
---

# Backward Compatibility with v0.3

Add compat modules alongside v1.0 modules to serve both protocol versions simultaneously. No changes to your `AgentExecutor` are needed.

## Server: Multi-Version Module (recommended)

```xml
<!-- JSON-RPC with automatic v1.0 + v0.3 routing -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-multiversion-jsonrpc</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>

<!-- REST with automatic v1.0 + v0.3 routing -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-multiversion-rest</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

## Server: Individual Compat Modules

```xml
<!-- v0.3 JSON-RPC support -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-reference-jsonrpc</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>

<!-- v0.3 REST support -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-reference-rest</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>

<!-- v0.3 gRPC support -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-reference-grpc</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

## How Version Routing Works

- **JSON-RPC and REST**: When serving multiple protocol versions, version routing inspects the `A2A-Version` HTTP header on each request. If the header is `"1.0"`, the request is routed to the v1.0 handler. If it is `"0.3"` or absent, the request is routed to the v0.3 handler.
- **gRPC**: Version dispatch is implicit — v0.3 clients use the `a2a.v1` protobuf package and v1.0 clients use `lf.a2a.v1`, so requests are routed to the correct service automatically.
- **Agent card**: When both v1.0 and v0.3 are enabled, the v1.0 `AgentCard` takes precedence and is served at `/.well-known/agent-card.json`. The v0.3 `AgentCard_v0_3` is ignored. If only v0.3 is enabled, the v0.3 agent card is used. If only v1.0 is enabled, the v1.0 agent card is used as-is.

## Making the v1.0 Agent Card Compatible with v0.3 Clients

When serving both protocol versions, you need to ensure the v1.0 agent card contains fields that v0.3 clients expect. Existing v0.3 client implementations (in any language) look for `url`, `preferredTransport`, and `additionalInterfaces` with `transport`/`url` entries — fields that don't exist in the v1.0 format by default.

To make your v1.0 `AgentCard` parsable by v0.3 clients, set these fields on the builder:

```java
AgentCard card = AgentCard.builder()
        .name("My Agent")
        // ... other v1.0 fields ...
        .supportedInterfaces(List.of(
                new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:9999")))
        // v0.3 backward-compatibility fields:
        .url("http://localhost:9999")
        .preferredTransport(TransportProtocol.JSONRPC.asString())
        .additionalInterfaces(List.of(
                new Legacy_0_3_AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:9999")))
        .build();
```

The two interface lists serve different clients:

- `supportedInterfaces` — used by **v1.0 clients** to discover endpoints (uses `AgentInterface` with `protocolBinding`/`url`/`tenant` fields)
- `additionalInterfaces` — used by **v0.3 clients** to discover endpoints (uses `Legacy_0_3_AgentInterface` with v0.3 field names: `transport`/`url`)
- `url` and `preferredTransport` — top-level fields that v0.3 clients use to discover the primary endpoint

## Push Notification Behavior

Push notification payloads are automatically formatted to match the protocol version used when the push notification configuration was registered. When a v0.3 client registers a push notification configuration (via any transport), the server records the protocol version alongside the configuration. When a notification is later sent to that webhook, the payload is formatted as a v0.3 Task object. Configurations registered by v1.0 clients receive v1.0 `StreamResponse` payloads as usual. This happens transparently — no additional configuration is needed beyond adding the compat reference module.

## Client: Communicating with v0.3 Agents

Use `Client_v0_3` to communicate with agents that only support protocol v0.3:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-client</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-client-transport-jsonrpc</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

gRPC and REST transports are also available:
- `a2a-java-sdk-compat-0.3-client-transport-grpc`
- `a2a-java-sdk-compat-0.3-client-transport-rest`

```java
// getAgentCard() handles agent card discovery internally
AgentCard_v0_3 agentCard = A2A_v0_3.getAgentCard("http://localhost:1234");

Client_v0_3 client = Client_v0_3.builder(agentCard)
        .withTransport(JSONRPCTransport_v0_3.class, new JSONRPCTransportConfigBuilder_v0_3())
        .build();
```

**Note:** `Client_v0_3` exposes only operations available in protocol v0.3. For example, `listTasks()` is not available (it was added in v1.0). Return types use v0.3 domain objects from the `org.a2aproject.sdk.compat03.spec` package.
