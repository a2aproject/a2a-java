# 1.0 Client Support for A2A 0.3 Servers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow applications to use the existing concrete 1.0 `Client` and 1.0 spec types against a 0.3 server when they explicitly opt in and install optional compat artifacts.

**Architecture:** Keep `Client` as the public API and adapt at its existing `ClientTransport` boundary. Add a raw-card compatibility parser SPI below the client transport SPI, and a separate versioned transport-adapter SPI used by `ClientBuilder`; neither collides with the existing binding-only transport provider registry. Extract pure 0.3/1.0 mappers from the server conversion module into a neutral artifact consumed by both client and server code.

**Tech Stack:** Java 17, Maven multi-module build, Gson/protobuf JSON parsing, MapStruct, `ServiceLoader`, JUnit 5, MockServer.

**Spec:** `docs/superpowers/specs/2026-09-14-client-v03-compatibility-tests-design.md`

## Global Constraints

- The public client remains `org.a2aproject.sdk.client.Client`; callers use only 1.0 request, result, event, card, context, config, and interceptor types.
- Existing `A2A.getAgentCard(...)` overloads stay 1.0-only; a new overload explicitly receives supported protocol versions.
- Legacy fallback is enabled only when the caller includes `"0.3"` and the relevant optional compat JARs are installed.
- Canonical supported versions are `"1.0"` and `"0.3"`. One shared normalization routine accepts only `"1.0"`, `"1.0.0"`, `"0.3"`, and `"0.3.0"`; it normalizes the patch forms and rejects blank, malformed, older, newer, and unknown values.
- Preserve the client/server dependency separation: no client artifact may depend on `server-common`, CDI, Quarkus, or reference-server modules.
- Do not register a compat transport as the existing `ClientTransportProvider`; its registry is keyed only by protocol binding and would overwrite the native provider.
- Against a 0.3 binding, reject locally before I/O: `listTasks`, non-empty tenants, extended-agent-card retrieval, and non-default push-config pagination.
- Use conventional commits. This work is not related to a GitHub issue, so do not add a `This fixes #...` footer.

## Implementation Clarifications and Checkpoints

- In the interceptor payload contract, the REST delete operation must use the concrete 1.0 generated protobuf type `DeleteTaskPushNotificationConfigRequest` for replacement validation. Do not use the `DeleteTaskPushNotificationConfigRequestOrBuilder` interface as the required replacement type.
- “Generic 1.0 config parameters” means the map returned by `ClientTransportConfig.getParameters()`. Every 0.3 adapter must reject a non-empty map locally before invoking an interceptor or legacy delegate; an empty map is accepted.
- Treat these as context-reset checkpoints. Stop after completing and verifying each checkpoint, report the result, and wait for the user to start a fresh session before continuing:
  1. Task 1: neutral conversion extraction.
  2. Task 3: version-aware `ClientBuilder` selection.
  3. Task 4: shared 0.3 adapter behavior.
  4. Task 6: all binding adapters, including gRPC.
  5. Task 7: packaging, documentation, and end-to-end verification.
- If implementation reveals that one of these clarifications conflicts with an existing public API or generated type, stop at the current checkpoint and update this plan before proceeding.

---

## Target file structure

| Path | Responsibility |
|---|---|
| `compat-0.3/conversion/` | New neutral MapStruct conversion JAR: only 0.3 spec, 1.0 spec, and MapStruct dependencies. |
| `compat-0.3/server-conversion/` | Retains CDI `Convert_v0_3_To10RequestHandler` and server-only formatters; consumes neutral conversion JAR. |
| `http-client/.../AgentCardCompatibilityParser.java` | Low-level `ServiceLoader` SPI for optional raw-card parsing. |
| `http-client/.../A2ACardResolver.java` | Fetches once; invokes the SPI only when 0.3 is requested. |
| `client/base/.../A2A.java` | Adds explicit version-policy card-discovery overloads. |
| `client/base/.../VersionedClientTransportProvider.java` | Separate `ServiceLoader` SPI for 0.3 transport adapters. |
| `client/base/.../ClientBuilder.java` | Selects by `(protocolBinding, protocolVersion)` and builds native or adapted transport. |
| `compat-0.3/client/adapter/` | Legacy-card parser, error/context/interceptor invocation utilities, and shared adapter code. |
| `compat-0.3/client/adapter-{jsonrpc,rest,grpc}/` | Per-binding 1.0 `ClientTransport` adapters and provider registrations. |

