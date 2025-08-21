package io.a2a.client.transport.spi;

import java.util.List;

import io.a2a.client.config.ClientCallInterceptor;
import io.a2a.client.config.ClientConfig;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;

/**
 * Client transport provider interface.
 */
public interface ClientTransportProvider {

    /**
     * Create a client transport.
     *
     * @param clientConfig the client config to use
     * @param agentCard the agent card for the remote agent
     * @param agentUrl the remote agent's URL
     * @param interceptors the optional interceptors to use for a client call (may be {@code null})
     * @return the client transport
     * @throws io.a2a.spec.A2AClientException if an error occurs trying to create the client
     */
    ClientTransport create(ClientConfig clientConfig, AgentCard agentCard,
                           String agentUrl, List<ClientCallInterceptor> interceptors) throws A2AClientException;

    /**
     * Get the name of the client transport.
     */
    String getTransportProtocol();

}

