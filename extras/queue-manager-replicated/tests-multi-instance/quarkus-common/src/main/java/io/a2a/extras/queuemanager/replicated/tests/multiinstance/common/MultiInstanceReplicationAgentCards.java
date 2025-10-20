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
public class MultiInstanceReplicationAgentCards {

    /**
     * Creates an AgentCard for a test instance.
     *
     * @param instanceNumber the instance number (e.g., 1, 2)
     * @param port the port number (e.g., 8081, 8082)
     * @return the configured AgentCard
     */
    public static AgentCard createAgentCard(int instanceNumber, int port) {
        String url = "http://localhost:" + port;

        return new AgentCard.Builder()
                .name("Multi-Instance Test Agent " + instanceNumber)
                .description("Test agent for multi-instance replication testing - Instance " + instanceNumber)
                .url(url)
                .version("1.0.0")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(false)
                        .stateTransitionHistory(false)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill.Builder()
                                .id("replication_test")
                                .name("Replication Test")
                                .description("Fire-and-forget agent for testing replication")
                                .tags(Collections.singletonList("test"))
                                .build()))
                .additionalInterfaces(List.of(
                        new AgentInterface(TransportProtocol.JSONRPC.asString(), url)))
                .protocolVersion("0.3.0")
                .build();
    }
}