The production dependency graph is fixed as follows. Do not rely on accidental reactor transitivity:

```text
compat-0.3/client/adapter
  -> compat-0.3/conversion
  -> a2a-java-sdk-spec
  -> a2a-java-sdk-compat-0.3-spec
  -> a2a-java-sdk-http-client
  -> a2a-java-sdk-client-transport-spi
  -> a2a-java-sdk-compat-0.3-client-transport-spi

compat-0.3/client/adapter-jsonrpc
  -> compat-0.3/client/adapter
  -> a2a-java-sdk-client
  -> a2a-java-sdk-client-transport-jsonrpc
  -> a2a-java-sdk-compat-0.3-client-transport-jsonrpc

compat-0.3/client/adapter-rest
  -> compat-0.3/client/adapter
  -> a2a-java-sdk-client
  -> a2a-java-sdk-client-transport-rest
  -> a2a-java-sdk-compat-0.3-client-transport-rest

compat-0.3/client/adapter-grpc
  -> compat-0.3/client/adapter
  -> a2a-java-sdk-client
  -> a2a-java-sdk-client-transport-grpc
  -> a2a-java-sdk-compat-0.3-client-transport-grpc
```

The `compat-0.3` reactor module order is `conversion`, `server-conversion`, shared `client/adapter`, then the three binding adapter modules.

### Task 1: Extract neutral conversion module

**Files:**

- Create: `compat-0.3/conversion/pom.xml`
- Create: `compat-0.3/conversion/src/main/java/org/a2aproject/sdk/compat03/conversion/mappers/**`
- Create: `compat-0.3/conversion/src/test/java/org/a2aproject/sdk/compat03/conversion/mappers/**`
- Modify: `compat-0.3/pom.xml`
- Modify: `compat-0.3/server-conversion/pom.xml`
- Modify: `compat-0.3/server-conversion/src/main/java/org/a2aproject/sdk/compat03/conversion/Convert_v0_3_To10RequestHandler.java`
- Modify: `compat-0.3/server-conversion/src/main/java/org/a2aproject/sdk/compat03/conversion/PushNotificationPayloadFormatter_v0_3.java`
- Modify: existing server-conversion tests whose imports move with the mappers.

**Consumes:** Current pure mapper package at `compat-0.3/server-conversion/src/main/java/org/a2aproject/sdk/compat03/conversion/mappers/`.

**Produces:** Artifact `a2a-java-sdk-compat-0.3-conversion`; all pure `toV10`/`fromV10` mappers compile there. Server conversion uses the new artifact and retains no client-facing CDI dependency.

- [ ] **Step 1: Write failing module-isolation tests**

Create mapper tests in the new module. Include existing `TaskMapper_v0_3_Test`, a round-trip `MessageSendParamsMapper_v0_3` test, and a new `AgentCardMapper_v0_3_Test`. The agent-card test must prove that a 0.3 card with primary URL, preferred transport, additional interfaces, streaming/push flags, security data needed for authentication, and signatures projects to a usable 1.0 card with `AgentInterface(..., "0.3")`, then converts back to a usable `AgentCard_v0_3`.

- [ ] **Step 2: Run the new module test and confirm it fails**

Run: `mvn -pl compat-0.3/conversion test`

Expected: Maven reports that the module does not exist.

- [ ] **Step 3: Create the neutral module and move only pure conversion code**

Move `mappers/config`, `mappers/domain`, `mappers/params`, and `mappers/result` without changing package names. Create `AgentCardMapper_v0_3` and any focused nested card/security mappers needed by it. Move the pure error conversion utility only if it has no server dependency; otherwise create a distinct pure error-value mapper and leave server-specific exception handling in `server-conversion`.

The new POM must depend only on `a2a-java-sdk-spec`, `a2a-java-sdk-compat-0.3-spec`, MapStruct, and test dependencies. Do not add CDI, `server-common`, or reference modules.

- [ ] **Step 4: Rewire server conversion**

Make `server-conversion` depend on the new neutral artifact. Keep `Convert_v0_3_To10RequestHandler`, `PushNotificationPayloadFormatter_v0_3`, CDI annotations, and server tests in `server-conversion`. Remove duplicate mapper sources from that module.

- [ ] **Step 5: Run focused verification**

Run: `mvn -pl compat-0.3/conversion,compat-0.3/server-conversion -am test`

Expected: Mapper round trips and existing server conversion tests pass.

- [ ] **Step 6: Commit**

