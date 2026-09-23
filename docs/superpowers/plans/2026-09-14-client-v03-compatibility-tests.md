# v1 Client Compatibility Tests Against v0.3 Servers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add parallel end-to-end tests proving that the public v1 `Client` compatibility layer communicates with standalone A2A v0.3 JSON-RPC, REST, and gRPC servers while preserving the existing v0.3 client tests unchanged.

**Architecture:** Keep the legacy v0.3 abstract tests and subclasses as a frozen regression suite. Add v1-only compatibility abstract bases to the root server test-commons test-jar, then add sibling subclasses to the standalone `compat-0.3/reference` test modules. HTTP/JSON-RPC and REST tests discover the genuine legacy card through the opt-in `A2A.getAgentCard(..., Set.of("0.3"))` API; gRPC supplies an equivalent v1 card fixture because it has no card endpoint.

**Tech Stack:** Java 17, Maven multi-module build, JUnit 5, Quarkus tests, REST Assured, Gson, v1 client transports, v0.3 client adapters, ServiceLoader.

**Spec:** `docs/superpowers/specs/2026-09-14-client-v03-compatibility-tests-design.md`

## Global Constraints

- Keep `AbstractA2AServerServerTest_v0_3`, `AbstractA2AServerWithAuthTest_v0_3`, and all existing v0.3 subclasses unchanged.
- New compatibility tests use only v1 `Client`, `ClientBuilder`, `ClientConfig`, v1 spec types, v1 transport configs, and v1 interceptors.
- Name new bases `AbstractA2AServerCompatibilityTest_v0_3` and `AbstractA2AServerCompatibilityWithAuthTest_v0_3`.
- Place the new bases in `tests/server-common/src/test/java/org/a2aproject/sdk/server/apps/common/` so they are exported by the existing `a2a-java-sdk-tests-server-common` test-jar.
- Target standalone `compat-0.3/reference/jsonrpc`, `compat-0.3/reference/rest`, and `compat-0.3/reference/grpc` for compatibility end-to-end tests; do not use the co-hosted `tests/multiversion/*` v1 card for legacy-only discovery assertions.
- Keep the existing legacy v0.3 dependencies in each reference test module and add explicit v1 client, native transport, shared adapter, binding adapter, and root server-common test-jar dependencies.
- Do not create a client facade shared by the v0.3 and v1 suites. Add only small helpers with one obvious responsibility when duplication is mechanical.
- Close every v1 `Client` created by a compatibility base after each test. Concrete subclasses retain ownership of caller-supplied gRPC channels and close them explicitly.
- Preserve expected local v0.3 rejections for `listTasks`, non-empty tenants, extended-agent-card retrieval, and non-default push-config pagination.
- Before end-to-end tests are enabled, make card conversion support primary-only cards, canonicalize `jsonrpc`/`http` or `rest`/`grpc` to `JSONRPC`/`HTTP+JSON`/`GRPC`, and map v0.3 HTTP Basic schemes to v1 `HTTPAuthSecurityScheme`.
- The one-fetch guarantee belongs in resolver/parser unit tests with a counting HTTP client; end-to-end tests prove that the projected card builds and operates a client.
- The scenario groups in the plan are independently reviewable slices: each group gets focused red/green verification before the next group begins. Do not translate the entire legacy base in one unverified edit.
- The implementation is not related to a GitHub issue; commits must not include a `This fixes #...` footer.

---

### Task 1: Make legacy agent-card conversion usable for compatibility discovery

**Files:**

- Modify: `compat-0.3/conversion/src/main/java/org/a2aproject/sdk/compat03/conversion/mappers/domain/AgentCardMapper_v0_3.java`
- Modify: `compat-0.3/conversion/src/test/java/org/a2aproject/sdk/compat03/conversion/mappers/domain/AgentCardMapper_v0_3_Test.java`
- Modify: `compat-0.3/client/adapter/src/test/java/org/a2aproject/sdk/compat03/client/adapter/Compat03AgentCardCompatibilityParserTest.java`

**Interfaces:**

