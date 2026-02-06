package io.a2a.extras.queuemanager.replicated.tests.multiinstance.common;


import java.util.Collections;
import java.util.List;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.TransportProtocol;

/**
 * Shared AgentCard factory for multi-instance replication tests.
 */
public final class MultiInstanceReplicationAgentCards {

    private MultiInstanceReplicationAgentCards() {
    }

    /**
     * Creates an AgentCard for a test instance.
     *
     * @param instanceNumber the instance number (e.g., 1, 2)
     * @param port the port number (e.g., 8081, 8082)
     * @return the configured AgentCard
     */
    public static AgentCard createAgentCard(int instanceNumber, int port) {
        String url = "http://localhost:" + port;

        return AgentCard.builder()
                .name("Multi-Instance Test Agent " + instanceNumber)
                .description("Test agent for multi-instance replication testing - Instance " + instanceNumber)
                .supportedInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), url)))
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(AgentSkill.builder()
                                .id("replication_test")
                                .name("Replication Test")
                                .description("Fire-and-forget agent for testing replication")
                                .tags(Collections.singletonList("test"))
                                .build()))
                .build();
    }
}
