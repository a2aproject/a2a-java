package org.a2aproject.sdk.extras.multitenancy;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.se.SeContainerInitializer;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CdiAgentExecutorRouterTest {

    private SeContainer container;

    @BeforeEach
    void startContainer() {
        container = SeContainerInitializer.newInstance()
                .disableDiscovery()
                .addBeanClasses(CdiAgentExecutorRouter.class, ExecutorProducer.class)
                .initialize();
    }

    @AfterEach
    void closeContainer() {
        container.close();
    }

    @Test
    void knownTenantResolvesToTenantSpecificExecutor() {
        CdiAgentExecutorRouter router = container.select(CdiAgentExecutorRouter.class).get();
        AgentExecutor resolved = router.resolve("acme");
        assertSame(ExecutorProducer.ACME, resolved);
    }

    @Test
    void unknownTenantFallsBackToDefault() {
        CdiAgentExecutorRouter router = container.select(CdiAgentExecutorRouter.class).get();
        assertSame(ExecutorProducer.DEFAULT, router.resolve("unknown"));
    }

    @Test
    void nullTenantReturnsDefault() {
        CdiAgentExecutorRouter router = container.select(CdiAgentExecutorRouter.class).get();
        assertSame(ExecutorProducer.DEFAULT, router.resolve(null));
    }

    @Test
    void blankTenantReturnsDefault() {
        CdiAgentExecutorRouter router = container.select(CdiAgentExecutorRouter.class).get();
        assertSame(ExecutorProducer.DEFAULT, router.resolve(""));
        assertSame(ExecutorProducer.DEFAULT, router.resolve("   "));
    }

    static class ExecutorProducer {

        static final AgentExecutor DEFAULT = new LabelExecutor("default");
        static final AgentExecutor ACME = new LabelExecutor("acme");

        @Produces
        AgentExecutor defaultExecutor() {
            return DEFAULT;
        }

        @Produces
        @Tenant("acme")
        AgentExecutor acmeExecutor() {
            return ACME;
        }
    }

    static class LabelExecutor implements AgentExecutor {
        private final String label;

        LabelExecutor(String label) {
            this.label = label;
        }

        @Override
        public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
            emitter.startWork();
            emitter.addArtifact(List.of(new TextPart(label)));
            emitter.complete();
        }

        @Override
        public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
            emitter.cancel();
        }
    }
}
