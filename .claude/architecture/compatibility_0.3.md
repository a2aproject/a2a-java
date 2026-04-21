# PRD: A2A Protocol 0.3 Backward Compatibility Layer

> **Goal**: Allow the A2A Java SDK (v1.0) to interoperate with agents running protocol v0.3 through a dedicated compatibility layer.

---

## Motivation

The A2A protocol evolved from v0.3 to v1.0 with significant breaking changes. Existing agents deployed with v0.3 cannot immediately upgrade. This compatibility layer enables a v1.0 SDK to communicate with v0.3 agents across all three transports (JSON-RPC, gRPC, REST) and allows v1.0 servers to accept v0.3 client requests.

---

## Scope

### In Scope

- Dedicated `compat-0.3` Maven module structure containing **only** 0.3-specific code
- gRPC code generation from the v0.3 `a2a.proto`
- Dedicated v0.3 client (`Client_v0_3`) exposing only features available in v0.3
- Server-side conversion layer (`Convert_v0_3_To10RequestHandler`) that accepts v0.3 requests and delegates to v1.0 server-common
- Server-side transport handlers for v0.3 (JSON-RPC, gRPC, REST)
- Bidirectional mapping layer between v0.3 and v1.0 domain objects
- Quarkus reference server implementations for v0.3
- TCK conformance tests for v0.3
- Integration test infrastructure (test-jar) for validating conversion layer
- Inclusion in the SDK BOM as separate optional dependencies

### Out of Scope

- Changes to existing v1.0 modules (no regressions, no API changes)
- Automatic protocol version detection (client must explicitly choose API version)
- Extras modules (OpenTelemetry, JPA stores, etc.) for v0.3
- v0.3 format agent card (v0.3 clients must be able to parse v1.0 agent card format)

### Deferred

- **Dual-version server deployment**: Running both v1.0 and v0.3 transports simultaneously in a single server instance with a unified agent card
  - **Current state**: v0.3 reference servers work standalone; need integration pattern for dual deployment
  - **Remaining work**: Define CDI qualifier pattern, path prefix strategy, and unified agent card production

---

## Breaking Changes: v0.3 → v1.0

The compatibility layer bridges the following differences:

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

### Naming Convention: `_v0_3` Suffix

All compat-0.3 classes use a `_v0_3` suffix to avoid naming conflicts with v1.0 classes and improve IDE navigation:

- `Task_v0_3`, `AgentCard_v0_3`, `Client_v0_3`
- `JSONRPCHandler_v0_3`, `GrpcHandler_v0_3`, `RestHandler_v0_3`
- `Convert_v0_3_To10RequestHandler`, `ErrorConverter_v0_3`
- Mappers: `TaskMapper_v0_3`, `MessageSendParamsMapper_v0_3`, etc.

**Exception**: Generated gRPC classes use the package name `org.a2aproject.sdk.compat03.grpc` without suffix (controlled by proto `java_package` option).

### Dedicated v0.3 Client

The compat layer exposes a **dedicated `Client_v0_3`** that only provides features available in v0.3:

- No `listTasks()` method (absent in v0.3)
- Method names reflect v0.3 semantics where they differ
- The client is a standalone API, not a wrapper around the v1.0 `Client`

Users must explicitly check the `protocolVersion` field from the agent card and instantiate the correct client accordingly. No automatic version detection.

### Server-Side Conversion Layer

Instead of embedding conversion logic in each transport handler, the implementation uses a **dedicated conversion layer** that sits between v0.3 transport handlers and v1.0 server-common:

```
v0.3 Client Request
    ↓
v0.3 Transport Handler (JSONRPC/gRPC/REST)
    ↓
Convert_v0_3_To10RequestHandler
    ↓ (converts v0.3 → v1.0)
v1.0 DefaultRequestHandler
    ↓
AgentExecutor → AgentEmitter → MainEventBus
    ↓
v1.0 Response
    ↓ (converts v1.0 → v0.3)
Convert_v0_3_To10RequestHandler
    ↓
v0.3 Transport Handler
    ↓
v0.3 Client Response
```

