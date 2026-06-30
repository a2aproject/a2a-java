# A2A Java SDK - Spring Boot Integration

This directory is the Maven aggregator for Spring Boot integration modules.

## Layout

- `server/`
  - Server-side Spring Boot integration tree.
  - Contains a shared runtime module plus transport-specific aggregators.
- `client/`
  - Reserved for future client-side Spring Boot integration modules.

## Server Tree

- `spring-boot-server-autoconfigure`
  - Artifact: `a2a-java-sdk-spring-boot-server-autoconfigure`
  - Shared Spring Boot runtime auto-configuration for A2A.
- `rest/`
  - Transport aggregator for the REST/MVC server integration.
  - Contains the REST controller, starter, integration tests, and the TCK SUT.
- `jsonrpc/`
  - Placeholder for future JSON-RPC server modules.
- `grpc/`
  - Placeholder for future gRPC server modules.

## Recommended Dependency

For Spring Boot applications that use the REST transport, depend on:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-spring-boot-starter-server-rest</artifactId>
</dependency>
```

## Server Configuration

The server runtime is configured with `a2a.*` properties.

Example `application.yml`:

```yaml
a2a:
  executor:
    core-pool-size: 5
    max-pool-size: 50
    keep-alive-seconds: 60
    queue-capacity: 100
  blocking:
    agent-timeout-seconds: 30
    consumption-timeout-seconds: 5
  agent-card:
    cache:
      max-age: 3600
```

## Build

```bash
mvn -pl integrations/spring-boot/server -am test
```

## REST TCK

Run the REST TCK end-to-end with the checked-in script:

```bash
bash ./scripts/run-spring-boot-rest-tck.sh
```

That script starts the REST SUT, waits for `/.well-known/agent-card.json`, runs the external `a2a-tck`, and stops the SUT on exit.

If you want to run only the SUT locally, use:

```bash
mvn -pl integrations/spring-boot/server/rest/spring-boot-server-rest-sut -am spring-boot:run
```

The SUT listens on `http://localhost:9999` by default.

If you have the external `a2a-tck` repository checked out, you can validate it with:

```bash
uv run ./run_tck.py --sut-host http://localhost:9999 -v
```

See also:

- `integrations/spring-boot/server/rest/TCK.md`
- `.github/workflows/run-spring-boot-rest-tck.yml`
