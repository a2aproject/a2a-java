# Security Policy and Threat Model

## Reporting of CVEs and Security Issues

### The A2A Java SDK community takes security bugs very seriously

We aim to take immediate action to address serious security-related problems that involve our project.

Note that we will only fix such issues in the most recent minor release of the A2A Java SDK.

### Reporting of Security Issues

When reporting a security vulnerability it is important to not accidentally broadcast to the world that
the issue exists, as this makes it easier for people to exploit it. The software industry uses the term
<a href="https://www.redhat.com/en/blog/security-embargoes-red-hat">embargo</a> to describe the time a
security issue is known internally until it is public knowledge.

Our preferred way of reporting security issues is listed below.

#### Email the A2A Java SDK team

To report a security issue, please email
<a href="mailto:kkhan@redhat.com">kkhan@redhat.com</a>, <a href="mailto:ehugonne@redhat.com">ehugonne@redhat.com</a>,
<a href="mailto:jmesnil@redhat.com">jmesnil@redhat.com</a> and/or <a href="mailto:smaestri@redhat.com">smaestri@redhat.com</a>.
A member of the team will open the required issues.

#### Other considerations

If you would like to work with us on a fix for the security vulnerability, please include your GitHub username
in the above email, and we will provide you access to a temporary private fork where we can collaborate on a
fix without it being disclosed publicly, **including in your own publicly visible git repository**.

Do not open a public issue, send a pull request, or disclose any information about the suspected vulnerability
publicly, **including in your own publicly visible git repository**. If you discover any publicly disclosed
security vulnerabilities, please notify us immediately through the emails listed in the section above.

