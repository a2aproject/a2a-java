package io.a2a.transport.grpc.context;

import io.grpc.Context;

/**
 * Shared gRPC context keys for A2A protocol data.
 * 
 * These keys provide access to gRPC context information similar to
 * Python's grpc.aio.ServicerContext, enabling rich context access
 * in service method implementations.
 */
public final class GrpcContextKeys {
    
    /**
     * Context key for storing the X-A2A-Version header value.
     * Set by server interceptors and accessed by service handlers.
     */
    public static final Context.Key<String> VERSION_HEADER_KEY =
        Context.key("x-a2a-version");

    /**
     * Context key for storing the X-A2A-Extensions header value.
     * Set by server interceptors and accessed by service handlers.
     */
    public static final Context.Key<String> EXTENSIONS_HEADER_KEY =
        Context.key("x-a2a-extensions");
    
    /**
     * Context key for storing the complete gRPC Metadata object.
     * Provides access to all request headers and metadata.
     */
    public static final Context.Key<io.grpc.Metadata> METADATA_KEY = 
        Context.key("grpc-metadata");
    
    /**
     * Context key for storing the method name being called.
     * Equivalent to Python's context.method() functionality.
     */
    public static final Context.Key<String> METHOD_NAME_KEY = 
        Context.key("grpc-method-name");
    
    /**
     * Context key for storing the peer information.
     * Provides access to client connection details.
     */
    public static final Context.Key<String> PEER_INFO_KEY = 
        Context.key("grpc-peer-info");

    private GrpcContextKeys() {
        // Utility class
    }
}
