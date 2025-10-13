package io.a2a.transport.grpc.handler;

import static io.a2a.grpc.utils.ProtoUtils.FromProto;
import static io.a2a.grpc.utils.ProtoUtils.ToProto;

import jakarta.enterprise.inject.Vetoed;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import io.grpc.Context;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import com.google.protobuf.Empty;
import io.a2a.common.A2AErrorMessages;
import io.a2a.grpc.A2AServiceGrpc;
import io.a2a.grpc.StreamResponse;
import io.a2a.server.AgentCardValidator;
import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.auth.User;
import io.a2a.server.extensions.A2AExtensions;
import io.a2a.transport.grpc.context.GrpcContextKeys;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.AgentCard;
import io.a2a.spec.ContentTypeNotSupportedError;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidAgentResponseError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.UnsupportedOperationError;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@Vetoed
public abstract class GrpcHandler extends A2AServiceGrpc.A2AServiceImplBase {

    // Hook so testing can wait until streaming subscriptions are established.
    // Without this we get intermittent failures
    private static volatile Runnable streamingSubscribedRunnable;

    private AtomicBoolean initialised = new AtomicBoolean(false);

    private static final Logger LOGGER = Logger.getLogger(GrpcHandler.class.getName());

    public GrpcHandler() {

    }

    @Override
    public void sendMessage(io.a2a.grpc.SendMessageRequest request,
                           StreamObserver<io.a2a.grpc.SendMessageResponse> responseObserver) {
        try {
            ServerCallContext context = createCallContext(responseObserver);
            MessageSendParams params = FromProto.messageSendParams(request);
            EventKind taskOrMessage = getRequestHandler().onMessageSend(params, context);
            io.a2a.grpc.SendMessageResponse response = ToProto.taskOrMessage(taskOrMessage);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (SecurityException e) {
            handleSecurityException(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void getTask(io.a2a.grpc.GetTaskRequest request,
                       StreamObserver<io.a2a.grpc.Task> responseObserver) {
        try {
            ServerCallContext context = createCallContext(responseObserver);
            TaskQueryParams params = FromProto.taskQueryParams(request);
            Task task = getRequestHandler().onGetTask(params, context);
            if (task != null) {
                responseObserver.onNext(ToProto.task(task));
                responseObserver.onCompleted();
            } else {
                handleError(responseObserver, new TaskNotFoundError());
            }
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (SecurityException e) {
            handleSecurityException(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void cancelTask(io.a2a.grpc.CancelTaskRequest request,
                          StreamObserver<io.a2a.grpc.Task> responseObserver) {
        try {
            ServerCallContext context = createCallContext(responseObserver);
            TaskIdParams params = FromProto.taskIdParams(request);
            Task task = getRequestHandler().onCancelTask(params, context);
            if (task != null) {
                responseObserver.onNext(ToProto.task(task));
                responseObserver.onCompleted();
            } else {
                handleError(responseObserver, new TaskNotFoundError());
            }
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (SecurityException e) {
            handleSecurityException(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void createTaskPushNotificationConfig(io.a2a.grpc.CreateTaskPushNotificationConfigRequest request,
                                               StreamObserver<io.a2a.grpc.TaskPushNotificationConfig> responseObserver) {
        if (!getAgentCardInternal().capabilities().pushNotifications()) {
            handleError(responseObserver, new PushNotificationNotSupportedError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            TaskPushNotificationConfig config = FromProto.taskPushNotificationConfig(request);
            TaskPushNotificationConfig responseConfig = getRequestHandler().onSetTaskPushNotificationConfig(config, context);
            responseObserver.onNext(ToProto.taskPushNotificationConfig(responseConfig));
            responseObserver.onCompleted();
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (SecurityException e) {
            handleSecurityException(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void getTaskPushNotificationConfig(io.a2a.grpc.GetTaskPushNotificationConfigRequest request,
                                            StreamObserver<io.a2a.grpc.TaskPushNotificationConfig> responseObserver) {
        if (!getAgentCardInternal().capabilities().pushNotifications()) {
            handleError(responseObserver, new PushNotificationNotSupportedError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            GetTaskPushNotificationConfigParams params = FromProto.getTaskPushNotificationConfigParams(request);
            TaskPushNotificationConfig config = getRequestHandler().onGetTaskPushNotificationConfig(params, context);
            responseObserver.onNext(ToProto.taskPushNotificationConfig(config));
            responseObserver.onCompleted();
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (SecurityException e) {
            handleSecurityException(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void listTaskPushNotificationConfig(io.a2a.grpc.ListTaskPushNotificationConfigRequest request,
                                             StreamObserver<io.a2a.grpc.ListTaskPushNotificationConfigResponse> responseObserver) {
        if (!getAgentCardInternal().capabilities().pushNotifications()) {
            handleError(responseObserver, new PushNotificationNotSupportedError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            ListTaskPushNotificationConfigParams params = FromProto.listTaskPushNotificationConfigParams(request);
            List<TaskPushNotificationConfig> configList = getRequestHandler().onListTaskPushNotificationConfig(params, context);
            io.a2a.grpc.ListTaskPushNotificationConfigResponse.Builder responseBuilder =
                io.a2a.grpc.ListTaskPushNotificationConfigResponse.newBuilder();
            for (TaskPushNotificationConfig config : configList) {
                responseBuilder.addConfigs(ToProto.taskPushNotificationConfig(config));
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (SecurityException e) {
            handleSecurityException(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void sendStreamingMessage(io.a2a.grpc.SendMessageRequest request,
                                     StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
        if (!getAgentCardInternal().capabilities().streaming()) {
            handleError(responseObserver, new InvalidRequestError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            MessageSendParams params = FromProto.messageSendParams(request);
            Flow.Publisher<StreamingEventKind> publisher = getRequestHandler().onMessageSendStream(params, context);
            convertToStreamResponse(publisher, responseObserver);
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (SecurityException e) {
            handleSecurityException(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void taskSubscription(io.a2a.grpc.TaskSubscriptionRequest request,
                                 StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
        if (!getAgentCardInternal().capabilities().streaming()) {
            handleError(responseObserver, new InvalidRequestError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            TaskIdParams params = FromProto.taskIdParams(request);
            Flow.Publisher<StreamingEventKind> publisher = getRequestHandler().onResubscribeToTask(params, context);
            convertToStreamResponse(publisher, responseObserver);
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (SecurityException e) {
            handleSecurityException(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    private void convertToStreamResponse(Flow.Publisher<StreamingEventKind> publisher,
                                         StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
        CompletableFuture.runAsync(() -> {
            publisher.subscribe(new Flow.Subscriber<StreamingEventKind>() {
                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    subscription.request(1);

                    // Notify tests that we are subscribed
                    Runnable runnable = streamingSubscribedRunnable;
                    if (runnable != null) {
                        runnable.run();
                    }
                }

                @Override
                public void onNext(StreamingEventKind event) {
                    StreamResponse response = ToProto.streamResponse(event);
                    responseObserver.onNext(response);
                    if (response.hasStatusUpdate() && response.getStatusUpdate().getFinal()) {
                        responseObserver.onCompleted();
                    } else {
                        subscription.request(1);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    if (throwable instanceof JSONRPCError jsonrpcError) {
                        handleError(responseObserver, jsonrpcError);
                    } else {
                        handleInternalError(responseObserver, throwable);
                    }
                    responseObserver.onCompleted();
                }

                @Override
                public void onComplete() {
                    responseObserver.onCompleted();
                }
            });
        }, getExecutor());
    }

    @Override
    public void getAgentCard(io.a2a.grpc.GetAgentCardRequest request,
                           StreamObserver<io.a2a.grpc.AgentCard> responseObserver) {
        try {
            responseObserver.onNext(ToProto.agentCard(getAgentCardInternal()));
            responseObserver.onCompleted();
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void deleteTaskPushNotificationConfig(io.a2a.grpc.DeleteTaskPushNotificationConfigRequest request,
                                               StreamObserver<Empty> responseObserver) {
        if (!getAgentCardInternal().capabilities().pushNotifications()) {
            handleError(responseObserver, new PushNotificationNotSupportedError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            DeleteTaskPushNotificationConfigParams params = FromProto.deleteTaskPushNotificationConfigParams(request);
            getRequestHandler().onDeleteTaskPushNotificationConfig(params, context);
            // void response
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (SecurityException e) {
            handleSecurityException(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    private <V> ServerCallContext createCallContext(StreamObserver<V> responseObserver) {
        CallContextFactory factory = getCallContextFactory();
        if (factory == null) {
            // Default implementation when no custom CallContextFactory is provided
            // This handles both CDI injection scenarios and test scenarios where callContextFactory is null
            User user = UnauthenticatedUser.INSTANCE;
            Map<String, Object> state = new HashMap<>();
            
            // Enhanced gRPC context access - equivalent to Python's grpc.aio.ServicerContext
            // The A2AExtensionsInterceptor captures ServerCall + Metadata and stores them in gRPC Context
            // This provides proper equivalence to Python's ServicerContext for metadata access
            // Note: StreamObserver is still stored for response handling
            state.put("grpc_response_observer", responseObserver);
            
            // Add rich gRPC context information if available (set by interceptor)
            // This provides equivalent functionality to Python's grpc.aio.ServicerContext
            try {
                Context currentContext = Context.current();
                if (currentContext != null) {
                    state.put("grpc_context", currentContext);
                    
                    // Add specific context information for easy access
                    io.grpc.Metadata grpcMetadata = GrpcContextKeys.METADATA_KEY.get(currentContext);
                    if (grpcMetadata != null) {
                        state.put("grpc_metadata", grpcMetadata);
                    }
                    
                    String methodName = GrpcContextKeys.METHOD_NAME_KEY.get(currentContext);
                    if (methodName != null) {
                        state.put("grpc_method_name", methodName);
                    }
                    
                    String peerInfo = GrpcContextKeys.PEER_INFO_KEY.get(currentContext);
                    if (peerInfo != null) {
                        state.put("grpc_peer_info", peerInfo);
                    }
                }
            } catch (Exception e) {
                // Context not available - continue without it
                LOGGER.fine(() -> "Error getting data from current context" + e);
            }
            
            // Extract requested extensions from gRPC context (set by interceptor)
            Set<String> requestedExtensions = new HashSet<>();
            String extensionsHeader = getExtensionsFromContext();
            if (extensionsHeader != null) {
                requestedExtensions = A2AExtensions.getRequestedExtensions(List.of(extensionsHeader));
            }
            
            return new ServerCallContext(user, state, requestedExtensions);
        } else {
            // TODO: CallContextFactory interface expects ServerCall + Metadata, but we only have StreamObserver
            // This is another manifestation of the architectural limitation mentioned above
            return factory.create(responseObserver); // Fall back to basic create() method for now
        }
    }

    private <V> void handleError(StreamObserver<V> responseObserver, JSONRPCError error) {
        Status status;
        String description;
        if (error instanceof InvalidRequestError) {
            status = Status.INVALID_ARGUMENT;
            description = "InvalidRequestError: " + error.getMessage();
        } else if (error instanceof MethodNotFoundError) {
            status = Status.NOT_FOUND;
            description = "MethodNotFoundError: " + error.getMessage();
        } else if (error instanceof InvalidParamsError) {
            status = Status.INVALID_ARGUMENT;
            description = "InvalidParamsError: " + error.getMessage();
        } else if (error instanceof InternalError) {
            status = Status.INTERNAL;
            description = "InternalError: " + error.getMessage();
        } else if (error instanceof TaskNotFoundError) {
            status = Status.NOT_FOUND;
            description = "TaskNotFoundError: " + error.getMessage();
        } else if (error instanceof TaskNotCancelableError) {
            status = Status.UNIMPLEMENTED;
            description = "TaskNotCancelableError: " + error.getMessage();
        } else if (error instanceof PushNotificationNotSupportedError) {
            status = Status.UNIMPLEMENTED;
            description = "PushNotificationNotSupportedError: " + error.getMessage();
        } else if (error instanceof UnsupportedOperationError) {
            status = Status.UNIMPLEMENTED;
            description = "UnsupportedOperationError: " + error.getMessage();
        } else if (error instanceof JSONParseError) {
            status = Status.INTERNAL;
            description = "JSONParseError: " + error.getMessage();
        } else if (error instanceof ContentTypeNotSupportedError) {
            status = Status.UNIMPLEMENTED;
            description = "ContentTypeNotSupportedError: " + error.getMessage();
        } else if (error instanceof InvalidAgentResponseError) {
            status = Status.INTERNAL;
            description = "InvalidAgentResponseError: " + error.getMessage();
        } else {
            status = Status.UNKNOWN;
            description = "Unknown error type: " + error.getMessage();
        }
        responseObserver.onError(status.withDescription(description).asRuntimeException());
    }

    private <V> void handleSecurityException(StreamObserver<V> responseObserver, SecurityException e) {
        Status status;
        String description;

        String exceptionClassName = e.getClass().getName();

        // Attempt to detect common authentication and authorization related exceptions
        if (exceptionClassName.contains("Unauthorized") ||
            exceptionClassName.contains("Unauthenticated") ||
            exceptionClassName.contains("Authentication")) {
            status = Status.UNAUTHENTICATED;
            description = A2AErrorMessages.AUTHENTICATION_FAILED;
        } else if (exceptionClassName.contains("Forbidden") ||
                 exceptionClassName.contains("AccessDenied") ||
                 exceptionClassName.contains("Authorization")) {
            status = Status.PERMISSION_DENIED;
            description = A2AErrorMessages.AUTHORIZATION_FAILED;
        } else {
            // If the security exception type cannot be detected, default to PERMISSION_DENIED
            status = Status.PERMISSION_DENIED;
            description = "Authorization failed: " + (e.getMessage() != null ? e.getMessage() : "Access denied");
        }

        responseObserver.onError(status.withDescription(description).asRuntimeException());
    }

    private <V> void handleInternalError(StreamObserver<V> responseObserver, Throwable t) {
        handleError(responseObserver, new InternalError(t.getMessage()));
    }

    private AgentCard getAgentCardInternal() {
        AgentCard agentCard = getAgentCard();
        if (initialised.compareAndSet(false, true)) {
            // Validate transport configuration with proper classloader context
            validateTransportConfigurationWithCorrectClassLoader(agentCard);
        }
        return agentCard;
    }

    private void validateTransportConfigurationWithCorrectClassLoader(AgentCard agentCard) {
        ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();
        ClassLoader deploymentCl = getDeploymentClassLoader();
        boolean switchCl = deploymentCl != null && deploymentCl != originalTccl;

        try {
            if (switchCl) {
                // Set TCCL to the classloader that loaded this class, which should have access
                // to the deployment classpath containing META-INF/services files
                Thread.currentThread().setContextClassLoader(deploymentCl);
            }
            AgentCardValidator.validateTransportConfiguration(agentCard);
        } finally {
            if (switchCl) {
                Thread.currentThread().setContextClassLoader(originalTccl);
            }
        }
    }

    protected ClassLoader getDeploymentClassLoader() {
        return this.getClass().getClassLoader();
    }

    public static void setStreamingSubscribedRunnable(Runnable runnable) {
        streamingSubscribedRunnable = runnable;
    }

    protected abstract RequestHandler getRequestHandler();

    protected abstract AgentCard getAgentCard();

    protected abstract CallContextFactory getCallContextFactory();

    protected abstract Executor getExecutor();

    /**
     * Attempts to extract the X-A2A-Extensions header from the current gRPC context.
     * This will only work if a server interceptor has been configured to capture
     * the metadata and store it in the context.
     * 
     * @return the extensions header value, or null if not available
     */
    private String getExtensionsFromContext() {
        try {
            return GrpcContextKeys.EXTENSIONS_HEADER_KEY.get();
        } catch (Exception e) {
            // Context not available or key not set
            return null;
        }
    }

    /**
     * Utility methods for accessing gRPC context information.
     * These provide equivalent functionality to Python's grpc.aio.ServicerContext methods.
     */
    
    /**
     * Generic helper method to safely access gRPC context values.
     * 
     * @param key the context key to retrieve
     * @return the context value, or null if not available
     */
    private static <T> T getFromContext(Context.Key<T> key) {
        try {
            return key.get();
        } catch (Exception e) {
            // Context not available or key not set
            return null;
        }
    }
    
    /**
     * Gets the complete gRPC metadata from the current context.
     * Equivalent to Python's context.invocation_metadata.
     * 
     * @return the gRPC Metadata object, or null if not available
     */
    protected static io.grpc.Metadata getCurrentMetadata() {
        return getFromContext(GrpcContextKeys.METADATA_KEY);
    }
    
    /**
     * Gets the current gRPC method name.
     * Equivalent to Python's context.method().
     * 
     * @return the method name, or null if not available
     */
    protected static String getCurrentMethodName() {
        return getFromContext(GrpcContextKeys.METHOD_NAME_KEY);
    }
    
    /**
     * Gets the peer information for the current gRPC call.
     * Equivalent to Python's context.peer().
     * 
     * @return the peer information, or null if not available
     */
    protected static String getCurrentPeerInfo() {
        return getFromContext(GrpcContextKeys.PEER_INFO_KEY);
    }
}