**Benefits:**
- **Single conversion point**: All v0.3↔v1.0 translation logic lives in `server-conversion` module
- **Transport independence**: JSONRPC, gRPC, and REST handlers share identical conversion logic
- **Testability**: Conversion layer can be tested independently of transport concerns
- **Maintainability**: Changes to conversion rules require updates in one place only

### TASK_STATE_REJECTED Handling

`TASK_STATE_REJECTED` (v1.0-only) is mapped to `TASK_STATE_FAILED` when converting to v0.3 wire format. The original state is preserved in metadata (`"original_state": "REJECTED"`) so information is not entirely lost. Both are terminal states, so v0.3 clients can handle the result correctly.

### Agent Card: v1.0 Only

The agent card (`/.well-known/agent-card.json`) is produced only using the v1.0 format. The compat layer does not produce a v0.3-format agent card.

A server that supports both versions should advertise this via a single v1.0 agent card containing multiple `AgentInterface` entries — one per version — each with its own URL and `protocolVersion` field. v0.3 clients must be able to parse the v1.0 agent card format to discover their endpoint.

**Pros:**
- **Single source of truth**: one agent card at one well-known URL describes all supported versions and transports
- **Simpler server implementation**: no need for separate v0.3 agent card endpoint, serializer, or CDI producer
- **Forward-looking**: encourages v0.3 clients to understand the v1.0 discovery format

**Cons:**
- **v0.3 clients must parse v1.0 agent card**: pure v0.3 clients that only understand the v0.3 structure need updating

---

## Module Structure

All compatibility code lives under a top-level `compat-0.3/` directory:

