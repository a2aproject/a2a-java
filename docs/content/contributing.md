---
title: Contributing
description: Developer guide for contributing to the A2A Java SDK.
layout: page
---

# Contributing

See [CONTRIBUTING.md](https://github.com/a2aproject/a2a-java/blob/main/CONTRIBUTING.md) for full contribution guidelines. Fork the repo, create a branch per issue, and submit PRs against `main`.

## Build

Requires Java 17+. Tests output is redirected to files by default.

```bash
mvn clean install
```

## Project Structure

| Module | Purpose |
|--------|---------|
| `spec/` | A2A specification types (Java records for the protocol) |
| `spec-grpc/` | gRPC protobuf definitions and generated classes |
| `common/` | Shared utilities |
| `client/base/` | Core client API |
| `client/transport/spi/` | Transport SPI |
| `client/transport/jsonrpc/`, `grpc/`, `rest/` | Transport implementations |
| `server-common/` | Server-side core (AgentExecutor, TaskStore, QueueManager) |
| `transport/` | Server transport layer |
| `reference/` | Reference server implementations (Quarkus) |
| `tck/` | Technology Compatibility Kit |
| `extras/` | Optional add-ons (OpenTelemetry, JPA stores, Kafka queue, Vert.x, Android) |
| `compat-0.3/` | Backward compatibility layer for A2A protocol v0.3 |
| `boms/` | Bill of Materials POMs |
| `examples/` | Sample applications |

## Code Conventions

- Package root: `org.a2aproject.sdk`
- Serialization: Gson
- Null safety: NullAway + JSpecify annotations via Error Prone
- Reference server runtime: Quarkus
- Testing: JUnit 5, Mockito, REST Assured, Testcontainers

### Style

- Sort import statements
- No star imports (e.g. `import java.util.*`)
- Use Java `record` for immutable data types
- Use `@Nullable` (from `org.jspecify.annotations`) for optional fields
- Use `org.a2aproject.sdk.util.Assert.checkNotNullParam()` in compact constructors
- Use `List.copyOf()` and `Map.copyOf()` for defensive copying
- Apply the Builder pattern for records with many fields (see `AgentCard.java`)

### gRPC Regeneration

Copy `a2a.proto` from upstream and adjust the `java_package` option:

```
option java_package = "org.a2aproject.sdk.grpc";
```

Then regenerate:

```bash
mvn clean install -Dskip.protobuf.generate=false -pl spec-grpc
```

## Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):

```
feat: add streaming support for REST transport
fix: handle null task in cancelTask flow
chore: bump quarkus to 3.x
```

If the commit relates to a GitHub issue, add `This fixes #<issue_number>` at the end of the commit body.

## Architecture Deep Dives

Detailed architecture documentation lives in `.claude/architecture/`:

- **EventQueue & Event Processing** — Queue lifecycle, request flows, usage scenarios
- **0.3 Compatibility Layer** — How v0.3 requests are converted to v1.0 internally
