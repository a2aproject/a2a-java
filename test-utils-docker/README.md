# Docker Test Utilities

JUnit 5 utilities for conditional execution of Docker/Podman-based tests.

## Overview

This module provides a `@RequiresDocker` annotation that conditionally executes tests based on Docker or Podman availability and user preferences. It automatically checks for both Docker and Podman container engines.

## Usage

Add the dependency to your test module:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-test-utils-docker</artifactId>
    <scope>test</scope>
</dependency>
```

Annotate test classes that require Docker:

```java
import org.a2aproject.sdk.testutils.docker.RequiresDocker;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
@RequiresDocker
public class MyDockerBasedTest {

    @Test
    public void testSomethingWithDocker() {
        // Test that requires Docker (e.g., Testcontainers, Quarkus Dev Services)
    }
}
```

## Behavior

| `-DskipDockerTests` | Docker/Podman Available | Behavior |
|---------------------|------------------------|----------|
| Not set | ✅ Yes | **RUN** tests normally |
| Not set | ❌ No | **ABORT** tests with error message |
| `true` | ✅ Yes | **SKIP** tests (shown as disabled) |
| `true` | ❌ No | **SKIP** tests (shown as disabled) |

## Examples

### Run all tests (requires Docker to be running)
```bash
mvn clean install
```

### Skip Docker tests when Docker is not available
```bash
mvn clean install -DskipDockerTests=true
```

### Expected behavior when Docker is NOT available

**Without skip flag:**
```bash
mvn clean install
# Tests ABORT with: "Docker/Podman is not available. Use -DskipDockerTests=true to skip these tests."
```

**With skip flag:**
```bash
mvn clean install -DskipDockerTests=true
# Tests are SKIPPED, build succeeds
# Modules still compile
```

## Implementation Details

- **Container Detection**: Checks for both `docker` and `podman` commands by executing `docker info` or `podman info`
- **JUnit 5 Extension**: Implements `ExecutionCondition` to control test execution
- **Class-Level Only**: Annotation is applied at the class level (not method level)
- **Compilation**: Modules are always compiled regardless of Docker/Podman availability or skip flag
- **No External Dependencies**: Uses only Java standard library for container detection (no Testcontainers dependency)

## Use Cases

This is useful for:
- **CI/CD pipelines** where Docker/Podman may not be available
- **Local development** when container daemon is not running
- **Quarkus Dev Services** tests that automatically start containers
- **Testcontainers-based** integration tests
- **Podman users** who use Podman instead of Docker
