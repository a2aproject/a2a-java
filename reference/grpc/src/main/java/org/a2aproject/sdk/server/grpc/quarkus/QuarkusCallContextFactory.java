package org.a2aproject.sdk.server.grpc.quarkus;

import static org.a2aproject.sdk.server.ServerCallContext.TRANSPORT_KEY;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.stub.StreamObserver;
import io.quarkus.security.identity.SecurityIdentity;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.AuthenticatedUser;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.auth.User;
import org.a2aproject.sdk.server.extensions.A2AExtensions;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.a2aproject.sdk.transport.grpc.context.GrpcContextKeys;
import org.a2aproject.sdk.transport.grpc.handler.CallContextFactory;

@ApplicationScoped
public class QuarkusCallContextFactory implements CallContextFactory {

    @Inject
    private Instance<SecurityIdentity> securityIdentityInstance;

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
            io.grpc.Metadata grpcMetadata = GrpcContextKeys.METADATA_KEY.get(currentContext);
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
            String methodName = GrpcContextKeys.GRPC_METHOD_NAME_KEY.get(currentContext);
            if (methodName != null) {
                state.put("grpc_method_name", methodName);
            }
            String peerInfo = GrpcContextKeys.PEER_INFO_KEY.get(currentContext);
            if (peerInfo != null) {
                state.put("grpc_peer_info", peerInfo);
            }
        }

        String requestedVersion = null;
        try {
            requestedVersion = GrpcContextKeys.VERSION_HEADER_KEY.get();
        } catch (Exception e) {
            // Context not available
        }

        Set<String> requestedExtensions = new HashSet<>();
        try {
            String extensionsHeader = GrpcContextKeys.EXTENSIONS_HEADER_KEY.get();
            if (extensionsHeader != null) {
                requestedExtensions = A2AExtensions.getRequestedExtensions(List.of(extensionsHeader));
            }
        } catch (Exception e) {
            // Context not available
        }

        return new ServerCallContext(user, state, requestedExtensions, requestedVersion);
    }
}
