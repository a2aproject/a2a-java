# PRD: A2A Protocol 0.3 Backward Compatibility Layer

> **Goal**: Allow the A2A Java SDK (currently targeting protocol v1.0) to interoperate with remote agents still running protocol v0.3.

---

## Motivation

The A2A protocol moved from v0.3 to v1.0 with significant breaking changes. Existing agents deployed with v0.3 will not be immediately upgraded. This compatibility layer enables a v1.0 SDK user to communicate with v0.3 agents across all three transports (JSON-RPC, gRPC, REST).

---

## Scope

### In Scope

- Dedicated `compat-0.3` Maven module (multi-module) containing **only** 0.3-specific code
- gRPC code generation from the v0.3 `a2a.proto`
- A dedicated v0.3 client (`Compat03Client`) exposing only features available in v0.3
- Server-side transport handlers for accepting v0.3 requests (JSON-RPC, gRPC, REST)
- Mapping layer between v0.3 and v1.0 domain objects
- Quarkus reference server implementations for v0.3
- TCK conformance tests for v0.3
- Inclusion in the SDK BOM as a separate optional dependency

### Out of Scope

- Changes to the existing v1.0 modules (no regressions, no API changes)
- Automatic protocol version detection (client must explicitly check `protocolVersion` from the agent card and choose the appropriate client API)
- Extras modules (OpenTelemetry, JPA stores, etc.)

---

## Breaking Changes: v0.3 → v1.0

The compatibility layer must bridge the following differences:

### 1. Proto Package Namespace
| Aspect | v0.3 | v1.0 |
|--------|------|------|
| Package | `a2a.v1` | `lf.a2a.v1` |

### 2. RPC Method Changes
| v0.3 | v1.0 | Change |
|------|------|--------|
| `TaskSubscription` | `SubscribeToTask` | Renamed |
| `GetAgentCard` | `GetExtendedAgentCard` | Renamed |
| `ListTaskPushNotificationConfig` | `ListTaskPushNotificationConfigs` | Pluralized |
| `CreateTaskPushNotificationConfig(CreateTaskPushNotificationConfigRequest)` | `CreateTaskPushNotificationConfig(TaskPushNotificationConfig)` | Parameter type changed |
| — | `ListTasks` | New in v1.0 (no v0.3 equivalent) |

### 3. HTTP Endpoint Changes
| v0.3 | v1.0 |
|------|------|
| `/v1/message:send` | `/message:send` (+ `/{tenant}/message:send`) |
| `/v1/message:stream` | `/message:stream` (+ tenant) |
| `/v1/{name=tasks/*}` | `/tasks/{id=*}` (+ tenant) |
| `/v1/{name=tasks/*}:cancel` | `/tasks/{id=*}:cancel` (+ tenant) |
| `/v1/{name=tasks/*}:subscribe` | `/tasks/{id=*}:subscribe` (+ tenant) |
| `/v1/card` | `/extendedAgentCard` (+ tenant) |
| `/v1/{parent=task/*/pushNotificationConfigs}` | `/tasks/{task_id=*}/pushNotificationConfigs` (+ tenant) |

### 4. Configuration Field Changes
| v0.3 `SendMessageConfiguration` | v1.0 `SendMessageConfiguration` |
|----------------------------------|----------------------------------|
| `push_notification` (PushNotificationConfig) | `task_push_notification_config` (TaskPushNotificationConfig) |
| `blocking` (bool, default true) | `return_immediately` (bool, default false) — inverted semantics |
| `history_length` (int32, 0 = unlimited) | `history_length` (optional int32, unset = no limit) |

### 5. AgentCard / AgentInterface Changes
| v0.3 | v1.0 |
|------|------|
| `url` + `preferred_transport` on AgentCard | Removed; replaced by `supported_interfaces` |
| `additional_interfaces` | Folded into `supported_interfaces` |
| No `tenant` field | `tenant` field added to AgentInterface |
| `transport` field | Renamed to `protocol_binding` |

### 6. Task State Naming
| v0.3 | v1.0 |
|------|------|
| `TASK_STATE_CANCELLED` | `TASK_STATE_CANCELED` |

