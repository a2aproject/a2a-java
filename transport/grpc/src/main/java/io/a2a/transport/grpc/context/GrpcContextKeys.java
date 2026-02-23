package io.a2a.transport.grpc.context;


import java.util.Map;

import io.a2a.spec.A2AMethods;
import io.grpc.Context;

/**
 * Shared gRPC context keys for A2A protocol data.
 *
 * <p>These keys provide access to gRPC context information stored in
 * {@link io.grpc.Context}, enabling rich context access in service method
 * implementations similar to Python's {@code grpc.aio.ServicerContext}.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public void processRequest(ServerCallContext context) {
 *     // Access gRPC context information
 *     Context grpcContext = Context.current();
 *     String method = GrpcContextKeys.GRPC_METHOD_NAME_KEY.get(grpcContext);
 *     Metadata metadata = GrpcContextKeys.METADATA_KEY.get(grpcContext);
 *     String peerInfo = GrpcContextKeys.PEER_INFO_KEY.get(grpcContext);
 *
 *     // Access A2A protocol headers
 *     String version = GrpcContextKeys.VERSION_HEADER_KEY.get(grpcContext);
 *     String extensions = GrpcContextKeys.EXTENSIONS_HEADER_KEY.get(grpcContext);
 * }
 * }</pre>
 *
 * <h2>Context Population</h2>
 * <p>These context keys are populated by server interceptors (typically
 * {@code A2AExtensionsInterceptor}) that capture request metadata and store
 * it in the gRPC context before service methods are called.
 *
 * @see io.grpc.Context
 * @see io.grpc.Metadata
 * @see io.a2a.server.ServerCallContext
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

    /**
     * Mapping from gRPC method names to A2A protocol method names.
     *
     * <p>This mapping translates gRPC protobuf method names (e.g., "SendMessage")
     * to A2A protocol method names (e.g., "sendMessage") for consistent method
     * identification across all transports.
     *
     * <p><b>Method Mappings:</b>
     * <ul>
     *   <li>SendMessage → sendMessage</li>
     *   <li>SendStreamingMessage → sendStreamingMessage</li>
     *   <li>GetTask → getTask</li>
     *   <li>ListTask → listTasks</li>
     *   <li>CancelTask → cancelTask</li>
     *   <li>SubscribeToTask → subscribeToTask</li>
     *   <li>CreateTaskPushNotification → setTaskPushNotificationConfig</li>
     *   <li>GetTaskPushNotification → getTaskPushNotificationConfig</li>
     *   <li>ListTaskPushNotification → listTaskPushNotificationConfig</li>
     *   <li>DeleteTaskPushNotification → deleteTaskPushNotificationConfig</li>
     * </ul>
     *
     * @see io.a2a.spec.A2AMethods
     */
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
