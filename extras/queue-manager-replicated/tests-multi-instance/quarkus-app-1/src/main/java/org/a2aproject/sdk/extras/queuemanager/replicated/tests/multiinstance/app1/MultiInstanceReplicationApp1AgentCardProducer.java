package org.a2aproject.sdk.extras.queuemanager.replicated.tests.multiinstance.app1;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.extras.queuemanager.replicated.tests.multiinstance.common.MultiInstanceReplicationAgentCards;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCard;

@ApplicationScoped
public class MultiInstanceReplicationApp1AgentCardProducer {

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return MultiInstanceReplicationAgentCards.createAgentCard(1, 8081);
    }
}
