---
title: Configuration
description: Configure the A2A Java SDK — properties, MicroProfile Config integration, custom providers.
layout: page
---

# Configuration

The A2A Java SDK uses a flexible configuration system that works across different frameworks.

**Default behavior:** Configuration values come from `META-INF/a2a-defaults.properties` files on the classpath (provided by core modules and extras). These defaults work out of the box without any additional setup.

**Customizing configuration:**
- **Quarkus/MicroProfile Config users**: Add the `microprofile-config` integration to override defaults via `application.properties`, environment variables, or system properties
- **Spring/other frameworks**: Implement a custom `A2AConfigProvider` (see [Custom Config Providers](#custom-config-providers) below)
- **Reference implementations**: Already include the MicroProfile Config integration

## Configuration Properties

### Executor Settings

The SDK uses a dedicated executor for async operations like streaming. Default: 5 core threads, 50 max threads.

```properties
# Core thread pool size for the @Internal executor (default: 5)
a2a.executor.core-pool-size=5

# Maximum thread pool size (default: 50)
a2a.executor.max-pool-size=50

# Thread keep-alive time in seconds (default: 60)
a2a.executor.keep-alive-seconds=60

# Queue capacity for pending tasks (default: 100)
# When the queue is full, new threads are created up to max-pool-size
a2a.executor.queue-capacity=100
```

### Blocking Call Timeouts

```properties
# Timeout for agent execution in blocking calls (default: 30 seconds)
a2a.blocking.agent.timeout.seconds=30

# Timeout for event consumption in blocking calls (default: 5 seconds)
a2a.blocking.consumption.timeout.seconds=5

# Timeout for TaskStore reconciliation polling in blocking calls (default: 1 second)
a2a.blocking.reconciliation.timeout.seconds=1
```

### Agent Card Caching

```properties
# HTTP Cache-Control max-age for Agent Card responses in seconds (default: 3600)
a2a.agent-card.cache.max-age=3600
```

### Request Context

```properties
# Load referenced tasks from the TaskStore and enforce authorization checks (default: true)
a2a.request-context.populate-referred-tasks=true
```

When enabled, task IDs referenced in incoming messages are looked up in the `TaskStore` and made available to the `AgentExecutor` via `RequestContext.getRelatedTasks()`. This is useful for multi-task conversations where the agent needs access to state from related tasks. Enabled by default; set to `false` to avoid extra `TaskStore` lookups when not needed.

### Authorization

```properties
# Require a TaskAuthorizationProvider to be configured (default: true)
# When true, all task operations are denied if no provider is present (fail-closed).
# Set to false for single-user deployments or testing where authorization is not needed.
a2a.authorization.required=true
```

See [Task Authorization](authorization.md) for details on implementing a `TaskAuthorizationProvider`.

### Push Notification Config Store

```properties
# Maximum push notification configs per task (default: 100)
a2a.push-notification-config.max-per-task=100
```

Limits the number of distinct push notification configurations a single task may register. Each config consumes memory and can trigger an outbound HTTP request on every task event. Re-registering an existing config ID (updating it) does not count against the limit. Both `InMemoryPushNotificationConfigStore` and `JpaDatabasePushNotificationConfigStore` enforce this limit, throwing `InvalidParamsError` when exceeded.

### Push Notifications

```properties
# Enable or disable push notification support (default: true)
a2a.push-notification.enabled=true

# Allowed URL schemes for push notification targets, comma-separated (default: https)
a2a.push-notification.url.allowed-schemes=https

# Allow push notification URLs targeting private/internal network addresses (default: false)
a2a.push-notification.url.allow-private-network-targets=false

# TTL in seconds for DNS resolution caching during URL validation (default: 30, 0 to disable)
a2a.push-notification.url.dns-cache-ttl-seconds=30
```

**`a2a.push-notification.enabled`** controls whether the server accepts push notification configurations. When `false`, push notification configs embedded in `sendMessage` requests are silently ignored, and explicit `CreateTaskPushNotificationConfig` calls return `UnsupportedOperationError`.

**`a2a.push-notification.url.allowed-schemes`** restricts which URL schemes are accepted for push notification targets. Defaults to `https` only. To also allow plain HTTP (e.g. in development), set to `http,https`.

**`a2a.push-notification.url.allow-private-network-targets`** controls whether push notifications can target private/internal network addresses. The default (`false`) blocks loopback addresses, link-local (169.254.x.x), site-local (10.x, 172.16.x, 192.168.x), RFC 6598 shared space (100.64.0.0/10), IPv6 unique-local (fc00::/7), IPv4-mapped IPv6 variants, and multicast addresses. This prevents Server-Side Request Forgery (SSRF) attacks where a client registers a push notification URL pointing to internal infrastructure. Set to `true` **only** in trusted development environments.

**`a2a.push-notification.url.dns-cache-ttl-seconds`** controls how long (in seconds) resolved DNS results are cached during URL validation. Defaults to `30`. Caching avoids a blocking DNS lookup on every push notification delivery. Set to `0` to disable caching entirely (every validation triggers a fresh DNS resolution).

The `DefaultPushNotificationUrlValidator` enforces these policies. Custom implementations can be provided via CDI by implementing `PushNotificationUrlValidator`.

> **Known limitation — DNS rebinding:** hostname resolution happens at validation time, not at connection time. An attacker controlling a domain could register a URL that initially resolves to a public IP, then change the DNS record to an internal address before the next push event fires. The DNS cache reduces this window but does not eliminate it. Deployments with strict SSRF requirements should use network-level firewall rules to block outbound traffic to internal ranges.

### Tuning Guidelines

- **Streaming Performance**: The executor handles streaming subscriptions. Too few threads can cause timeouts under concurrent load.
- **Resource Management**: The dedicated executor prevents streaming operations from competing with the ForkJoinPool.
- **Concurrency**: In production with high concurrent streaming, increase pool sizes accordingly.
- **Agent Timeouts**: LLM-based agents may need longer timeouts (60-120s) compared to simple agents.
- **Reconciliation Timeout**: Increase if blocking calls fail with "Could not find a Task/Message" under heavy load or with slow TaskStore implementations.

## MicroProfile Config Integration

Add the integration dependency to override configuration via standard MicroProfile Config sources:

```xml
<dependency>
    <groupId>org.a2aproject.sdk</groupId>
    <artifactId>a2a-java-sdk-microprofile-config</artifactId>
    <version>$\{org.a2aproject.sdk.version}</version>
</dependency>
```

Once added, you can set any A2A property through:

**application.properties:**
```properties
a2a.executor.core-pool-size=10
a2a.executor.max-pool-size=100
a2a.blocking.agent.timeout.seconds=60
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

### Configuration Fallback Chain

```
MicroProfile Config Sources (application.properties, env vars, -D flags)
  | (not found?)
DefaultValuesConfigProvider
  -> Scans classpath for ALL META-INF/a2a-defaults.properties files
  -> Merges all discovered properties together
  -> Throws exception if duplicate keys found
  | (property exists?)
Return merged default value
  | (not found?)
IllegalArgumentException
```

All `META-INF/a2a-defaults.properties` files (from server-common, extras modules, etc.) are loaded and merged together by `DefaultValuesConfigProvider` at startup. This is not a sequential fallback chain, but a single merged set of defaults.

### Framework Compatibility

The MicroProfile Config integration works with any MicroProfile Config implementation:

- **Quarkus** -- Built-in MicroProfile Config support
- **Helidon** -- Built-in MicroProfile Config support
- **Open Liberty** -- Built-in MicroProfile Config support
- **WildFly/JBoss EAP** -- Add `smallrye-config` dependency
- **Other Jakarta EE servers** -- Add MicroProfile Config implementation

## Custom Config Providers

If you're using a different framework (Spring, Micronaut, etc.), implement your own `A2AConfigProvider`:

```java
@ApplicationScoped
@Alternative
@Priority(100)  // Higher than MicroProfileConfigProvider's priority of 50
public class MyConfigProvider implements A2AConfigProvider {

    private final Properties customConfig;

    @Inject
    DefaultValuesConfigProvider defaultValues;

    MyConfigProvider() {
        customConfig = loadFromYourFramework();
    }

    @Override
    public String getValue(String name) {
        String value = customConfig.getProperty(name);
        if (value != null) {
            return value;
        }
        return defaultValues.getValue(name);
    }

    @Override
    public Optional<String> getOptionalValue(String name) {
        String value = customConfig.getProperty(name);
        if (value != null) {
            return Optional.of(value);
        }
        return defaultValues.getOptionalValue(name);
    }
}
```

**Note:** The reference server implementations (Quarkus-based) automatically include the MicroProfile Config integration, so properties work out of the box in `application.properties`.
