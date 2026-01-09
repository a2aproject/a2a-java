package io.a2a.server.grpc.quarkus;

import jakarta.enterprise.context.ApplicationScoped;

import io.a2a.common.A2AHeaders;
import io.a2a.transport.grpc.context.GrpcContextKeys;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

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
public class A2AExtensionsInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall,
            Metadata metadata,
            ServerCallHandler<ReqT, RespT> serverCallHandler) {

        // Extract A2A protocol version header
        Metadata.Key<String> versionKey =
            Metadata.Key.of(A2AHeaders.X_A2A_VERSION, Metadata.ASCII_STRING_MARSHALLER);
        String version = metadata.get(versionKey);

        // Extract A2A extensions header
        Metadata.Key<String> extensionsKey =
            Metadata.Key.of(A2AHeaders.X_A2A_EXTENSIONS, Metadata.ASCII_STRING_MARSHALLER);
        String extensions = metadata.get(extensionsKey);

        // Create enhanced context with rich information (equivalent to Python's ServicerContext)
        Context context = Context.current()
                // Store complete metadata for full header access
                .withValue(GrpcContextKeys.METADATA_KEY, metadata)
                // Store Grpc method name 
                .withValue(GrpcContextKeys.GRPC_METHOD_NAME_KEY, serverCall.getMethodDescriptor().getFullMethodName())
                // Store method name (equivalent to Python's context.method())
                .withValue(GrpcContextKeys.METHOD_NAME_KEY, GrpcContextKeys.METHOD_MAPPING.get(serverCall.getMethodDescriptor().getBareMethodName()))
                // Store peer information for client connection details
                .withValue(GrpcContextKeys.PEER_INFO_KEY, getPeerInfo(serverCall));

        // Store A2A version if present
        if (version != null) {
            context = context.withValue(GrpcContextKeys.VERSION_HEADER_KEY, version);
        }

        // Store A2A extensions if present
        if (extensions != null) {
            context = context.withValue(GrpcContextKeys.EXTENSIONS_HEADER_KEY, extensions);
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
