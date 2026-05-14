# A2A Protocol 0.3 Backward Compatibility Layer

> The A2A Java SDK (v1.0) interoperates with agents running protocol v0.3 through a dedicated compatibility layer.

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
- Multi-version server deployment: `reference/multiversion-jsonrpc` and `reference/multiversion-rest` modules that dispatch requests to v1.0 or v0.3 handlers based on `A2A-Version` header via `VersionRouter`
- Multi-version integration tests under `tests/multiversion/{jsonrpc,rest,grpc}` (package `org.a2aproject.sdk.tests.multiversion`)

### Out of Scope

- Changes to existing v1.0 modules (no regressions, no API changes)
- Automatic protocol version detection (client must explicitly choose API version)
- Extras modules (OpenTelemetry, JPA stores, etc.) for v0.3
- Serving a separate v0.3-format agent card (the v1.0 card is served, with optional v0.3-compatible fields added by the user)

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

### Agent Card Precedence in Multi-Version Mode

When both v1.0 and v0.3 protocol support are enabled, a single agent card is served at `/.well-known/agent-card.json`:

- **Both v1.0 and v0.3 enabled**: The v1.0 `AgentCard` takes precedence. The v0.3 `AgentCard_v0_3` is ignored.
- **Only v0.3 enabled**: The v0.3 `AgentCard_v0_3` is used.
- **Only v1.0 enabled**: The v1.0 `AgentCard` is used as-is.

### Making the v1.0 Agent Card Parsable by v0.3 Clients

Existing v0.3 client implementations (across all languages) expect specific fields in the agent card that don't exist in the v1.0 format. Since we cannot control what external v0.3 clients can parse, the v1.0 `AgentCard` must include backward-compatible fields when serving both protocol versions:

- `url` and `preferredTransport` — top-level fields that v0.3 clients use to discover the primary endpoint
- `additionalInterfaces` — a list of `Legacy_0_3_AgentInterface(transport, url)` entries using v0.3 field names (`transport` instead of v1.0's `protocolBinding`)

These fields coexist alongside the v1.0 `supportedInterfaces` field. v1.0 clients use `supportedInterfaces`; v0.3 clients use `url`, `preferredTransport`, and `additionalInterfaces`.

`Legacy_0_3_AgentInterface` is a separate record from `AgentInterface` because they serialize to different JSON field names (`transport`/`url` vs `protocolBinding`/`url`/`tenant`).

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

### Multi-Version Reference Modules

These top-level modules provide version-dispatching routes that serve both v1.0 and v0.3 requests from a single server instance:

```
reference/
├── common/                              # Shared utilities
│   └── src/main/java/org/a2aproject/sdk/server/common/quarkus/
│       └── VersionRouter.java           # Resolves protocol version from A2A-Version header
├── multiversion-jsonrpc/                # Version-dispatching JSON-RPC routes
│   └── src/main/java/org/a2aproject/sdk/server/multiversion/jsonrpc/
│       └── MultiVersionJSONRPCRoutes.java
└── multiversion-rest/                   # Version-dispatching REST routes
    └── src/main/java/org/a2aproject/sdk/server/multiversion/rest/
        └── MultiVersionRestRoutes.java

tests/multiversion/                      # Multi-version integration tests
├── jsonrpc/                             # JSON-RPC multi-version tests
├── rest/                                # REST multi-version tests
└── grpc/                                # gRPC multi-version tests
```

**Note**: gRPC version dispatch is handled by protobuf package namespace (`a2a.v1` for v0.3 vs `lf.a2a.v1` for v1.0), so no `multiversion-grpc` module is needed.

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

### Version Routing: `VersionRouter`

The `VersionRouter` class in `reference/common` resolves the protocol version for incoming requests:

**Location**: `reference/common/src/main/java/org/a2aproject/sdk/server/common/quarkus/VersionRouter.java`

**Resolution Logic** (in `VersionRouter.resolveVersion()`):
1. Check the `A2A-Version` HTTP header
2. If absent, check the `A2A-Version` query parameter
3. If neither is present, default to `"0.3"` (backward compatibility with clients that don't send a version header)

The caller then dispatches based on the resolved version:
```java
String version = VersionRouter.resolveVersion(routingContext);
if (VersionRouter.isV10(version)) {
    // delegate to v1.0 handler
} else if (VersionRouter.isV03(version)) {
    // delegate to v0.3 handler
} else {
    // unrecognized version string — reject
    throw new VersionNotSupportedError(...);
}
```

The multi-version route classes (`MultiVersionJSONRPCRoutes`, `MultiVersionRestRoutes`) use this pattern to dispatch every endpoint to the appropriate version handler.

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

**2. Find the v0.3 interface and create the client:**

```java
AgentCard card = // ... fetch agent card from /.well-known/agent-card.json

// Find the v0.3 interface from the agent card
AgentInterface v03Interface = card.supportedInterfaces().stream()
        .filter(iface -> "0.3".equals(iface.protocolVersion()))
        .findFirst()
        .orElseThrow();

// Create the v0.3 compatibility client
Client_v0_3 client = ClientBuilder_v0_3.forUrl(v03Interface.url())
        .withTransport(JSONRPCTransport_v0_3.class, new JSONRPCTransportConfigBuilder_v0_3())
        .build();
```

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

### Server: Serving Multiple Protocol Versions

To serve multiple protocol versions from the same server, add the compat reference module for each version you want to support alongside the v1.0 reference module. For example, to serve both v1.0 and v0.3 over JSON-RPC:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-jsonrpc</artifactId>
</dependency>
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-compat-0.3-reference-jsonrpc</artifactId>
</dependency>
```

For JSON-RPC and REST, multi-version convenience modules are also available that bundle all supported protocol versions with version-dispatching routes:

```xml
<!-- Includes v1.0 + v0.3 JSON-RPC with automatic version routing -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-multiversion-jsonrpc</artifactId>
</dependency>

<!-- Includes v1.0 + v0.3 REST with automatic version routing -->
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-reference-multiversion-rest</artifactId>
</dependency>
```

**Version routing:**
- **JSON-RPC and REST**: Clients indicate their protocol version via the `A2A-Version` header (or query parameter). If absent, the server defaults to v0.3 for backward compatibility.
- **gRPC**: Version dispatch is handled by protobuf package namespace (`a2a.v1` vs `lf.a2a.v1`), so both v1.0 and v0.3 gRPC services are registered on the same port without a dedicated multi-version module.

**Agent card**: The agent card is served in v1.0 format only. Older clients must be able to parse the v1.0 agent card format to discover their endpoint.

**Integration tests**: Multi-version scenarios are tested under `tests/multiversion/{jsonrpc,rest,grpc}` using both v1.0 and v0.3 clients against a single server instance.

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

The v0.3 compatibility layer is fully implemented: spec types, gRPC generation, conversion layer, all three transport handlers (JSON-RPC, gRPC, REST), client API and transports, reference servers, multi-version deployment, test infrastructure, 125+ integration tests, and TCK module are all in place.

🔲 **Outstanding:**
- Push notification test porting (requires TestHttpClient; version-aware push notifications in PR #857)
- Test metadata classes (classpath scanning)
- Replace FQNs with imports (97 occurrences in 34 files)
- Unify AgentCard producers across reference modules
