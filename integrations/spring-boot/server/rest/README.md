# A2A Java SDK - Spring Boot Server REST Aggregator

This directory aggregates the REST-specific Spring Boot server modules.

## Modules

- `spring-boot-server-rest`
  - Servlet and Spring MVC transport adapter.
- `spring-boot-starter-server-rest`
  - Dependency-only starter for REST/MVC applications.
- `spring-boot-server-integration-tests`
  - Integration tests for the REST server stack.
- `spring-boot-server-rest-sut`
  - Runnable REST SUT for the external A2A TCK.

## Build

```bash
mvn -pl integrations/spring-boot/server/rest -am test
```

## TCK SUT

Run the REST TCK end-to-end with:

```bash
bash ./scripts/run-spring-boot-rest-tck.sh
```

If you want to run only the REST SUT locally, use:

```bash
mvn -pl integrations/spring-boot/server/rest/spring-boot-server-rest-sut -am spring-boot:run
```