```bash
git add compat-0.3/conversion compat-0.3/pom.xml compat-0.3/server-conversion
git commit -m "refactor: extract 0.3 compatibility mappers"
```

### Task 2: Add explicit, optional legacy card discovery

**Files:**

- Create: `http-client/src/main/java/org/a2aproject/sdk/client/http/AgentCardCompatibilityParser.java`
- Modify: `http-client/src/main/java/org/a2aproject/sdk/client/http/A2ACardResolver.java`
- Modify: `http-client/src/test/java/org/a2aproject/sdk/client/http/A2ACardResolverTest.java`
- Modify: `client/base/src/main/java/org/a2aproject/sdk/A2A.java`
- Modify: `client/base/src/test/java/org/a2aproject/sdk/A2ATest.java` or create `client/base/src/test/java/org/a2aproject/sdk/A2AAgentCardResolutionTest.java`
- Create: `compat-0.3/client/adapter/pom.xml`
- Create: `compat-0.3/client/adapter/src/main/java/org/a2aproject/sdk/compat03/client/adapter/Compat03AgentCardCompatibilityParser.java`
- Create: `compat-0.3/client/adapter/src/main/resources/META-INF/services/org.a2aproject.sdk.client.http.AgentCardCompatibilityParser`
- Create: adapter parser tests and card fixtures.

**Interfaces:**

```java
// In http-client; it must reference no 0.3 class.
public interface AgentCardCompatibilityParser {
    String supportedProtocolVersion();
    java.util.Optional<AgentCard> parse(
            String rawCardJson,
            @Nullable AgentCard parsedV10Card,
            java.util.Set<String> requestedProtocolVersions);
}
```

Put `normalizeSupportedProtocolVersion(String)` in `http-client` and use it from both resolver and builder. `A2ACardResolver.Builder` gains `supportedProtocolVersions(Set<String>)`, makes a defensive copy, normalizes it, and defaults to `Set.of("1.0")`. `A2A` adds exactly these overloads:

```java
public static AgentCard getAgentCard(
        String agentUrl, Set<String> supportedProtocolVersions)
        throws A2AClientError, A2AClientJSONError;

public static AgentCard getAgentCard(
        A2AHttpClient client, String agentUrl, Set<String> supportedProtocolVersions)
        throws A2AClientError, A2AClientJSONError;

public static AgentCard getAgentCard(
        String agentUrl, String path, Map<String, String> headers,
        Set<String> supportedProtocolVersions)
        throws A2AClientError, A2AClientJSONError;

public static AgentCard getAgentCard(
        A2AHttpClient client, String agentUrl, String path, Map<String, String> headers,
        Set<String> supportedProtocolVersions)
        throws A2AClientError, A2AClientJSONError;
```

- [ ] **Step 1: Write resolver tests first**

Add tests using a fake `AgentCardCompatibilityParser` registered through the test classloader:

```java
assertThrows(A2AClientJSONError.class,
    () -> resolverFor(v03Json).getAgentCard());

assertEquals("0.3", resolverFor(v03Json)
    .supportedProtocolVersions(Set.of("1.0", "0.3"))
    .getAgentCard().supportedInterfaces().get(0).protocolVersion());
```

Also test: raw body is parsed without a second fetch; a usable 1.0 card remains native; a dual-format card prefers its 1.0 interface; a requested 0.3 parser absent gives an `A2AClientJSONError` naming `a2a-java-sdk-compat-0.3-client-adapter`; invalid requested values fail before `createGet()`; declared `0.3.0` succeeds; declared `0.2.9`, `0.3.1`, blank, and malformed legacy versions fail before client construction; custom card path and authorization headers survive fallback. Preserve the existing 404 URL retry: "one fetch" means no second fetch merely to parse legacy JSON.

- [ ] **Step 2: Run tests to verify failure**

