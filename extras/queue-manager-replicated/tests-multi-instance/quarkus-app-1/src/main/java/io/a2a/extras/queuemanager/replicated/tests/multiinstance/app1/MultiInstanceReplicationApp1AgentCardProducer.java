package io.a2a.extras.queuemanager.replicated.tests.multiinstance.app1;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.a2a.extras.queuemanager.replicated.tests.multiinstance.common.MultiInstanceReplicationAgentCards;
import io.a2a.server.PublicAgentCard;
import io.a2a.spec.AgentCard;

@ApplicationScoped
public class MultiInstanceReplicationApp1AgentCardProducer {

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return MultiInstanceReplicationAgentCards.createAgentCard(1, 8081);
    }
}
