package io.a2a.client.transport;

import java.util.List;

import io.a2a.client.ClientCallInterceptor;
import io.a2a.client.ClientConfig;
import io.a2a.http.A2AHttpClient;
import io.a2a.spec.AgentCard;
import io.a2a.spec.TransportProtocol;
import io.grpc.Channel;

import io.grpc.ManagedChannelBuilder;

/**
 * Provider for gRPC transport implementation.
 */
public class GrpcTransportProvider implements ClientTransportProvider {

    @Override
    public ClientTransport create(ClientConfig clientConfig, AgentCard agentCard,
                                  String agentUrl, List<ClientCallInterceptor> interceptors) {
        // not making use of the interceptors for gRPC for now
        return new GrpcTransport(clientConfig.getChannel(), agentCard);
    }

    @Override
    public String getTransportProtocol() {
        return TransportProtocol.GRPC.asString();
    }

}
