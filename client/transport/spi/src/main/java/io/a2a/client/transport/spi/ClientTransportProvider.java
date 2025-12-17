package io.a2a.client.transport.spi;

import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;

/**
 * Client transport provider interface.
 */
public interface ClientTransportProvider<T extends ClientTransport, C extends ClientTransportConfig<T>> {

    /**
     * Create a client transport.
     *
     * @param clientTransportConfig the client transport config to use
     * @param agentInterface the remote agent's interface
     * @return the client transport
     * @throws io.a2a.spec.A2AClientException if an error occurs trying to create the client
     */
    T create(C clientTransportConfig, AgentCard agentCard, AgentInterface agentInterface) throws A2AClientException;

    /**
     * Get the name of the client transport.
     */
    String getTransportProtocol();

    Class<T> getTransportProtocolClass();
}

