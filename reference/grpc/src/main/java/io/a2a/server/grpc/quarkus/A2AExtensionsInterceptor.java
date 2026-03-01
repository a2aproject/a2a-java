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
 * providing equivalent functionality to Python's {@code grpc.aio.ServicerContext}.
 *
 * <p>This interceptor executes before service methods are invoked, extracting A2A protocol
 * headers and request metadata from the gRPC call and storing them in the gRPC {@link Context}
 * for access by {@link io.a2a.transport.grpc.handler.GrpcHandler} and agent implementations.
 *
 * <h2>Captured Information</h2>
 * <ul>
 *   <li><b>A2A Protocol Version</b>: {@code X-A2A-Version} header</li>
 *   <li><b>A2A Extensions</b>: {@code X-A2A-Extensions} header</li>
 *   <li><b>Complete Metadata</b>: All request headers via {@link io.grpc.Metadata}</li>
 *   <li><b>Method Name</b>: gRPC method being invoked</li>
 *   <li><b>Peer Information</b>: Client connection details</li>
 * </ul>
 *
 * <h2>Context Storage</h2>
 * <p>All captured information is stored in the gRPC {@link Context} using keys from
 * {@link io.a2a.transport.grpc.context.GrpcContextKeys}:
 * <ul>
 *   <li>{@link io.a2a.transport.grpc.context.GrpcContextKeys#VERSION_HEADER_KEY VERSION_HEADER_KEY}</li>
 *   <li>{@link io.a2a.transport.grpc.context.GrpcContextKeys#EXTENSIONS_HEADER_KEY EXTENSIONS_HEADER_KEY}</li>
 *   <li>{@link io.a2a.transport.grpc.context.GrpcContextKeys#METADATA_KEY METADATA_KEY}</li>
 *   <li>{@link io.a2a.transport.grpc.context.GrpcContextKeys#GRPC_METHOD_NAME_KEY GRPC_METHOD_NAME_KEY}</li>
 *   <li>{@link io.a2a.transport.grpc.context.GrpcContextKeys#METHOD_NAME_KEY METHOD_NAME_KEY}</li>
 *   <li>{@link io.a2a.transport.grpc.context.GrpcContextKeys#PEER_INFO_KEY PEER_INFO_KEY}</li>
 * </ul>
 *
 * <h2>CDI Integration</h2>
 * <p>This interceptor is registered as an {@code @ApplicationScoped} CDI bean and automatically
 * applied to gRPC services through Quarkus gRPC's {@code @RegisterInterceptor} annotation.
 *
 * <h2>Python Equivalence</h2>
 * <p>This interceptor provides functionality equivalent to Python's {@code grpc.aio.ServicerContext},
 * enabling Java handlers to access the same rich context information available in Python implementations:
 * <ul>
 *   <li>{@code context.invocation_metadata()} → {@link io.grpc.Metadata}</li>
 *   <li>{@code context.method()} → Method name via {@code GRPC_METHOD_NAME_KEY}</li>
 *   <li>{@code context.peer()} → Peer info via {@code PEER_INFO_KEY}</li>
 * </ul>
 *
 * @see io.a2a.transport.grpc.context.GrpcContextKeys
 * @see io.a2a.transport.grpc.handler.GrpcHandler
 * @see io.grpc.ServerInterceptor
 */
@ApplicationScoped
public class A2AExtensionsInterceptor implements ServerInterceptor {

    /**
     * Intercepts incoming gRPC calls to capture metadata and context information.
     *
     * <p>This method extracts A2A protocol headers and request metadata, stores them
     * in the gRPC {@link Context}, and proceeds with the call in the enhanced context.
     *
     * <p><b>Extraction Process:</b>
     * <ol>
     *   <li>Extract {@code X-A2A-Version} header from metadata</li>
     *   <li>Extract {@code X-A2A-Extensions} header from metadata</li>
     *   <li>Capture complete {@link Metadata} object</li>
     *   <li>Capture gRPC method name from {@link ServerCall}</li>
     *   <li>Map gRPC method to A2A protocol method name</li>
     *   <li>Extract peer information from server call attributes</li>
     *   <li>Create enhanced {@link Context} with all captured information</li>
     *   <li>Proceed with call in enhanced context</li>
     * </ol>
     *
     * @param <ReqT> the request message type
     * @param <RespT> the response message type
     * @param serverCall the gRPC server call
     * @param metadata the request metadata (headers)
     * @param serverCallHandler the next handler in the interceptor chain
     * @return a listener for the server call
     */
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
