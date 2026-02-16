package io.a2a.server.apps.common;

import io.a2a.client.Client;
import io.a2a.client.ClientBuilder;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.grpc.GrpcTransport;
import io.a2a.client.transport.grpc.GrpcTransportConfigBuilder;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.a2a.client.transport.rest.RestTransport;
import io.a2a.client.transport.rest.RestTransportConfigBuilder;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Helper class for creating A2A clients for agent-to-agent communication testing.
 * Uses inner classes to avoid class loading issues when transport dependencies aren't on the classpath.
 */
public class AgentToAgentClientFactory {

    /**
     * Creates a client for the specified transport protocol.
     *
     * @param agentCard the agentcard of the remote server
     * @param transportProtocol the transport protocol to use
     * @param serverUrl         the server URL (e.g., "http://localhost:8081" or "localhost:9090")
     * @return configured client
     * @throws A2AClientException if client creation fails
     */
    public static Client createClient(AgentCard agentCard, TransportProtocol transportProtocol, String serverUrl)
            throws A2AClientException {
        ClientConfig clientConfig = ClientConfig.builder()
            .setStreaming(false)
            .build();

        ClientBuilder clientBuilder = Client.builder(agentCard)
            .clientConfig(clientConfig);

        ClientTransportEnhancer enhancer = switch (transportProtocol) {
            case JSONRPC -> new JsonRpcClientEnhancer();
            case GRPC -> new GrpcClientEnhancer();
            case HTTP_JSON -> new RestClientEnhancer();
            default -> throw new IllegalArgumentException("Unsupported transport: " + transportProtocol);
        };

        enhancer.enhance(clientBuilder, serverUrl);
        return clientBuilder.build();
    }

    /**
     * The implementations of this interface are needed to avoid ClassNotFoundErrors for client transports that are
     * not on the classpath.
     */
    interface ClientTransportEnhancer {
        void enhance(ClientBuilder clientBuilder, String serverUrl);
    }

    private static class GrpcClientEnhancer implements AgentToAgentClientFactory.ClientTransportEnhancer {
        @Override
        public void enhance(ClientBuilder clientBuilder, String serverUrl) {
            clientBuilder.withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(target -> {
                ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                return channel;
            }));
        }
    }

    private static class JsonRpcClientEnhancer implements AgentToAgentClientFactory.ClientTransportEnhancer {
        @Override
        public void enhance(ClientBuilder clientBuilder, String serverUrl) {
            clientBuilder.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder());
        }
    }

    private static class RestClientEnhancer implements AgentToAgentClientFactory.ClientTransportEnhancer {
        @Override
        public void enhance(ClientBuilder clientBuilder, String serverUrl) {
            clientBuilder.withTransport(RestTransport.class, new RestTransportConfigBuilder());
        }
    }
}

