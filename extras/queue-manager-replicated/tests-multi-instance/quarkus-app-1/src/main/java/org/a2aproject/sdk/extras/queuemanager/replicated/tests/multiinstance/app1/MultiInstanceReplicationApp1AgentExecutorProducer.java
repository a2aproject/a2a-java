package org.a2aproject.sdk.extras.queuemanager.replicated.tests.multiinstance.app1;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.extras.queuemanager.replicated.tests.multiinstance.common.MultiInstanceReplicationAgentExecutor;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;

@ApplicationScoped
public class MultiInstanceReplicationApp1AgentExecutorProducer {

    @Produces
    public AgentExecutor agentExecutor() {
        return new MultiInstanceReplicationAgentExecutor();
    }
}
