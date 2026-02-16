package io.a2a.server.apps.common;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import io.a2a.client.Client;
import io.a2a.client.ClientBuilder;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.grpc.GrpcTransport;
import io.a2a.client.transport.grpc.GrpcTransportConfigBuilder;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.a2a.client.transport.rest.RestTransport;
import io.a2a.client.transport.rest.RestTransportConfigBuilder;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Task;
import io.a2a.spec.TransportProtocol;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Helper class for creating A2A clients for agent-to-agent communication testing.
 * Uses inner classes to avoid class loading issues when transport dependencies aren't on the classpath.
 */
public class AgentToAgentClientFactory {

    /**
     * Creates a BiConsumer that captures the final task state.
     * This utility method is used by both test classes and agent executors to avoid code duplication.
     *
     * @param taskRef the AtomicReference to store the final task
     * @param latch the CountDownLatch to signal completion
     * @return a BiConsumer that captures completed tasks
     */
    public static BiConsumer<ClientEvent, AgentCard> createTaskCaptureConsumer(
            AtomicReference<Task> taskRef, CountDownLatch latch) {
        return (event, agentCard) -> {
            Task task = null;
            if (event instanceof TaskEvent taskEvent) {
                task = taskEvent.getTask();
            } else if (event instanceof TaskUpdateEvent taskUpdateEvent) {
                task = taskUpdateEvent.getTask();
            }

            if (task != null && task.status().state().isFinal()) {
                taskRef.set(task);
                latch.countDown();
            }
        };
    }

    /**
     * Creates a client for the specified transport protocol.
     * The agent card parameter already contains the correct local endpoint URLs
     * configured by the test's AgentCardProducer.
     *
     * @param agentCard the agent card with correct local endpoints
     * @param transportProtocol the transport protocol to use
     * @return configured client
     * @throws A2AClientException if client creation fails
     */
    public static Client createClient(AgentCard agentCard, TransportProtocol transportProtocol)
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

        enhancer.enhance(clientBuilder);
        return clientBuilder.build();
    }

    /**
     * The implementations of this interface are needed to avoid ClassNotFoundErrors for client transports that are
     * not on the classpath.
     */
    interface ClientTransportEnhancer {
        void enhance(ClientBuilder clientBuilder);
    }

    private static class GrpcClientEnhancer implements AgentToAgentClientFactory.ClientTransportEnhancer {
        @Override
        public void enhance(ClientBuilder clientBuilder) {
            clientBuilder.withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(target -> {
                ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                return channel;
            }));
        }
    }

    private static class JsonRpcClientEnhancer implements AgentToAgentClientFactory.ClientTransportEnhancer {
        @Override
        public void enhance(ClientBuilder clientBuilder) {
            clientBuilder.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder());
        }
    }

    private static class RestClientEnhancer implements AgentToAgentClientFactory.ClientTransportEnhancer {
        @Override
        public void enhance(ClientBuilder clientBuilder) {
            clientBuilder.withTransport(RestTransport.class, new RestTransportConfigBuilder());
        }
    }
}