- `AgentCardMapper_v0_3.toV10(AgentCard_v0_3)` must return at least one usable v1 `AgentInterface` when the legacy card has a primary `url` and `preferredTransport` but no `additionalInterfaces`.
- `AgentCardMapper_v0_3` must map the legacy binding spellings `jsonrpc`, `http`, `rest`, and `grpc` case-insensitively to `JSONRPC`, `HTTP+JSON`, and `GRPC`; canonical values remain unchanged.
- `AgentCardMapper_v0_3` must map `HTTPAuthSecurityScheme_v0_3` to v1 `HTTPAuthSecurityScheme`, preserving `bearerFormat`, `scheme`, and `description`, and map that v1 type back to the legacy type.

- [ ] **Step 1: Add failing primary-interface mapper tests**

Extend `AgentCardMapper_v0_3_Test` with a parameterized test using a card whose `additionalInterfaces` is empty. Supply `(preferredTransport, expectedBinding)` rows for `jsonrpc`/`JSONRPC`, `http`/`HTTP+JSON`, `rest`/`HTTP+JSON`, `grpc`/`GRPC`, and canonical `HTTP+JSON`/`HTTP+JSON`. Assert that `supportedInterfaces()` contains exactly one interface with `url`, expected binding, and protocol version `"0.3"`.

Add a test using `new HTTPAuthSecurityScheme_v0_3.Builder().scheme("basic").bearerFormat("none").description("HTTP Basic authentication").build()`. Assert that the projected v1 card contains `HTTPAuthSecurityScheme` with the same scheme, bearer format, and description, and that `fromV10(toV10(card))` restores those fields on the legacy HTTP scheme.

In `Compat03AgentCardCompatibilityParserTest`, add the same primary-only and Basic-auth fixtures serialized with `JsonUtil_v0_3`, using `protocolVersion("0.3.0")`. Assert that parsing returns one canonical `0.3` interface and the projected v1 HTTP auth scheme. These parser tests must be part of the initial red test run.

```java
private static AgentCard_v0_3 primaryOnlyCard(String preferredTransport) {
    return new AgentCard_v0_3.Builder()
            .name("legacy")
            .description("legacy")
            .url("http://localhost:8081")
            .version("1.0.0")
            .preferredTransport(preferredTransport)
            .protocolVersion("0.3.0")
            .capabilities(new AgentCapabilities_v0_3.Builder().build())
            .defaultInputModes(List.of("text"))
            .defaultOutputModes(List.of("text"))
            .skills(List.of())
            .additionalInterfaces(List.of())
            .build();
}

@ParameterizedTest
@CsvSource({
    "jsonrpc,JSONRPC",
    "http,HTTP+JSON",
    "rest,HTTP+JSON",
    "grpc,GRPC",
    "HTTP+JSON,HTTP+JSON"
})
void primaryEndpointBecomesSupportedInterface(String preferred, String expectedBinding) {
    AgentCard_v0_3 card = primaryOnlyCard(preferred);

    AgentCard projected = AgentCardMapper_v0_3.INSTANCE.toV10(card);

    assertEquals(1, projected.supportedInterfaces().size());
    assertEquals(expectedBinding, projected.supportedInterfaces().get(0).protocolBinding());
    assertEquals("0.3", projected.supportedInterfaces().get(0).protocolVersion());
    assertEquals(expectedBinding, projected.preferredTransport());
}
```

- [ ] **Step 2: Run the focused tests and confirm failure**

Run:

