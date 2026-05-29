package org.a2aproject.sdk.server.common.quarkus;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import org.a2aproject.sdk.server.config.A2AConfigProvider;
import org.a2aproject.sdk.server.util.async.Internal;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alternative executor producer that provides CDI request context propagation to the agent executor thread.
 * <p>
 * This producer replaces the default {@code AsyncExecutorProducer} so that {@code @RequestScoped} beans
 * — including security identity, OIDC token credentials, and other request-scoped state — are
 * available inside {@link org.a2aproject.sdk.server.agentexecution.AgentExecutor#execute}.
 * <p>
 * The security identity is captured from the submitting thread and set up in a fresh CDI request context
 * on the agent thread within a proper Vert.x duplicated context. This is necessary because:
 * <ul>
 *   <li>Quarkus ArC stores CDI request context state in the Vert.x duplicated context's local data</li>
 *   <li>For streaming requests, the original CDI context is terminated before the agent thread runs</li>
 *   <li>The REST client creates its own Vert.x contexts that must find the CDI state</li>
 * </ul>
 * <p>
 * Priority 20 ensures this alternative takes precedence over the default producer (priority 10).
 */
@ApplicationScoped
@Alternative
@Priority(20)
public class CdiPropagatingExecutorProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CdiPropagatingExecutorProducer.class);

    @Inject
    A2AConfigProvider configProvider;

    @Inject
    Vertx vertx;

    @Inject
    Instance<CurrentIdentityAssociation> currentIdentityAssociation;

    private @Nullable ExecutorService executor;

    @PostConstruct
    public void init() {
        int corePoolSize = Integer.parseInt(configProvider.getValue("a2a.executor.core-pool-size"));
        int maxPoolSize = Integer.parseInt(configProvider.getValue("a2a.executor.max-pool-size"));
        long keepAliveSeconds = Long.parseLong(configProvider.getValue("a2a.executor.keep-alive-seconds"));
        int queueCapacity = Integer.parseInt(configProvider.getValue("a2a.executor.queue-capacity"));

        LOGGER.info("Initializing CDI-propagating executor: corePoolSize={}, maxPoolSize={}, keepAliveSeconds={}, queueCapacity={}",
                corePoolSize, maxPoolSize, keepAliveSeconds, queueCapacity);

        ThreadPoolExecutor tpe = new ThreadPoolExecutor(
                corePoolSize, maxPoolSize, keepAliveSeconds, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new A2AThreadFactory());
        tpe.allowCoreThreadTimeOut(true);
        executor = tpe;
    }

    @PreDestroy
    public void close() {
        if (executor == null) {
            return;
        }
        LOGGER.info("Shutting down CDI-propagating executor");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Produces
    @Internal
    public Executor produce() {
        if (executor == null) {
            throw new IllegalStateException("Executor not initialized");
        }
        return new CdiContextPropagatingExecutor(executor, vertx, currentIdentityAssociation);
    }

    /**
     * Executor wrapper that captures the authenticated security identity from the submitting thread
     * and sets it up in a fresh CDI request context on the agent thread.
     * <p>
     * The agent thread runs within a Vert.x duplicated context so that ArC's
     * {@code VertxCurrentContextFactory} stores the CDI state in Vert.x context local data.
     * This ensures that downstream components (like the Quarkus REST client with {@code @AccessToken})
     * can find the CDI state even if they create their own Vert.x contexts.
     */
    private static class CdiContextPropagatingExecutor implements Executor {

        private final ExecutorService delegate;
        private final Vertx vertx;
        private final Instance<CurrentIdentityAssociation> identityAssociationInstance;

        CdiContextPropagatingExecutor(ExecutorService delegate, Vertx vertx,
                Instance<CurrentIdentityAssociation> identityAssociationInstance) {
            this.delegate = java.util.Objects.requireNonNull(delegate, "delegate must not be null");
            this.vertx = java.util.Objects.requireNonNull(vertx, "vertx must not be null");
            this.identityAssociationInstance = java.util.Objects.requireNonNull(identityAssociationInstance, "identityAssociationInstance must not be null");
        }

        @Override
        public void execute(Runnable command) {
            java.util.Objects.requireNonNull(command, "command must not be null");
            SecurityIdentity capturedIdentity = captureSecurityIdentity();

            delegate.execute(() -> {
                ContextInternal dupCtx = ((ContextInternal) vertx.getOrCreateContext()).duplicate();
                io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle.setContextSafe(dupCtx, true);
                ContextInternal previous = dupCtx.beginDispatch();
                try {
                    ManagedContext rc = Arc.container().requestContext();
                    rc.activate();
                    try {
                        restoreSecurityIdentity(capturedIdentity);
                        command.run();
                    } finally {
                        rc.terminate();
                    }
                } finally {
                    dupCtx.endDispatch(previous);
                }
            });
        }

        private @Nullable SecurityIdentity captureSecurityIdentity() {
            if (identityAssociationInstance.isResolvable()) {
                try {
                    return identityAssociationInstance.get().getIdentity();
                } catch (Exception e) {
                    LOGGER.debug("Could not capture security identity", e);
                }
            }
            return null;
        }

        private void restoreSecurityIdentity(@Nullable SecurityIdentity identity) {
            if (identity != null && identityAssociationInstance.isResolvable()) {
                try {
                    identityAssociationInstance.get().setIdentity(identity);
                } catch (Exception e) {
                    LOGGER.debug("Could not restore security identity on agent thread", e);
                }
            }
        }
    }

    private static class A2AThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "a2a-agent-executor-" + threadNumber.getAndIncrement());
            t.setDaemon(false);
            return t;
        }
    }
}
