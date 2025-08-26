package io.a2a.client;

import static io.a2a.util.Assert.checkNotNullParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.a2a.client.config.ClientCallInterceptor;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.ClientTransportProvider;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.TransportProtocol;

/**
 * Used to generate the appropriate client for the agent.
 */
public class ClientFactory {

    private final ClientConfig clientConfig;
    private final Map<String, ClientTransportProvider> transportProviderRegistry = new HashMap<>();

    /**
     * Create a client factory used to generate the appropriate client for the agent.
     *
     * @param clientConfig the client config to use
     */
    public ClientFactory(ClientConfig clientConfig) {
        checkNotNullParam("clientConfig", clientConfig);
        this.clientConfig = clientConfig;
        ServiceLoader<ClientTransportProvider> loader = ServiceLoader.load(ClientTransportProvider.class);
        for (ClientTransportProvider transport : loader) {
            this.transportProviderRegistry.put(transport.getTransportProtocol(), transport);
        }
    }

    /**
     * Create a new A2A client for the given agent card.
     *
     * @param agentCard the agent card for the remote agent
     * @param consumers a list of consumers to pass responses from the remote agent to
     * @param streamingErrorHandler an error handler that should be used for the streaming case if an error occurs
     * @return the client to use
     * @throws A2AClientException if the client cannot be created for any reason
     */
    public Client create(AgentCard agentCard, List<BiConsumer<ClientEvent, AgentCard>> consumers,
                         Consumer<Throwable> streamingErrorHandler) throws A2AClientException {
        return create(agentCard, consumers, streamingErrorHandler, null);
    }

    /**
     * Create a new A2A client for the given agent card.
     *
     * @param agentCard the agent card for the remote agent
     * @param consumers a list of consumers to pass responses from the remote agent to
     * @param streamingErrorHandler an error handler that should be used for the streaming case if an error occurs
     * @param interceptors the optional list of client call interceptors (may be {@code null})
     * @return the client to use
     * @throws A2AClientException if the client cannot be created for any reason
     */
    public Client create(AgentCard agentCard, List<BiConsumer<ClientEvent, AgentCard>> consumers,
                         Consumer<Throwable> streamingErrorHandler, List<ClientCallInterceptor> interceptors) throws A2AClientException {
        checkNotNullParam("agentCard", agentCard);
        checkNotNullParam("consumers", consumers);
        LinkedHashMap<String, String> serverPreferredTransports = getServerPreferredTransports(agentCard);
        List<String> clientPreferredTransports = getClientPreferredTransports();
        ClientTransport clientTransport = getClientTransport(clientPreferredTransports, serverPreferredTransports,
                agentCard, interceptors);
        return new Client(agentCard, clientConfig, clientTransport, consumers, streamingErrorHandler);
    }

    private static LinkedHashMap<String, String> getServerPreferredTransports(AgentCard agentCard) {
        LinkedHashMap<String, String> serverPreferredTransports = new LinkedHashMap<>();
        serverPreferredTransports.put(agentCard.preferredTransport(), agentCard.url());
        if (agentCard.additionalInterfaces() != null) {
            for (AgentInterface agentInterface : agentCard.additionalInterfaces()) {
                serverPreferredTransports.putIfAbsent(agentInterface.transport(), agentInterface.url());
            }
        }
        return serverPreferredTransports;
    }

    private List<String> getClientPreferredTransports() {
        List<String> preferredClientTransports = clientConfig.getSupportedTransports();
        if (preferredClientTransports == null) {
            preferredClientTransports = new ArrayList<>();
        }
        if (preferredClientTransports.isEmpty()) {
            // default to JSONRPC if not specified
            preferredClientTransports.add(TransportProtocol.JSONRPC.asString());
        }
        return preferredClientTransports;
    }

    private ClientTransport getClientTransport(List<String> clientPreferredTransports,
                                               LinkedHashMap<String, String> serverPreferredTransports,
                                               AgentCard agentCard,
                                               List<ClientCallInterceptor> interceptors) throws A2AClientException {
        String transportProtocol = null;
        String transportUrl = null;
        if (clientConfig.isUseClientPreference()) {
            for (String clientPreferredTransport : clientPreferredTransports) {
                if (serverPreferredTransports.containsKey(clientPreferredTransport)) {
                    transportProtocol = clientPreferredTransport;
                    transportUrl = serverPreferredTransports.get(transportProtocol);
                    break;
                }
            }
        } else {
            for (Map.Entry<String, String> transport : serverPreferredTransports.entrySet()) {
                if (clientPreferredTransports.contains(transport.getKey())) {
                    transportProtocol = transport.getKey();
                    transportUrl = transport.getValue();
                    break;
                }
            }
        }
        if (transportProtocol == null || transportUrl == null) {
            throw new A2AClientException("No compatible transports found");
        }
        if (! transportProviderRegistry.containsKey(transportProtocol)) {
            throw new A2AClientException("No client available for " + transportProtocol);
        }

        ClientTransportProvider clientTransportProvider = transportProviderRegistry.get(transportProtocol);
        return clientTransportProvider.create(clientConfig, agentCard, transportUrl, interceptors);
    }

}