Run: `mvn -pl http-client,client/base -am -Dtest=A2ACardResolverTest,A2AAgentCardResolutionTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: compilation fails because the options/SPI do not exist.

- [ ] **Step 3: Implement raw-card parsing without a second request**

Refactor `A2ACardResolver.fetchAgentCard` so it retains the fetched response body, parses native 1.0 once, and only loads/invokes `AgentCardCompatibilityParser` if normalized requested versions include `"0.3"`. A native parse counts as usable only when it yields at least one eligible interface. Validate a legacy card's own declared protocol version through the shared normalizer before projecting it. Pass the same raw body to the parser; never re-fetch it merely for parsing.

Implement the compat parser with `JsonUtil_v0_3`, then use `AgentCardMapper_v0_3` to project/merge 0.3 interfaces into the returned public 1.0 card. Filter returned interfaces to the requested versions. Prefer 1.0 over 0.3 for identical bindings.

- [ ] **Step 4: Run focused verification**

Run: `mvn -pl http-client,client/base,compat-0.3/client/adapter -am test`

Expected: all old resolver tests and the new opt-in/fallback tests pass.

- [ ] **Step 5: Commit**

```bash
git add http-client client/base compat-0.3/client/adapter compat-0.3/pom.xml
git commit -m "feat: add opt-in 0.3 agent card discovery"
```

### Task 3: Make `ClientBuilder` select a versioned delegate safely

**Files:**

- Create: `client/base/src/main/java/org/a2aproject/sdk/client/VersionedClientTransportProvider.java`
- Modify: `client/base/src/main/java/org/a2aproject/sdk/client/ClientBuilder.java`
- Modify: `client/base/src/test/java/org/a2aproject/sdk/client/ClientBuilderTest.java`
- Create: `client/base/src/test/java/org/a2aproject/sdk/client/VersionedClientTransportProviderTest.java`

**Interfaces:**

```java
public interface VersionedClientTransportProvider {
    String protocolBinding();
    String protocolVersion();
    Class<? extends ClientTransport> configuredTransportClass();
    ClientTransport create(
        ClientTransportConfig<?> config, AgentCard card, AgentInterface agentInterface)
        throws A2AClientException;
}
```

**Produces:** `ClientBuilder` selects candidates by `(protocolBinding, normalized protocolVersion)` and uses the native `ClientTransportProvider` only for `"1.0"`; it uses the separate versioned SPI for `"0.3"`.

- [ ] **Step 1: Write failing selection tests**

Add cards with JSON-RPC interfaces in both orders: 0.3 then 1.0, and 1.0 then 0.3. Assert that native 1.0 is selected when both are available under both `useClientPreference` modes. Assert a manually constructed `AgentInterface(..., "0.3.0")` normalizes and selects a fake `VersionedClientTransportProvider`; assert no provider and no matching configured native transport produce distinct actionable errors. Assert duplicate providers for one canonical `(binding, version)` fail descriptively rather than using `ServiceLoader` order.

- [ ] **Step 2: Run tests to verify failure**

Run: `mvn -pl client/base -am -Dtest=ClientBuilderTest,VersionedClientTransportProviderTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: compilation fails because the versioned provider SPI and version-aware selection do not exist.

- [ ] **Step 3: Implement version-aware candidate selection**

Replace `getServerInterfacesMap()`'s binding-only deduplication with an ordered candidate list keyed by binding plus normalized version. Preserve server order between different candidates. For duplicate binding/version entries retain the first. When both versions are eligible for one binding, place the 1.0 candidate before 0.3 regardless of order. Apply the same native-first tie breaker in client-preference selection.

Load `VersionedClientTransportProvider` separately from `ClientTransportProvider`; reject duplicate canonical `(binding, version)` claims during registry creation. Never add it to `transportProviderRegistry`. In `findBestClientTransport()`, validate a 1.0 candidate against the native registry and a 0.3 candidate against the versioned registry; unknown versions fail without falling through to native. In `buildClientTransport()`, use `configuredTransportClass()` to retrieve the ordinary 1.0 config supplied by `withTransport(...)`, then create and wrap the adapter like a native transport. Wrappers receive that unchanged ordinary 1.0 configuration.

- [ ] **Step 4: Run focused verification**

Run: `mvn -pl client/base -am test`

Expected: current builder tests, new candidate-order tests, and wrapper behavior pass.

- [ ] **Step 5: Commit**

```bash
git add client/base
git commit -m "feat: route client transports by protocol version"
```

### Task 4: Implement the shared 0.3 adapter behavior

**Files:**

- Create: `compat-0.3/client/adapter/src/main/java/org/a2aproject/sdk/compat03/client/adapter/Compat03ClientTransportSupport.java`
- Create: `compat-0.3/client/adapter/src/main/java/org/a2aproject/sdk/compat03/client/adapter/Compat03ClientCallContextMapper.java`
- Create: `compat-0.3/client/adapter/src/main/java/org/a2aproject/sdk/compat03/client/adapter/Compat03ClientErrorMapper.java`
- Create: corresponding unit tests.

**Consumes:** neutral mappers, 1.0 `ClientTransport` contract, 0.3 spec/client transport SPI.