```bash
mvn -pl compat-0.3/conversion,compat-0.3/client/adapter -am -Dtest=AgentCardMapper_v0_3_Test,Compat03AgentCardCompatibilityParserTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: the new tests fail because the mapper drops the primary endpoint and rejects HTTP auth schemes.

- [ ] **Step 3: Implement the minimal mapper changes**

In `toV10`, build interfaces from `additionalInterfaces`; when that list is empty and both `url` and `preferredTransport` are non-null, create one `AgentInterface(canonicalBinding(preferredTransport), url, null, "0.3")`. Use the same canonicalization helper for additional interfaces and set the projected card’s top-level `preferredTransport` to the canonical binding as well. Add explicit `HTTPAuthSecurityScheme_v0_3` and v1 `HTTPAuthSecurityScheme` branches to the bidirectional security conversion methods, preserving bearer format. Continue throwing for security scheme types with no representable v1 equivalent.

- [ ] **Step 4: Run conversion and parser verification**

Run:

```bash
mvn -pl compat-0.3/conversion,compat-0.3/client/adapter -am test
```

Expected: all mapper, parser, and existing conversion tests pass.

- [ ] **Step 5: Commit**

```bash
git add compat-0.3/conversion/src/main/java/org/a2aproject/sdk/compat03/conversion/mappers/domain/AgentCardMapper_v0_3.java compat-0.3/conversion/src/test/java/org/a2aproject/sdk/compat03/conversion/mappers/domain/AgentCardMapper_v0_3_Test.java compat-0.3/client/adapter/src/test/java/org/a2aproject/sdk/compat03/client/adapter/Compat03AgentCardCompatibilityParserTest.java
git commit -m "fix: support legacy agent card compatibility fields"
```

### Task 2: Add the v1-only compatibility test bases and client lifecycle

**Files:**

- Create: `tests/server-common/src/test/java/org/a2aproject/sdk/server/apps/common/AbstractA2AServerCompatibilityTest_v0_3.java`
- Create: `tests/server-common/src/test/java/org/a2aproject/sdk/server/apps/common/AbstractA2AServerCompatibilityWithAuthTest_v0_3.java`

**Interfaces:**

```java
protected abstract String getTransportProtocol();
protected abstract String getTransportUrl();
protected abstract void configureTransport(ClientBuilder builder);
protected AgentCard getAgentCard();
protected Client getClient() throws A2AClientException;
protected Client getNonStreamingClient() throws A2AClientException;
protected Client getPollingClient() throws A2AClientException;
protected Client createClient(boolean streaming) throws A2AClientException;
protected Client createPollingClient() throws A2AClientException;
```

The auth base additionally exposes the existing v1-shaped hooks:

```java
protected abstract void configureTransportWithAuth(ClientBuilder builder);
protected Client createAuthenticatedClient() throws A2AClientException;
protected Client createUnauthenticatedClient() throws A2AClientException;
protected Client getAuthenticatedClient() throws A2AClientException;
protected Client getUnauthenticatedClient() throws A2AClientException;
```

- [ ] **Step 1: Create the general compatibility base skeleton**

Copy only the test-support structure from `compat-0.3/server-conversion/src/test/java/org/a2aproject/sdk/compat03/conversion/AbstractA2AServerServerTest_v0_3.java`. Replace every legacy client/spec import with its v1 equivalent. Do not copy direct wire tests, task-store sanity tests, or the legacy `getAgentCard()` test.

Use v1 constants and builders:

```java
protected static final Task MINIMAL_TASK = Task.builder()
        .id("task-123")
        .contextId("session-xyz")
        .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
        .build();

protected static final Message MESSAGE = Message.builder()
        .messageId("111")
        .role(Message.Role.ROLE_AGENT)
        .parts(new TextPart("test message"))
        .build();
