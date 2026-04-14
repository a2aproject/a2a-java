package org.a2aproject.sdk.compat03.server.apps.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities;
import org.a2aproject.sdk.compat03.spec.AgentCard;
import io.quarkus.arc.DefaultBean;

import java.util.List;

/**
 * Test bean producers for compat-0.3 JSONRPC reference server tests.
 * Provides default implementations of required CDI beans for testing.
 */
@ApplicationScoped
public class TestBeanProducers {

    @Produces
    @PublicAgentCard
    @DefaultBean
    public AgentCard createTestAgentCard() {
        return new AgentCard.Builder()
            .name("compat-0.3-test-agent")
            .version("1.0.0")
            .capabilities(new AgentCapabilities(
                true,   // streaming
                false,  // pushNotifications
                false,  // stateTransitionHistory
                List.of()  // extensions
            ))
            .build();
    }
}