**Produces:** shared request/result/event/context/error conversion plus local validation used by each binding adapter.

- [x] **Step 1: Write failing unit tests for local semantics**

Test every 0.3-rejected call before mapper, interceptor, or delegate invocation: `listTasks`, `getExtendedAgentCard`, non-empty tenant on every tenant-bearing request and `TaskPushNotificationConfig`, non-default list-push page size/token. For accepted list-push defaults, assert conversion returns `nextPageToken = ""`. Test `CancelTaskParams`, `TaskQueryParams`, `TaskIdParams`, `MessageSendParams`, tasks, events, and push config in both directions. Test known 0.3 errors become 1.0 `A2AClientException` with a meaningful 1.0 cause.

- [x] **Step 2: Run tests to verify failure**

Run: `mvn -pl compat-0.3/client/adapter -am test`

Expected: compilation fails because shared support classes do not exist.

- [x] **Step 3: Implement shared support**

Use neutral mappers for all domain conversions. Convert contexts by copying state and headers. Implement the exact method mapping: `cancelTask(CancelTaskParams)` → legacy `cancelTask(TaskIdParams_v0_3)`; `subscribeToTask` → legacy `resubscribe`; `createTaskPushNotificationConfiguration` → legacy `setTaskPushNotificationConfiguration`; legacy list-push `List<TaskPushNotificationConfig_v0_3>` → 1.0 `ListTaskPushNotificationConfigsResult(configs, "")`. Make adapter `close()` idempotent; do not claim ownership of caller-supplied gRPC channels. Copy the HTTP client/channel factory but **do not install 1.0 interceptors on a legacy delegate**: each binding adapter invokes them before conversion, because several legacy delegates ignore replacement payloads. Generic 1.0 config `parameters` are unsupported and must cause a local error when non-empty rather than being discarded.

- [x] **Step 4: Run focused verification**

Run: `mvn -pl compat-0.3/client/adapter -am test`

Expected: all conversion, error, validation, context, and interceptor bridge tests pass.

- [x] **Step 5: Commit**

```bash
git add compat-0.3/client/adapter
git commit -m "feat: add shared 0.3 client adapter support"
```

### Task 5: Add JSON-RPC and REST adapter transports

**Files:**

- Create: `compat-0.3/client/adapter-jsonrpc/pom.xml`
- Create: JSON-RPC adapter transport/provider and `META-INF/services/org.a2aproject.sdk.client.VersionedClientTransportProvider`
- Create: `compat-0.3/client/adapter-rest/pom.xml`
- Create: REST adapter transport/provider and service registration
- Create: unit/integration tests under each adapter module.
- Modify: `compat-0.3/pom.xml` dependency management and module list.

**Produces:** 1.0 `ClientTransport` implementations delegating respectively to `JSONRPCTransport_v0_3` and `RestTransport_v0_3`, configured with ordinary `JSONRPCTransportConfig` and `RestTransportConfig`.

Each binding module owns its interceptor invoker: `JsonRpcCompat03InterceptorBridge` in `adapter-jsonrpc` and `RestCompat03InterceptorBridge` in `adapter-rest`. Before calling a legacy delegate, it constructs the documented 1.0 protobuf payload, invokes the ordinary 1.0 interceptors with the public 1.0 card/context, validates a replacement has the exact expected 1.0 type, converts that replacement back to a 1.0 domain request and then a legacy 0.3 domain request, and supplies a copied legacy context containing the resulting headers. It must not install payload-mutating interceptors on the legacy delegate: gRPC delegates ignore replacement payloads, while REST derives several routes from its original domain parameters. Wrong replacement type, disallowed null replacement, and an attempted `A2A-Version` override are local failures.

The following is the complete interceptor payload contract. `grpc.*` means the 1.0 generated protobuf class; `compat03.*` means a 0.3 domain type. Every entry has `null = forbidden`; the bridge invokes the ordinary 1.0 interceptor with the exact 1.0 method constant shown, requires the exact replacement type, converts the replacement through the 1.0 domain type to the listed legacy domain argument, then calls the legacy delegate. The JSON-RPC delegate subsequently creates its own envelope, so no envelope is passed through an interceptor bridge.

