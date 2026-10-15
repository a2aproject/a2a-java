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

The normal concrete 1.0 `Client` can communicate with a 0.3-only agent when
legacy support is explicitly requested during agent-card discovery. The
compatibility parser and one binding adapter are optional dependencies:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-client-adapter</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-client-adapter-jsonrpc</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

Use `a2a-java-sdk-compat-0.3-client-adapter-rest` for REST or
`a2a-java-sdk-compat-0.3-client-adapter-grpc` for gRPC instead.

```java
AgentCard agentCard = A2A.getAgentCard(
        "http://localhost:1234", Set.of("1.0", "0.3"));

Client client = Client.builder(agentCard)
        .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder()
                .httpClient(A2AHttpClientFactory.create())
                .build())
        .build();
```

The returned card contains a 1.0 `AgentInterface` whose protocol version is
`"0.3"`, so the ordinary builder selects the matching optional adapter through
the versioned transport-provider SPI. With the default server preference,
the builder selects the first usable interface in the card's order. Requesting
both versions does not give 1.0 priority over an earlier 0.3 interface. With
client transport preference enabled, the builder first restricts selection to
usable 1.0 interfaces if any configured binding provides one, then applies
configured binding order. It considers 0.3 only when no such 1.0 interface is
available.

The adapter rejects 1.0 operations that have no 0.3 equivalent (such as
`listTasks`), non-empty tenant values, extended-agent-card retrieval, and
non-default push-configuration pagination before any network request. Generic
1.0 transport parameters are also unsupported for 0.3 adapters. REST and gRPC
also reject non-empty `Message.referenceTaskIds` and cancellation metadata,
which the 0.3 protobuf schema cannot represent. JSON-RPC preserves these fields.
Validation also applies to requests modified by interceptors.

JSON-RPC retains the original JSON numbers in fields whose protobuf values
interceptors leave unchanged. Interceptors see protobuf doubles. Fields with
identical protobuf values are treated as unchanged, including replacements or
reordering of numbers with the same double representation. Such mutations are
unsupported; use a string for
an identifier that must be edited with exact precision. If an interceptor edits
an array containing numbers that protobuf cannot represent exactly, the adapter
rejects the request unless both the original and modified array contain a single
element. This also applies to message parts and arrays in data or metadata.
Unchanged arrays retain their original values. REST and gRPC use protobuf
numeric precision throughout.

If 0.3 is not requested, the optional parser is not used. If it is requested
but the parser or binding adapter is absent, discovery or client construction
fails with an actionable error identifying the missing optional artifact.
Client-only applications do not need to depend on 0.3 domain types, server
libraries, CDI, Quarkus, or reference-server modules.