```
compat-0.3/
├── pom.xml                          # Parent POM for all compat-0.3 submodules
├── spec/                            # v0.3 spec types (POJOs)
│   ├── pom.xml
│   └── src/main/java/org/a2aproject/sdk/compat03/spec/
│       ├── Task_v0_3.java
│       ├── AgentCard_v0_3.java
│       ├── Message_v0_3.java
│       ├── A2AError_v0_3.java       # Base error class
│       ├── *Error_v0_3.java         # Specific error types
│       └── ...
├── spec-grpc/                       # v0.3 proto + generated classes
│   ├── pom.xml
│   └── src/main/
│       ├── proto/a2a_v0_3.proto     # v0.3 proto file (package a2a.v1)
│       └── java/org/a2aproject/sdk/compat03/grpc/
│           └── [generated classes]  # No _v0_3 suffix (generated code)
├── http-client/                     # HTTP client abstraction for v0.3
│   └── pom.xml
├── server-conversion/               # ⭐ Core conversion layer (NEW)
│   ├── pom.xml
│   └── src/main/java/org/a2aproject/sdk/compat03/conversion/
│       ├── Convert_v0_3_To10RequestHandler.java  # Main adapter
│       ├── ErrorConverter_v0_3.java              # Error conversion
│       └── mappers/
│           ├── config/
│           │   ├── A03ToV10MapperConfig.java     # MapStruct config
│           │   └── A2AMappers_v0_3.java          # Mapper registry
│           ├── params/                           # Request param mappers
│           │   ├── MessageSendParamsMapper_v0_3.java
│           │   ├── TaskQueryParamsMapper_v0_3.java
│           │   ├── CancelTaskParamsMapper_v0_3.java
│           │   └── ...
│           ├── domain/                           # Domain object mappers
│           │   ├── TaskMapper_v0_3.java
│           │   ├── MessageMapper_v0_3.java
│           │   ├── TaskStateMapper_v0_3.java
│           │   ├── EventKindMapper_v0_3.java
│           │   └── ...
│           └── result/                           # Response result mappers
│               └── ListTaskPushNotificationConfigsResultMapper_v0_3.java
├── client/                          # v0.3-compatible client
│   ├── base/                        # Client_v0_3 — dedicated 0.3 API
│   │   └── pom.xml
│   └── transport/
│       ├── spi/                     # Transport SPI
│       │   └── pom.xml
│       ├── jsonrpc/                 # JSON-RPC client transport for v0.3
│       │   └── pom.xml
│       ├── grpc/                    # gRPC client transport for v0.3
│       │   └── pom.xml
│       └── rest/                    # REST client transport for v0.3
│           └── pom.xml
├── transport/                       # Server-side transport handlers for v0.3
│   ├── jsonrpc/                     # Accept v0.3 JSON-RPC requests
│   │   └── pom.xml
│   ├── grpc/                        # Accept v0.3 gRPC requests
│   │   └── pom.xml
│   └── rest/                        # Accept v0.3 REST requests
│       └── pom.xml
├── reference/                       # Quarkus reference servers for v0.3
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

All compat-0.3 code uses the `org.a2aproject.sdk.compat03` package root:

- `org.a2aproject.sdk.compat03.spec` — v0.3 spec types
- `org.a2aproject.sdk.compat03.grpc` — generated proto classes
- `org.a2aproject.sdk.compat03.conversion` — conversion layer and mappers
- `org.a2aproject.sdk.compat03.client` — dedicated v0.3 client API
- `org.a2aproject.sdk.compat03.client.transport.{jsonrpc,grpc,rest}` — client transports
- `org.a2aproject.sdk.compat03.transport.{jsonrpc,grpc,rest}` — server transports
- `org.a2aproject.sdk.compat03.server.{apps,grpc,rest}.quarkus` — reference servers
- `org.a2aproject.sdk.compat03.tck` — conformance tests

**Note**: During this implementation, the main codebase was migrated from `io.github.a2asdk` (groupId) and `io.a2a` (package) to `org.a2aproject.sdk` (both groupId and package) via PRs #750 and #786.

---

## Conversion Layer Architecture

### Core Component: `Convert_v0_3_To10RequestHandler`

This is the central adapter that bridges v0.3 transport handlers and v1.0 server-common:

**Responsibilities:**
- Convert v0.3 params → v1.0 params using mappers
- Delegate to v1.0 `RequestHandler`
- Convert v1.0 results → v0.3 results
- Handle streaming publishers with element-by-element conversion
- Map method name differences (e.g., `onSetTaskPushNotificationConfig` → `onCreateTaskPushNotificationConfig`)

**Location**: `compat-0.3/server-conversion/src/main/java/org/a2aproject/sdk/compat03/conversion/Convert_v0_3_To10RequestHandler.java`

### Mapper Organization

Mappers are organized by function using MapStruct:

| Category | Purpose | Examples |
|----------|---------|----------|
| **params/** | Convert v0.3 request params → v1.0 | `MessageSendParamsMapper_v0_3`, `TaskQueryParamsMapper_v0_3` |
| **domain/** | Convert core domain objects bidirectionally | `TaskMapper_v0_3`, `MessageMapper_v0_3`, `TaskStateMapper_v0_3` |
| **result/** | Convert v1.0 results → v0.3 | `ListTaskPushNotificationConfigsResultMapper_v0_3` |

**Key Mappings:**

| v1.0 Type | v0.3 Type | Notes |
|-----------|-----------|-------|
| `SendMessageConfiguration` | `SendMessageConfiguration_v0_3` | `return_immediately` ↔ `!blocking`, `task_push_notification_config` ↔ `push_notification` |
| `Task` | `Task_v0_3` | `CANCELED` ↔ `CANCELLED` |
| `TaskState.TASK_STATE_REJECTED` | `TaskState_v0_3.TASK_STATE_FAILED` | Map to FAILED + metadata `"original_state": "REJECTED"` |
| `Message` | `Message_v0_3` | Drop `reference_task_ids` for v0.3 |

### Error Mapping

The `ErrorConverter_v0_3` class centralizes error translation between v0.3 and v1.0:

**v0.3 → v1.0 (receiving errors):**
- Extract `code` and `message` from v0.3 `A2AError_v0_3`
- Convert `data` (Object) to `details` (Map): if `data` is a Map, use directly; otherwise wrap as `{"data": value}`
- Instantiate correct v1.0 error class using `A2AErrorCodes.fromCode(code)`

**v1.0 → v0.3 (sending errors):**
- Extract `code`, `message`, and `details` from v1.0 `A2AError`
- Convert `details` (Map) to `data` (Object)
- For v1.0-only error codes, produce generic error with same code/message

**Location**: `compat-0.3/server-conversion/src/main/java/org/a2aproject/sdk/compat03/conversion/ErrorConverter_v0_3.java`

---

## Test Infrastructure

### Test-JAR Pattern

The `server-conversion` module produces a test-jar containing shared test infrastructure:

**Exported Classes:**
- `AbstractA2ARequestHandlerTest_v0_3` — Base test class with v1.0 backend setup
- `AbstractA2AServerServerTest_v0_3` — Integration test base for reference servers
- Test fixtures and utilities

**Maven Configuration:**
```xml
<plugin>
    <artifactId>maven-jar-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>test-jar</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Consumers:**
