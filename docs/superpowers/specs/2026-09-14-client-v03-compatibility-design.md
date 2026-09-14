# 1.0 Client Support for A2A 0.3 Servers

## Status

Design study only. This document authorizes no implementation.

## Goal

Let an application use the existing concrete `org.a2aproject.sdk.client.Client` and the 1.0 spec types when calling either 1.0 or 0.3 A2A servers. Protocol selection and 0.3 type translation are internal implementation details.

The default remains a 1.0-only client. Legacy support is explicit and is only available when optional compatibility artifacts are on the classpath.

## Public API Contract

Existing 1.0 client construction remains valid:

```java
AgentCard card = A2A.getAgentCard("https://agent.example");

Client client = Client.builder(card)
        .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
        .build();
```

Legacy discovery is explicit on `A2A.getAgentCard(...)`. An overload accepts a supported-version policy; its default supports only `"1.0"`:

```java
AgentCard card = A2A.getAgentCard(
        "https://agent.example", supportedVersions("1.0", "0.3"));
```

The version policy filters discovered interface candidates before `ClientBuilder` sees the returned card. `ClientBuilder` preserves its existing API while selecting the provider appropriate to each retained interface's protocol version. Including a compatibility JAR does not alter default 1.0-only discovery or cause automatic protocol downgrade. When a caller requests `"0.3"` but the corresponding compatibility provider is absent, card resolution fails with a descriptive error that names the required artifact.

Applications continue to use only 1.0 `Client`, `AgentCard`, request/result, event, configuration, context, and interceptor classes. They do not import `Client_v0_3` or 0.3 spec types.

## Card Resolution and Version Selection

`A2A.getAgentCard(...)` remains the discovery entry point. Its existing overloads preserve 1.0-only behavior; new overloads or resolver options carry the explicit version policy.

1. It fetches the card once.
2. It parses a usable 1.0 card normally.
3. Only when the caller opted into `"0.3"`, and a compatibility card resolver is available through `ServiceLoader`, a legacy-only card is parsed from the same raw JSON as `AgentCard_v0_3`.
4. The compatibility resolver projects the legacy card into a public 1.0 `AgentCard`, with selected interfaces marked `protocolVersion = "0.3"`.
5. The resolver filters candidates by the requested versions and returns a public card containing the eligible interfaces.
6. `ClientBuilder` selects a configured binding for an eligible interface. Its candidates are keyed by `(protocolBinding, protocolVersion)`, not by binding alone.

The fallback is shape-based, not solely exception-based. The current 1.0 resolver uses the 1.0 protobuf mapper, which ignores the legacy top-level fields, so a legacy card can otherwise parse without providing usable 1.0 interfaces. The lower-level raw-card parser SPI belongs in `http-client` (or a new discovery module below the client transport SPI), so it cannot create a dependency cycle with `client-transport-spi`.

For a cohosted server, the documented dual-format card remains unchanged. Its usable 1.0 interface is preferred over a 0.3 interface with the same binding, including when client transport preference is enabled. The legacy fields continue to support existing 0.3 clients.

The compatibility projection must contain enough information to recreate the `AgentCard_v0_3` required by the underlying 0.3 transport. It must not use a global cache keyed by a public `AgentCard`. A new bidirectional agent-card mapper must preserve all fields required by legacy transport selection and authentication; unrepresentable features, including the legacy state-transition-history capability, are not advertised through the 1.0 projection.

## Internal Delegate Design

`Client` remains concrete and owns the existing 1.0 callback, `ClientEvent`, task-tracking, error-handler, and `close()` behavior.

It already delegates wire operations to `ClientTransport`. A compat provider supplies a 1.0 `ClientTransport` implementation that wraps the matching 0.3 transport:

```text
Client (1.0 public API)
  -> ClientTransport
       -> native 1.0 transport, or
       -> 0.3 adapter transport -> 0.3 wire transport
```

The adapter maps 1.0 requests to 0.3, delegates the request, and maps responses and streaming events back to 1.0. It must not wrap `Client_v0_3` as its primary boundary: that client owns callback dispatch and its send methods do not return the raw values required by the 1.0 `ClientTransport` contract.

