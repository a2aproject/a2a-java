package org.a2aproject.sdk.extras.multitenancy;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.multitenancy.AgentExecutorRouter;
import org.a2aproject.sdk.server.multitenancy.Tenant;
import org.a2aproject.sdk.server.util.CdiUtils;
import org.jspecify.annotations.Nullable;

/**
 * CDI-based {@link AgentExecutorRouter} that resolves tenant-specific {@link AgentExecutor} beans
 * using the {@link Tenant} qualifier.
 * <p>
 * Falls back to the default (unqualified) executor when the tenant is {@code null}, blank,
 * or does not match any {@code @Tenant}-qualified bean.
 */
@ApplicationScoped
public class CdiAgentExecutorRouter implements AgentExecutorRouter {

    @Inject
    @Any
    Instance<AgentExecutor> allExecutors;

    private @Nullable AgentExecutor defaultExecutor;

    @PostConstruct
    void init() {
        defaultExecutor = CdiUtils.resolveDefaultBean(
                allExecutors, Tenant.class, null, "AgentExecutor");
    }

    @Override
    public AgentExecutor resolve(@Nullable String tenant) {
        if (tenant == null || tenant.isBlank()) {
            return requireDefaultExecutor();
        }
        Instance<AgentExecutor> selected = allExecutors.select(new Tenant.Literal(tenant));
        if (selected.isResolvable()) {
            return selected.get();
        }
        return requireDefaultExecutor();
    }

    /**
     * Returns the default executor, throwing if none was discovered at startup.
     *
     * @return the default executor, never {@code null}
     * @throws IllegalStateException if no default {@link AgentExecutor} bean exists
     */
    private AgentExecutor requireDefaultExecutor() {
        if (defaultExecutor == null) {
            throw new IllegalStateException(
                    "No default AgentExecutor bean found — provide an AgentExecutor bean without @Tenant qualifier");
        }
        return defaultExecutor;
    }
}
