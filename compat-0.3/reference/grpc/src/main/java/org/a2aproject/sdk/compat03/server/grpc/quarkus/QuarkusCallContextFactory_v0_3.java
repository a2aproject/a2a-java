package org.a2aproject.sdk.compat03.server.grpc.quarkus;

import static org.a2aproject.sdk.server.ServerCallContext.TRANSPORT_KEY;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;
import io.quarkus.security.identity.SecurityIdentity;
import org.a2aproject.sdk.compat03.conversion.A2AProtocol_v0_3;
import org.a2aproject.sdk.compat03.transport.grpc.context.GrpcContextKeys_v0_3;
import org.a2aproject.sdk.compat03.transport.grpc.handler.CallContextFactory_v0_3;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.AuthenticatedUser;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.auth.User;
import org.a2aproject.sdk.spec.TransportProtocol;

@ApplicationScoped
public class QuarkusCallContextFactory_v0_3 implements CallContextFactory_v0_3 {

    @Inject
    Instance<SecurityIdentity> securityIdentityInstance;

    @Override
    public <V> ServerCallContext create(StreamObserver<V> responseObserver) {
        User user;
        if (securityIdentityInstance.isResolvable()) {
            SecurityIdentity securityIdentity = securityIdentityInstance.get();
            if (!securityIdentity.isAnonymous()) {
                user = new AuthenticatedUser(securityIdentity.getPrincipal().getName());
            } else {
                user = UnauthenticatedUser.INSTANCE;
            }
        } else {
            user = UnauthenticatedUser.INSTANCE;
        }

        Map<String, Object> state = new HashMap<>();
        state.put(TRANSPORT_KEY, TransportProtocol.GRPC);
        state.put("grpc_response_observer", responseObserver);

        Context currentContext = Context.current();
        if (currentContext != null) {
            state.put("grpc_context", currentContext);
            io.grpc.Metadata grpcMetadata = GrpcContextKeys_v0_3.METADATA_KEY.get(currentContext);
            if (grpcMetadata != null) {
                state.put("grpc_metadata", grpcMetadata);
                Map<String, String> headers = new HashMap<>();
                for (String key : grpcMetadata.keys()) {
                    if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) {
                        continue;
                    }
                    headers.put(key, grpcMetadata.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)));
                }
                state.put("headers", headers);
            }
            String methodName = GrpcContextKeys_v0_3.METHOD_NAME_KEY.get(currentContext);
            if (methodName != null) {
                state.put("grpc_method_name", methodName);
            }
            String peerInfo = GrpcContextKeys_v0_3.PEER_INFO_KEY.get(currentContext);
            if (peerInfo != null) {
                state.put("grpc_peer_info", peerInfo);
            }
        }

        return new ServerCallContext(user, state, new HashSet<>(), A2AProtocol_v0_3.PROTOCOL_VERSION);
    }
}
