package org.a2aproject.sdk.extras.opentelemetry;

import org.a2aproject.sdk.server.util.async.Internal;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import java.util.concurrent.Executor;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alternative executor producer that provides a ManagedExecutor with OpenTelemetry context propagation.
 * <p>
 * This producer replaces the default {@code AsyncExecutorProducer} when the OpenTelemetry extras module
 * is included in the application. The ManagedExecutor ensures that OpenTelemetry trace context is
 * properly propagated across asynchronous boundaries.
 * <p>
 * Priority 20 ensures this alternative takes precedence over the default producer (priority 10).
 * 
 * <h2>Configuration</h2>
 * The ManagedExecutor is container-managed and injected via CDI. Its configuration depends on the
 * runtime environment:
 * <ul>
 *   <li><b>Quarkus:</b> Configure via {@code quarkus.thread-pool.*} properties</li>
 *   <li><b>Other runtimes:</b> Consult your MicroProfile Context Propagation implementation documentation</li>
 * </ul>
 * <p>
 * Unlike the default {@code AsyncExecutorProducer}, this producer does not use the {@code a2a.executor.*}
 * configuration properties. The executor pool sizing and behavior are controlled by the container's
 * ManagedExecutor configuration.
 *
 * @see org.eclipse.microprofile.context.ManagedExecutor
 */
@ApplicationScoped
@Alternative
@Priority(20)
public class AsyncManagedExecutorProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncManagedExecutorProducer.class);
    
    @Inject
    ManagedExecutor managedExecutor;

    @PostConstruct
    public void init() {
        LOGGER.info("Initializing OpenTelemetry-aware ManagedExecutor for async operations");
        if (managedExecutor == null) {
            LOGGER.warn("ManagedExecutor not available - context propagation may not work correctly");
        }
    }

    @Produces
    @Internal
    public Executor produce() {
        LOGGER.debug("Using ManagedExecutor for async operations with OpenTelemetry context propagation");
        if (managedExecutor == null) {
            throw new IllegalStateException("ManagedExecutor not injected - ensure MicroProfile Context Propagation is available");
        }
        return managedExecutor;
    }

}
