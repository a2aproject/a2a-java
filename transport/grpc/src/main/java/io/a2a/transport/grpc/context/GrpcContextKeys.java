package io.a2a.transport.grpc.context;

import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.ListTasksRequest;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.TaskResubscriptionRequest;
import io.grpc.Context;
import java.util.Map;

/**
 * Shared gRPC context keys for A2A protocol data.
 *
 * These keys provide access to gRPC context information similar to
 * Python's grpc.aio.ServicerContext, enabling rich context access
 * in service method implementations.
 */
public final class GrpcContextKeys {

    /**
     * Context key for storing the X-A2A-Extensions header value.
     * Set by server interceptors and accessed by service handlers.
     */
    public static final Context.Key<String> EXTENSIONS_HEADER_KEY
            = Context.key("x-a2a-extensions");

    /**
     * Context key for storing the complete gRPC Metadata object.
     * Provides access to all request headers and metadata.
     */
    public static final Context.Key<io.grpc.Metadata> METADATA_KEY
            = Context.key("grpc-metadata");

    /**
     * Context key for storing the method name being called.
     * Equivalent to Python's context.method() functionality.
     */
    public static final Context.Key<String> GRPC_METHOD_NAME_KEY
            = Context.key("grpc-method-name");

    /**
     * Context key for storing the method name being called.
     * Equivalent to Python's context.method() functionality.
     */
    public static final Context.Key<String> METHOD_NAME_KEY
            = Context.key("method");

    /**
     * Context key for storing the peer information.
     * Provides access to client connection details.
     */
    public static final Context.Key<String> PEER_INFO_KEY
            = Context.key("grpc-peer-info");

    public static final Map<String, String> METHOD_MAPPING = Map.of(
            "SendMessage", SendMessageRequest.METHOD,
            "SendStreamingMessage", SendStreamingMessageRequest.METHOD,
            "GetTask", GetTaskRequest.METHOD,
            "ListTask", ListTasksRequest.METHOD,
            "CancelTask", CancelTaskRequest.METHOD,
            "TaskSubscription", TaskResubscriptionRequest.METHOD,
            "CreateTaskPushNotification", SetTaskPushNotificationConfigRequest.METHOD,
            "GetTaskPushNotification", GetTaskPushNotificationConfigRequest.METHOD,
            "ListTaskPushNotification", ListTaskPushNotificationConfigRequest.METHOD,
            "DeleteTaskPushNotification", DeleteTaskPushNotificationConfigRequest.METHOD);

    private GrpcContextKeys() {
        // Utility class
    }
}
