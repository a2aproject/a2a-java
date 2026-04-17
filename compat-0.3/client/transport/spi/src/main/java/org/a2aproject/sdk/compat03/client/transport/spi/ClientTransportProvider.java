package org.a2aproject.sdk.compat03.client.transport.spi;

import org.a2aproject.sdk.compat03.spec.A2AClientException;
import org.a2aproject.sdk.compat03.spec.AgentCard;

/**
 * Client transport provider interface.
 */
public interface ClientTransportProvider<T extends ClientTransport, C extends ClientTransportConfig<T>> {

    /**
     * Create a client transport.
     *
     * @param clientTransportConfig the client transport config to use
     * @param agentCard the remote agent's agent card
     * @param agentUrl the remote agent's URL
     * @return the client transport
     * @throws A2AClientException if an error occurs trying to create the client
     */
    T create(C clientTransportConfig, AgentCard agentCard,
                           String agentUrl) throws A2AClientException;

    /**
     * Get the name of the client transport.
     */
    String getTransportProtocol();

    Class<T> getTransportProtocolClass();
}

