# A2A Java SDK - MicroProfile Config Integration

This optional integration module provides MicroProfile Config support for the A2A Java SDK configuration system.

## Overview

The A2A Java SDK core uses the `A2AConfigProvider` interface for configuration, with default values loaded from `META-INF/a2a-defaults.properties` files on the classpath.

This module provides `MicroProfileConfigProvider`, which integrates with MicroProfile Config to allow configuration via:
- `application.properties`
- Environment variables
- System properties (`-D` flags)
- Custom ConfigSources

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-sdk-microprofile-config</artifactId>
    <version>${io.a2a.sdk.version}</version>
</dependency>
```

### 2. Configure Properties

Once the dependency is added, you can override any A2A configuration property:

**application.properties:**
```properties
# Executor configuration
a2a.executor.core-pool-size=10
a2a.executor.max-pool-size=100

# Timeout configuration
a2a.blocking.agent.timeout.seconds=60
a2a.blocking.consumption.timeout.seconds=10
```

**Environment variables:**
```bash
export A2A_EXECUTOR_CORE_POOL_SIZE=10
export A2A_BLOCKING_AGENT_TIMEOUT_SECONDS=60
```

**System properties:**
```bash
java -Da2a.executor.core-pool-size=10 -jar your-app.jar
```

## How It Works

The `MicroProfileConfigProvider` implementation:

1. **First tries MicroProfile Config** - Checks `application.properties`, environment variables, system properties, and custom ConfigSources
2. **Falls back to defaults** - If not found, uses values from `META-INF/a2a-defaults.properties` provided by core modules and extras
3. **Priority 50** - Can be overridden by custom providers with higher priority

## Configuration Fallback Chain

```
MicroProfile Config Sources (application.properties, env vars, -D flags)
  ↓ (not found?)
DefaultValuesConfigProvider
  → Scans classpath for ALL META-INF/a2a-defaults.properties files
  → Merges all discovered properties together
  → Throws exception if duplicate keys found
  ↓ (property exists?)
Return merged default value
  ↓ (not found?)
IllegalArgumentException
```

**Note**: All `META-INF/a2a-defaults.properties` files (from server-common, extras modules, etc.) are loaded and merged together by `DefaultValuesConfigProvider` at startup. This is not a sequential fallback chain, but a single merged set of defaults.

## Available Configuration Properties

See the [main README](../../README.md#configuration-system) for a complete list of configuration properties.

## Framework Compatibility

This module works with any MicroProfile Config implementation:

- **Quarkus** - Built-in MicroProfile Config support
- **Helidon** - Built-in MicroProfile Config support
- **Open Liberty** - Built-in MicroProfile Config support
- **WildFly/JBoss EAP** - Add `smallrye-config` dependency
- **Other Jakarta EE servers** - Add MicroProfile Config implementation

## Custom Config Providers

If you're using a different framework (Spring, Micronaut, etc.), you can implement your own `A2AConfigProvider`:

```java
import io.a2a.server.config.A2AConfigProvider;
import io.a2a.server.config.DefaultValuesConfigProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;

@ApplicationScoped
@Alternative
@Priority(100)  // Higher than MicroProfileConfigProvider's priority of 50
public class OtherEnvironmentConfigProvider implements A2AConfigProvider {

    @Inject
    Environment env;

    @Inject
    DefaultValuesConfigProvider defaultValues;

    @Override
    public String getValue(String name) {
        String value = env.getProperty(name);
        if (value != null) {
            return value;
        }
        // Fallback to defaults
        return defaultValues.getValue(name);
    }

    @Override
    public Optional<String> getOptionalValue(String name) {
        String value = env.getProperty(name);
        if (value != null) {
            return Optional.of(value);
        }
        return defaultValues.getOptionalValue(name);
    }
}
```

## Implementation Details

- **Package**: `io.a2a.integrations.microprofile`
- **Class**: `MicroProfileConfigProvider`
- **Priority**: 50 (can be overridden)
- **Scope**: `@ApplicationScoped`
- **Dependencies**: MicroProfile Config API, A2A SDK server-common

## Reference Implementations

The A2A Java SDK reference implementations (Quarkus-based) automatically include this integration module, so MicroProfile Config properties work out of the box.

If you're building a custom server implementation, add this dependency to enable property-based configuration.