Findings that fall under [§8](#8-security-properties-the-project-provides) of the threat model below should be
reported through the channel above. Findings that fall under [§3](#3-out-of-scope-explicit-non-goals) or
[§9](#9-security-properties-the-project-does-not-provide) will be closed citing this document (see
[§13](#13-triage-dispositions)).

---

## Threat Model

### §1 Header

**Provenance legend:** *(documented)* — stated in the project's own docs/Javadoc/code comments, cited inline.
*(maintainer)* — confirmed directly by a maintainer. *(anticipated)* — a forward-looking risk hypothesis (§8a
only), not a fact about the code to be confirmed or denied.

**Confidence:** ~12 documented / ~15 maintainer-confirmed / ~7 anticipated (§8a). All non-anticipated claims are
either directly documented in the project's own sources or maintainer-confirmed as of this revision — see
[§14](#14-resolved-questions) for the record of what was reviewed and when.

**Version binding:** This threat model is written against `a2a-java` `1.3.0.Final`. A report against a released
version *N* should be triaged against the model as it stood at *N*'s release, not necessarily at `HEAD`. The four
advisories published for `1.3.0.Final` —
[GHSA-qw47-mcm5-934w](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-qw47-mcm5-934w),
[GHSA-q78c-5jjq-57g8](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-q78c-5jjq-57g8),
[GHSA-9rhm-2h4x-jwmx](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-9rhm-2h4x-jwmx),
[GHSA-x32g-jvvm-4725](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-x32g-jvvm-4725) — have
been incorporated; the sections they affected are annotated with the advisory identifier.

**Status:** Maintainer-reviewed.

The A2A Java SDK is a multi-module Java library implementing the client and server sides of the
[Agent2Agent (A2A) Protocol](https://a2a-protocol.org/) over JSON-RPC, gRPC, and HTTP+JSON (REST). It is
embedded into agentic Java applications: as a client library to talk to remote A2A agents, and/or as a
server-side library (with Quarkus-based reference implementations) that turns a user-provided `AgentExecutor`
into a protocol-conformant A2A server *(documented, README.md)*.

### §2 Scope and intended use

**Primary intended use cases** *(documented, README.md, AGENTS.md)*:
- As a **client**: discover and call remote A2A agents over JSON-RPC, gRPC, or REST.
- As a **server**: expose a user-supplied `AgentExecutor` (the agent's business logic) as an A2A-conformant
  server, using the reference Quarkus modules or a custom Jakarta EE integration.

**Deployment contexts** *(maintainer)*: the SDK is an **embedded library**, not a standalone daemon —
it is always linked into a host application (a Quarkus service, a Jakarta EE web application, an Android app for
the client side, etc.). The `reference/*` modules produce a runnable Quarkus server, but that server is still an
application the operator builds and deploys, not a product shipped by this project.

**Caller expectations / roles** *(maintainer)*: because the server side accepts
network requests, there is no single "caller" —

| Role | Trust level | What this role can do |
| --- | --- | --- |
| Server operator/deployer | Trusted | Builds and configures the server — wires `AgentExecutor`, `TaskStore`, `TaskAuthorizationProvider`, transport modules, and the runtime's authentication; decides what is exposed on the network. |
| `AgentExecutor` author (agent business logic) | Trusted (often the same party as the operator) | Interprets `Message`/`Part` content however they choose — feed it to an LLM, build a query, shell out, etc. The SDK does not sandbox this; code executed here is by design, not a vulnerability in the SDK (§3). |
| Remote client (untrusted) | Untrusted | Sends JSON-RPC/gRPC/REST requests to a deployed A2A server. This is the primary adversary for server usage (§7). No trust is assumed beyond what the deployment's authentication layer establishes. |
| Remote agent peer (semi-trusted, client-side) | Semi-trusted | When this SDK is used as a client, the remote A2A agent it talks to. Its responses are treated as data by the client library, never executed as code. |

**Component-family table:**

| Family | Representative entry point | Touches outside process? | In scope? |
| --- | --- | --- | --- |
| `spec/`, `spec-grpc/` | Protocol record types (`Task`, `AgentCard`, `SecurityScheme`, …) | No | Yes |
| `client/base`, `client/transport/*` | `Client` / `ClientTransport` SPI implementations | Network (outbound) | Yes |
| `http-client/`, `extras/http-client-vertx`, `extras/http-client-android` | `A2AHttpClient` implementations | Network (outbound) | Yes |
| `server-common/` | `DefaultRequestHandler`, `TaskStore`, `QueueManager`, `TaskAuthorizationProvider` SPI | In-memory by default; network if push notifications enabled | Yes |
| `transport/{jsonrpc,grpc,rest}` | Server-side transport handlers (`JSONRPCHandler`, `GrpcHandler`, …) | Network (inbound) | Yes |
| `reference/*` | Quarkus reference server wiring, `VertxSecurityHelper` | Network (inbound), CDI/security integration | Yes |
| `compat-0.3/*` | v0.3 protocol compatibility layer (conversion, transports, clients) | Same as above, for v0.3 wire format | Yes |
| `extras/task-store-database-jpa`, `extras/push-notification-config-store-database-jpa` | JPA-backed stores | Database | Yes (opt-in) |
| `extras/queue-manager-replicated` | Kafka/broker-backed event replication | Message broker | Yes (opt-in) |
| `extras/opentelemetry` | Tracing decorators/wrappers | Exports spans to a collector | Yes (opt-in) |
| `extras/multitenancy` | CDI routers for per-tenant `AgentCard`/`AgentExecutor` | No | Yes (opt-in) |
| `examples/` | Sample applications | N/A | **No** — see §3 |
| `tck/`, `tests/`, `test-utils-docker/` | Conformance and integration test suites | N/A | **No** — see §3 |

### §3 Out of scope (explicit non-goals)

- **`examples/`** (helloworld, cloud-deployment, stream-lifecycle, etc.) — separately authored demonstration
  code. Findings here are threat-modeled separately, if at all, and do not extend to the core SDK.
- **`tck/`, `tests/`, `test-utils-docker/`** — conformance and integration test infrastructure, not shipped as
  a runtime dependency of applications.
- **Authentication of inbound requests.** The SDK provides *hooks* (`ServerCallContext`, `User`,
  `AuthenticatedUser`, `UnauthenticatedUser`) for a runtime to attach identity information to a request, but it
  does not itself verify credentials, terminate TLS, or validate bearer tokens/API keys/mTLS certificates. This
  is delegated to the host runtime (e.g., Quarkus HTTP security, a custom `CallContextFactory`) — *(maintainer —
  confirmed against `reference/common/.../VertxSecurityHelper.java`, which delegates to Quarkus's
  `HttpAuthenticator`/`CurrentIdentityAssociation`, and the absence of any credential-verification code in
  `server-common`)*. Reason: not a security boundary this library controls — the transport/runtime layer, which
  varies per deployment, owns this.
- **Transport-layer encryption (TLS/mTLS).** Enabling and configuring TLS for JSON-RPC/REST (HTTP) and gRPC
  endpoints is a deployment concern (reverse proxy, Quarkus HTTP TLS config, gRPC channel credentials), not
  something this SDK configures or enforces *(maintainer — confirmed no TLS-related code exists in `transport/*`
  or `reference/*`)*.
- **Declarative `SecurityScheme` enforcement.** `AgentCard.securitySchemes()` and the `SecurityScheme` family
  (`APIKeySecurityScheme`, `HTTPAuthSecurityScheme`, `OAuth2SecurityScheme`, `OpenIdConnectSecurityScheme`,
  `MutualTLSSecurityScheme`, in `spec/`) are OpenAPI-style **metadata records** describing what a server claims
  to require of callers. The SDK does not implement or enforce any of these schemes — see §9. Reason: this is
  intentionally descriptive metadata per the A2A protocol spec, not an SDK-level access-control mechanism.
- **Broker/database security for opt-in extras.** `extras/queue-manager-replicated` (Kafka/Pulsar/AMQP) and the
  JPA-backed stores rely entirely on the security posture of the external broker/database the operator
  configures (ACLs, encryption in transit, credentials). Reason: out of layer — these are third-party systems
  configured by the operator, not owned by this SDK.
- **Prompt-injection / semantic misuse of message content by an `AgentExecutor`.** How a user-supplied agent
  implementation interprets untrusted `Message`/`Part` content (e.g., feeding it to an LLM, building a shell
  command, or a SQL query) is entirely up to that implementation. Reason: the SDK is a transport/orchestration
  layer; it has no visibility into or control over agent business logic.

### §4 Trust boundaries and data flow

For **server usage**, the trust boundary is the transport API surface: JSON-RPC/REST HTTP endpoints and gRPC
service methods exposed by `transport/{jsonrpc,grpc,rest}` and `reference/*`. Everything arriving at those
entry points is untrusted network input until (a) the host runtime's authentication mechanism populates
`ServerCallContext` with a `User`, and (b) an operator-supplied `TaskAuthorizationProvider` (if configured)
authorizes the specific operation *(maintainer — confirmed against `AuthorizationRequestHandlerDecorator` and
`ServerCallContext`)*.

Once a request is admitted, its `Message`/`Task`/`Part` payload flows: transport handler → `DefaultRequestHandler`
→ `AgentExecutor.execute()` (user code) → `AgentEmitter` → `EventQueue`/`MainEventBus` → `TaskStore` (persistence)
→ back to the client (blocking response or SSE/gRPC stream) → optionally to a push-notification webhook via
`PushNotificationSender` *(documented, `.claude/architecture/EVENTQUEUE.md`)*. The SDK does not interpret or
execute message content at any point in this pipeline — it is opaque data until it reaches the user's
`AgentExecutor`.

Authorization enforcement (when a `TaskAuthorizationProvider` is configured) is applied in
`DefaultRequestHandler` as of 1.3.0.Final — previously it was applied only via the CDI decorator
`AuthorizationRequestHandlerDecorator` *(see [GHSA-qw47-mcm5-934w](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-qw47-mcm5-934w))*.

For **client usage**, remote agent responses (task state, artifacts, streamed events) are deserialized into the
`spec/` record types and returned to the calling application as data; they are never executed as code by the
client library.

**Reachability preconditions per component:**
- A finding in `server-common`'s request-handling path (`DefaultRequestHandler`, `EventQueue`, `TaskStore`) is
  in-model only if reachable from an inbound transport request (JSON-RPC/gRPC/REST) or from
  `AgentExecutor`-enqueued events.
- A finding in `BasePushNotificationSender` is in-model only if reachable from a `TaskPushNotificationConfig`
  that a caller was able to register (i.e., via `onCreateTaskPushNotificationConfig`).
- A finding in a `client/transport/*` module is in-model only if reachable from data returned by a remote agent
  the application chose to call, or from application-supplied request parameters.
- A finding in `extras/*` is in-model only if that extras module is present on the deployment's classpath (they
  are opt-in, drop-in replacements for in-memory defaults).

### §5 Assumptions about the environment

- **Runtime:** Java 17+ *(documented, AGENTS.md)*. Reference server implementations target Quarkus; the SDK core
  (`server-common`) uses `jakarta.inject`/CDI annotations but most classes also expose plain constructors for
  non-CDI runtimes (Spring, etc.) *(documented, e.g. `InMemoryTaskStore`, `AuthorizationRequestHandlerDecorator`
  Javadoc: "This constructor supports programmatic wiring in non-CDI runtimes").*
- **Concurrency:** core mutable state (`InMemoryTaskStore`, `InMemoryPushNotificationConfigStore`,
  `EventQueue`, `MainEventBus`) is documented as thread-safe, backed by `ConcurrentHashMap`,
  `LinkedBlockingDeque`, and `Semaphore` *(documented, class-level Javadoc across `server-common/.../tasks` and
  `.../events`)*. SPI implementors (`TaskAuthorizationProvider`, `PushNotificationSender`, custom `TaskStore`)
  are explicitly required to be thread-safe by their interface contracts *(documented, Javadoc)*.
- **Time/clock:** task ordering/pagination in `InMemoryTaskStore.list()` relies on `Instant` timestamps and
  assumes a monotonically-reasonable system clock; no protection against clock skew is implemented *(maintainer)*.
- **Filesystem:** the SDK core does not read or write application files; `InMemoryTaskStore` and
  `InMemoryPushNotificationConfigStore` are pure in-memory (data lost on restart, by design) *(documented,
  `InMemoryTaskStore` Javadoc)*. The JPA extras write to whatever relational database the operator configures.
- **Network (outbound):** the SDK opens outbound HTTP connections for (1) push-notification webhook delivery
  (`BasePushNotificationSender`), (2) client transports contacting remote agents, and (3) agent-card discovery
  (`A2ACardResolver`). No other outbound network activity occurs from `server-common`/`http-client` *(maintainer)*.
- **Network (inbound):** only the `transport/*` and `reference/*` / `compat-0.3/transport/*` modules bind and
  handle inbound network requests; `server-common` itself performs no socket binding *(maintainer)*.
- **What the project does *not* do to its host** *(maintainer — verified by code search)*: no spawning of child
  processes; no installation of signal handlers; the only reads of environment / system state are (a) a handful of
  `Boolean.getBoolean()` flags for transport-validation skipping (`AgentCardValidator.SKIP_PROPERTY` and
  per-transport variants), OpenTelemetry request/response extraction toggles
  (`org.a2aproject.sdk.server.extract.request`/`.response`), and a non-security-relevant
  `EventConsumer.bufferFlushDelayMs` timing knob (`a2a.eventconsumer.bufferFlushDelayMs`), and (b) standard
  MicroProfile/CDI configuration lookups in the reference modules. The SDK does not mutate global JVM state
  (locale, default `Locale`/`TimeZone`, JVM-wide `Authenticator`).

### §5a Build-time and configuration variants

| Knob | Default | Effect on the model | Maintainer stance |
| --- | --- | --- | --- |
| Absence of a `TaskAuthorizationProvider` bean | **Fail-closed** (as of 1.3.0.Final; was fail-open in ≤1.2.x — [GHSA-qw47-mcm5-934w](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-qw47-mcm5-934w)) | All task operations are **denied** by default when no provider is configured; set `a2a.authorization.required=false` to restore the open behavior for development/single-user deployments | *(maintainer)* Fail-closed chosen as the safer default for production deployments |
| `a2a.authorization.required` | `true` | When `false`, restores the pre-1.3.0 fail-open behavior: all task operations are permitted regardless of whether a `TaskAuthorizationProvider` is registered | *(maintainer)* Intended only for development, testing, or fully-trusted single-user deployments |
| `a2a.push-notifications.enabled` | `true` | When `false`, the server rejects all push-notification configuration requests (`onCreateTaskPushNotificationConfig`), preventing SSRF via webhook registration ([GHSA-q78c-5jjq-57g8](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-q78c-5jjq-57g8)) | *(maintainer)* Set to `false` to disable push notifications entirely in deployments that don't need them |
| `org.a2aproject.sdk.transport.skipValidation` (and `.jsonrpc./.grpc./.rest.skipValidation`) | `false` | Skips `AgentCardValidator` consistency checks between declared and available transports; a misconfiguration/availability issue, not directly an authz/authn bypass | *(maintainer)* debugging/testing convenience; not security-relevant beyond hiding a misconfiguration warning |
| `org.a2aproject.sdk.server.extract.request` / `.extract.response` (OpenTelemetry extras) | `false` | When `true`, full request/response payloads (potentially including sensitive message content) are attached as span attributes *(documented, `extras/opentelemetry/README.md`: "Extracting request/response data may expose sensitive information in traces. Use with caution in production environments.")* | *(documented)* explicitly discouraged in production by the module's own README |
| `a2a.request-context.populate-referred-tasks` | `true` | When enabled, referenced task IDs are resolved via `TaskStore` and pushed through the same `TaskAuthorizationProvider.checkRead` gate; disabling it skips that lookup/check for referenced tasks | *(maintainer)* |

No `ZLIB`-style compile-time defines exist in this project; the variants above are all runtime configuration
properties (MicroProfile Config / system properties), resolved through `A2AConfigProvider` and
`META-INF/a2a-defaults.properties`.

### §6 Assumptions about inputs

Per-parameter trust table for the server-side transport surface (route/message name rather than a single
function, per the network-service convention):

| Route / Message | Parameter | Attacker-controllable? | Caller/operator must enforce |
| --- | --- | --- | --- |
| `message/send`, `message/stream` (all transports) | `Message` (parts, metadata, `taskId`, `contextId`) | **Yes** | Authentication (runtime) + `TaskAuthorizationProvider.checkCreate`/`checkWrite` (if configured); `AgentExecutor` must treat content as untrusted |
| `tasks/get`, `tasks/list`, `tasks/cancel`, `tasks/subscribe` | `taskId` / query params | **Yes** | `TaskAuthorizationProvider.checkRead`/`checkWrite` (if configured) — otherwise any caller can read/cancel any task |
| `tasks/pushNotificationConfig/*` (create/get/list/delete) | `TaskPushNotificationConfig` (`url`, `token`, `authentication.scheme`/`.credentials`) | **Yes** | SDK rejects CR/LF in `token`/`scheme`/`credentials` (header-injection defense, see §8); operator must still validate/restrict destination `url` (SSRF, see §9) |
| `.well-known/agent-card.json` / `GetExtendedAgentCard` | none (read-only) | No (server-controlled response) | N/A |
| gRPC/JSON-RPC/REST framing itself | request size, header count, etc. | Yes, but bounded by the underlying HTTP/gRPC library (Vert.x, gRPC-Java), not by this SDK | Operator should configure runtime-level request-size/timeout limits |
| A2A `A2A-Version` header/query param | version string | Yes | Validated by `A2AVersionValidator` / `VersionRouter`; unsupported values are rejected with `VersionNotSupportedError` |

Client-side, the equivalent table has `AgentCard` fields and remote-agent responses as the attacker-relevant
inputs (an untrusted remote agent could return a malformed or hostile `AgentCard`/`Task`); the client library
deserializes them into typed records and does not execute their content.

Size/shape/rate assumptions: individual `EventQueue` instances are bounded at 1000 in-flight events
(`EventQueue.DEFAULT_QUEUE_SIZE`) and push-notification configs are capped at 100 per task by default
(`a2a.push-notification-config.max-per-task`) — see §8. No SDK-level limit exists on the *number* of concurrent
tasks/queues a server will create in response to inbound requests *(maintainer, see §9.6)*.

### §7 Adversary model

- **Primary adversary (server side):** an untrusted remote network client sending arbitrary, potentially
  malformed or adversarial JSON-RPC/gRPC/REST requests to a deployed A2A server. Assumed capable of sending any
  bytes the transport will parse, including malicious `Message` content, arbitrary `TaskPushNotificationConfig`
  webhook URLs, and requests for tasks it does not own. Assumed *not* capable of bypassing the host runtime's
  network-level controls (firewalling, mTLS at the edge) unless those are absent.
- **Secondary adversary (client side):** a remote A2A agent returning adversarial responses to a client using
  this SDK. Assumed capable of returning malformed/oversized payloads or misleading task state; not assumed
  capable of achieving code execution in the client process via response content alone (the SDK does not
  `eval`/deserialize-and-instantiate arbitrary types from wire data outside the fixed `spec/` record set)
  *(maintainer)*.
- **Explicitly out of scope:** an attacker who already has arbitrary code execution in the JVM process hosting
  the SDK (they have already won); an attacker with direct access to the `TaskStore` database, the
  `PushNotificationConfigStore` database, or the Kafka/broker topic used by `queue-manager-replicated` (broker
  and database access control are the operator's responsibility per §3); the server operator/deployer
  themselves (trusted by definition — they configure `AgentExecutor`, `TaskAuthorizationProvider`, and the
  runtime's authentication).
- This is not a distributed-consensus system, so no Byzantine-participant threshold applies. The
  `queue-manager-replicated` extra does have multiple cooperating server instances exchanging events over a
  broker, but it assumes all instances and the broker are within the operator's trust domain — a
  Byzantine/compromised broker participant is out of scope (§3).

### §8 Security properties the project provides

**CVSS disclaimer:** the indicative CVSS ranges below reflect how a violation of the *stated* property would
plausibly be scored, following the shape of similar classes in comparable projects (e.g. Apache Camel's
[security model](https://camel.apache.org/manual/security-model.html)); they are not commitments and actual
scores depend on the specific violation and deployment context.

1. **HTTP header-injection (CRLF) protection for push-notification headers.** `BasePushNotificationSender`
   rejects `token`, `Authorization` scheme, and credentials values containing CR/LF before building the outbound
   webhook request, citing CWE-113 *(documented, code comments in `BasePushNotificationSender.rejectCrlf`/
   `buildAuthorizationHeader`)*.
   - *Violation symptom:* a crafted push-notification config value causing header/response splitting on the
     outbound webhook call.
   - *Severity:* security-critical (would warrant a CVE if bypassed). Indicative CVSS 6.5–8.6 (CWE-113), depending
     on what the split response lets an attacker do to the receiving webhook endpoint.
2. **Non-disclosure of task existence to unauthorized callers.** When a `TaskAuthorizationProvider` is
   configured and denies access, `AuthorizationRequestHandlerDecorator` throws the same `TaskNotFoundError` used
   for a genuinely missing task, so a caller cannot distinguish "does not exist" from "not authorized"
   *(documented, `TaskAuthorizationProvider` Javadoc: "the caller cannot distinguish 'does not exist' from 'not
   authorized', preventing information leakage")*.
   - *Condition:* only holds when a `TaskAuthorizationProvider` is actually configured (see §9.2 for the
     fail-open default).
   - *Violation symptom:* a distinguishable error/response for "exists but forbidden" vs. "does not exist".
   - *Severity:* security-critical for multi-tenant deployments that rely on it; not applicable otherwise.
     Indicative CVSS 4.3–6.5 (information disclosure of task existence), not the higher-severity authorization
     bypass itself, which is disclaimed outright in §9.2.
3. **Bounded per-task queue and push-notification-config memory footprint.** `EventQueue` enforces a semaphore
   bound (default 1000 in-flight events) per task, and `PushNotificationConfigStore` implementations enforce a
   configurable per-task cap (default 100 configs), rejecting excess registrations with `InvalidParamsError`
   *(documented, `EventQueue.DEFAULT_QUEUE_SIZE`; `PushNotificationConfigStore` Javadoc and
   `a2a-defaults.properties`)*.
   - *Violation symptom:* unbounded memory growth for a single task's queue or push-notification configs.
   - *Severity:* availability/correctness (resource exhaustion for a single task), not confidentiality/integrity.
   - *Threshold:* explicit and documented (1000 events; 100 configs by default, operator-configurable) —
     exceeding it is rejected, not silently allowed.
4. **Thread-safe concurrent access to default in-memory stores and queues.** *(documented, class-level Javadoc
   across `InMemoryTaskStore`, `InMemoryPushNotificationConfigStore`, `EventQueue`, `MainEventBus`)*.
   - *Violation symptom:* data races/corruption under concurrent access.
   - *Severity:* correctness-only, unless a race enables an authorization bypass (see the TOCTOU caveat in §9.8,
     which is explicitly *not* covered for custom SPI implementations).
5. **Protocol version compatibility enforcement.** `A2AVersionValidator`/`VersionRouter` reject client requests
   whose major protocol version does not match any of the server's supported interfaces, returning
   `VersionNotSupportedError` *(documented, code + Javadoc)*.
   - *Violation symptom:* a client on an incompatible major version being silently processed instead of
     rejected.
   - *Severity:* correctness-only.
6. **Fail-closed authorization default.** *(Added in 1.3.0.Final — [GHSA-qw47-mcm5-934w](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-qw47-mcm5-934w))* When no
   `TaskAuthorizationProvider` is configured, `DefaultRequestHandler` denies all task operations by default.
   Authorization enforcement is now applied on both the CDI and builder paths directly in
   `DefaultRequestHandler`, eliminating the earlier gap where the builder path bypassed
   `AuthorizationRequestHandlerDecorator`. Deployments can opt back to open behavior with
   `a2a.authorization.required=false` (intended only for development or fully-trusted single-user deployments).
   - *Violation symptom:* an absent or misconfigured `TaskAuthorizationProvider` silently permitting operations
     for callers it should deny.
   - *Severity:* security-critical for multi-tenant deployments. Indicative CVSS 7.5 (CWE-862 + CWE-1188).
7. **SSRF protection for push-notification webhooks.** *(Added in 1.3.0.Final — [GHSA-q78c-5jjq-57g8](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-q78c-5jjq-57g8))* Push
   notification URLs are validated against an SSRF-safe policy before delivery: only `http`/`https` schemes are
   accepted, and private-network destinations (RFC 1918, loopback, link-local including IPv4-mapped IPv6 such as
   `169.254.169.254`) are blocked. HTTP redirect following is disabled on push-notification POST requests to
   prevent bypass via redirect chains. Push notification config storage can be disabled entirely with
   `a2a.push-notifications.enabled=false`.
   - *Violation symptom:* an attacker-supplied `TaskPushNotificationConfig.url()` causing the server to issue
     requests to internal or metadata endpoints.
   - *Severity:* security-critical for internet-reachable deployments. Indicative CVSS 5.8 (CWE-918).
8. **API key header allowlist and redirect disable.** *(Added in 1.3.0.Final — [GHSA-9rhm-2h4x-jwmx](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-9rhm-2h4x-jwmx))* The
   client-side `AuthInterceptor` validates that `APIKeySecurityScheme.name()` (the header name carrying API key
   credentials) matches a safe allowlist before placing credentials in a request header; invalid names are
   rejected. Automatic HTTP redirect following is disabled in all HTTP client implementations (JDK, Vert.x,
   Android), preventing credential leakage to attacker-controlled redirect destinations.
   - *Violation symptom:* a malicious agent's AgentCard naming an arbitrary header causes the client to forward
     API key credentials to an attacker-controlled server via redirect.
   - *Severity:* moderate. Indicative CVSS moderate range (CWE-522).
9. **Push-notification credential log suppression.** *(Added in 1.3.0.Final — [GHSA-x32g-jvvm-4725](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-x32g-jvvm-4725))* The REST
   transport handler no longer logs the raw request body on parse failure. Previously, a protobuf parse error in
   `parseRequestBody` caused the full JSON body — which may contain push-notification `token` and
   `authentication.credentials` fields — to be written to server logs at `SEVERE` level. The fix restricts error
   logging to metadata only (body length and content type).
   - *Violation symptom:* push-notification bearer tokens or credentials appearing in server logs after a
     malformed request.
   - *Severity:* moderate (CWE-532).
10. **Authorization check for `listTasks` operation.** *(Added in 1.3.0.Final)* The `onListTasks` handler now
    performs a list-scoped read authorization check before delegating to the task store, consistent with the
    checks applied to all other task-reading operations.
    - *Violation symptom:* a caller denied read access on individual tasks still being able to enumerate all
      tasks via `listTasks`.
    - *Severity:* moderate information disclosure.

### §8a Anticipated vulnerability classes

Four advisories were published for 1.3.0.Final, converting the first two anticipated classes below into
confirmed historical cases. The remaining classes are still *anticipated* — attack shapes that the adversary
model (§7) and the code paths reviewed in this document make plausible.

- **Authorization bypass via a missing or misconfigured `TaskAuthorizationProvider`.** *(confirmed and fixed)*
  Disclosed as [GHSA-qw47-mcm5-934w](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-qw47-mcm5-934w)
  (High, CVSS 7.5). The fail-open default in ≤1.2.x allowed any caller to read, cancel, or modify any task
  without authentication. Fixed in 1.3.0.Final by switching to a fail-closed default (§8.6) and moving
  enforcement into `DefaultRequestHandler` so both CDI and builder paths are covered.
- **SSRF via push-notification webhook registration.** *(confirmed and fixed)* Disclosed as
  [GHSA-q78c-5jjq-57g8](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-q78c-5jjq-57g8)
  (Moderate, CVSS 5.8). An unauthenticated caller could register a webhook pointing at internal or metadata
  endpoints; the server would POST task data to them. Fixed in 1.3.0.Final with SSRF-safe URL validation and
  redirect-following disabled (§8.7).
- **Client credential leakage via attacker-controlled redirect.** *(confirmed and fixed)* Disclosed as
  [GHSA-9rhm-2h4x-jwmx](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-9rhm-2h4x-jwmx)
  (Moderate). A malicious agent could name an arbitrary HTTP header in its AgentCard's `APIKeySecurityScheme`,
  causing the client to forward API key credentials to an attacker-controlled server via redirect. Fixed in
  1.3.0.Final with an allowlist for header names and redirect-following disabled (§8.8).
- **Credential disclosure via error-path logging.** *(confirmed and fixed)* Disclosed as
  [GHSA-x32g-jvvm-4725](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-x32g-jvvm-4725)
  (Moderate, CWE-532). Push-notification bearer tokens were written to server logs in cleartext on parse failure.
  Fixed in 1.3.0.Final by restricting error logging to metadata only (§8.9).
- **HTTP header/response-splitting bypass in push-notification delivery.** Would require finding a gap in the
  CRLF rejection described in §8.1 — the class §8.1 exists specifically to close. *(anticipated)*
- **Resource exhaustion via unbounded task/queue creation.** §8.3 bounds a single task's queue and
  push-notification-config count, but nothing bounds the number of tasks or queues an unauthenticated (or
  authenticated but unthrottled) caller can create (§9.6). *(anticipated)*
- **Information disclosure via a custom `TaskStore`/`QueueManager`/`TaskAuthorizationProvider` implementation**
  that logs or exposes `Message`/`Task` content more broadly than the in-memory defaults do. *(anticipated)*

A report matching one of these classes is triaged the same way as any other (§13); this list does not itself
grant or withhold in-scope status.

### §9 Security properties the project does *not* provide

1. **No authentication.** The SDK does not verify credentials, tokens, or certificates. `ServerCallContext`,
   `User`, `AuthenticatedUser`, and `UnauthenticatedUser` are passive carriers for whatever identity the host
   runtime chooses to attach — *(maintainer — confirmed: `AuthenticatedUser.isAuthenticated()` is a hardcoded
   `return true` with no verification logic; see §3)*. **False-friend:** `AuthenticatedUser.isAuthenticated()`
   returning `true` reflects only that *some* upstream code populated the context that way; it is not itself a
   verification step.
2. **No authorization by default (≤1.2.x only — fixed in 1.3.0.Final).** In versions up to and including
   1.2.0.Final, absent a `TaskAuthorizationProvider` bean, every `RequestHandler` operation was permitted for
   every caller regardless of identity ([GHSA-qw47-mcm5-934w](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-qw47-mcm5-934w)). As of 1.3.0.Final the default is
   **fail-closed**: operations are denied when no provider is configured. Set `a2a.authorization.required=false`
   to restore the open behavior for development or fully-trusted single-user deployments. See §8.6.
3. **No SSRF protection for push-notification webhooks (≤1.2.x only — fixed in 1.3.0.Final).** In versions up
   to and including 1.2.0.Final, `BasePushNotificationSender` sent an HTTP POST to whatever URL a caller
   supplied in `TaskPushNotificationConfig`, with no destination validation ([GHSA-q78c-5jjq-57g8](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-q78c-5jjq-57g8)). As of 1.3.0.Final,
   push-notification URLs are validated against an SSRF-safe policy and redirect following is disabled. See §8.7.
4. **No transport-layer encryption.** TLS/mTLS for any of the three transports is entirely a deployment concern;
   the SDK neither defaults to nor enforces encrypted transport (§3).
5. **`SecurityScheme` declarations are not enforcement.** **False-friend:** an `AgentCard` declaring
   `OAuth2SecurityScheme`/`APIKeySecurityScheme`/etc. describes what the agent *claims* to require; it does not
   cause the SDK to validate any such credential. Enforcement, if any, must be implemented by the deployment
   (e.g., via the runtime's security layer feeding `ServerCallContext`).
6. **No rate limiting or global resource-exhaustion protection.** The bounded-queue guarantee in §8.3 bounds a
   single task's memory footprint, not the number of tasks, queues, requests, or push-notification dispatches a
   server will create or perform. A flood of new `message/send` calls creating unbounded tasks/queues, or a
   push-notification config pointing at a slow/many endpoints, is not throttled by the SDK. Global request-rate
   and connection limits are a deployment concern (reverse proxy, Quarkus rate-limiter, runtime thread-pool
   sizing). This is a permanent design position: acceptable destination counts and rates are
   deployment-specific and vary too widely for a one-size-fits-all SDK-level cap.
7. **No sanitization of message content.** `Message`/`Part` payloads are passed through to `AgentExecutor`
   unmodified. Prompt-injection against an LLM-backed agent, or unsafe downstream use of message content (shell
   commands, SQL, file paths) inside a custom `AgentExecutor`, is outside the SDK's control — a well-known attack
   class for this category of project (agent/LLM-adjacent frameworks) that is explicitly left to the caller.
8. **No atomicity guarantee for custom `TaskAuthorizationProvider.recordOwnership` implementations.** The
   `isTaskRecorded()` → `recordOwnership()` sequence is documented as non-atomic; the SDK relies on the
   implementor to use `putIfAbsent`/`INSERT ... ON CONFLICT DO NOTHING`-style atomic inserts. A naive
   implementation is vulnerable to a first-writer race *(documented, `TaskAuthorizationProvider` Javadoc,
   "Common pitfalls")*.
9. **Broker/database security for opt-in extras is not provided.** `extras/queue-manager-replicated` and the
   JPA-backed stores inherit whatever security posture the operator gives the underlying Kafka/Pulsar/AMQP
   broker or relational database (§3).
10. **No protection against clock skew** in timestamp-ordered task listing/pagination (§5) — an operator running
    server instances with badly skewed clocks may see inconsistent pagination ordering; this is a correctness,
    not confidentiality/integrity, concern.

### §10 Downstream responsibilities

- **Configure authentication** at the runtime/transport layer (Quarkus HTTP security, a custom
  `CallContextFactory`, mTLS, etc.) before exposing a server to any network the deployer does not fully trust.
- **Configure a `TaskAuthorizationProvider`** for any deployment reachable by more than one user or tenant. As
  of 1.3.0.Final the default is fail-closed (§8.6, §9.2), so absent a provider all task operations are denied
  rather than permitted — but providing a real implementation with proper ownership semantics is still essential
  for multi-tenant security. Do not set `a2a.authorization.required=false` in production.
- **Use fail-closed ownership semantics** in custom `TaskAuthorizationProvider` implementations for production
  (deny when ownership data is unknown); reserve `owner == null → allow` for testing/single-user setups
  *(documented, Javadoc)*.
- **Use atomic inserts** for `recordOwnership` in custom `TaskAuthorizationProvider` implementations to avoid the
  TOCTOU race in §9.8.
- **Terminate/enforce TLS** for whichever transports are exposed; the SDK will not do this.
- **Push-notification SSRF protection is provided by the SDK** as of 1.3.0.Final (§8.7), but you can further
  restrict the attack surface by setting `a2a.push-notifications.enabled=false` in deployments that do not need
  push notifications at all.
- **Leave OpenTelemetry request/response extraction disabled** (`-Dorg.a2aproject.sdk.server.extract.request`
  and `.extract.response`) in production unless the sensitivity of captured data has been reviewed (§5a).
- **Treat all `Message`/`Task` content reaching `AgentExecutor` as untrusted** network input; never build shell
  commands, SQL, or file paths directly from it without validation.
- **Secure the broker and database** backing `extras/queue-manager-replicated` and the JPA-backed stores
  (authentication, encryption in transit, ACLs) — this is entirely the operator's responsibility.
- **Do not rely on `AgentCard.securitySchemes()` alone** to protect an endpoint; it is descriptive metadata, not
  an enforcement mechanism (§9.5).

### §10a Guidance for SPI implementors

Anyone implementing a custom `TaskStore`, `QueueManager`, `TaskAuthorizationProvider`, `PushNotificationSender`,
`PushNotificationConfigStore`, or a new transport/reference module should check the following before shipping it
— modeled on the same review questions comparable projects ask of new components:

- **Is the implementation thread-safe?** Every SPI in `server-common` is documented as requiring thread-safety
  (§5); a non-thread-safe custom implementation reintroduces the data races the in-memory defaults were designed
  to avoid.
- **Does `TaskAuthorizationProvider.recordOwnership` use an atomic insert?** A `isTaskRecorded()` →
  `recordOwnership()` sequence backed by a non-atomic read-then-write is vulnerable to the TOCTOU race described
  in §9.8. Use `putIfAbsent`, `INSERT ... ON CONFLICT DO NOTHING`, or an equivalent atomic primitive.
- **Does the ownership check fail closed?** Treating `owner == null` (or any "unknown" state) as "allow" is
  appropriate only for single-user/test setups (§11); production implementations should deny when ownership
  cannot be established.
- **Does the implementation add a new outbound network call** (e.g., a custom `PushNotificationSender`)? Apply
  the same CRLF-rejection discipline as `BasePushNotificationSender` (§8.1) to any caller-supplied header/token
  value, and consider whether the destination needs URL validation (§9.3) — the SDK will not add this for you.
- **Does the implementation persist or transmit `Message`/`Task` content to a new sink** (a database, a broker,
  a trace exporter)? Treat that content as sensitive by default; document the exposure the same way
  `extras/opentelemetry` documents its request/response extraction flags (§5a).
- **Does the new component add a configuration knob that changes a security-relevant default?** Document it in
  a table following the §5a pattern (knob, default, effect on the model) so downstream users and future model
  revisions can find it.

This checklist is advisory, not enforced by the SDK — there is no annotation-driven, compile-time policy
framework analogous to what some other projects use to flag insecure defaults.

### §11 Known misuse patterns

- **Setting `a2a.authorization.required=false` in production.** This restores the pre-1.3.0 fail-open behavior
  where every operation is permitted for every caller regardless of identity. It is appropriate only for
  development, testing, or fully-trusted single-user deployments. What it looks like: a multi-tenant deployment
  where any caller can read/cancel/modify any other user's tasks. What to do instead: provide a real
  `TaskAuthorizationProvider` implementation with proper ownership semantics.
- **Enabling OpenTelemetry request/response extraction in production.** This captures full message content
  (which may include PII or secrets) into trace spans exported to a collector. What to do instead: enable only
  in controlled debugging sessions, and review collector access controls first.
- **Building a custom `TaskAuthorizationProvider` with `owner == null → allow`.** Appropriate only for
  single-user or test deployments; in production this means any task without recorded ownership (e.g., legacy
  tasks, or tasks created by a bug) is accessible to everyone. What to do instead: fail closed, and add a
  migration step to backfill ownership when enabling authorization on an existing deployment.

### §11a Known non-findings (recurring false positives)

- **"There is no authentication check in `server-common`."** By design — see §3 and §9.1; authentication is a
  runtime/deployment concern this SDK deliberately does not implement.
- **"`SecurityScheme` records have no associated validation code."** By design — they are declarative metadata
  per the A2A protocol spec, not an enforcement mechanism; see §9.5. `BY-DESIGN: property-disclaimed`.
- **"Any caller can read/cancel any task when no `TaskAuthorizationProvider` is registered."** As of 1.3.0.Final
  this is no longer the default behavior: operations are **denied** when no provider is configured (§8.6). A
  report of this behavior on 1.3.0.Final+ is a valid finding unless `a2a.authorization.required=false` has been
  set, in which case the open behavior is intentional by the deployer. On ≤1.2.x the behavior was
  `BY-DESIGN: property-disclaimed` per §9.2; it was fixed via [GHSA-qw47-mcm5-934w](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-qw47-mcm5-934w).
- **"`InMemoryTaskStore`/`InMemoryQueueManager` lose all data on restart."** By design — they are the
  non-persistent defaults; use the JPA/replicated `extras/*` modules for persistence across restarts. `OUT-OF-MODEL`
  is not quite right here since it's core behavior, not an unsupported component — treat as
  `BY-DESIGN: property-disclaimed`, cf. `InMemoryTaskStore` Javadoc.
- **"`BasePushNotificationSender` will POST to any URL, including internal addresses."** As of 1.3.0.Final this
  is no longer accurate: push-notification URLs are validated against an SSRF-safe policy and redirect following
  is disabled (§8.7). A report of SSRF via push notifications on 1.3.0.Final+ should be evaluated against the
  current policy implementation. On ≤1.2.x the behavior was `BY-DESIGN: property-disclaimed` per §9.3; it was
  fixed via [GHSA-q78c-5jjq-57g8](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-q78c-5jjq-57g8).
- **"An attacker can create unlimited tasks/queues, exhausting server memory."** Documented gap — see §9.6;
  global rate and resource limits are a deployment concern (reverse proxy, runtime configuration), not an
  SDK-level control. `BY-DESIGN: property-disclaimed`.

### §12 Conditions that would change this model

- Addition of a built-in authentication mechanism to `server-common` (would revise §3, §9.1, §9.2, §10).
- ~~Addition of SSRF filtering to `BasePushNotificationSender` or `PushNotificationConfigStore`~~ —
  **triggered in 1.3.0.Final** ([GHSA-q78c-5jjq-57g8](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-q78c-5jjq-57g8)); §8.7, §9.3, §10, §11a revised accordingly.
- ~~The first confirmed vulnerability report against this project~~ — **triggered in 1.3.0.Final**; four
  advisories published (GHSA-qw47-mcm5-934w, GHSA-q78c-5jjq-57g8, GHSA-9rhm-2h4x-jwmx, GHSA-x32g-jvvm-4725).
  §8a, §8.6–§8.9, §9.2–§9.3, §11, §11a, §14 revised accordingly.
- A new transport, extras module, or reference implementation gaining direct filesystem or process-spawning
  access (would revise §5, §5a).
- Promotion of any `examples/` code into a supported, shipped module (would revise §3, §2's component table).
- A vulnerability report that cannot be cleanly routed to one of the §13 dispositions — this signals the model
  has a gap and should be revised (add the property to §8 or §9) rather than resolved ad hoc.

### §13 Triage dispositions

| Disposition | Meaning | Licensed by |
| --- | --- | --- |
| `VALID` | Violates a property the project claims (§8), via an in-scope adversary (§7) and input (§6). | §8, §6, §7 |
| `VALID-HARDENING` | No §8 property is violated, but the API makes a §11 misuse easy enough that the project elects to harden it anyway. Typically no CVE. | §11 |
| `OUT-OF-MODEL: trusted-input` | Requires attacker control of a parameter the model marks trusted (e.g., the operator's own `AgentExecutor` or configuration). | §6 |
| `OUT-OF-MODEL: adversary-not-in-scope` | Requires an attacker capability the model excludes (e.g., existing JVM code execution, direct DB/broker access). | §7 |
| `OUT-OF-MODEL: unsupported-component` | Lands in `examples/`, `tck/`, `tests/`, or other code placed out of scope. | §3 |
| `OUT-OF-MODEL: non-default-build` | Only manifests when a discouraged, non-default §5a knob is enabled (e.g., OpenTelemetry payload extraction). | §5a |
| `BY-DESIGN: property-disclaimed` | Concerns a property the project explicitly does not provide (§9), e.g. missing authentication/authorization by default, or SSRF on push-notification URLs. | §9 |
| `KNOWN-NON-FINDING` | Matches a documented recurring false positive. | §11a |
| `MODEL-GAP` | Cannot be cleanly routed to any of the above — triggers a model revision. | §12 |

### §13a Incomplete fixes

An incomplete fix for a `VALID` finding is a **new finding**, not a reopening of the original. Three shapes:

- **Not all call sites.** The fix was applied to some but not all code paths that reach the same vulnerable
  operation (e.g., CRLF rejection added in `BasePushNotificationSender.rejectCrlf` but not in a new
  `PushNotificationSender` implementation that builds headers independently).
- **Under-strength mitigation.** A mitigation is present but can be bypassed (e.g., a deny-list that misses an
  encoding variant, or a check that runs after the value has already been used).
- **Different route to the same sink.** The original entry point is fixed, but a different transport or code path
  reaches the same vulnerable operation without passing through the fix.

Each shape gets its own CVE (if warranted) and its own triage; the original CVE remains closed as fixed.

### §14 Resolved questions

All questions from the initial draft have been resolved. Answers are recorded here for traceability.

**Wave 1 — scope and defaults (resolved):**

1. **Fail-open default.** Confirmed: intentionally fail-open for backward compatibility and simplicity in
   single-tenant/dev use. Clearly documented in §5a, §9.2, §10, and §11 rather than changed.
2. **SSRF filtering for push-notification webhooks.** Confirmed: permanently a deployer responsibility, since
   acceptable destinations are deployment-specific. Reflected in §9.3, §9.6, §10, §11, and §11a.
3. **TLS enforcement.** Confirmed: permanently a deployment concern. Reflected in §3 and §9.4.
4. **Broker security posture for `queue-manager-replicated`.** Confirmed: left as a strong recommendation in the
   module's documentation, not enforced by the SDK. Reflected in §3 and §10.

**Wave 2 — environmental side-effect claims (resolved):**

5. **No process spawning, signal handlers, or JVM-global mutation.** Confirmed by code search: no
   `ProcessBuilder`/`Runtime.exec()` calls, no `addShutdownHook`/signal-handler installations (one Javadoc
   example in `GrpcTransportConfigBuilder` is documentation only, not executable code), no
   `Locale.setDefault`/`TimeZone.setDefault`/`Authenticator.setDefault` calls in any production module.
   Reflected in §5.
6. **System-property reads.** Confirmed: the only security-irrelevant addition beyond §5a is
   `a2a.eventconsumer.bufferFlushDelayMs` (a timing knob with no security implications) and
   `java.runtime.name` in the Android HTTP client provider (runtime detection only). No environment-variable
   reads affect security-relevant behavior. Reflected in §5.

**Wave 3 — remaining inferred claims outside §8a (resolved):**

7. **§2 roles table.** Confirmed as an accurate structural summary of the roles established throughout this
   document.
8. **§3 absence of TLS code.** Re-confirmed by code search (`grep -rli "tls|ssl|SSLContext|X509"` across
   `transport/*`, `reference/*`): no genuine matches.
9. **§4 trust-boundary mechanics.** Confirmed against `AuthorizationRequestHandlerDecorator`: every
   `enforceRead`/`enforceWrite`/`enforceCreate` call is gated on `authorizationProvider != null`.
10. **§9.1 no authentication.** Confirmed against `AuthenticatedUser.isAuthenticated()`: a hardcoded `return
    true` with no credential-verification logic.
11. **§9.3 no SSRF protection.** Confirmed against `BasePushNotificationSender.dispatchNotification`: the
    outbound URL is taken from caller-supplied `TaskPushNotificationConfig.url()` with no validation.

The five items in §8a remain tagged *(anticipated)* rather than promoted to *(maintainer)*: they are
forward-looking risk hypotheses, not facts about the code, and are not subject to confirmation in the same
sense as the claims above.

**Wave 4 — 1.3.0.Final advisories (resolved):**

12. **Fail-open authorization default ([GHSA-qw47-mcm5-934w](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-qw47-mcm5-934w)).** Confirmed: absent a
    `TaskAuthorizationProvider`, all task operations were permitted for any caller in ≤1.2.x. Fixed in 1.3.0.Final
    by switching to a fail-closed default in `DefaultRequestHandler` and enforcing it on both CDI and builder
    paths. §5a, §8.6, §9.2, §10, §11, §11a updated.
13. **SSRF via push-notification webhook ([GHSA-q78c-5jjq-57g8](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-q78c-5jjq-57g8)).** Confirmed: `BasePushNotificationSender`
    POSTed to caller-supplied URLs with no destination validation in ≤1.2.x. Fixed in 1.3.0.Final with SSRF-safe
    URL validation (scheme allowlist, private-network blocking, IPv4-mapped IPv6 blocking) and redirect-following
    disabled. §5a, §8.7, §9.3, §10, §11a updated.
14. **API key credential leakage via redirect ([GHSA-9rhm-2h4x-jwmx](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-9rhm-2h4x-jwmx)).** Confirmed: `AuthInterceptor` used
    `APIKeySecurityScheme.name()` as an HTTP header name without validation, and all HTTP clients followed
    redirects by default, allowing an attacker-controlled agent to harvest credentials. Fixed in 1.3.0.Final with
    an allowlist for API key header names and redirect-following disabled across all HTTP client implementations.
    §8.8 added.
15. **Push-notification credentials logged in cleartext ([GHSA-x32g-jvvm-4725](https://github.com/a2aproject/a2a-java/security/advisories/GHSA-x32g-jvvm-4725)).** Confirmed: parse failures
    in the REST handler's `parseRequestBody` logged the full JSON request body at `SEVERE` level, exposing push
    notification `token` and `authentication.credentials` fields. Fixed in 1.3.0.Final by restricting error
    logging to body metadata (length, content type) only. §8.9 added.
