package io.a2a.client;

import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.spi.ClientTransport;
import io.a2a.client.transport.spi.ClientTransportConfig;
import io.a2a.client.transport.spi.ClientTransportConfigBuilder;
import io.a2a.client.transport.spi.ClientTransportProvider;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.TransportProtocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ClientBuilder {

    private static final Map<String, ClientTransportProvider<? extends ClientTransport, ? extends ClientTransportConfig<?>>> transportProviderRegistry = new HashMap<>();
    private static final Map<Class<? extends ClientTransport>, String> transportProtocolMapping = new HashMap<>();

    static {
        ServiceLoader<ClientTransportProvider> loader = ServiceLoader.load(ClientTransportProvider.class);
        for (ClientTransportProvider<?, ?> transport : loader) {
            transportProviderRegistry.put(transport.getTransportProtocol(), transport);
            transportProtocolMapping.put(transport.getTransportProtocolClass(), transport.getTransportProtocol());
        }
    }

    private final AgentCard agentCard;

    private final List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();
    private @Nullable Consumer<Throwable> streamErrorHandler;
    private ClientConfig clientConfig = new ClientConfig.Builder().build();

    private final Map<Class<? extends ClientTransport>, ClientTransportConfig<? extends ClientTransport>> clientTransports = new LinkedHashMap<>();

    ClientBuilder(@NonNull AgentCard agentCard) {
        this.agentCard = agentCard;
    }

    public <T extends ClientTransport> ClientBuilder withTransport(Class<T> clazz, ClientTransportConfigBuilder<? extends ClientTransportConfig<T>, ?> configBuilder) {
        return withTransport(clazz, configBuilder.build());
    }

    public <T extends ClientTransport> ClientBuilder withTransport(Class<T> clazz, ClientTransportConfig<T> config) {
        clientTransports.put(clazz, config);

        return this;
    }

    public ClientBuilder addConsumer(BiConsumer<ClientEvent, AgentCard> consumer) {
        this.consumers.add(consumer);
        return this;
    }

    public ClientBuilder addConsumers(List<BiConsumer<ClientEvent, AgentCard>> consumers) {
        this.consumers.addAll(consumers);
        return this;
    }

    public ClientBuilder streamingErrorHandler(Consumer<Throwable> streamErrorHandler) {
        this.streamErrorHandler = streamErrorHandler;
        return this;
    }

    public ClientBuilder clientConfig(@NonNull ClientConfig clientConfig) {
        this.clientConfig = clientConfig;
        return this;
    }

    public Client build() throws A2AClientException {
        if (this.clientConfig == null) {
            this.clientConfig = new ClientConfig.Builder().build();
        }

        ClientTransport clientTransport = buildClientTransport();

        return new Client(agentCard, clientConfig, clientTransport, consumers, streamErrorHandler);
    }

    @SuppressWarnings("unchecked")
    private ClientTransport buildClientTransport() throws A2AClientException {
        // Get the preferred transport
        AgentInterface agentInterface = findBestClientTransport();

        // Get the transport provider associated with the protocol
        ClientTransportProvider clientTransportProvider = transportProviderRegistry.get(agentInterface.transport());
        if (clientTransportProvider == null) {
            throw new A2AClientException("No client available for " + agentInterface.transport());
        }
        Class<? extends ClientTransport> transportProtocolClass = clientTransportProvider.getTransportProtocolClass();

        // Retrieve the configuration associated with the preferred transport
        ClientTransportConfig<? extends ClientTransport> clientTransportConfig = clientTransports.get(transportProtocolClass);

        if (clientTransportConfig == null) {
            throw new A2AClientException("Missing required TransportConfig for " + agentInterface.transport());
        }

        return clientTransportProvider.create(clientTransportConfig, agentCard, agentInterface.url());
    }

    private Map<String, String> getServerPreferredTransports() {
        Map<String, String> serverPreferredTransports = new LinkedHashMap<>();
        serverPreferredTransports.put(agentCard.preferredTransport(), agentCard.url());
        if (agentCard.additionalInterfaces() != null) {
            for (AgentInterface agentInterface : agentCard.additionalInterfaces()) {
                serverPreferredTransports.putIfAbsent(agentInterface.transport(), agentInterface.url());
            }
        }
        return serverPreferredTransports;
    }

    private List<String> getClientPreferredTransports() {
        List<String> supportedClientTransports = new ArrayList<>();

        if (clientTransports.isEmpty()) {
            // default to JSONRPC if not specified
            supportedClientTransports.add(TransportProtocol.JSONRPC.asString());
        } else {
            clientTransports.forEach((aClass, clientTransportConfig) -> supportedClientTransports.add(transportProtocolMapping.get(aClass)));
        }
        return supportedClientTransports;
    }

    private AgentInterface findBestClientTransport() throws A2AClientException {
        // Retrieve transport supported by the A2A server
        Map<String, String> serverPreferredTransports = getServerPreferredTransports();

        // Retrieve transport configured for this client (using withTransport methods)
        List<String> clientPreferredTransports = getClientPreferredTransports();

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
            throw new A2AClientException("No compatible transport found");
        }
        if (! transportProviderRegistry.containsKey(transportProtocol)) {
            throw new A2AClientException("No client available for " + transportProtocol);
        }

        return new AgentInterface(transportProtocol, transportUrl);
    }
}
