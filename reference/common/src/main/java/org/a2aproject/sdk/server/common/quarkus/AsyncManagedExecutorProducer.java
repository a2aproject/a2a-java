package org.a2aproject.sdk.server.common.quarkus;

import java.util.concurrent.Executor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.a2aproject.sdk.server.util.async.Internal;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alternative executor producer that provides a ManagedExecutor with CDI context propagation.
 * <p>
 * This producer replaces the default {@code AsyncExecutorProducer} so that CDI request context
 * (and any other registered thread context types) are propagated to the agent executor thread.
 * This allows {@code @RequestScoped} beans to be injected and used inside
 * {@link org.a2aproject.sdk.server.agentexecution.AgentExecutor#execute}.
 * <p>
 * Priority 20 ensures this alternative takes precedence over the default producer (priority 10).
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
        LOGGER.info("Initializing ManagedExecutor for async operations with CDI context propagation");
        if (managedExecutor == null) {
            LOGGER.warn("ManagedExecutor not available - context propagation may not work correctly");
        }
    }

    @Produces
    @Internal
    public Executor produce() {
        LOGGER.debug("Using ManagedExecutor for async operations with CDI context propagation");
        if (managedExecutor == null) {
            throw new IllegalStateException("ManagedExecutor not injected - ensure MicroProfile Context Propagation is available");
        }
        return managedExecutor;
    }
}
