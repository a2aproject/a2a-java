package io.a2a.extras.queuemanager.replicated.tests.multiinstance.app1;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.a2a.extras.queuemanager.replicated.tests.multiinstance.common.MultiInstanceReplicationAgentExecutor;
import io.a2a.server.agentexecution.AgentExecutor;

@ApplicationScoped
public class MultiInstanceReplicationApp1AgentExecutorProducer {

    @Produces
    public AgentExecutor agentExecutor() {
        return new MultiInstanceReplicationAgentExecutor();
    }
}
