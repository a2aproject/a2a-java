# A2A Java SDK - Spring Boot REST TCK SUT

This module is a runnable Spring Boot REST system-under-test for the external A2A TCK.

## What It Is For

- Start a real Spring Boot REST server that exposes the A2A protocol endpoints.
- Feed that server to the external `a2a-tck` runner.
- Keep the REST SUT separate from JSON-RPC and gRPC so each transport can be validated independently later.

## Run

From the repository root:

```bash
mvn -pl integrations/spring-boot/server/rest/spring-boot-server-rest-sut -am spring-boot:run
```

The SUT listens on `http://localhost:9999` by default.

## TCK

For the full end-to-end run, use the checked-in helper:

```bash
bash ./scripts/run-spring-boot-rest-tck.sh
```

Point the external A2A TCK at the SUT URL:

```text
http://localhost:9999
```

The TCK should then verify the REST transport contract against the live Spring Boot app.

If you have the external `a2a-tck` repository checked out, the manual run looks like this:

```bash
uv run ./run_tck.py --sut-host http://localhost:9999 -v
```

For CI, see `.github/workflows/run-spring-boot-rest-tck.yml` and `integrations/spring-boot/server/rest/TCK.md`.

## Build

```bash
mvn -pl integrations/spring-boot/server/rest/spring-boot-server-rest-sut -am test
```
