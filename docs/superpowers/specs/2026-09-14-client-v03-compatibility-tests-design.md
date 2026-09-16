# v1 Client Compatibility Tests Against v0.3 Servers

## Goal

Add end-to-end tests proving that applications using the public v1 `Client` API can communicate with v0.3 servers through the new optional compatibility layer. Keep the existing v0.3 client tests unchanged as regression coverage for the legacy API.

The two suites intentionally overlap. They validate different public client APIs and should remain independently readable. The v0.3 protocol is frozen, so duplicated scenarios are acceptable and may be preferable to a test facade that obscures the behavior under test.

## Test architecture

Create parallel abstract test bases for the compatibility client with distinct names, because the legacy bases already occupy the obvious v0.3 names:

- `AbstractA2AServerCompatibilityTest_v0_3` for unauthenticated and general protocol behavior.
- `AbstractA2AServerCompatibilityWithAuthTest_v0_3` for authenticated and unauthenticated client behavior.

Place these v1-only bases in the existing root `tests/server-common` test-jar, in the `org.a2aproject.sdk.server.apps.common` test-support package. The existing legacy bases remain in the compat-0.3 server-conversion test-jar and are not renamed or modified.

The new bases use only v1 client-facing types:

- `Client`, `ClientBuilder`, `ClientConfig`;
- v1 request, result, event, and `AgentCard` types;
- v1 transport configuration and interceptor types.

They may share small test-only helpers with the existing suites when the helper has an obvious, stable responsibility—for example, constructing equivalent minimal v1 tasks or cleaning up server-side test data. Do not introduce a common client facade merely to remove duplicated test methods. The legacy bases and their subclasses remain unchanged.

Both new bases must close every created v1 `Client` after each test or at test-instance teardown. Transport-specific resources such as caller-owned gRPC channels remain under the concrete subclass's ownership and cleanup.

## Card setup and discovery

HTTP-based compatibility subclasses (JSON-RPC and REST) obtain the card through the new opt-in discovery API:

```java
A2A.getAgentCard(getTransportUrl(), Set.of("0.3"));
```

This validates the complete discovery path: one raw-card fetch, v0.3 parsing, projection into a v1 `AgentCard`, and version-aware transport selection.

These discovery tests target the standalone legacy reference modules `compat-0.3/reference/jsonrpc` and `compat-0.3/reference/rest`. Their v0.3 route serves the legacy card because no non-default v1 `@PublicAgentCard` replaces it. The co-hosted `tests/multiversion/*` modules serve the v1 public card in their normal configuration, so their existing v0.3 subclasses remain legacy regression coverage and are not used to assert legacy-only card discovery.

Before the end-to-end subclasses are enabled, focused mapper tests must prove that a legacy card with only its primary `url` and `preferredTransport` projects one usable v1 interface. The mapper must canonicalize the legacy binding values `jsonrpc`, `http`/`rest`, and `grpc` to `JSONRPC`, `HTTP+JSON`, and `GRPC` respectively, while preserving already canonical values. Authenticated HTTP tests must also use `HTTPAuthSecurityScheme_v0_3` with Basic authentication and prove projection to v1 `HTTPAuthSecurityScheme`. These tests currently expose missing behavior in `AgentCardMapper_v0_3`; the compatibility conversion layer must be fixed before the end-to-end subclasses are enabled.

The gRPC compatibility subclass constructs a v1 `AgentCard` fixture directly with a `0.3` `AgentInterface`. gRPC has no agent-card HTTP endpoint, so this is consistent with the existing gRPC tests while still exercising the compatibility transport adapter. The fixture must contain the fields required for transport selection and authentication.

The test code should not use `Client_v0_3` or v0.3 request/result/event types. Legacy types remain in the existing v0.3 suite and server-side fixtures only.

## Transport subclasses

Add compatibility-client subclasses for JSON-RPC, REST, and gRPC, plus authenticated variants where the existing v0.3 suite has them. Each subclass supplies:

- the binding and endpoint URL;
- the native v1 transport class and configuration;
- transport-specific resource cleanup;
- the v1 `AuthInterceptor` for authenticated cases.

The compatibility adapter is selected by the card interface’s `protocolVersion = "0.3"`; subclasses configure the ordinary v1 transport class and configuration expected by `ClientBuilder`.

Transport-specific exceptions remain explicit. For example, gRPC subclasses can disable HTTP-only card/public-endpoint tests, matching the current test behavior.

