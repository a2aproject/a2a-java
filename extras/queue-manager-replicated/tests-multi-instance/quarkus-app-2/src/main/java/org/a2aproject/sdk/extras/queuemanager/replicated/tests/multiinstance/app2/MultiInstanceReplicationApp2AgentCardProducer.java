package org.a2aproject.sdk.extras.queuemanager.replicated.tests.multiinstance.app2;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.extras.queuemanager.replicated.tests.multiinstance.common.MultiInstanceReplicationAgentCards;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.spec.AgentCard;

@ApplicationScoped
public class MultiInstanceReplicationApp2AgentCardProducer {

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return MultiInstanceReplicationAgentCards.createAgentCard(2, 8082);
    }
}
