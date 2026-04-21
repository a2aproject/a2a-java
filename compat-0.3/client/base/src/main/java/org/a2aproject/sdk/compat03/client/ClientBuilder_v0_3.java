package org.a2aproject.sdk.compat03.client;

import org.a2aproject.sdk.compat03.client.config.ClientConfig_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfig_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportProvider_v0_3;
import org.a2aproject.sdk.compat03.spec.A2AClientException_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentInterface_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;

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

public class ClientBuilder_v0_3 {

    private static final Map<String, ClientTransportProvider_v0_3<? extends ClientTransport_v0_3, ? extends ClientTransportConfig_v0_3<?>>> transportProviderRegistry = new HashMap<>();
    private static final Map<Class<? extends ClientTransport_v0_3>, String> transportProtocolMapping = new HashMap<>();

    static {
        ServiceLoader<ClientTransportProvider_v0_3> loader = ServiceLoader.load(ClientTransportProvider_v0_3.class);
        for (ClientTransportProvider_v0_3<?, ?> transport : loader) {
            transportProviderRegistry.put(transport.getTransportProtocol(), transport);
            transportProtocolMapping.put(transport.getTransportProtocolClass(), transport.getTransportProtocol());
        }
    }

    private final AgentCard_v0_3 agentCard;

    private final List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers = new ArrayList<>();
    private @Nullable Consumer<Throwable> streamErrorHandler;
    private ClientConfig_v0_3 clientConfig = new ClientConfig_v0_3.Builder().build();

    private final Map<Class<? extends ClientTransport_v0_3>, ClientTransportConfig_v0_3<? extends ClientTransport_v0_3>> clientTransports = new LinkedHashMap<>();

    ClientBuilder_v0_3(@NonNull AgentCard_v0_3 agentCard) {
        this.agentCard = agentCard;
    }

    public <T extends ClientTransport_v0_3> ClientBuilder_v0_3 withTransport(Class<T> clazz, ClientTransportConfigBuilder_v0_3<? extends ClientTransportConfig_v0_3<T>, ?> configBuilder) {
        return withTransport(clazz, configBuilder.build());
    }

    public <T extends ClientTransport_v0_3> ClientBuilder_v0_3 withTransport(Class<T> clazz, ClientTransportConfig_v0_3<T> config) {
        clientTransports.put(clazz, config);

        return this;
    }

    public ClientBuilder_v0_3 addConsumer(BiConsumer<ClientEvent_v0_3, AgentCard_v0_3> consumer) {
        this.consumers.add(consumer);
        return this;
    }

    public ClientBuilder_v0_3 addConsumers(List<BiConsumer<ClientEvent_v0_3, AgentCard_v0_3>> consumers) {
        this.consumers.addAll(consumers);
        return this;
    }

    public ClientBuilder_v0_3 streamingErrorHandler(Consumer<Throwable> streamErrorHandler) {
        this.streamErrorHandler = streamErrorHandler;
        return this;
    }

    public ClientBuilder_v0_3 clientConfig(@NonNull ClientConfig_v0_3 clientConfig) {
        this.clientConfig = clientConfig;
        return this;
    }

    public Client_v0_3 build() throws A2AClientException_v0_3 {
        if (this.clientConfig == null) {
            this.clientConfig = new ClientConfig_v0_3.Builder().build();
        }

        ClientTransport_v0_3 clientTransport = buildClientTransport();

        return new Client_v0_3(agentCard, clientConfig, clientTransport, consumers, streamErrorHandler);
    }

    @SuppressWarnings("unchecked")
    private ClientTransport_v0_3 buildClientTransport() throws A2AClientException_v0_3 {
        // Get the preferred transport
        AgentInterface_v0_3 agentInterface = findBestClientTransport();

        // Get the transport provider associated with the protocol
        ClientTransportProvider_v0_3 clientTransportProvider = transportProviderRegistry.get(agentInterface.transport());
        if (clientTransportProvider == null) {
            throw new A2AClientException_v0_3("No client available for " + agentInterface.transport());
        }
        Class<? extends ClientTransport_v0_3> transportProtocolClass = clientTransportProvider.getTransportProtocolClass();

        // Retrieve the configuration associated with the preferred transport
        ClientTransportConfig_v0_3<? extends ClientTransport_v0_3> clientTransportConfig = clientTransports.get(transportProtocolClass);

        if (clientTransportConfig == null) {
            throw new A2AClientException_v0_3("Missing required TransportConfig for " + agentInterface.transport());
        }

        return clientTransportProvider.create(clientTransportConfig, agentCard, agentInterface.url());
    }

    private Map<String, String> getServerPreferredTransports() {
        Map<String, String> serverPreferredTransports = new LinkedHashMap<>();
        serverPreferredTransports.put(agentCard.preferredTransport(), agentCard.url());
        if (agentCard.additionalInterfaces() != null) {
            for (AgentInterface_v0_3 agentInterface : agentCard.additionalInterfaces()) {
                serverPreferredTransports.putIfAbsent(agentInterface.transport(), agentInterface.url());
            }
        }
        return serverPreferredTransports;
    }

    private List<String> getClientPreferredTransports() {
        List<String> supportedClientTransports = new ArrayList<>();

        if (clientTransports.isEmpty()) {
            // default to JSONRPC if not specified
            supportedClientTransports.add(TransportProtocol_v0_3.JSONRPC.asString());
        } else {
            clientTransports.forEach((aClass, clientTransportConfig) -> supportedClientTransports.add(transportProtocolMapping.get(aClass)));
        }
        return supportedClientTransports;
    }

    private AgentInterface_v0_3 findBestClientTransport() throws A2AClientException_v0_3 {
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
            throw new A2AClientException_v0_3("No compatible transport found");
        }
        if (! transportProviderRegistry.containsKey(transportProtocol)) {
            throw new A2AClientException_v0_3("No client available for " + transportProtocol);
        }

        return new AgentInterface_v0_3(transportProtocol, transportUrl);
    }
}
