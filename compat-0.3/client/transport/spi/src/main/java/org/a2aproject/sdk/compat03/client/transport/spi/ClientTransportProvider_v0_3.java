package org.a2aproject.sdk.compat03.client.transport.spi;

import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;

/**
 * Client transport provider interface.
 */
public interface ClientTransportProvider_v0_3<T extends ClientTransport_v0_3, C extends ClientTransportConfig_v0_3<T>> {

    /**
     * Create a client transport.
     *
     * @param clientTransportConfig the client transport config to use
     * @param agentCard the remote agent's agent card
     * @param agentUrl the remote agent's URL
     * @return the client transport
     * @throws A2AClientException_v0_3 if an error occurs trying to create the client
     */
    T create(C clientTransportConfig, AgentCard_v0_3 agentCard,
                           String agentUrl) throws A2AClientException_v0_3;

    /**
     * Get the name of the client transport.
     */
    String getTransportProtocol();

    Class<T> getTransportProtocolClass();
}