### 7. Structural Changes
- v1.0 removed the `kind` discriminator field from messages
- v1.0 added `reference_task_ids` to `Message`
- v1.0 added `TASK_STATE_REJECTED` enum value (no v0.3 equivalent)

---

## Design Decisions

### Dedicated v0.3 Client

The compat layer exposes a **dedicated `Compat03Client`** that only provides features available in v0.3. This means:

- No `listTasks()` method (absent in v0.3)
- Method names reflect v0.3 semantics where they differ
- The client is a standalone API, not a wrapper around the v1.0 `Client`

Users must explicitly check the `protocolVersion` field from the agent card and instantiate the correct client accordingly. No automatic version detection.

### TASK_STATE_REJECTED Handling

`TASK_STATE_REJECTED` (v1.0-only) is mapped to `TASK_STATE_FAILED` when converting to v0.3 wire format. The original state is preserved in metadata (`"original_state": "REJECTED"`) so information is not entirely lost. Both are terminal states, so v0.3 clients can handle the result correctly.

### BOM Inclusion

The compat-0.3 modules are included in the SDK BOM as **separate optional dependencies**, so users opt in explicitly.

---

## Module Structure

All compatibility code lives under a top-level `compat-0.3/` directory, mirroring the structure of the main SDK modules but containing **only** the code needed for 0.3 interop.

```
compat-0.3/
├── pom.xml                          # Parent POM for all compat-0.3 submodules
├── spec-grpc/                       # v0.3 proto + generated classes
│   ├── pom.xml
│   └── src/main/
│       ├── proto/a2a.proto          # v0.3 proto file (package a2a.v1)
│       └── java/io/a2a/compat03/grpc/
│           ├── [generated classes]
│           └── mapper/              # MapStruct mappers: v0.3 proto ↔ v1.0 spec types
├── client/                          # v0.3-compatible client
│   ├── base/                        # Compat03Client — dedicated 0.3 API
│   │   └── pom.xml
│   └── transport/
│       ├── jsonrpc/                  # JSON-RPC client transport for v0.3
│       │   └── pom.xml
│       ├── grpc/                     # gRPC client transport for v0.3
│       │   └── pom.xml
│       └── rest/                     # REST client transport for v0.3
│           └── pom.xml
├── transport/                       # Server-side transport handlers for v0.3
│   ├── jsonrpc/                     # Accept v0.3 JSON-RPC requests
│   │   └── pom.xml
│   ├── grpc/                        # Accept v0.3 gRPC requests
│   │   └── pom.xml
│   └── rest/                        # Accept v0.3 REST requests
│       └── pom.xml
├── reference/                       # Quarkus reference servers for v0.3
│   ├── common/                      # Shared reference server base
│   │   └── pom.xml
│   ├── jsonrpc/                     # Reference JSON-RPC server
│   │   └── pom.xml
│   ├── grpc/                        # Reference gRPC server
│   │   └── pom.xml
│   └── rest/                        # Reference REST server
│       └── pom.xml
└── tck/                             # v0.3 conformance tests
    └── pom.xml
```

### Java Package Convention

All compat-0.3 code uses the `io.a2a.compat03` package root to avoid classpath collisions with the v1.0 modules:

- `io.a2a.compat03.grpc` — generated proto classes and mappers
- `io.a2a.compat03.client` — dedicated v0.3 client API
- `io.a2a.compat03.client.transport.jsonrpc` — JSON-RPC client transport
- `io.a2a.compat03.client.transport.grpc` — gRPC client transport
- `io.a2a.compat03.client.transport.rest` — REST client transport
- `io.a2a.compat03.transport.jsonrpc` — server JSON-RPC transport
- `io.a2a.compat03.transport.grpc` — server gRPC transport
- `io.a2a.compat03.transport.rest` — server REST transport
- `io.a2a.compat03.reference` — Quarkus reference server
- `io.a2a.compat03.tck` — conformance tests

---

## Mapping Layer Design

The core of the compatibility layer is a bidirectional mapper between v0.3 and v1.0 types.

### Direction