The adapter maps 1.0 HTTP client/channel configuration to the equivalent 0.3 configuration. It bridges 1.0 interceptors by translating payload, card, and call context around each interceptor invocation, so users configure only normal 1.0 interceptors.

Interceptor bridging is binding- and operation-specific. The adapter defines the 1.0 protocol payload presented to each interceptor, validates any replacement payload before translating it back, copies the call context, and preserves headers. For a selected 0.3 binding it forces the legacy version routing and prevents an interceptor from changing the effective A2A version.

## Compatibility Semantics

Supported operations are message send (blocking and streaming), get task, cancel task, subscribe to task, and push-notification configuration operations, subject to 0.3 server support.

The adapter fails locally, before making a request, for a 1.0 feature that cannot be faithfully represented by 0.3:

- `listTasks()`;
- a non-empty tenant;
- extended-agent-card retrieval; mapping it to the 0.3 authenticated-card operation is outside this design's scope.

For list-push-configuration requests, the only accepted pagination values are `pageSize <= 0` and an empty `pageToken`; the adapter returns an empty next-page token. Other pagination values fail locally. 0.3 client/transport exceptions require a client-side mapping to 1.0 `A2AClientException` and compatible 1.0 protocol-error causes.

## Artifact and SPI Boundary

Client and server compatibility code must remain independent.

Create a neutral compatibility-conversion artifact containing the bidirectional 0.3/1.0 mappers. It depends only on the two spec artifacts and MapStruct. The existing `compat-0.3/server-conversion` module consumes it; it must not be a dependency of client applications.

An optional client compatibility adapter artifact consumes the neutral conversion artifact plus the 1.0 client SPI and contributes two separate `ServiceLoader` providers: a raw-card parser at the discovery layer, and a version-aware transport adapter at the client-builder layer. It supplies legacy-card parsing and adapted 0.3 transports. It must not implement the existing binding-only `ClientTransportProvider`: its registry is keyed only by binding, so registering a second JSON-RPC, REST, or gRPC provider would overwrite the native provider. The actual 0.3 JSON-RPC, REST, and gRPC transport artifacts remain independently optional. Selecting an unavailable legacy binding reports the missing transport artifact clearly.

The normal 1.0 client artifacts have no compile-time dependency on 0.3 client, server, CDI, Quarkus, or reference-server artifacts.

## Acceptance Matrix

| Scenario | Expected behavior |
|---|---|
| Standard 1.0 artifacts only | Existing behavior; no 0.3 discovery or routing. |
| Compat present, 0.3 not explicitly enabled | Existing 1.0-only discovery and behavior. |
| 0.3 explicitly enabled, adapter absent | Construction error naming the compatibility artifact. |
| Standalone 0.3 card, adapter installed | Normal concrete `Client`, using 1.0 types and an adapted transport. |
| Dual-format/cohosted card | Native 1.0 interface preferred. |
| 0.3 JSON-RPC, REST, gRPC | Supported operations use 1.0 public types and correct legacy wire protocol. |
| 1.0-only operation/tenant against 0.3 | Local descriptive failure; no wire request. |
| Authentication/interceptors | Existing 1.0 interceptor API applies through the adapter. |
| Streaming error and close | Existing 1.0 error callback behavior; adapter close is idempotent and documents caller ownership of gRPC channels. |

## Required Validation

Tests must cover the matrix above and run against genuinely 0.3-only JSON-RPC, REST, and gRPC servers or wire fixtures. Cohosted reference servers are useful regression coverage but insufficient on their own, because their 1.0 endpoint can mask an invalid legacy request. Include card-shape fallback, explicit opt-in, absent-provider, unavailable-transport, auth/interceptor observation and mutation, all streaming-event variants, error-cause mapping, local rejection, custom card path/auth headers, both interface orders, and both client/server transport-preference modes. Classpath-absence tests must use isolated module/runtime classpaths because the current provider registry is initialized statically.
