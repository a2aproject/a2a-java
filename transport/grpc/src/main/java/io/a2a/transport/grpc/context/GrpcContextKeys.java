package io.a2a.transport.grpc.context;


import java.util.Map;

import io.a2a.spec.A2AMethods;
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
    public static final Context.Key<String> GRPC_METHOD_NAME_KEY = 
        Context.key("grpc-method-name");
    
    /**
     * Context key for storing the method name being called.
     * Equivalent to Python's context.method() functionality.
     */
    public static final Context.Key<String> METHOD_NAME_KEY = 
            Context.key("method");

    /**
     * Context key for storing the peer information.
     * Provides access to client connection details.
     */
    public static final Context.Key<String> PEER_INFO_KEY = 
        Context.key("grpc-peer-info");

    public static final Map<String, String> METHOD_MAPPING = Map.of(
            "SendMessage", A2AMethods.SEND_MESSAGE_METHOD,
            "SendStreamingMessage", A2AMethods.SEND_STREAMING_MESSAGE_METHOD,
            "GetTask", A2AMethods.GET_TASK_METHOD,
            "ListTask", A2AMethods.LIST_TASK_METHOD,
            "CancelTask", A2AMethods.CANCEL_TASK_METHOD,
            "SubscribeToTask", A2AMethods.SUBSCRIBE_TO_TASK_METHOD,
            "CreateTaskPushNotification", A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
            "GetTaskPushNotification", A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
            "ListTaskPushNotification", A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD,
            "DeleteTaskPushNotification", A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

    private GrpcContextKeys() {
        // Utility class
    }
}
