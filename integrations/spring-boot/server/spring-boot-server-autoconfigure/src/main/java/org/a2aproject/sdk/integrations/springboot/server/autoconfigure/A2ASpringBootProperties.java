package org.a2aproject.sdk.integrations.springboot.server.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Spring Boot configuration properties for the A2A server runtime.
 *
 * <p>The properties are intentionally aligned with the core {@code a2a.*} configuration keys
 * already understood by the underlying server modules:
 * <ul>
 *   <li>{@code a2a.executor.*} for async worker pool sizing</li>
 *   <li>{@code a2a.blocking.*} for blocking request timeout behavior</li>
 *   <li>{@code a2a.agent-card.cache.max-age} for discovery caching</li>
 * </ul>
 *
 * <p>Keeping this class narrow avoids inventing a second Spring-specific configuration surface.
 */
@ConfigurationProperties(prefix = "a2a")
public class A2ASpringBootProperties {

    private final Executor executor = new Executor();
    private final Blocking blocking = new Blocking();
    private final AgentCardCache agentCardCache = new AgentCardCache();

    public Executor getExecutor() {
        return executor;
    }

    public Blocking getBlocking() {
        return blocking;
    }

    public AgentCardCache getAgentCardCache() {
        return agentCardCache;
    }

    public static class Executor {
        private int corePoolSize = 5;
        private int maxPoolSize = 50;
        private long keepAliveSeconds = 60;
        private int queueCapacity = 100;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public long getKeepAliveSeconds() {
            return keepAliveSeconds;
        }

        public void setKeepAliveSeconds(long keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    public static class Blocking {
        private long agentTimeoutSeconds = 30;
        private long consumptionTimeoutSeconds = 5;

        public long getAgentTimeoutSeconds() {
            return agentTimeoutSeconds;
        }

        public void setAgentTimeoutSeconds(long agentTimeoutSeconds) {
            this.agentTimeoutSeconds = agentTimeoutSeconds;
        }

        public long getConsumptionTimeoutSeconds() {
            return consumptionTimeoutSeconds;
        }

        public void setConsumptionTimeoutSeconds(long consumptionTimeoutSeconds) {
            this.consumptionTimeoutSeconds = consumptionTimeoutSeconds;
        }
    }

    public static class AgentCardCache {
        private long maxAge = 3600;

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }
}
