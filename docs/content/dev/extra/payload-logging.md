---
title: Structured Payload Logging
description: Structured payload logging for A2A protocol messages with truncation, masking, and trace correlation.
layout: page
---

# Structured Payload Logging

Provides structured, DEBUG-level log events for A2A protocol message payloads with configurable truncation, sensitive-data masking, and trace correlation.

This is the **proper logging mechanism** for payload inspection — unlike the previous opt-in `toString()` on span attributes, it gives you structured fields, safe masking, and trace-linked logs suitable for production.

## Why Not Span Attributes?

The previous mechanism (`-Dorg.a2aproject.sdk.server.extract.request=true` / `...response=true`) puts raw `toString()` into span attributes:

- No truncation → risk of huge spans
- No masking → sensitive data leaks into tracing backend
- Not structured → cannot query by fields
- Not a log → no log-level control, no audit trail

Payload logging fixes all of these: DEBUG-structured logs with masking, truncation, and `trace_id`/`span_id` correlation.

## What Is Logged

When enabled, the SDK emits structured logs (logger `org.a2aproject.sdk.payload` at DEBUG) for:

| Category | Event Type | Logged Entities |
|----------|------------|-----------------|
| **Task requests** | `task.request` | `message/send`, `message/stream`, `tasks/get`, `tasks/list`, `tasks/cancel` |
| **State changes** | `task.state_change` | `TaskStatusUpdateEvent` (e.g., `submitted → working → completed`) |
| **Result artifacts** | `task.artifact` | `TaskArtifactUpdateEvent` with `Artifact` parts |
| **Task results** | `task.result` | Final `Task` snapshots |
| **Negotiation** | `negotiation` | Extension lists, `GetExtendedAgentCard` |
| **Authorization** | `authorization` | Allow/deny decisions from `TaskAuthorizationProvider` |
| **Push subscriptions** | `notification.subscription` | Create/get/list/delete push configs, `subscribeToTask` |
| **Push deliveries** | `notification.delivery` | Webhook dispatch attempts (url, success) |
| **Streaming events** | `streaming.event` | Per-event SSE payloads |

Each log line includes:

- `a2a.event.type` — one of the values above
- `a2a.payload` — the JSON payload (masked, truncated)
- `a2a.payload.size` — original size
- `a2a.payload.truncated` — `true` if truncated
- `a2a.task_id`, `a2a.context_id`, `a2a.operation`, etc. — as applicable
- `trace_id` / `span_id` — from OpenTelemetry `Span.current()` or MDC (`trace_id`/`traceId`, `span_id`/`spanId`)

Example (Logback/Text):

```
10:15:23.123 [executor-thread-1] DEBUG org.a2aproject.sdk.payload - a2a.payload event_type=task.request payload={"message":{"taskId":"t-123","parts":[{"text":"hello"}]}} a2a.task_id=t-123 a2a.operation=message/send trace_id=abc123 span_id=def456
```

With a JSON encoder (e.g., logstash-logback), the `keyValue` pairs become JSON fields.

## Enabling

### System Properties (quickest)

```bash
# Enable payload logging
-Da2a.logging.payload.enabled=true

# Optional tuning (defaults shown)
-Da2a.logging.payload.max-size=4096
-Da2a.logging.payload.masking.enabled=true
```

And set the logger to DEBUG (Logback `logback.xml` or `application.properties`):

```xml
<logger name="org.a2aproject.sdk.payload" level="DEBUG"/>
```

```properties
quarkus.log.category."org.a2aproject.sdk.payload".level=DEBUG
```

### Programmatic (Quarkus, tests)

```java
import org.a2aproject.sdk.common.logging.*;

A2APayloadLoggingConfig cfg = A2APayloadLoggingConfig.builder()
        .enabled(true)
        .maxSize(4096)
        .maskingEnabled(true)
        .build();
StructuredPayloadLogger.setConfig(cfg);

// Optional: custom masking strategy
StructuredPayloadLogger.setMaskingStrategy(new MyMaskingStrategy());
```

### In `a2a-defaults.properties`

These defaults ship with `server-common`:

```properties
a2a.logging.payload.enabled=false
a2a.logging.payload.max-size=4096
a2a.logging.payload.masking.enabled=true
```

Override via MicroProfile Config, system properties, or environment variables per your runtime.

## Truncation

- Default limit is **4096 characters** (system property `a2a.logging.payload.max-size`).
- If the payload exceeds the limit, it is cut and ` [truncated]` is appended.
- Log field `a2a.payload.truncated=true` signals truncation; `a2a.payload.size` keeps original length.

```json
{"a2a.payload":"{\"large\": \"...\"} [truncated]","a2a.payload.truncated":"true","a2a.payload.size":"8192"}
```

## Sensitive Data Masking