| Use case | Direction |
|----------|-----------|
| Client sending to v0.3 agent | v1.0 domain → v0.3 wire format |
| Client receiving from v0.3 agent | v0.3 wire format → v1.0 domain |
| Server receiving v0.3 request | v0.3 wire format → v1.0 domain |
| Server sending v0.3 response | v1.0 domain → v0.3 wire format |

### Key Mappings

| v1.0 Spec Type | v0.3 Proto Type | Notes |
|----------------|-----------------|-------|
| `SendMessageConfiguration` | `a2a.v1.SendMessageConfiguration` | `return_immediately` ↔ `!blocking`, `task_push_notification_config` ↔ `push_notification` |
| `Task` | `a2a.v1.Task` | `CANCELED` ↔ `CANCELLED`, no `REJECTED` in v0.3 |
| `TaskState.TASK_STATE_REJECTED` | `TaskState.TASK_STATE_FAILED` | Map to FAILED + metadata `"original_state": "REJECTED"` |
| `AgentCard` | `a2a.v1.AgentCard` | `supported_interfaces` → `url` + `preferred_transport` + `additional_interfaces` |
| `Message` | `a2a.v1.Message` | Drop `reference_task_ids` for v0.3 |

### v1.0 Features Absent in v0.3

| Feature | Handling |
|---------|----------|
| `ListTasks` | Not exposed in `Compat03Client` |
| `TASK_STATE_REJECTED` | Mapped to `TASK_STATE_FAILED` with metadata |
| Tenant support | Silently ignored |
| `reference_task_ids` | Silently dropped |

### Error Mapping

The error handling hierarchy changed significantly between v0.3 and v1.0. The compat layer must translate errors bidirectionally at the wire boundary.

#### Error Hierarchy Changes

| Aspect | v0.3 | v1.0 |
|--------|------|------|
| Base type | `JSONRPCError` (extends `Error`) | `A2AError` (extends `RuntimeException`) |
| Protocol errors | Extend `JSONRPCError` directly | Extend `A2AProtocolError` (extends `A2AError`) |
| Error details field | `Object data` (single value) | `Map<String, Object> details` |
| Error code registry | Hardcoded in constructors | `A2AErrorCodes` enum with gRPC/HTTP mappings |
| JSON serialization | Custom `JSONRPCErrorSerializer`/`JSONRPCErrorDeserializer` (Jackson) | Gson-based serialization |

#### Error Types: v0.3 vs v1.0

| Error Code | v0.3 | v1.0 | Mapping |
|------------|------|------|---------|
| -32700 | `JSONParseError` | `JSONParseError` | Direct (same code) |
| -32600 | `InvalidRequestError` | `InvalidRequestError` | Direct |
| -32601 | `MethodNotFoundError` | `MethodNotFoundError` | Direct |
| -32602 | `InvalidParamsError` | `InvalidParamsError` | Direct |
| -32603 | `InternalError` | `InternalError` | Direct |
| -32001 | `TaskNotFoundError` | `TaskNotFoundError` | Direct |
| -32002 | `TaskNotCancelableError` | `TaskNotCancelableError` | Direct |
| -32003 | — | `PushNotificationNotSupportedError` | v1.0→v0.3: map to generic `JSONRPCError(-32003)` |
| -32004 | — | `UnsupportedOperationError` | v1.0→v0.3: map to generic `JSONRPCError(-32004)` |
| -32005 | — | `ContentTypeNotSupportedError` | v1.0→v0.3: map to generic `JSONRPCError(-32005)` |
| -32006 | — | `InvalidAgentResponseError` | v1.0→v0.3: map to generic `JSONRPCError(-32006)` |
| -32007 | `AuthenticatedExtendedCardNotConfiguredError` | `ExtendedAgentCardNotConfiguredError` | Direct (same code, renamed) |
| -32008 | — | `ExtensionSupportRequiredError` | v1.0→v0.3: map to generic `JSONRPCError(-32008)` |
| -32009 | — | `VersionNotSupportedError` | v1.0→v0.3: map to generic `JSONRPCError(-32009)` |

#### Mapping Strategy

