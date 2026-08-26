package org.a2aproject.sdk.server.multitenancy;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.jspecify.annotations.Nullable;

/**
 * Resolves the {@link AgentExecutor} for a given tenant.
 * <p>
 * Implementations should return the default (unqualified) executor when the tenant
 * is {@code null}, blank, or does not match any registered tenant.
 */
public interface AgentExecutorRouter {

    /**
     * Resolves the {@link AgentExecutor} for the given tenant.
     *
     * @param tenant the tenant identifier, may be {@code null}
     * @return the resolved executor, never {@code null}
     */
    AgentExecutor resolve(@Nullable String tenant);
}