The new standalone compatibility subclasses live in the existing `compat-0.3/reference/jsonrpc`, `compat-0.3/reference/rest`, and `compat-0.3/reference/grpc` test modules. Each retains its legacy v0.3 test dependencies and adds the corresponding rows below. The new bases come from the existing root `tests/server-common` test-jar. The adapter artifacts must remain available on the test runtime classpath so their `ServiceLoader` registrations are visible during `ClientBuilder` initialization.

| Test module | Explicit v1/client dependencies | Compatibility dependencies | Card strategy |
|---|---|---|---|
| `compat-0.3/reference/jsonrpc` | `a2a-java-sdk-client`, `a2a-java-sdk-client-transport-jsonrpc`, root `a2a-java-sdk-tests-server-common` test-jar | `a2a-java-sdk-compat-0.3-client-adapter`, `a2a-java-sdk-compat-0.3-client-adapter-jsonrpc` | Fetch standalone v0.3 card through `A2A.getAgentCard(..., Set.of("0.3"))` |
| `compat-0.3/reference/rest` | `a2a-java-sdk-client`, `a2a-java-sdk-client-transport-rest`, root `a2a-java-sdk-tests-server-common` test-jar | `a2a-java-sdk-compat-0.3-client-adapter`, `a2a-java-sdk-compat-0.3-client-adapter-rest` | Fetch standalone v0.3 card through `A2A.getAgentCard(..., Set.of("0.3"))` |
| `compat-0.3/reference/grpc` | `a2a-java-sdk-client`, `a2a-java-sdk-client-transport-grpc`, root `a2a-java-sdk-tests-server-common` test-jar | `a2a-java-sdk-compat-0.3-client-adapter`, `a2a-java-sdk-compat-0.3-client-adapter-grpc` | Construct a v1 card with a `0.3` interface; gRPC has no card endpoint |

All entries are test-scoped where appropriate. The existing legacy client/transport artifacts remain because the old suite continues to compile and run in the same modules.

## Scenario coverage

Translate the client-facing portions of the existing v0.3 abstract scenarios to v1 types and retain their intent, including:

- task retrieval, cancellation, and not-found/error behavior;
- non-streaming and streaming message flows;
- resubscription and event delivery;
- push-notification configuration behavior supported by v0.3;
- unsupported-operation behavior where the compatibility plan requires local rejection;
- authentication success, failure, and public-card behavior.

Assertions should use v1 error and event types, while preserving the v0.3 wire-level expectations. Operations that v0.3 cannot represent—such as `listTasks`, non-empty tenants, extended-card retrieval, or non-default push-config pagination—should have explicit compatibility-client rejection tests rather than being silently omitted.

The new suite does not duplicate tests whose subject is independent of the client API:

- direct malformed-wire, HTTP-method, content-negotiation, and header tests;
- task-store utility sanity tests;
- direct SSE/wire-stream tests;
- the legacy client's `getAgentCard()` test, which is replaced by resolver tests and the HTTP end-to-end bootstrap;
- task-authorization coverage, which remains in the existing dedicated v0.3 and v1 task-authorization suites.

The existing v0.3 suite remains responsible for those cases. A small scenario matrix should be recorded alongside the implementation so every omitted legacy test is intentional.

Create `docs/superpowers/specs/2026-09-14-client-v03-compatibility-tests-matrix.md` during implementation. It must contain one row for every client-facing test or test group in the two legacy abstract bases, identifying the new compatibility test, an intentional exclusion with rationale, or a separate adapter/resolver test. Include explicit rows for push notifications, streaming/resubscription, authentication, transport-specific overrides, and unsupported v0.3 operations.

The one-fetch guarantee is tested in `A2ACardResolver`/compatibility-parser unit tests with a counting HTTP client. End-to-end tests only need to prove that the resulting projected card can build and use the v1 client.

## Verification

Run focused Maven tests for the new compatibility subclasses in each binding, then run the existing v0.3 subclasses to confirm the legacy suite remains unchanged. At minimum, verify:

1. Focused card-mapper tests cover primary URL fallback and HTTP authentication projection.
2. HTTP card discovery selects a v0.3 interface; resolver tests prove only one card fetch.
3. Each adapter maps ordinary v1 requests and responses successfully.
4. Streaming and resubscription events arrive as v1 events.
5. v1 authentication interceptors work through the adapters.
6. Expected unsupported operations fail locally.
7. Existing v0.3 client tests continue to pass.

The plan's standalone classpath and packaging tests remain separate Task 7 coverage. They must still verify absent parser/adapter behavior, duplicate provider handling, client-only dependency boundaries, and the standard v1-only classpath; this end-to-end suite does not replace them.

No production API changes are part of this test addition unless a test exposes a genuine compatibility-layer defect.
