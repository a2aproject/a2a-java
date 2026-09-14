# 1.0 Client Support for A2A 0.3 Servers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow applications to use the existing concrete 1.0 `Client` and 1.0 spec types against a 0.3 server when they explicitly opt in and install optional compat artifacts.

**Architecture:** Keep `Client` as the public API and adapt at its existing `ClientTransport` boundary. Add a raw-card compatibility parser SPI below the client transport SPI, and a separate versioned transport-adapter SPI used by `ClientBuilder`; neither collides with the existing binding-only transport provider registry. Extract pure 0.3/1.0 mappers from the server conversion module into a neutral artifact consumed by both client and server code.

**Tech Stack:** Java 17, Maven multi-module build, Gson/protobuf JSON parsing, MapStruct, `ServiceLoader`, JUnit 5, MockServer.

**Spec:** `docs/superpowers/specs/2026-09-14-client-v03-compatibility-design.md`

## Global Constraints

- The public client remains `org.a2aproject.sdk.client.Client`; callers use only 1.0 request, result, event, card, context, config, and interceptor types.
- Existing `A2A.getAgentCard(...)` overloads stay 1.0-only; a new overload explicitly receives supported protocol versions.
- Legacy fallback is enabled only when the caller includes `"0.3"` and the relevant optional compat JARs are installed.
- Canonical supported versions are `"1.0"` and `"0.3"`; normalize a patch suffix of `.0` before matching (for example `"0.3.0"` → `"0.3"`). Reject any other version instead of guessing compatibility.
- Preserve the client/server dependency separation: no client artifact may depend on `server-common`, CDI, Quarkus, or reference-server modules.
- Do not register a compat transport as the existing `ClientTransportProvider`; its registry is keyed only by protocol binding and would overwrite the native provider.
- Against a 0.3 binding, reject locally before I/O: `listTasks`, non-empty tenants, extended-agent-card retrieval, and non-default push-config pagination.
- Use conventional commits. This work is not related to a GitHub issue, so do not add a `This fixes #...` footer.

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
| `compat-0.3/client/adapter/` | Legacy-card parser, error/context/interceptor bridge utilities, and shared adapter code. |
| `compat-0.3/client/adapter-{jsonrpc,rest,grpc}/` | Per-binding 1.0 `ClientTransport` adapters and provider registrations. |

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

`A2ACardResolver.Builder` gains `supportedProtocolVersions(Set<String>)`. `A2A` adds this overload and analogous custom-client/card-path overloads, forwarding the set to the resolver:

