# AGENTS.md

## Project Overview

Java SDK for the [Agent2Agent (A2A) Protocol](https://a2a-protocol.org/). Multi-module Maven project (`io.github.a2asdk` group) providing client and server libraries for A2A agent communication over JSON-RPC, gRPC, and REST transports.

## Build

Requires Java 17+.
Tests output is redirected to files by default.

```bash
mvn clean install
```


## Project Structure

- `spec/` — A2A specification types (Java POJOs for the protocol)
- `spec-grpc/` — gRPC protobuf definitions and generated classes
- `common/` — Shared utilities used across modules
- `client/` — Client SDK
  - `base/` — Core client API
  - `transport/spi/` — Transport SPI
  - `transport/jsonrpc/`, `transport/grpc/`, `transport/rest/` — Transport implementations
- `server-common/` — Server-side core (AgentExecutor, TaskStore, QueueManager)
- `transport/` — Server transport layer (jsonrpc, grpc, rest)
- `http-client/` — HTTP client abstraction
- `jsonrpc-common/` — Shared JSON-RPC utilities
- `reference/` — Reference server implementations built on Quarkus
  - `common/`, `jsonrpc/`, `grpc/`, `rest/`
- `tck/` — Technology Compatibility Kit (protocol conformance tests)
- `tests/` — Integration tests
- `extras/` — Optional add-ons (OpenTelemetry, JPA task/notification stores, replicated queue manager, Vert.x HTTP client)
- `integrations/` — Runtime integrations (e.g., MicroProfile Config)
- `boms/` — Bill of Materials POMs (sdk, extras, reference, test-utils)
- `examples/` — Sample applications (helloworld, cloud-deployment)

## Key Conventions

- Package root: `io.a2a`
- Serialization: Gson (see `gson.version` in parent POM)
- Null safety: NullAway + JSpecify annotations enforced via Error Prone
- Reference server runtime: Quarkus
- Testing: JUnit 5, Mockito, REST Assured, Testcontainers

### Code Style

- Import statements are sorted
- Remove any unused import statements
- Do not use "star" imports (eg `import java.util.*`)
- Use Java `record` for immutable data types
- Use `@Nullable` (from org.jspecify.annotations) for optional fields
- Use `io.a2a.util.Assert.checkNotNullParam()` in the compact constructor to validate required fields
- Use `List.copyOf()` and `Map.copyOf()` for defensive copying of collections
- Apply the Builder pattern for records with many fields (see `AgentCard.java` as reference)

### Code generation

- Be concise
- Try to use existing code instead of generating new similar code
- Use the same code convention than existing code. If the existing convention seems incorrect, make suggestion before doing any changes

### PR instructions
- Follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/#summary) for the commit title and message
- Always ask if the commit is related to a GitHub issue. If that's the case, add a `This fixes #{issue_number}` at the end of the commit message

### Skills

- [update-a2a-proto](skills/update-a2a-proto/SKILL.md) — Update the gRPC proto file `a2a.proto` from upstream and regenerate Java sources

### Commands

- `mvn clean install` — Clean build of the project

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Fork the repo, create a branch per issue, submit PRs against `main`.