By default, fields whose keys contain these substrings (case-insensitive) are masked to `"***"`:

`token`, `credentials`, `password`, `secret`, `apikey`/`api_key`, `privatekey`/`private_key`, `authorization`, `authentication`, `accesstoken`/`access_token`, `refreshtoken`/`refresh_token`, `clientsecret`/`client_secret`, `url`

This covers push notification `url`/`token`, `authentication.credentials`, security schemes, etc.

- Masking is **recursive**: nested objects and arrays are traversed.
- Non-JSON payloads fall back to regex masking.
- Malformed payloads return the original string (never drop a log due to masking).

### Customizing Keys

```bash
-Da2a.logging.payload.masking.sensitive-keys=token,mySecret,internalUrl
```

Or programmatically:

```java
DefaultPayloadMaskingStrategy strategy = new DefaultPayloadMaskingStrategy(Set.of("token","mySecret"));
StructuredPayloadLogger.setMaskingStrategy(strategy);
```

### Disabling Masking

```bash
-Da2a.logging.payload.masking.enabled=false
```

Or `NoOpPayloadMaskingStrategy`:

```java
StructuredPayloadLogger.setMaskingStrategy(new NoOpPayloadMaskingStrategy());
```

### Custom Strategy

Implement `PayloadMaskingStrategy`:

```java
public class MyMasking implements PayloadMaskingStrategy {
    public String mask(String json) {
        // your logic, e.g., encrypt, hash, or different placeholder
        return ...;
    }
}
```

Register via system property:

```bash
-Da2a.logging.payload.masking.strategy=com.example.MyMasking
```

Or via `A2APayloadLoggingConfig.builder().maskingStrategy(new MyMasking()).build()`.

## Trace Correlation

Logs auto-include `trace_id`/`span_id` if available:

1. **OpenTelemetry**: reflection on `io.opentelemetry.api.trace.Span.current().getSpanContext()` (no hard dependency — if OTel not on classpath, skipped).
2. **MDC**: keys `trace_id`/`traceId`/`X-B3-TraceId` and `span_id`/`spanId`/`X-B3-SpanId`.

If you use OpenTelemetry, spans and logs share the same IDs — link them in your backend (Jaeger, Grafana, etc.) for root-cause analysis.

You can also set MDC manually:

```java
TraceCorrelation.setMdc(traceId, spanId);
// ... log will pick it up
TraceCorrelation.clearMdc();
```

## Architecture

```
MessageSendParams → DefaultRequestHandler → StructuredPayloadLogger → SLF4J (DEBUG)
                                            ├─ JsonUtil.toJson (serialize)
                                            ├─ PayloadMaskingStrategy (mask)
                                            ├─ PayloadTruncator (4KB → [truncated])
                                            └─ TraceCorrelation (trace_id/span_id)

AgentEmitter.updateStatus / addArtifact → ServerPayloadLoggingHelper → StructuredPayloadLogger
BasePushNotificationSender.dispatch → ServerPayloadLoggingHelper.logPushDelivery
DefaultRequestHandler.enforce* → ServerPayloadLoggingHelper.logAuthDecision
JSONRPCTransport / OpenTelemetry decorators → JsonPayloadLoggingHelper → StructuredPayloadLogger
```

All call sites check `StructuredPayloadLogger.isEnabled()` first (config enabled + logger DEBUG), so overhead is near-zero when disabled.

## Security Best Practices

- **Keep masking ON** in production (default).
- **Don't enable `extract.request/response` span attributes** in production — they have no masking/truncation.
- **Use a JSON log encoder** (e.g., logstash-logback-encoder) to get true structured fields; the keyValues are then queryable in Loki/Elasticsearch.
- **Sampling**: payload logging can be verbose for high-throughput agents; enable selectively or with a short retention for diagnostics.

## Troubleshooting

**No logs appear?**

- Ensure `a2a.logging.payload.enabled=true` and logger level DEBUG.
- Check `A2APayloadLoggingConfig.fromSystemProperties()` is re-read after you set system properties (or call `StructuredPayloadLogger.resetToSystemProperties()` in tests).

**Payload shows `***` unexpectedly?**

- A field name matched a sensitive key (e.g., `url` masks `pushUrl`). Customize `sensitive-keys` or provide your own strategy.

**Trace IDs missing?**

- Ensure OTel is initialized and the current `Span` is valid (`isValid()`), or populate MDC (`trace_id`, `span_id`).
- If using Quarkus, the `quarkus-opentelemetry` extension propagates context automatically; check your collector config.

## See Also

- [OpenTelemetry](../extra/opentelemetry) — span-based tracing (payload logging adds log/trace linking)
- [Configuration](../../configuration) — `a2a.*` properties
- [Server](../../server) — request handler and task lifecycle