- `compat-0.3/transport/jsonrpc` — `JSONRPCHandlerTest_v0_3`
- `compat-0.3/transport/grpc` — `GrpcHandlerTest_v0_3`
- `compat-0.3/transport/rest` — `RestHandlerTest_v0_3`
- `compat-0.3/reference/{jsonrpc,grpc,rest}` — Integration tests

### Test Coverage

✅ **Complete:**
- Core transport handler tests (JSONRPC, gRPC, REST)
- Streaming tests (Flow.Publisher, SSE, gRPC server streaming)
- Error mapping tests
- Task state conversion tests
- Reference server integration tests

🔲 **Deferred:**
- Push notification tests (depends on TestHttpClient porting)
- Test metadata classes (classpath scanning)

---

## User Experience

### Client: Talking to a v0.3 Agent

**1. Add the compat client dependency:**

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-client</artifactId>
</dependency>
<!-- Plus the desired transport -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-client-transport-jsonrpc</artifactId>
</dependency>
```

**2. Check the agent card and choose the right client:**

```java
AgentCard card = // ... fetch agent card from /.well-known/agent-card.json

for (AgentInterface iface : card.supportedInterfaces()) {
    if ("0.3".equals(iface.protocolVersion())) {
        // Use the compat client for v0.3 agents
        Client_v0_3 client = ClientBuilder_v0_3.forUrl(iface.url())
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

`Client_v0_3` exposes only operations available in v0.3. Return types are v0.3 `org.a2aproject.sdk.compat03.spec` domain objects.

### Server: Serving v0.3 Clients

A server operator that wants to accept v0.3 clients:

**1. Add the compat Maven dependency:**

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-reference-jsonrpc</artifactId>
</dependency>
```

**2. Provide a v0.3 AgentCard:**

```java
@Produces @PublicAgentCard
public AgentCard_v0_3 agentCard() {
    return AgentCard_v0_3.builder()
            .name("My Agent")
            .url("http://localhost:8081")
            .preferredTransport("JSONRPC")
            // ... rest of agent card
            .build();
}
```

**3. No changes to AgentExecutor:**

The existing `AgentExecutor` implementation works unchanged. The compat reference module registers v0.3 transport endpoints via Quarkus CDI auto-discovery and delegates to the same `AgentExecutor` through the v1.0 server pipeline.

### Server: Serving Both v0.3 and v1.0 (Deferred)

**Status**: Architecture is in place but dual-version deployment pattern is not yet defined.

**Remaining work:**
- Define CDI qualifier pattern to differentiate v0.3 and v1.0 beans
- Establish path prefix strategy (e.g., `/v0.3` for compat endpoints)
- Unified agent card production with multiple `AgentInterface` entries
- Integration testing for dual-version scenarios

**Recommended approach:**
1. Use separate URLs per protocol version (e.g., `http://localhost:9999` for v1.0, `http://localhost:9999/v0.3` for v0.3)
2. Declare both in the v1.0 agent card with different `protocolVersion` values
3. Register both transport handlers via Quarkus CDI with path-based routing

---

## Implementation Summary

The implementation was completed in phases:

### Phase 1: Foundation
- Set up module structure and POMs
- Port v0.3 spec types to `compat-0.3/spec`
- Generate v0.3 gRPC classes in `compat-0.3/spec-grpc`
- Apply `_v0_3` suffix naming convention

### Phase 2: Conversion Layer
- Create `server-conversion` module
- Implement `Convert_v0_3_To10RequestHandler`
- Build MapStruct mappers (params, domain, result)
- Centralize error conversion in `ErrorConverter_v0_3`
- Export test infrastructure via test-jar

### Phase 3: Transport Handlers
- Implement server-side transport handlers (JSONRPC, gRPC, REST)
- Wire each transport to `Convert_v0_3_To10RequestHandler`
- Test with v1.0 backend via `AbstractA2ARequestHandlerTest_v0_3`

### Phase 4: Client
- Implement `Client_v0_3` API
- Build client transports (JSONRPC, gRPC, REST)
- Validate against v0.3 spec constraints

### Phase 5: Reference Servers
- Port Quarkus reference servers (JSONRPC, gRPC, REST)
- Integrate with test infrastructure
- Run integration tests using v0.3 client against v0.3 reference servers

### Phase 6: Testing & Validation
- Port transport handler tests (37+ tests across 3 transports)
- Port streaming tests (Flow.Publisher, SSE, gRPC)
- Port reference server tests (125+ integration tests passing)
- Enable TCK module

---

## Key Differences from Original Plan

1. **Conversion layer as separate module**: Original plan embedded mapping in `spec-grpc`; implementation uses dedicated `server-conversion` module with `Convert_v0_3_To10RequestHandler`

2. **`_v0_3` suffix naming**: Not in original plan; adopted to eliminate IDE naming conflicts (233 out of 284 classes had conflicts)

3. **No `reference/common` module**: Each reference server is standalone; no shared reference base

4. **Test-jar pattern**: Test infrastructure exported from `server-conversion` module rather than reference common

5. **Package migration during implementation**: The main codebase migrated from `io.a2a` to `org.a2aproject.sdk` while this work was in progress (PRs #750, #786)

6. **Implementation order**: Test infrastructure built early; tests ported incrementally to validate conversion layer

---

## Testing Strategy

| Component | Test Type | Coverage |
|-----------|-----------|----------|
| Mappers | Unit tests | Round-trip conversion for every mapped type; edge cases (missing fields, v1.0-only features, REJECTED→FAILED) |
| `Convert_v0_3_To10RequestHandler` | Integration tests | Via transport handler tests using real v1.0 backend |
| Transport handlers | Unit + Integration | Handler-level tests + end-to-end via reference servers |
| Client transports | Unit tests | Mocked v0.3 endpoints |
| `Client_v0_3` | Unit tests | API coverage, absence of v1.0-only methods |
| Reference servers | Integration tests | Full request/response cycle with v0.3 client |
| TCK | Conformance tests | Protocol conformance against v0.3 spec |

---

## Status

✅ **Complete:**
- Module structure and POMs
- v0.3 spec types and gRPC generation
- Server conversion layer with all mappers
- Server-side transport handlers (JSONRPC, gRPC, REST)
- Client API and transports
- Reference servers (JSONRPC, gRPC, REST)
- Test infrastructure (test-jar pattern)
- Core integration tests (125+ passing)
- TCK module enabled

🔲 **Deferred:**
- Dual v1.0/v0.3 server deployment pattern
- Push notification test porting (requires TestHttpClient)
- Test metadata classes (classpath scanning)

🧹 **Nice-to-have cleanup:**
- Replace FQNs with imports (97 occurrences in 34 files)
- Unify AgentCard producers across reference modules
- Remove obsolete TODOs