| Binding | Client operation / 1.0 interceptor method | 1.0 payload and permitted replacement | Legacy delegate argument after mutation |
|---|---|---|---|
| JSON-RPC | `sendMessage` / `SendMessage` | `grpc.SendMessageRequest` | `compat03.MessageSendParams_v0_3` |
| JSON-RPC | `sendMessageStreaming` / `SendStreamingMessage` | `grpc.SendMessageRequest` | `compat03.MessageSendParams_v0_3` |
| JSON-RPC | `getTask` / `GetTask` | `grpc.GetTaskRequest` | `compat03.TaskQueryParams_v0_3` |
| JSON-RPC | `cancelTask` / `CancelTask` | `grpc.CancelTaskRequest` | `compat03.TaskIdParams_v0_3` |
| JSON-RPC | `createTaskPushNotificationConfiguration` / `CreateTaskPushNotificationConfig` | `grpc.TaskPushNotificationConfig` | `compat03.TaskPushNotificationConfig_v0_3` |
| JSON-RPC | `getTaskPushNotificationConfiguration` / `GetTaskPushNotificationConfig` | `grpc.GetTaskPushNotificationConfigRequest` | `compat03.GetTaskPushNotificationConfigParams_v0_3` |
| JSON-RPC | `listTaskPushNotificationConfigurations` / `ListTaskPushNotificationConfigs` | `grpc.ListTaskPushNotificationConfigsRequest` | `compat03.ListTaskPushNotificationConfigParams_v0_3` |
| JSON-RPC | `deleteTaskPushNotificationConfigurations` / `DeleteTaskPushNotificationConfig` | `grpc.DeleteTaskPushNotificationConfigRequest` | `compat03.DeleteTaskPushNotificationConfigParams_v0_3` |
| JSON-RPC | `subscribeToTask` / `SubscribeToTask` | `grpc.SubscribeToTaskRequest` | `compat03.TaskIdParams_v0_3` |
| REST | `sendMessage` / `SendMessage` | `grpc.SendMessageRequest.Builder` | `compat03.MessageSendParams_v0_3` |
| REST | `sendMessageStreaming` / `SendStreamingMessage` | `grpc.SendMessageRequest.Builder` | `compat03.MessageSendParams_v0_3` |
| REST | `getTask` / `GetTask` | `grpc.GetTaskRequest.Builder` | `compat03.TaskQueryParams_v0_3` |
| REST | `cancelTask` / `CancelTask` | `grpc.CancelTaskRequest.Builder` | `compat03.TaskIdParams_v0_3` |
| REST | `createTaskPushNotificationConfiguration` / `CreateTaskPushNotificationConfig` | `grpc.TaskPushNotificationConfig.Builder` | `compat03.TaskPushNotificationConfig_v0_3` |
| REST | `getTaskPushNotificationConfiguration` / `GetTaskPushNotificationConfig` | `grpc.GetTaskPushNotificationConfigRequest.Builder` | `compat03.GetTaskPushNotificationConfigParams_v0_3` |
| REST | `listTaskPushNotificationConfigurations` / `ListTaskPushNotificationConfigs` | `grpc.ListTaskPushNotificationConfigsRequest.Builder` | `compat03.ListTaskPushNotificationConfigParams_v0_3` |
| REST | `deleteTaskPushNotificationConfigurations` / `DeleteTaskPushNotificationConfig` | `grpc.DeleteTaskPushNotificationConfigRequestOrBuilder` | `compat03.DeleteTaskPushNotificationConfigParams_v0_3` |
| REST | `subscribeToTask` / `SubscribeToTask` | `grpc.SubscribeToTaskRequest.Builder` | `compat03.TaskIdParams_v0_3` |

Header routing is deliberately binding-specific and matches the existing 0.3 transports: after every 1.0 interceptor returns, JSON-RPC and REST bridges remove `A2A-Version` case-insensitively from context and interceptor headers, and do not add it. The legacy 0.3 clients originally sent no version header, which routes to the 0.3 endpoint on a cohosted HTTP server. Any interceptor attempt to set that header is rejected rather than silently removed. All other interceptor headers survive. Task 5 wire tests must assert header absence for all nine operations on both bindings.

- [ ] **Step 1: Write failing wire-contract tests**

For each binding, use MockServer or the existing legacy transport fixture style. Build a normal 1.0 `Client` with a projected 0.3 `AgentCard`, ordinary 1.0 config, and the adapter module. Assert legacy request envelope and parameter field shape, REST path, and absence of `A2A-Version`; do not assert different JSON-RPC method names because many legacy method strings are identical. Assert absence of 1.0-only tenant/page fields. Test blocking send, streaming send, get, cancel, subscribe, and push-config operations. Test all streaming event variants: message, task, status update, artifact update. For every operation in the table, test interceptor observation, valid mutation, wrong-type replacement, forbidden null replacement, and version-header override.

