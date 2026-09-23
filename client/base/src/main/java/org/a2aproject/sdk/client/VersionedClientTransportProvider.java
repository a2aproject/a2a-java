package org.a2aproject.sdk.client;

import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;

/** Provider SPI for client transports targeting a protocol version other than 1.0. */
public interface VersionedClientTransportProvider {
    String protocolBinding();

    String protocolVersion();

    Class<? extends ClientTransport> configuredTransportClass();

    ClientTransport create(ClientTransportConfig<?> config, AgentCard card, AgentInterface agentInterface)
            throws A2AClientException;
}