**v0.3 → v1.0 (client receiving errors, server receiving errors):**
- Extract `code` and `message` from v0.3 `JSONRPCError`
- Convert `data` (Object) to `details` (Map): if `data` is a Map, use directly; otherwise wrap as `{"data": value}`
- Use `A2AErrorCodes.fromCode(code)` to instantiate the correct v1.0 error class
- For unknown codes, create a generic `A2AError`

**v1.0 → v0.3 (server sending errors, client sending errors):**
- Extract `code`, `message`, and `details` from v1.0 `A2AError`
- Convert `details` (Map) to `data` (Object): serialize as-is (v0.3 `data` accepts any Object)
- For v1.0-only error types (codes -32003 to -32009 except -32007), produce a generic error with the same code and message — v0.3 clients will see it as an unknown JSON-RPC error but can still read the code and message

---

## Server-Common Integration Notes

The compat server transport layer translates at the **wire boundary only**. It converts v0.3 wire-format requests into v1.0 domain objects and delegates to the existing v1.0 server pipeline (`DefaultRequestHandler` → `AgentExecutor` → `AgentEmitter` → `MainEventBus`). Responses flow back through the mapper to produce v0.3 wire-format responses.

This means the following v1.0 internal changes since v0.3 do **not** require special handling in the compat layer:

- **AgentEmitter** replacing `TaskUpdater` — the compat transport never touches the executor API
- **MainEventBus architecture** — event processing is internal to server-common
- **Domain classes as records** — the compat mappers use v1.0 record accessors directly
- **Package reorganization** (jsonrpc-common) — the compat modules depend on `jsonrpc-common` and import from `io.a2a.jsonrpc.common.wrappers`

The compat layer **does** need to handle:
- Error translation (see Error Mapping above)
- `kind` discriminator: v0.3 JSON includes `kind` field; v1.0 does not — the compat JSON-RPC and REST transports must add/strip this field
- `blocking` ↔ `returnImmediately` semantic inversion at the request parsing boundary

---

## User Experience

### Server: Serving Both v0.3 and v1.0

A server operator that wants to accept both v0.3 and v1.0 clients needs to:

**1. Add the compat Maven dependency alongside their existing reference dependency:**

```xml
<!-- Existing v1.0 transport -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-jsonrpc</artifactId>
</dependency>

<!-- Add v0.3 compatibility -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-reference-jsonrpc</artifactId>
</dependency>
```

The same pattern applies for gRPC and REST transports.

**2. Declare both protocol versions in the AgentCard with separate URLs:**

Each protocol version should use a distinct URL to avoid any dispatch ambiguity. The recommended pattern is to use a `/v0.3` path prefix for compat endpoints:

```java
@Produces @PublicAgentCard
public AgentCard agentCard() {
    return AgentCard.builder()
            .name("My Agent")
            .supportedInterfaces(List.of(
                new AgentInterface("JSONRPC", "http://localhost:9999", "", "1.0"),
                new AgentInterface("JSONRPC", "http://localhost:9999/v0.3", "", "0.3")
            ))
            // ... rest of agent card
            .build();
}
```

Separate URLs are **recommended** because they cleanly isolate the two protocol versions without relying on request-body inspection or method-name dispatch to differentiate them. The compat reference module registers its routes under the `/v0.3` prefix automatically via Quarkus CDI.