- [ ] **Step 2: Run tests to verify failure**

Run: `mvn -pl compat-0.3/client/adapter-jsonrpc,compat-0.3/client/adapter-rest -am -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: Maven reports the adapter modules are absent.

- [ ] **Step 3: Implement per-binding adapters**

Each provider declares its binding (`JSONRPC` or `HTTP+JSON`), canonical `"0.3"` version, and the corresponding ordinary 1.0 configured transport class. Copy the HTTP client into the legacy config, retain the ordinary 1.0 interceptor list in the adapter, and pass no payload-mutating interceptors to the legacy transport. Each adapter implements every `ClientTransport` method, applying Task 4 validation, its own interceptor invocation, and conversion before delegating to the legacy transport. Do not call `Client_v0_3`.

- [ ] **Step 4: Run focused verification**

Run: `mvn -pl compat-0.3/client/adapter-jsonrpc,compat-0.3/client/adapter-rest -am test`

Expected: legacy wire assertions and ordinary 1.0 client callbacks pass.

- [ ] **Step 5: Commit**

```bash
git add compat-0.3/client/adapter-jsonrpc compat-0.3/client/adapter-rest compat-0.3/pom.xml
git commit -m "feat: add 0.3 JSON-RPC and REST client adapters"
```

### Task 6: Add gRPC adapter transport

**Files:**

- Create: `compat-0.3/client/adapter-grpc/pom.xml`
- Create: gRPC adapter transport/provider and `META-INF/services/org.a2aproject.sdk.client.VersionedClientTransportProvider`
- Create: gRPC adapter tests using an in-process 0.3 gRPC service.
- Modify: `compat-0.3/pom.xml` dependency management and module list.

**Produces:** A 1.0 `ClientTransport` adapter over `GrpcTransport_v0_3`, configured from ordinary `GrpcTransportConfig` and using the legacy `a2a.v1` service rather than the 1.0 `lf.a2a.v1` service.

The module owns `GrpcCompat03InterceptorBridge`. Its complete contract is below. `grpc.*` means the 1.0 generated protobuf class; `compat03.*` the legacy domain class. Every row passes the specified immutable 1.0 protobuf payload to the ordinary 1.0 interceptor, permits only the same immutable type as a replacement, rejects null, converts it through the 1.0 domain type to the listed legacy domain argument, and then calls `GrpcTransport_v0_3`. Do not install interceptors on that delegate: it ignores a replacement payload returned from its own legacy interceptor chain.

| Client operation / 1.0 interceptor method | 1.0 payload and permitted replacement | Legacy delegate argument after mutation |
|---|---|---|
| `sendMessage` / `SendMessage` | `grpc.SendMessageRequest` | `compat03.MessageSendParams_v0_3` |
| `sendMessageStreaming` / `SendStreamingMessage` | `grpc.SendMessageRequest` | `compat03.MessageSendParams_v0_3` |
| `getTask` / `GetTask` | `grpc.GetTaskRequest` | `compat03.TaskQueryParams_v0_3` |
| `cancelTask` / `CancelTask` | `grpc.CancelTaskRequest` | `compat03.TaskIdParams_v0_3` |
| `createTaskPushNotificationConfiguration` / `CreateTaskPushNotificationConfig` | `grpc.TaskPushNotificationConfig` | `compat03.TaskPushNotificationConfig_v0_3` |
| `getTaskPushNotificationConfiguration` / `GetTaskPushNotificationConfig` | `grpc.GetTaskPushNotificationConfigRequest` | `compat03.GetTaskPushNotificationConfigParams_v0_3` |
| `listTaskPushNotificationConfigurations` / `ListTaskPushNotificationConfigs` | `grpc.ListTaskPushNotificationConfigsRequest` | `compat03.ListTaskPushNotificationConfigParams_v0_3` |
| `deleteTaskPushNotificationConfigurations` / `DeleteTaskPushNotificationConfig` | `grpc.DeleteTaskPushNotificationConfigRequest` | `compat03.DeleteTaskPushNotificationConfigParams_v0_3` |
| `subscribeToTask` / `SubscribeToTask` | `grpc.SubscribeToTaskRequest` | `compat03.TaskIdParams_v0_3` |

After each interceptor, remove `A2A-Version` case-insensitively and reject an interceptor which supplied it; do not add version metadata. Selection of the legacy `a2a.v1.A2AService` service, rather than a metadata header, is the gRPC version-routing mechanism. Preserve all other metadata.

- [x] **Step 1: Write failing in-process gRPC tests**

Start only the legacy generated gRPC service. Build the normal concrete 1.0 `Client` through the versioned adapter and verify blocking, streaming, get, cancel, subscribe, push config, error conversion, metadata/auth interceptor bridging, interceptor mutation failures, rejection of a version-header override, absence of `A2A-Version` metadata, and double `close()`. Assert a request reaches `a2a.v1.A2AService` and never the 1.0 `lf.a2a.v1` service.

- [x] **Step 2: Run tests to verify failure**

Run: `mvn -pl compat-0.3/client/adapter-grpc -am -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: Maven reports the adapter module is absent.

