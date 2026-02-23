package io.a2a.transport.grpc.handler;

import static io.a2a.grpc.utils.ProtoUtils.FromProto;
import static io.a2a.grpc.utils.ProtoUtils.ToProto;
import static io.a2a.server.ServerCallContext.TRANSPORT_KEY;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import jakarta.enterprise.inject.Vetoed;

import com.google.protobuf.Empty;
import io.a2a.common.A2AErrorMessages;
import io.a2a.grpc.A2AServiceGrpc;
import io.a2a.grpc.StreamResponse;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.server.AgentCardValidator;
import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.auth.User;
import io.a2a.server.extensions.A2AExtensions;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.version.A2AVersionValidator;
import io.a2a.spec.A2AError;
import io.a2a.spec.AgentCard;
import io.a2a.spec.ContentTypeNotSupportedError;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.ExtendedAgentCardNotConfiguredError;
import io.a2a.spec.ExtensionSupportRequiredError;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidAgentResponseError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
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
import io.a2a.spec.TransportProtocol;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.spec.VersionNotSupportedError;
import io.a2a.transport.grpc.context.GrpcContextKeys;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.jspecify.annotations.Nullable;

/**
 * gRPC transport handler for processing A2A protocol requests.
 *
 * <p>This abstract class implements the gRPC service interface for the A2A protocol,
 * handling both unary (blocking) and server streaming RPC calls. It translates gRPC
 * requests to A2A protocol operations, coordinates with the request handler and agent
 * executor, and manages error handling with appropriate gRPC status codes.
 *
 * <h2>Request Flow</h2>
 * <pre>
 * gRPC Request → GrpcHandler (this class)
 *     ↓
 * Protobuf → Domain conversion
 *     ↓
 * RequestHandler → AgentExecutor
 *     ↓
 * Domain → Protobuf conversion
 *     ↓
 * gRPC Response (unary or streaming)
 * </pre>
 *
 * <h2>Supported Operations</h2>
 *
 * <p><b>Unary RPC (blocking):</b>
 * <ul>
 *   <li>{@link #sendMessage} - Send message and wait for completion</li>
 *   <li>{@link #getTask} - Retrieve task by ID</li>
 *   <li>{@link #cancelTask} - Cancel task execution</li>
 *   <li>{@link #listTasks} - List tasks with filtering</li>
 *   <li>{@link #createTaskPushNotificationConfig} - Configure push notifications</li>
 *   <li>{@link #getTaskPushNotificationConfig} - Get push notification config</li>
 *   <li>{@link #listTaskPushNotificationConfig} - List push notification configs</li>
 *   <li>{@link #deleteTaskPushNotificationConfig} - Delete push notification config</li>
 *   <li>{@link #getExtendedAgentCard} - Get extended agent capabilities</li>
 * </ul>
 *
 * <p><b>Server Streaming RPC:</b>
 * <ul>
 *   <li>{@link #sendStreamingMessage} - Send message with streaming response</li>
 *   <li>{@link #subscribeToTask} - Subscribe to task events</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>A2A errors are mapped to gRPC status codes:
 * <ul>
 *   <li>{@link io.a2a.spec.InvalidRequestError} → {@link Status#INVALID_ARGUMENT}</li>
 *   <li>{@link io.a2a.spec.MethodNotFoundError} → {@link Status#NOT_FOUND}</li>
 *   <li>{@link io.a2a.spec.TaskNotFoundError} → {@link Status#NOT_FOUND}</li>
 *   <li>{@link io.a2a.spec.InternalError} → {@link Status#INTERNAL}</li>
 *   <li>{@link io.a2a.spec.UnsupportedOperationError} → {@link Status#UNIMPLEMENTED}</li>
 *   <li>{@link SecurityException} → {@link Status#UNAUTHENTICATED} or {@link Status#PERMISSION_DENIED}</li>
 * </ul>
 *
 * <h2>Context Access</h2>
 * <p>The handler provides rich context information equivalent to Python's
 * {@code grpc.aio.ServicerContext}:
 * <ul>
 *   <li>{@link #getCurrentMetadata()} - Request metadata (headers)</li>
 *   <li>{@link #getCurrentMethodName()} - gRPC method name</li>
 *   <li>{@link #getCurrentPeerInfo()} - Client connection details</li>
 * </ul>
 *
 * <h2>Extension Points</h2>
 * <p>Subclasses must implement:
 * <ul>
 *   <li>{@link #getRequestHandler()} - Request handler instance</li>
 *   <li>{@link #getAgentCard()} - Public agent card</li>
 *   <li>{@link #getExtendedAgentCard()} - Extended agent card (nullable)</li>
 *   <li>{@link #getCallContextFactory()} - Custom context factory (nullable)</li>
 *   <li>{@link #getExecutor()} - Executor for async operations</li>
 * </ul>
 *
 * <h2>CDI Integration</h2>
 * <p>This class is marked with {@code @Vetoed} to prevent direct CDI management.
 * Subclasses should be CDI beans (e.g., {@code @GrpcService} in Quarkus) that
 * inject dependencies and provide them through the abstract methods.
 *
 * @see io.a2a.grpc.A2AServiceGrpc.A2AServiceImplBase
 * @see io.a2a.server.requesthandlers.RequestHandler
 * @see CallContextFactory
 * @see io.a2a.transport.grpc.context.GrpcContextKeys
 */
