package org.a2aproject.sdk.client;

import org.a2aproject.sdk.client.transport.spi.ClientTransport;
import org.a2aproject.sdk.client.transport.spi.ClientTransportConfig;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;

/**
 * Provider SPI for client transports targeting a protocol version other than 1.0.
 */
public interface VersionedClientTransportProvider {
    /**
     * Returns the protocol binding supported by this provider.
     *
     * @return the protocol binding name
     */
    String protocolBinding();

    /**
     * Returns the protocol version supported by this provider.
     *
     * @return the protocol version
     */
    String protocolVersion();

    /**
     * Returns the native transport class whose configuration this provider consumes.
     *
     * @return the configured transport class
     */
    Class<? extends ClientTransport> configuredTransportClass();

    /**
     * Creates a transport for the supplied agent interface.
     *
     * @param config the native transport configuration
     * @param card the agent card
     * @param agentInterface the selected agent interface
     * @return the configured transport
     * @throws A2AClientException if the configuration or agent interface is invalid
     */
    ClientTransport create(ClientTransportConfig<?> config, AgentCard card, AgentInterface agentInterface)
            throws A2AClientException;
}