```

The default `getAgentCard()` must call `A2A.getAgentCard(getTransportUrl(), Set.of("0.3"))`. A gRPC subclass will override it with a v1 fixture. `createClient(boolean)` must build `ClientConfig` with the requested streaming setting, call `Client.builder(getAgentCard())`, invoke `configureTransport`, and retain the returned client for cleanup. `createPollingClient()` uses `ClientConfig.Builder().setStreaming(false).setPolling(true).build()`.

- [ ] **Step 2: Add deterministic client cleanup**

Track every client created by `createClient`, `createPollingClient`, and the auth base’s authenticated/unauthenticated factories in a `List<Client>`. Add `@AfterEach` that closes each client exactly once, clears the list, and sets `client`, `nonStreamingClient`, `pollingClient`, `authenticatedClient`, and `unauthenticatedClient` to `null`; this prevents a closed cached client from being reused by a later test. Do not close subclass-owned channels from the base.

- [ ] **Step 3: Add v1 server utility helpers**

Copy the v1 versions of `saveTaskInTaskStore`, `getTaskFromTaskStore`, `deleteTaskInTaskStore`, `ensureQueueForTask`, `enqueueEventOnServer`, and push-config store helpers from `AbstractA2AServerTest`. Preserve the existing `/test/*` endpoints and v1 Gson mapper. Do not use `TaskMapper_v0_3` or any v0.3 type in these new files.

- [ ] **Step 4: Compile the test-jar sources**

Run:

```bash
mvn -pl tests/server-common -am test-compile
```

Expected: the two new bases compile while no compatibility adapter dependency is added to the root test-common POM.

- [ ] **Step 5: Commit**

```bash
git add tests/server-common/src/test/java/org/a2aproject/sdk/server/apps/common/AbstractA2AServerCompatibilityTest_v0_3.java tests/server-common/src/test/java/org/a2aproject/sdk/server/apps/common/AbstractA2AServerCompatibilityWithAuthTest_v0_3.java
git commit -m "test: add v1 compatibility test bases"
```

### Task 3: Translate supported general, streaming, resubscription, and push scenarios

**Files:**

- Modify: `tests/server-common/src/test/java/org/a2aproject/sdk/server/apps/common/AbstractA2AServerCompatibilityTest_v0_3.java`
- Create: `docs/superpowers/specs/2026-09-14-client-v03-compatibility-tests-matrix.md`

**Interfaces:**

- The base’s client methods use v1 signatures such as `getTask(new TaskQueryParams(id))`, `cancelTask(new CancelTaskParams(id))`, `sendMessage(message, consumers, errorHandler)`, `subscribeToTask(new TaskIdParams(id), ...)`, and v1 push-config parameter records. Do not call the package-level transport overload `sendMessage(MessageSendParams)` as if it were a one-argument public convenience method.
- Consumers use `BiConsumer<ClientEvent, AgentCard>` and inspect `MessageEvent`, `TaskEvent`, and `TaskUpdateEvent` with v1 getters.
- Event fixtures use v1 `Event`, `TaskStatusUpdateEvent`, `TaskArtifactUpdateEvent`, `TaskState`, and `TextPart` values.

Implement the scenario groups below as separate red/green slices. For each slice, add only that group, run `mvn -pl tests/server-common -am test-compile`, correct compile/type failures, and commit before starting the next slice. The end-to-end red/green run becomes available after Task 5 adds the concrete subclasses; at that point run the focused reference-module command for the transport being added before marking its slice complete.

- [ ] **Step 1: Add the synchronous v1 scenario groups**

Translate these legacy methods into the new base, preserving their test names with a `Compatibility` suffix only when needed to prevent collisions:

`testGetTaskSuccess`, `testGetTaskNotFound`, `testCancelTaskSuccess`, `testCancelTaskNotSupported`, `testCancelTaskNotFound`, `testSendMessageNewMessageSuccess`, `testRequestScopedBeanAvailableOnAgentExecutorThread`, `testSendMessageExistingTaskSuccess`, `testSetPushNotificationSuccess`, `testGetPushNotificationSuccess`, and `testError`.

Use v1 errors in assertions, for example:

```java
A2AClientException error = assertThrows(A2AClientException.class,
        () -> getClient().getTask(new TaskQueryParams("non-existent-task")));
assertInstanceOf(TaskNotFoundError.class, error.getCause());
```
Commit this slice as `test: cover v1 compatibility synchronous scenarios`.

- [ ] **Step 2: Add streaming scenarios**

Translate `testSendMessageStreamNewMessageSuccess` and `testSendMessageStreamExistingTaskSuccess`. Preserve the existing latches, queue setup, event ordering, and timeout values. Replace legacy callback types with v1 `ClientEvent` callbacks and replace legacy enum values with `TaskState.TASK_STATE_*` and v1 `StreamingEventKind` values.
Commit this slice as `test: cover v1 compatibility streaming scenarios`.

- [ ] **Step 3: Add resubscription and queue-lifecycle scenarios**

Translate `testResubscribeExistingTaskSuccess`, `testResubscribeNoExistingTaskError`, `testMainQueueReferenceCountingWithMultipleConsumers`, `testNonBlockingWithMultipleMessages`, `testMainQueueStaysOpenForNonFinalTasks`, and `testMainQueueClosesForFinalizedTasks`, preserving their latches, queue setup, event ordering, and timeout values.
Commit this slice as `test: cover v1 compatibility resubscription scenarios`.

- [ ] **Step 4: Add supported push-config scenarios**

Translate the config-id, no-config-id, empty-list, task-not-found, valid-delete, missing-delete, and delete-without-config-id cases. Use the v1 default constructor `new ListTaskPushNotificationConfigsParams(taskId)` for the supported non-paginated list operation and assert the v1 `ListTaskPushNotificationConfigsResult`.
Commit this slice as `test: cover v1 compatibility push configuration scenarios`.

- [ ] **Step 5: Add explicit local rejection scenarios**

Add tests that invoke the v1 client and assert `A2AClientException` with an `UnsupportedOperationError` cause before any server utility state changes:

```java
assertThrows(A2AClientException.class,
        () -> getClient().listTasks(new ListTasksParams()));
assertThrows(A2AClientException.class,
        () -> getClient().getExtendedAgentCard());
assertThrows(A2AClientException.class,
        () -> getClient().getTask(new TaskQueryParams("task-123", null, "tenant-a")));
assertThrows(A2AClientException.class,
        () -> getClient().listTaskPushNotificationConfigurations(
                new ListTaskPushNotificationConfigsParams("task-123", 10, "next", null)));
```

Assert the `UnsupportedOperationError` cause and use `Compat03ClientTransportSupportTest.rejectsUnsupportedOperationsBeforeDelegateUse` as the unit-level proof that the adapter rejects these calls before delegation. The end-to-end tests should only verify the public client behavior and should not add a second counting HTTP fixture to the shared server base.
Commit this slice as `test: cover v1 compatibility local rejections`.

- [ ] **Step 6: Write the scenario matrix**

Create `docs/superpowers/specs/2026-09-14-client-v03-compatibility-tests-matrix.md` with one row for every client-facing test or test group in the two legacy abstract bases. Each row must name the legacy source method, the compatibility test method or resolver/adapter test that replaces it, and whether it is included or intentionally excluded. Include explicit rows for the direct-wire exclusions, task-store exclusion, legacy card retrieval replacement, task authorization exclusion, transport-specific overrides, push operations, streaming/resubscription, and local unsupported operations.
Commit the matrix as `docs: record the v1 compatibility scenario matrix`.

- [ ] **Step 7: Run the compile-level test**

Run:

```bash
mvn -pl tests/server-common -am test-compile
```

Expected: the translated test base and matrix compile without adding v0.3 imports to root test-common production or test code.

- [ ] **Step 8: Confirm the completed scenario slices**

```bash
git status --short
git diff --check
```

The preceding scenario slices and the matrix are already committed separately; do not create a broad catch-all commit here.

### Task 4: Add the authenticated compatibility base

**Files:**

- Modify: `tests/server-common/src/test/java/org/a2aproject/sdk/server/apps/common/AbstractA2AServerCompatibilityWithAuthTest_v0_3.java`

**Interfaces:**

- `createAuthenticatedClient()` and `createUnauthenticatedClient()` use `Client.builder(v1Card)`, v1 `ClientConfig`, and the corresponding v1 transport configuration hook.
- Authenticated configuration uses `org.a2aproject.sdk.client.transport.spi.interceptors.auth.AuthInterceptor` and the existing v1 credential callback shape.

- [ ] **Step 1: Add the auth-base scenario skeleton**

Translate the three v1-client auth cases:

1. unauthenticated `getTask` fails with `A2AClientException` whose message or cause identifies authentication failure;
2. authenticated `getTask` returns the expected v1 `Task`;
3. the public card endpoint is accessible without credentials for HTTP transports.

Use `A2A.getAgentCard(..., Set.of("0.3"))` for the public-card assertion and HTTP auth-client bootstrap. Ensure the card’s projected `HTTPAuthSecurityScheme` is available to the v1 auth interceptor. The old raw `testBasicAuthWorksViaHttp` request is intentionally excluded from this v1-client suite; retain it only in the frozen legacy suite.

- [ ] **Step 2: Compile the auth base before transport subclasses exist**

Run:

```bash
mvn -pl tests/server-common -am test-compile
```

Expected: the v1 auth base compiles independently of any compatibility adapter or Quarkus reference module. Its tests are expected to become runnable only after the concrete subclasses are added in Task 5.

- [ ] **Step 3: Implement v1 auth translation**

Use `AuthInterceptor` with the same scheme-name callback as the v0.3 suite, but configure it through the v1 transport config. Define `getAuthenticatedClient()` and `getUnauthenticatedClient()` as cached accessors over exact `createAuthenticatedClient()` and `createUnauthenticatedClient()` factories, and reset those caches in the shared cleanup. Do not add raw HTTP request tests to this base, and do not import `AuthInterceptor_v0_3`.

- [ ] **Step 4: Commit**

```bash
git add tests/server-common/src/test/java/org/a2aproject/sdk/server/apps/common/AbstractA2AServerCompatibilityWithAuthTest_v0_3.java
git commit -m "test: add v1 compatibility authentication coverage"
```

### Task 5: Add standalone JSON-RPC, REST, and gRPC compatibility subclasses and Maven wiring

**Files:**

- Create sibling compatibility subclasses under `compat-0.3/reference/jsonrpc/src/test/java/org/a2aproject/sdk/compat03/server/apps/quarkus/` for each existing non-task-authorization JSON-RPC client variant: JDK, Android, Vert.x, and authenticated JDK/Android/Vert.x variants.
- Create sibling compatibility subclasses under `compat-0.3/reference/rest/src/test/java/org/a2aproject/sdk/compat03/server/rest/quarkus/` for each existing non-task-authorization REST client variant: JDK, Android, Vert.x, and authenticated JDK/Android/Vert.x variants.
- Create: `compat-0.3/reference/grpc/src/test/java/org/a2aproject/sdk/compat03/server/grpc/quarkus/QuarkusA2AGrpc_v0_3_CompatibilityTest.java`
- Create: `compat-0.3/reference/grpc/src/test/java/org/a2aproject/sdk/compat03/server/grpc/quarkus/QuarkusA2AGrpc_v0_3_WithAuthCompatibilityTest.java`
- Modify: `compat-0.3/reference/jsonrpc/pom.xml`
- Modify: `compat-0.3/reference/rest/pom.xml`
- Modify: `compat-0.3/reference/grpc/pom.xml`

Use the naming pattern `Compatibility` immediately before the transport/client suffix. For example, `QuarkusA2AJSONRPC_v0_3_JdkTest` becomes `QuarkusA2AJSONRPC_v0_3_CompatibilityJdkTest`, and `QuarkusA2AJSONRPC_v0_3_WithAuthVertxTest` becomes `QuarkusA2AJSONRPC_v0_3_WithAuthCompatibilityVertxTest`.

The concrete classes are exact siblings of the current legacy classes: `QuarkusA2AJSONRPC_v0_3_CompatibilityJdkTest`, `...CompatibilityAndroidTest`, `...CompatibilityVertxTest`, `...WithAuthCompatibilityTest`, `...WithAuthCompatibilityAndroidTest`, and `...WithAuthCompatibilityVertxTest`; the same six names with `QuarkusA2ARest_v0_3_` in the REST module; and `QuarkusA2AGrpc_v0_3_CompatibilityTest` plus `QuarkusA2AGrpc_v0_3_WithAuthCompatibilityTest` in the gRPC module. Non-auth classes use `@QuarkusTest`; auth classes use both `@QuarkusTest` and `@TestProfile(AuthTestProfile_v0_3.class)`. Every class calls the same `super(8081)` constructor as its legacy sibling and extends the corresponding new compatibility base.

**Maven dependency matrix:** add test-scoped dependencies while retaining existing legacy dependencies:

| Module | v1 client/native transport | Compatibility adapter |
|---|---|---|
| `compat-0.3/reference/jsonrpc` | `a2a-java-sdk-client`, `a2a-java-sdk-client-transport-jsonrpc`, `a2a-java-sdk-tests-server-common` with `type=test-jar` | `a2a-java-sdk-compat-0.3-client-adapter`, `a2a-java-sdk-compat-0.3-client-adapter-jsonrpc` |
| `compat-0.3/reference/rest` | `a2a-java-sdk-client`, `a2a-java-sdk-client-transport-rest`, `a2a-java-sdk-tests-server-common` with `type=test-jar` | `a2a-java-sdk-compat-0.3-client-adapter`, `a2a-java-sdk-compat-0.3-client-adapter-rest` |
| `compat-0.3/reference/grpc` | `a2a-java-sdk-client`, `a2a-java-sdk-client-transport-grpc`, `a2a-java-sdk-tests-server-common` with `type=test-jar` | `a2a-java-sdk-compat-0.3-client-adapter`, `a2a-java-sdk-compat-0.3-client-adapter-grpc` |

- [ ] **Step 1: Add the general JSON-RPC subclasses**

For each JDK, Android, and Vert.x variant, implement `getTransportProtocol()` and `getTransportUrl()` using the same values as the legacy sibling, configure `JSONRPCTransport`, and use the same HTTP client implementation. The inherited `getAgentCard()` must fetch the standalone v0.3 card and select the adapter through `protocolVersion = "0.3"`. Run the JSON-RPC focused command before proceeding and commit this slice as `test: add v1 compatibility JSON-RPC subclasses`.

- [ ] **Step 2: Add the general REST subclasses**

For each JDK, Android, and Vert.x variant, configure `RestTransport` and the same HTTP client implementation as its legacy sibling. Run the REST focused command before proceeding and commit this slice as `test: add v1 compatibility REST subclasses`.

- [ ] **Step 3: Add authenticated JSON-RPC and REST subclasses**

Copy only the transport setup from each legacy auth sibling. Replace `AuthInterceptor_v0_3` with v1 `AuthInterceptor`, preserve the `basicAuth` scheme callback, and add it to the native v1 transport config. Keep the public-card assertion in the compatibility base; do not copy the legacy raw `testBasicAuthWorksViaHttp` request. Run both focused HTTP module commands before proceeding and commit this slice as `test: add v1 compatibility HTTP auth subclasses`.

- [ ] **Step 4: Add the gRPC general subclass**

Configure `GrpcTransport` with the existing channel factory pattern and return a v1 `AgentCard` containing one `new AgentInterface("GRPC", getTransportUrl(), null, "0.3")`. The fixture must also contain the required v1 card fields plus `securitySchemes` with `basicAuth` as an `HTTPAuthSecurityScheme` (including non-null `bearerFormat`, for example `"none"`) and a matching `securityRequirements` entry for the auth subclass. Do not call HTTP card discovery. Close the channel in `@AfterAll` and let the base close only clients.
Run the gRPC focused command and commit this slice as `test: add v1 compatibility gRPC subclass`.

- [ ] **Step 5: Add the gRPC auth subclass**

Use v1 `AuthInterceptor` in `GrpcTransportConfigBuilder`, preserve separate authenticated and unauthenticated channel factories, and disable the HTTP-only auth tests. Keep channel shutdown behavior identical to the existing legacy gRPC auth test.
Run the gRPC focused command and commit this slice as `test: add v1 compatibility gRPC auth subclass`.

- [ ] **Step 6: Add exact POM dependencies and verify ServiceLoader discovery**

Add the matrix artifacts with `${project.version}` where the compat parent does not already supply dependency management. Do not copy service files into test resources; the adapter JARs’ existing `META-INF/services/org.a2aproject.sdk.client.http.AgentCardCompatibilityParser` and `META-INF/services/org.a2aproject.sdk.client.VersionedClientTransportProvider` must be discovered from the test runtime classpath.
Run each focused command again after the POM changes and commit the exact POM edits as `build: wire v1 compatibility test dependencies`.

- [ ] **Step 7: Run each reference module’s focused compatibility tests**

Run:

```bash
mvn -pl compat-0.3/reference/jsonrpc -am -Dtest='*Compatibility*' -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl compat-0.3/reference/rest -am -Dtest='*Compatibility*' -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl compat-0.3/reference/grpc -am -Dtest='*Compatibility*' -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: the new v1 clients discover/select the v0.3 adapter, complete supported operations, pass authentication tests, and preserve expected local rejection behavior.

- [ ] **Step 8: Run the unchanged legacy reference tests**

Run the same three module commands without the `-Dtest` selector. Expected: existing v0.3 tests still pass, demonstrating that the new v1 test dependencies and subclasses did not alter legacy behavior.

- [ ] **Step 9: Confirm the standalone reference slices**

```bash
git status --short
git diff --check
```

The transport and POM slices are already committed separately; do not create a broad catch-all commit here.

### Task 6: Complete the test matrix

**Files:**

- Modify: `docs/superpowers/specs/2026-09-14-client-v03-compatibility-tests-matrix.md`

- [ ] **Step 1: Compare the matrix against both legacy abstract bases**

Use `rg -n '^    public void test'` on both legacy bases and ensure every client-facing method is represented. Mark direct-wire, task-store, legacy card, and task-authorization exclusions with their existing test location. Mark any scenario that is covered by adapter unit tests rather than end-to-end tests with its exact adapter test class.

- [ ] **Step 2: Document the runnable compatibility test matrix**

Record the three reference module commands, the required adapter artifacts, the standalone legacy-card requirement, the gRPC card-fixture exception, and the fact that the original v0.3 tests remain in place.

- [ ] **Step 3: Commit**

```bash
git add docs/superpowers/specs/2026-09-14-client-v03-compatibility-tests-matrix.md
git commit -m "docs: record v0.3 compatibility test matrix"
```

### Task 7: Run final verification and inspect dependency boundaries

**Files:**

- No production files should change in this task.
- Inspect: `compat-0.3/reference/jsonrpc/pom.xml`, `compat-0.3/reference/rest/pom.xml`, `compat-0.3/reference/grpc/pom.xml`, and the three compatibility test classpaths.

- [ ] **Step 1: Run focused conversion, adapter, and builder tests**

Run:

```bash
mvn -pl compat-0.3/conversion,compat-0.3/client/adapter,client/base -am test
```

Expected: mapper, resolver, adapter, and version-aware builder tests pass.

- [ ] **Step 2: Run the full standalone reference matrix**

Run:

```bash
mvn -pl compat-0.3/reference/jsonrpc,compat-0.3/reference/rest,compat-0.3/reference/grpc -am test
```

Expected: both the new compatibility subclasses and the unchanged legacy subclasses pass.

- [ ] **Step 3: Run packaging/classpath verification from the existing implementation plan**

The original client-compatibility implementation plan owns the isolated fixture/JVM packaging tests. Do not treat the existing multiversion Quarkus suites as proof of those cases. Confirm that the original plan’s dedicated tests exist, then run the exact command specified there. The cases must cover missing parser, missing binding adapter, duplicate provider, parser-not-requested, client-only dependency-tree behavior, and the standard v1-only classpath.

Run the existing multiversion suites only as a supplementary regression check:

```bash
mvn -pl tests/multiversion/jsonrpc,tests/multiversion/rest,tests/multiversion/grpc -am -Dsurefire.failIfNoSpecifiedTests=false test
```

Do not claim this command verifies the isolated parser/provider/dependency-boundary cases; those belong to the original implementation plan’s dedicated tests.

- [ ] **Step 4: Inspect dependency trees**

Run:

```bash
mvn -pl compat-0.3/client/adapter dependency:tree
mvn -pl compat-0.3/client/adapter-jsonrpc dependency:tree
mvn -pl compat-0.3/client/adapter-rest dependency:tree
mvn -pl compat-0.3/client/adapter-grpc dependency:tree
```

Expected: no `a2a-java-sdk-server-common`, CDI, Quarkus, or reference-server artifact appears in client adapter dependency trees.

- [ ] **Step 5: Run the complete reactor when the environment permits**

```bash
mvn clean install
```

If environment constraints prevent the complete reactor, report the exact skipped or failing module and preserve the focused results from Steps 1 and 2.

- [ ] **Step 6: Finish with a verification handoff**

```bash
git status --short
git diff --check
git log -5 --oneline
```

Do not create a merge commit or modify unrelated worktree changes. This final step does not create another commit; the implementation session should hand off the commit list and focused/full test results.
