package io.a2a.client.transport;

import java.util.List;

import io.a2a.client.ClientCallInterceptor;
import io.a2a.client.ClientConfig;
import io.a2a.http.A2AHttpClient;
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
     */
    ClientTransport create(ClientConfig clientConfig, AgentCard agentCard,
                           String agentUrl, List<ClientCallInterceptor> interceptors);

    /**
     * Get the name of the client transport.
     */
    String getTransportProtocol();

}

