package org.a2aproject.sdk.server.apps.common;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.ClientEvent;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.TaskUpdateEvent;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.rest.RestTransport;
import org.a2aproject.sdk.client.transport.rest.RestTransportConfigBuilder;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TransportProtocol;
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