- [x] **Step 3: Implement the adapter**

Copy the ordinary 1.0 channel factory into `GrpcTransportConfig_v0_3`, pass no payload-mutating interceptors to the legacy delegate, and delegate every supported operation through the legacy gRPC transport after the adapter has invoked/mapped its ordinary 1.0 interceptors and copied their headers into the legacy context. Preserve caller ownership of the supplied channel and make only adapter-local close state idempotent.

- [x] **Step 4: Run focused verification**

Run: `mvn -pl compat-0.3/client/adapter-grpc -am test`

Expected: all calls use the legacy service and consumers receive 1.0 events.

- [x] **Step 5: Commit**

```bash
git add compat-0.3/client/adapter-grpc compat-0.3/pom.xml
git commit -m "feat: add 0.3 gRPC client adapter"
```

### Task 7: Package, document, and run the end-to-end matrix

**Files:**

- Modify: `boms/sdk/pom.xml` to publish the neutral conversion and client compatibility adapter artifacts; do not add them to extras or reference BOMs.
- Modify: `docs/content/dev/compatibility.md`
- Create or modify: standalone-legacy test fixtures/modules under `compat-0.3/reference/{jsonrpc,rest,grpc}` (or another fixture that does not replace the legacy card with a cohosted 1.0 card), without using a cohosted 1.0 endpoint for legacy assertions.
- Modify: parent/compat reactor POMs for all new modules.

- [ ] **Step 1: Write failing packaging and classpath tests**

Add isolated forked-JVM or Maven fixture-project coverage for: parser absent; parser present with no binding adapter; each binding adapter; duplicate versioned providers; and standard client-only classpath. Do not try to add `META-INF/services` resources after `ClientBuilder` has loaded. Also test compat parser present but 0.3 not requested, 0.3 requested but binding adapter absent, and a client-only dependency-tree assertion that contains no server, CDI, Quarkus, or reference artifacts.

- [ ] **Step 2: Run tests to verify failure**

Run: `mvn -pl compat-0.3/reference/jsonrpc,compat-0.3/reference/rest,compat-0.3/reference/grpc -am -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: new standalone legacy/classpath scenarios are missing.

- [ ] **Step 3: Document and implement packaging**

Add all new compat artifacts to `compat-0.3/pom.xml` dependency management and add client-facing artifacts only to `boms/sdk/pom.xml`. If test modules use the new artifacts without explicit `${project.version}`, add them to root `pom.xml` dependency management first. Update the compatibility guide with the explicit `A2A.getAgentCard(..., Set.of("1.0", "0.3"))` call, the normal concrete `Client.builder(card)` usage, required optional artifacts per binding, unsupported-operation behavior, and the guarantee that client-only users do not import 0.3 types or server libraries.

- [ ] **Step 4: Run full verification**

Run: `mvn clean install`

Expected: the complete reactor passes. If environment constraints prevent full integration tests, report the exact skipped/failing module and run all unaffected adapter, resolver, builder, and conversion module tests.

- [ ] **Step 5: Inspect dependency boundaries**

Run:

```bash
mvn -pl compat-0.3/client/adapter dependency:tree
mvn -pl compat-0.3/client/adapter-jsonrpc dependency:tree
mvn -pl compat-0.3/client/adapter-rest dependency:tree
mvn -pl compat-0.3/client/adapter-grpc dependency:tree
```

Expected: no `a2a-java-sdk-server-common`, CDI, Quarkus, or reference-server artifact appears in the client adapter dependency trees.

- [ ] **Step 6: Commit**

```bash
git add pom.xml boms/sdk compat-0.3 docs/content/dev tests
git commit -m "build: publish 0.3 client compatibility artifacts"
```