> **Note:** Using the same URL for both versions is technically possible (REST paths don't collide since v0.3 uses `/v1/...`; gRPC service packages differ: `a2a.v1` vs `lf.a2a.v1`) but is not recommended — it creates ambiguity for JSON-RPC where both versions share `POST /` and differ only by method names in the request body.

**3. No changes to AgentExecutor:**

The existing `AgentExecutor` implementation works unchanged. The compat reference module registers v0.3 transport endpoints via Quarkus CDI auto-discovery and delegates to the same `AgentExecutor` through the v1.0 server pipeline.

### Client: Talking to a v0.3 Agent

A client application that needs to communicate with a v0.3 agent:

**1. Add the compat client dependency:**

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-client-base</artifactId>
</dependency>
<!-- Plus the desired transport -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-client-transport-jsonrpc</artifactId>
</dependency>
```

**2. Check the agent card and choose the right client:**

The client must inspect the agent card's `supportedInterfaces` to determine which protocol version the agent offers, then instantiate the appropriate client. There is **no automatic version detection** — the user explicitly selects the client based on the advertised protocol version.

```java
AgentCard card = // ... fetch agent card from /.well-known/agent-card.json

for (AgentInterface iface : card.supportedInterfaces()) {
    if ("0.3".equals(iface.protocolVersion())) {
        // Use the compat client for v0.3 agents
        Compat03Client client = Compat03ClientBuilder.forUrl(iface.url())
                .transport("JSONRPC")
                .build();
    } else if ("1.0".equals(iface.protocolVersion())) {
        // Use the standard client for v1.0 agents
        Client client = ClientBuilder.forUrl(iface.url())
                .transport("JSONRPC")
                .build();
    }
}
```

**3. Use the v0.3 client API:**

`Compat03Client` exposes only operations available in v0.3 (no `listTasks()`, etc.). The return types are the standard v1.0 `io.a2a.spec` domain objects — the transport layer handles wire-format translation transparently.

---

## Implementation Plan

### Phase 1: Proto Generation (no dependencies)

**Module**: `compat-0.3/spec-grpc`

1. Retrieve the v0.3 proto file from `https://github.com/a2aproject/A2A/blob/v0.3.0/specification/grpc/a2a.proto`
2. Set `java_package` to `io.a2a.compat03.grpc`
3. Configure `protobuf-maven-plugin` to generate Java sources
4. Generate gRPC service stubs

**Output**: Compiled v0.3 protobuf message classes and gRPC stubs in `io.a2a.compat03.grpc`

---

### Phase 2: Mapping Layer (depends on Phase 1)

**Module**: `compat-0.3/spec-grpc` (mapper sub-package)

1. Create MapStruct mappers for bidirectional conversion between v0.3 proto types and v1.0 `io.a2a.spec` types
2. Handle semantic differences:
   - `blocking` ↔ `return_immediately` inversion
   - `push_notification` ↔ `task_push_notification_config`
   - `CANCELLED` ↔ `CANCELED`
   - `REJECTED` → `FAILED` with metadata
   - `AgentCard` structural flattening/unflattening
   - `name` ↔ `id` field translation (e.g., `tasks/123` → `123`)
3. Define explicit handling for v1.0-only features (drop or omit from client)

**Output**: `io.a2a.compat03.grpc.mapper` package with tested, bidirectional mappers

---

### Phase 3: Client Transports (depends on Phase 2; 3a/3b/3c run in parallel)

Each transport module implements a v0.3-specific client transport, translating calls through the mapper.

#### Phase 3a: JSON-RPC Client Transport
**Module**: `compat-0.3/client/transport/jsonrpc`
- Implement v0.3 JSON-RPC transport
- Use v0.3 method names and payload shapes
- Reuse `http-client` for HTTP communication

#### Phase 3b: gRPC Client Transport
**Module**: `compat-0.3/client/transport/grpc`
- Implement v0.3 gRPC transport using v0.3 stubs
- Map v0.3 service method names (`TaskSubscription`, `GetAgentCard`, etc.)

#### Phase 3c: REST Client Transport
**Module**: `compat-0.3/client/transport/rest`
- Implement v0.3 REST transport
- Use `/v1/` prefixed URL paths
- Handle `{name=tasks/*}` AIP-style resource patterns

**Output**: Three client transport implementations, one per transport

---

### Phase 4: Dedicated v0.3 Client (depends on Phase 3)

**Module**: `compat-0.3/client/base`

1. Create `Compat03Client` with only v0.3-available operations:
   - `sendMessage` / `sendStreamingMessage`
   - `getTask` / `cancelTask`
   - `subscribeToTask` (maps to v0.3 `TaskSubscription`)
   - `createTaskPushNotificationConfig` / `getTaskPushNotificationConfig` / `listTaskPushNotificationConfig` / `deleteTaskPushNotificationConfig`
   - `getAgentCard` (maps to v0.3 `GetAgentCard`)
2. No `listTasks()` — absent in v0.3
3. Create `Compat03ClientBuilder` for construction

**Output**: A dedicated client API for communicating with v0.3 agents

---

### Phase 5: Server Transports (depends on Phase 2; 5a/5b/5c run in parallel)

Server-side handlers accept v0.3 requests, translate to v1.0, delegate to the existing `AgentExecutor`/request handler pipeline, and translate responses back.

#### Phase 5a: JSON-RPC Server Transport
**Module**: `compat-0.3/transport/jsonrpc`
- Accept v0.3 JSON-RPC method names and payloads
- Translate to v1.0 and delegate to existing `DefaultRequestHandler`
- Translate responses back to v0.3 format

#### Phase 5b: gRPC Server Transport
**Module**: `compat-0.3/transport/grpc`
- Implement v0.3 `A2AService` gRPC service (from `a2a.v1` package)
- Delegate to v1.0 server-common components
- Translate responses back to v0.3 proto types

#### Phase 5c: REST Server Transport
**Module**: `compat-0.3/transport/rest`
- Handle v0.3 URL patterns (`/v1/...`, `{name=tasks/*}`)
- Translate to v1.0 and delegate
- Translate responses back

**Output**: Three server transport handlers, one per transport

---

### Phase 6: Reference Servers (depends on Phase 5; 6b/6c/6d run in parallel after 6a)

Quarkus-based reference server implementations for v0.3.

#### Phase 6a: Reference Common
**Module**: `compat-0.3/reference/common`
- Shared Quarkus CDI configuration for v0.3 reference servers
- Reactive route registration for v0.3 endpoints

#### Phase 6b: Reference JSON-RPC Server (depends on 6a)
**Module**: `compat-0.3/reference/jsonrpc`

#### Phase 6c: Reference gRPC Server (depends on 6a)
**Module**: `compat-0.3/reference/grpc`

#### Phase 6d: Reference REST Server (depends on 6a)
**Module**: `compat-0.3/reference/rest`

**Output**: Three runnable Quarkus reference servers serving v0.3 protocol

---

### Phase 7: TCK Tests (depends on Phase 4 + Phase 6)

**Module**: `compat-0.3/tck`

1. Port or adapt existing TCK tests to exercise v0.3 protocol conformance
2. Test each transport (JSON-RPC, gRPC, REST) against the v0.3 reference servers
3. Validate round-trip compatibility: v0.3 client ↔ v0.3 reference server
4. Validate cross-version interop where applicable

**Output**: Passing conformance test suite for v0.3

---

## Task Dependency Graph

```
Phase 1: Proto Generation
    │
    ▼
Phase 2: Mapping Layer
    │
    ├────────────────────────────────────┐
    ▼                                    ▼
Phase 3: Client Transports           Phase 5: Server Transports
(3a, 3b, 3c in parallel)            (5a, 5b, 5c in parallel)
    │                                    │
    ▼                                    ▼
Phase 4: Dedicated Client            Phase 6: Reference Servers
                                     (6a → 6b, 6c, 6d in parallel)
    │                                    │
    └──────────────┬─────────────────────┘
                   ▼
            Phase 7: TCK Tests
```

**Key parallelism opportunities:**
- Phase 3 (client transports) and Phase 5 (server transports) can proceed **in parallel** once Phase 2 is done
- Within Phase 3, all three transports (3a, 3b, 3c) are **independent**
- Within Phase 5, all three transports (5a, 5b, 5c) are **independent**
- Phase 4 depends on Phase 3 only; Phase 6 depends on Phase 5 only
- Phase 7 requires both Phase 4 and Phase 6

---

## Testing Strategy

| Phase | Test Type | What to Verify |
|-------|-----------|----------------|
| Phase 1 | Compilation | Proto compilation succeeds, classes generated |
| Phase 2 | Unit tests | Round-trip mapper tests for every mapped type; edge cases (missing fields, v1.0-only features, REJECTED→FAILED) |
| Phase 3 | Unit tests | Client transport tests using mocked v0.3 endpoints |
| Phase 4 | Unit tests | `Compat03Client` API coverage, absence of v1.0-only methods |
| Phase 5 | Unit tests | Server transport tests using v0.3 wire-format requests against a real `AgentExecutor` |
| Phase 6 | Integration | Reference servers start and serve v0.3 endpoints |
| Phase 7 | Conformance | Full TCK suite against v0.3 reference servers |