```java
public static AgentCard getAgentCard(
        String agentUrl, Set<String> supportedProtocolVersions)
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

Also test: raw body is fetched once; a usable 1.0 card remains native; a dual-format card prefers its 1.0 interface; a requested 0.3 parser absent gives an actionable error; invalid/unsupported versions fail before the HTTP request; custom card path and authorization headers survive the fallback path.

- [ ] **Step 2: Run tests to verify failure**

Run: `mvn -pl http-client,client/base -am -Dtest=A2ACardResolverTest,A2AAgentCardResolutionTest test`

Expected: compilation fails because the options/SPI do not exist.

- [ ] **Step 3: Implement raw-card parsing without a second request**

Refactor `A2ACardResolver.fetchAgentCard` so it retains the fetched response body, parses native 1.0 once, and only loads/invokes `AgentCardCompatibilityParser` if normalized requested versions include `"0.3"`. A native parse counts as usable only when it yields at least one eligible interface. Pass the same raw body to the parser; never re-fetch it.

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

Add cards with JSON-RPC interfaces in both orders: 0.3 then 1.0, and 1.0 then 0.3. Assert that native 1.0 is selected when both are available under both `useClientPreference` modes. Assert an explicit 0.3-only card selects a fake `VersionedClientTransportProvider`; assert no provider and no matching configured native transport produce distinct actionable errors.

- [ ] **Step 2: Run tests to verify failure**

Run: `mvn -pl client/base -am -Dtest=ClientBuilderTest,VersionedClientTransportProviderTest test`

Expected: compilation fails because the versioned provider SPI and version-aware selection do not exist.

- [ ] **Step 3: Implement version-aware candidate selection**

Replace `getServerInterfacesMap()`'s binding-only deduplication with an ordered candidate list keyed by binding plus normalized version. Preserve server order between different candidates. For duplicate binding/version entries retain the first. When both versions are eligible for one binding, place the 1.0 candidate before 0.3 regardless of order. Apply the same native-first tie breaker in client-preference selection.

Load `VersionedClientTransportProvider` separately from `ClientTransportProvider`. Never add it to `transportProviderRegistry`. Use its `configuredTransportClass()` to retrieve the ordinary 1.0 config supplied by `withTransport(...)`, then create and wrap the adapter like a native transport.

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
- Create: `compat-0.3/client/adapter/src/main/java/org/a2aproject/sdk/compat03/client/adapter/Compat03InterceptorBridge.java`
- Create: corresponding unit tests.

**Consumes:** neutral mappers, 1.0 `ClientTransport` contract, 0.3 spec/client transport SPI.

**Produces:** shared request/result/event/context/error conversion plus local validation used by each binding adapter.

- [ ] **Step 1: Write failing unit tests for local semantics**

Test every 0.3-rejected call before delegate invocation: `listTasks`, `getExtendedAgentCard`, non-empty tenant on every tenant-bearing request and push config, non-default list-push page size/token. For accepted list-push defaults, assert conversion returns `nextPageToken = ""`. Test `CancelTaskParams`, `TaskQueryParams`, `TaskIdParams`, `MessageSendParams`, tasks, events, and push config in both directions. Test known 0.3 errors become 1.0 `A2AClientException` with a meaningful 1.0 cause.

- [ ] **Step 2: Run tests to verify failure**

Run: `mvn -pl compat-0.3/client/adapter -am test`

Expected: compilation fails because shared support classes do not exist.

- [ ] **Step 3: Implement shared support**

Use neutral mappers for all domain conversions. Convert contexts by copying state and headers. Implement interceptor bridges per protocol payload type; preserve headers but enforce the selected 0.3 version after interceptor output, rejecting an attempted version override. Make adapter `close()` idempotent; do not claim ownership of caller-supplied gRPC channels.

- [ ] **Step 4: Run focused verification**

Run: `mvn -pl compat-0.3/client/adapter -am test`

Expected: all conversion, error, validation, context, and interceptor bridge tests pass.

- [ ] **Step 5: Commit**

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

- [ ] **Step 1: Write failing wire-contract tests**

For each binding, use MockServer or the existing legacy transport fixture style. Build a normal 1.0 `Client` with a projected 0.3 `AgentCard`, ordinary 1.0 config, and the adapter module. Assert emitted JSON has 0.3 method names and payload shape, does not contain 1.0-only tenant/page fields, and routes as 0.3. Test blocking send, streaming send, get, cancel, subscribe, and push-config operations. Test all streaming event variants: message, task, status update, artifact update.

- [ ] **Step 2: Run tests to verify failure**

Run: `mvn -pl compat-0.3/client/adapter-jsonrpc,compat-0.3/client/adapter-rest -am test`

Expected: Maven reports the adapter modules are absent.

- [ ] **Step 3: Implement per-binding adapters**

Each provider declares its binding (`JSONRPC` or `HTTP+JSON`), canonical `"0.3"` version, and the corresponding ordinary 1.0 configured transport class. Copy the HTTP client into the legacy config and install bridged interceptors. Each adapter implements every `ClientTransport` method, applying Task 4 validation/conversion and delegating to the legacy transport. Do not call `Client_v0_3`.

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

- [ ] **Step 1: Write failing in-process gRPC tests**

Start only the legacy generated gRPC service. Build the normal concrete 1.0 `Client` through the versioned adapter and verify blocking, streaming, get, cancel, subscribe, push config, error conversion, metadata/auth interceptor bridging, and double `close()`. Assert a request never reaches the 1.0 gRPC service.

- [ ] **Step 2: Run tests to verify failure**

Run: `mvn -pl compat-0.3/client/adapter-grpc -am test`

Expected: Maven reports the adapter module is absent.

- [ ] **Step 3: Implement the adapter**

Copy the ordinary 1.0 channel factory into `GrpcTransportConfig_v0_3`, install bridged interceptors/metadata, and delegate every supported operation through the legacy gRPC transport. Preserve caller ownership of the supplied channel and make only adapter-local close state idempotent.

- [ ] **Step 4: Run focused verification**

Run: `mvn -pl compat-0.3/client/adapter-grpc -am test`

Expected: all calls use the legacy service and consumers receive 1.0 events.

- [ ] **Step 5: Commit**

```bash
git add compat-0.3/client/adapter-grpc compat-0.3/pom.xml
git commit -m "feat: add 0.3 gRPC client adapter"
```

### Task 7: Package, document, and run the end-to-end matrix

**Files:**

- Modify: relevant BOM POMs under `boms/` to publish the neutral conversion and compat adapter artifacts.
- Modify: `docs/content/dev/compatibility.md`
- Create or modify: standalone-legacy test fixtures/modules under `tests/multiversion/` or the adapter modules, without using a cohosted 1.0 endpoint for legacy assertions.
- Modify: parent/compat reactor POMs for all new modules.

- [ ] **Step 1: Write failing packaging and classpath tests**

Add Maven-level/integration coverage for: standard client-only classpath; compat parser absent; compat parser present but 0.3 not requested; 0.3 requested but binding adapter absent; each installed binding adapter; and a client-only dependency-tree assertion that contains no server, CDI, Quarkus, or reference artifacts.

- [ ] **Step 2: Run tests to verify failure**

Run: `mvn -pl tests/multiversion/jsonrpc,tests/multiversion/rest,tests/multiversion/grpc -am test`

Expected: new standalone legacy/classpath scenarios are missing.

- [ ] **Step 3: Document and implement packaging**

Add all artifacts to dependency management/BOMs. Update the compatibility guide with the explicit `A2A.getAgentCard(..., Set.of("1.0", "0.3"))` call, the normal concrete `Client.builder(card)` usage, required optional artifacts per binding, unsupported-operation behavior, and the guarantee that client-only users do not import 0.3 types or server libraries.

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
git add boms compat-0.3 docs/content/dev tests
git commit -m "docs: document 0.3 client compatibility"
```
