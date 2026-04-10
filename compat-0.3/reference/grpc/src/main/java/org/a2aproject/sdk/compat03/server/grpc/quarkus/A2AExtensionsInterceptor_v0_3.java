package org.a2aproject.sdk.compat03.server.grpc.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.a2aproject.sdk.compat03.common.A2AHeaders_v0_3;
import org.a2aproject.sdk.compat03.transport.grpc.context.GrpcContextKeys_v0_3;

/**
 * gRPC server interceptor that captures request metadata and context information,
 * providing equivalent functionality to Python's grpc.aio.ServicerContext.
 *
 * This interceptor:
 * - Extracts A2A extension headers from incoming requests
 * - Captures ServerCall and Metadata for rich context access
 * - Stores context information in gRPC Context for service method access
 * - Provides proper equivalence to Python's ServicerContext
 */
@ApplicationScoped
public class A2AExtensionsInterceptor_v0_3 implements ServerInterceptor {


    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall,
            Metadata metadata,
            ServerCallHandler<ReqT, RespT> serverCallHandler) {

        // Extract A2A extensions header
        Metadata.Key<String> extensionsKey =
            Metadata.Key.of(A2AHeaders_v0_3.X_A2A_EXTENSIONS, Metadata.ASCII_STRING_MARSHALLER);
        String extensions = metadata.get(extensionsKey);

        // Create enhanced context with rich information (equivalent to Python's ServicerContext)
        Context context = Context.current()
            // Store complete metadata for full header access
            .withValue(GrpcContextKeys_v0_3.METADATA_KEY, metadata)
            // Store method name (equivalent to Python's context.method())
            .withValue(GrpcContextKeys_v0_3.METHOD_NAME_KEY, serverCall.getMethodDescriptor().getFullMethodName())
            // Store peer information for client connection details
            .withValue(GrpcContextKeys_v0_3.PEER_INFO_KEY, getPeerInfo(serverCall));

        // Store A2A extensions if present
        if (extensions != null) {
            context = context.withValue(GrpcContextKeys_v0_3.EXTENSIONS_HEADER_KEY, extensions);
        }

        // Proceed with the call in the enhanced context
        return Contexts.interceptCall(context, serverCall, metadata, serverCallHandler);
    }

    /**
     * Safely extracts peer information from the ServerCall.
     *
     * @param serverCall the gRPC ServerCall
     * @return peer information string, or "unknown" if not available
     */
    private String getPeerInfo(ServerCall<?, ?> serverCall) {
        try {
            Object remoteAddr = serverCall.getAttributes().get(io.grpc.Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
            return remoteAddr != null ? remoteAddr.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