@Vetoed
public abstract class GrpcHandler extends A2AServiceGrpc.A2AServiceImplBase {

    // Hook so testing can wait until streaming subscriptions are established.
    // Without this we get intermittent failures
    private static volatile @Nullable Runnable streamingSubscribedRunnable;

    private final AtomicBoolean initialised = new AtomicBoolean(false);

    private static final Logger LOGGER = Logger.getLogger(GrpcHandler.class.getName());

    /**
     * Constructs a new GrpcHandler.
     */
    public GrpcHandler() {

    }

    /**
     * Handles a unary (blocking) message send request.
     *
     * <p>This method processes a message send request, waits for the agent to complete
     * processing, and returns either a Task or Message in the response.
     *
     * <p><b>Protocol Flow:</b>
     * <ol>
     *   <li>Validate A2A protocol version and extensions</li>
     *   <li>Convert protobuf request to domain {@link MessageSendParams}</li>
     *   <li>Invoke {@link io.a2a.server.requesthandlers.RequestHandler#onMessageSend}</li>
     *   <li>Convert domain response to protobuf {@link io.a2a.grpc.SendMessageResponse}</li>
     *   <li>Send response and complete the RPC</li>
     * </ol>
     *
     * <p><b>Error Handling:</b>
     * <ul>
     *   <li>{@link A2AError} → mapped to appropriate gRPC status code</li>
     *   <li>{@link SecurityException} → {@code UNAUTHENTICATED} or {@code PERMISSION_DENIED}</li>
     *   <li>{@link Throwable} → {@code INTERNAL} error</li>
     * </ul>
     *
     * @param request the gRPC message send request
     * @param responseObserver the gRPC response stream observer
     */
    @Override
    public void sendMessage(io.a2a.grpc.SendMessageRequest request,
                           StreamObserver<io.a2a.grpc.SendMessageResponse> responseObserver) {
        try {
            ServerCallContext context = createCallContext(responseObserver);
            A2AVersionValidator.validateProtocolVersion(getAgentCardInternal(), context);
            A2AExtensions.validateRequiredExtensions(getAgentCardInternal(), context);
            MessageSendParams params = FromProto.messageSendParams(request);
            EventKind taskOrMessage = getRequestHandler().onMessageSend(params, context);
            io.a2a.grpc.SendMessageResponse response = ToProto.taskOrMessage(taskOrMessage);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (A2AError e) {
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
        } catch (A2AError e) {
            handleError(responseObserver, e);
        } catch (SecurityException e) {
            handleSecurityException(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void listTasks(io.a2a.grpc.ListTasksRequest request,
                         StreamObserver<io.a2a.grpc.ListTasksResponse> responseObserver) {
        try {
            ServerCallContext context = createCallContext(responseObserver);
            io.a2a.spec.ListTasksParams params = FromProto.listTasksParams(request);
            ListTasksResult result = getRequestHandler().onListTasks(params, context);
            responseObserver.onNext(ToProto.listTasksResult(result));
            responseObserver.onCompleted();
        } catch (A2AError e) {
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
        } catch (A2AError e) {
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
            TaskPushNotificationConfig config = FromProto.CreateTaskPushNotificationConfig(request);
            TaskPushNotificationConfig responseConfig = getRequestHandler().onCreateTaskPushNotificationConfig(config, context);
            responseObserver.onNext(ToProto.taskPushNotificationConfig(responseConfig));
            responseObserver.onCompleted();
        } catch (A2AError e) {
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
        } catch (A2AError e) {
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
            ListTaskPushNotificationConfigResult result = getRequestHandler().onListTaskPushNotificationConfig(params, context);
            io.a2a.grpc.ListTaskPushNotificationConfigResponse response = ToProto.listTaskPushNotificationConfigResponse(result);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (A2AError e) {
            handleError(responseObserver, e);
        } catch (SecurityException e) {
            handleSecurityException(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    /**
     * Handles a server streaming message send request.
     *
     * <p>This method processes a message send request with streaming response, where
     * the agent can emit multiple events (artifacts, status updates, messages) as the
     * task progresses.
     *
     * <p><b>Protocol Flow:</b>
     * <ol>
     *   <li>Verify streaming capability is enabled in agent card</li>
     *   <li>Validate A2A protocol version and extensions</li>
     *   <li>Convert protobuf request to domain {@link MessageSendParams}</li>
     *   <li>Invoke {@link io.a2a.server.requesthandlers.RequestHandler#onMessageSendStream}</li>
     *   <li>Subscribe to event publisher and stream responses</li>
     *   <li>Convert each domain event to protobuf {@link io.a2a.grpc.StreamResponse}</li>
     *   <li>Complete RPC when final event received or error occurs</li>
     * </ol>
     *
     * <p><b>Streaming Characteristics:</b>
     * <ul>
     *   <li>Server streaming RPC - server sends multiple responses</li>
     *   <li>Backpressure handled through reactive streams subscription</li>
     *   <li>Client disconnect detection via gRPC context cancellation</li>
     *   <li>Automatic cleanup when stream completes or errors</li>
     * </ul>
     *
     * <p><b>Error Handling:</b>
     * <ul>
     *   <li>Streaming not enabled → {@link io.a2a.spec.InvalidRequestError}</li>
     *   <li>Other {@link A2AError} → mapped to appropriate gRPC status code</li>
     *   <li>{@link SecurityException} → {@code UNAUTHENTICATED} or {@code PERMISSION_DENIED}</li>
     *   <li>{@link Throwable} → {@code INTERNAL} error</li>
     * </ul>
     *
     * @param request the gRPC message send request
     * @param responseObserver the gRPC response stream observer
     */
    @Override
    public void sendStreamingMessage(io.a2a.grpc.SendMessageRequest request,
                                     StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
        if (!getAgentCardInternal().capabilities().streaming()) {
            handleError(responseObserver, new InvalidRequestError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            A2AVersionValidator.validateProtocolVersion(getAgentCardInternal(), context);
            A2AExtensions.validateRequiredExtensions(getAgentCardInternal(), context);
            MessageSendParams params = FromProto.messageSendParams(request);
            Flow.Publisher<StreamingEventKind> publisher = getRequestHandler().onMessageSendStream(params, context);
            convertToStreamResponse(publisher, responseObserver, context);
        } catch (A2AError e) {
            handleError(responseObserver, e);
        } catch (SecurityException e) {
            handleSecurityException(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void subscribeToTask(io.a2a.grpc.SubscribeToTaskRequest request,
                                 StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
        if (!getAgentCardInternal().capabilities().streaming()) {
            handleError(responseObserver, new InvalidRequestError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            TaskIdParams params = FromProto.taskIdParams(request);
            Flow.Publisher<StreamingEventKind> publisher = getRequestHandler().onSubscribeToTask(params, context);
            convertToStreamResponse(publisher, responseObserver, context);
        } catch (A2AError e) {
            handleError(responseObserver, e);
        } catch (SecurityException e) {
            handleSecurityException(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    /**
     * Converts a reactive stream of domain events to gRPC streaming responses.
     *
     * <p>This method subscribes to the event publisher and converts each domain event
     * to a protobuf {@link StreamResponse}, handling backpressure through the reactive
     * streams subscription and detecting client disconnections.
     *
     * <p><b>Backpressure Handling:</b>
     * <ol>
     *   <li>Request 1 event from upstream</li>
     *   <li>Send event to gRPC response observer</li>
     *   <li>Wait for send completion</li>
     *   <li>Request next event (backpressure)</li>
     * </ol>
     *
     * <p><b>Disconnect Detection:</b>
     * <p>When the gRPC client disconnects:
     * <ol>
     *   <li>gRPC Context cancellation listener fires</li>
     *   <li>Invokes {@link ServerCallContext#invokeEventConsumerCancelCallback()}</li>
     *   <li>Cancels upstream subscription</li>
     *   <li>Stops event polling</li>
     * </ol>
     *
     * <p><b>Final Event Detection:</b>
     * <p>The stream completes automatically when a final task status is received:
     * <ul>
     *   <li>{@code TASK_STATE_COMPLETED}</li>
     *   <li>{@code TASK_STATE_CANCELED}</li>
     *   <li>{@code TASK_STATE_FAILED}</li>
     *   <li>{@code TASK_STATE_REJECTED}</li>
     * </ul>
     *
     * @param publisher the reactive publisher of streaming events
     * @param responseObserver the gRPC response stream observer
     * @param context the server call context for disconnect detection
     */
    private void convertToStreamResponse(Flow.Publisher<StreamingEventKind> publisher,
                                         StreamObserver<io.a2a.grpc.StreamResponse> responseObserver,
                                         ServerCallContext context) {
        CompletableFuture.runAsync(() -> {
            publisher.subscribe(new Flow.Subscriber<StreamingEventKind>() {
                private  Flow.@Nullable Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    if (this.subscription != null) {
                        this.subscription.request(1);
                    }

                    // Detect gRPC client disconnect and call EventConsumer.cancel() directly
                    // This stops the polling loop without relying on subscription cancellation propagation
                    Context grpcContext = Context.current();
                    grpcContext.addListener(new Context.CancellationListener() {
                        @Override
                        public void cancelled(Context ctx) {
                            LOGGER.fine(() -> "gRPC call cancelled by client, calling EventConsumer.cancel() to stop polling loop");
                            context.invokeEventConsumerCancelCallback();
                            subscription.cancel();
                        }
                    }, getExecutor());

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
                    if (response.hasStatusUpdate()) {
                        io.a2a.grpc.TaskState state = response.getStatusUpdate().getStatus().getState();
                        boolean isFinal = state == io.a2a.grpc.TaskState.TASK_STATE_CANCELED
                                || state == io.a2a.grpc.TaskState.TASK_STATE_COMPLETED
                                || state == io.a2a.grpc.TaskState.TASK_STATE_FAILED
                                || state == io.a2a.grpc.TaskState.TASK_STATE_REJECTED;
                        if (isFinal) {
                            responseObserver.onCompleted();
                        } else {
                            if (this.subscription != null) {
                                subscription.request(1);
                            }
                        }
                    } else {
                        if (this.subscription != null) {
                            this.subscription.request(1);
                        }
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    // Cancel upstream to stop EventConsumer when error occurs
                    if (this.subscription != null) {
                        subscription.cancel();
                     }
                    if (throwable instanceof A2AError jsonrpcError) {
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
    public void getExtendedAgentCard(io.a2a.grpc.GetExtendedAgentCardRequest request,
                           StreamObserver<io.a2a.grpc.AgentCard> responseObserver) {
        try {
            AgentCard extendedAgentCard = getExtendedAgentCard();
            if (extendedAgentCard != null) {
                responseObserver.onNext(ToProto.agentCard(extendedAgentCard));
                responseObserver.onCompleted();
            } else {
                // Extended agent card not configured - return error instead of hanging
                handleError(responseObserver, new ExtendedAgentCardNotConfiguredError(null, "Extended agent card not configured", null));
            }
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
        } catch (A2AError e) {
            handleError(responseObserver, e);
        } catch (SecurityException e) {
            handleSecurityException(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    /**
     * Creates a {@link ServerCallContext} from the current gRPC request context.
     *
     * <p>This method extracts authentication, metadata, and A2A protocol information
     * from the gRPC context and packages them into a context object for use by the
     * request handler and agent executor.
     *
     * <p><b>Default Context Creation:</b>
     * <p>If no {@link CallContextFactory} is provided, creates a context with:
     * <ul>
     *   <li>User authentication (defaults to {@link UnauthenticatedUser})</li>
     *   <li>Transport protocol ({@link TransportProtocol#GRPC})</li>
     *   <li>gRPC response observer for streaming</li>
     *   <li>gRPC context and metadata (equivalent to Python's ServicerContext)</li>
     *   <li>HTTP headers extracted from metadata</li>
     *   <li>gRPC method name</li>
     *   <li>Peer information (client connection details)</li>
     *   <li>A2A protocol version from {@code X-A2A-Version} header (via context)</li>
     *   <li>Required extensions from {@code X-A2A-Extensions} header (via context)</li>
     * </ul>
     *
     * <p><b>Custom Context Creation:</b>
     * <p>If a {@link CallContextFactory} bean is present, delegates to
     * {@link CallContextFactory#create(StreamObserver)} for custom context creation.
     *
     * <p><b>Context Information:</b>
     * <p>The gRPC context information is populated by server interceptors (typically
     * {@code A2AExtensionsInterceptor}) that capture request metadata before service
     * methods are invoked.
     *
     * @param <V> the response type for the gRPC method
     * @param responseObserver the gRPC response stream observer
     * @return the server call context
     * @see CallContextFactory
     * @see io.a2a.transport.grpc.context.GrpcContextKeys
     */
    private <V> ServerCallContext createCallContext(StreamObserver<V> responseObserver) {
        CallContextFactory factory = getCallContextFactory();
        if (factory == null) {
            // Default implementation when no custom CallContextFactory is provided
            // This handles both CDI injection scenarios and test scenarios where callContextFactory is null
            User user = UnauthenticatedUser.INSTANCE;
            Map<String, Object> state = new HashMap<>();
            state.put(TRANSPORT_KEY, TransportProtocol.GRPC);

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
                    Map<String, String> headers= new HashMap<>();
                    for(String key : grpcMetadata.keys()) {
                        headers.put(key, grpcMetadata.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)));
                    }
                    state.put("headers", headers);
                    String methodName = GrpcContextKeys.GRPC_METHOD_NAME_KEY.get(currentContext);
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
            
            // Extract requested protocol version from gRPC context (set by interceptor)
            String requestedVersion = getVersionFromContext();

            // Extract requested extensions from gRPC context (set by interceptor)
            Set<String> requestedExtensions = new HashSet<>();
            String extensionsHeader = getExtensionsFromContext();
            if (extensionsHeader != null) {
                requestedExtensions = A2AExtensions.getRequestedExtensions(List.of(extensionsHeader));
            }

            return new ServerCallContext(user, state, requestedExtensions, requestedVersion);
        } else {
            // TODO: CallContextFactory interface expects ServerCall + Metadata, but we only have StreamObserver
            // This is another manifestation of the architectural limitation mentioned above
            return factory.create(responseObserver); // Fall back to basic create() method for now
        }
    }

    /**
     * Handles A2A protocol errors by mapping them to appropriate gRPC status codes.
     *
     * <p>This method converts domain-specific A2A errors to gRPC status codes with
     * descriptive error messages, allowing clients to understand and handle errors
     * appropriately.
     *
     * <p><b>Error Mappings:</b>
     * <ul>
     *   <li>{@link InvalidRequestError} → {@code INVALID_ARGUMENT}</li>
     *   <li>{@link MethodNotFoundError} → {@code NOT_FOUND}</li>
     *   <li>{@link InvalidParamsError} → {@code INVALID_ARGUMENT}</li>
     *   <li>{@link InternalError} → {@code INTERNAL}</li>
     *   <li>{@link TaskNotFoundError} → {@code NOT_FOUND}</li>
     *   <li>{@link TaskNotCancelableError} → {@code FAILED_PRECONDITION}</li>
     *   <li>{@link PushNotificationNotSupportedError} → {@code UNIMPLEMENTED}</li>
     *   <li>{@link UnsupportedOperationError} → {@code UNIMPLEMENTED}</li>
     *   <li>{@link JSONParseError} → {@code INTERNAL}</li>
     *   <li>{@link ContentTypeNotSupportedError} → {@code INVALID_ARGUMENT}</li>
     *   <li>{@link InvalidAgentResponseError} → {@code INTERNAL}</li>
     *   <li>{@link ExtendedAgentCardNotConfiguredError} → {@code FAILED_PRECONDITION}</li>
     *   <li>{@link ExtensionSupportRequiredError} → {@code FAILED_PRECONDITION}</li>
     *   <li>{@link VersionNotSupportedError} → {@code UNIMPLEMENTED}</li>
     *   <li>Unknown errors → {@code UNKNOWN}</li>
     * </ul>
     *
     * @param <V> the response type for the gRPC method
     * @param responseObserver the gRPC response stream observer
     * @param error the A2A protocol error
     */
    private <V> void handleError(StreamObserver<V> responseObserver, A2AError error) {
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
            status = Status.FAILED_PRECONDITION;
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
            status = Status.INVALID_ARGUMENT;
            description = "ContentTypeNotSupportedError: " + error.getMessage();
        } else if (error instanceof InvalidAgentResponseError) {
            status = Status.INTERNAL;
            description = "InvalidAgentResponseError: " + error.getMessage();
        } else if (error instanceof ExtendedAgentCardNotConfiguredError) {
            status = Status.FAILED_PRECONDITION;
            description = "ExtendedCardNotConfiguredError: " + error.getMessage();
        } else if (error instanceof ExtensionSupportRequiredError) {
            status = Status.FAILED_PRECONDITION;
            description = "ExtensionSupportRequiredError: " + error.getMessage();
        } else if (error instanceof VersionNotSupportedError) {
            status = Status.UNIMPLEMENTED;
            description = "VersionNotSupportedError: " + error.getMessage();
        } else {
            status = Status.UNKNOWN;
            description = "Unknown error type: " + error.getMessage();
        }
        responseObserver.onError(status.withDescription(description).asRuntimeException());
    }

    /**
     * Handles security-related exceptions by mapping them to gRPC authentication/authorization errors.
     *
     * <p>This method attempts to detect the type of security exception based on the exception
     * class name and maps it to the appropriate gRPC status code.
     *
     * <p><b>Error Detection:</b>
     * <ul>
     *   <li>Unauthorized/Unauthenticated/Authentication exceptions → {@code UNAUTHENTICATED}</li>
     *   <li>Forbidden/AccessDenied/Authorization exceptions → {@code PERMISSION_DENIED}</li>
     *   <li>Other SecurityException → {@code PERMISSION_DENIED} (default)</li>
     * </ul>
     *
     * @param <V> the response type for the gRPC method
     * @param responseObserver the gRPC response stream observer
     * @param e the security exception
     */
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

    /**
     * Returns the deployment classloader for this handler.
     *
     * <p>Used for transport configuration validation with proper classloader context.
     *
     * @return the deployment classloader
     */
    protected ClassLoader getDeploymentClassLoader() {
        return this.getClass().getClassLoader();
    }

    /**
     * Sets a callback to be invoked when streaming subscription starts.
     *
     * <p>This is a testing hook used to synchronize test execution with streaming setup.
     * In production, this remains null.
     *
     * @param runnable the callback to invoke on subscription
     */
    public static void setStreamingSubscribedRunnable(Runnable runnable) {
        streamingSubscribedRunnable = runnable;
    }

    /**
     * Returns the request handler instance for processing A2A protocol requests.
     *
     * @return the request handler
     */
    protected abstract RequestHandler getRequestHandler();

    /**
     * Returns the public agent card defining the agent's capabilities and metadata.
     *
     * @return the agent card
     */
    protected abstract AgentCard getAgentCard();

    /**
     * Returns the extended agent card with additional capabilities, or null if not configured.
     *
     * @return the extended agent card, or null if not available
     */
    protected abstract AgentCard getExtendedAgentCard();

    /**
     * Returns the custom call context factory, or null to use default context creation.
     *
     * @return the call context factory, or null for default behavior
     */
    protected abstract CallContextFactory getCallContextFactory();

    /**
     * Returns the executor for running async operations (streaming subscriptions, etc.).
     *
     * @return the executor
     */
    protected abstract Executor getExecutor();

    /**
     * Attempts to extract the X-A2A-Version header from the current gRPC context.
     * This will only work if a server interceptor has been configured to capture
     * the metadata and store it in the context.
     *
     * @return the version header value, or null if not available
     */
    private @Nullable String getVersionFromContext() {
        try {
            return GrpcContextKeys.VERSION_HEADER_KEY.get();
        } catch (Exception e) {
            // Context not available or key not set
            return null;
        }
    }

    /**
     * Attempts to extract the X-A2A-Extensions header from the current gRPC context.
     * This will only work if a server interceptor has been configured to capture
     * the metadata and store it in the context.
     *
     * @return the extensions header value, or null if not available
     */
    private @Nullable String getExtensionsFromContext() {
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
    private static @Nullable <T> T getFromContext(Context.Key<T> key) {
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
    protected static io.grpc.@Nullable Metadata getCurrentMetadata() {
        return getFromContext(GrpcContextKeys.METADATA_KEY);
    }
    
    /**
     * Gets the current gRPC method name.
     * Equivalent to Python's context.method().
     * 
     * @return the method name, or null if not available
     */
    protected static @Nullable String getCurrentMethodName() {
        return getFromContext(GrpcContextKeys.GRPC_METHOD_NAME_KEY);
    }
    
    /**
     * Gets the peer information for the current gRPC call.
     * Equivalent to Python's context.peer().
     * 
     * @return the peer information, or null if not available
     */
    protected static @Nullable String getCurrentPeerInfo() {
        return getFromContext(GrpcContextKeys.PEER_INFO_KEY);
    }
}
