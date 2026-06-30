# Spring Boot REST TCK

This directory contains the REST transport stack and the runnable TCK SUT.

## What The TCK Checks

- `/.well-known/agent-card.json`
- `POST /message:send`
- `POST /message:stream`
- task lifecycle endpoints exposed by the REST transport

## Local Run

From the repository root:

```bash
bash ./scripts/run-spring-boot-rest-tck.sh
```

The script:

1. starts the REST SUT from `integrations/spring-boot/server/rest/spring-boot-server-rest-sut`
2. waits for `http://localhost:9999/.well-known/agent-card.json`
3. runs the external `a2a-tck` against that URL
4. stops the SUT on exit

## Manual Steps

If you want to run the pieces separately:

```bash
mvn -B -pl integrations/spring-boot/server/rest/spring-boot-server-rest-sut -am spring-boot:run
```

In another terminal, run the TCK from the checked-out `a2a-tck` repository:

```bash
uv run ./run_tck.py --sut-host http://localhost:9999 -v
```

## CI

The GitHub Actions workflow is:

```text
.github/workflows/run-spring-boot-rest-tck.yml
```
