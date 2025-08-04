package io.a2a.grpc.handler;

import static io.a2a.grpc.utils.ProtoUtils.FromProto;
import static io.a2a.grpc.utils.ProtoUtils.ToProto;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import com.google.protobuf.Empty;
import io.a2a.grpc.A2AServiceGrpc;
import io.a2a.grpc.StreamResponse;
import io.a2a.server.PublicAgentCard;
import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.auth.User;
import io.a2a.server.requesthandlers.CallContextFactory;
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

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class GrpcHandler extends A2AServiceGrpc.A2AServiceImplBase {

    private AgentCard agentCard;
    private RequestHandler requestHandler;

    // Hook so testing can wait until streaming subscriptions are established.
    // Without this we get intermittent failures
    private static volatile Runnable streamingSubscribedRunnable;

    @Inject
    Instance<CallContextFactory> callContextFactory;

    protected GrpcHandler() {
    }

    @Inject
    public GrpcHandler(@PublicAgentCard AgentCard agentCard, RequestHandler requestHandler) {
        this.agentCard = agentCard;
        this.requestHandler = requestHandler;
    }

    @Override
    public void sendMessage(io.a2a.grpc.SendMessageRequest request,
                           StreamObserver<io.a2a.grpc.SendMessageResponse> responseObserver) {
        try {
            ServerCallContext context = createCallContext(responseObserver);
            MessageSendParams params = FromProto.messageSendParams(request);
            EventKind taskOrMessage = requestHandler.onMessageSend(params, context);
            io.a2a.grpc.SendMessageResponse response = ToProto.taskOrMessage(taskOrMessage);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
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
            Task task = requestHandler.onGetTask(params, context);
            if (task != null) {
                responseObserver.onNext(ToProto.task(task));
                responseObserver.onCompleted();
            } else {
                handleError(responseObserver, new TaskNotFoundError());
            }
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
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
            Task task = requestHandler.onCancelTask(params, context);
            if (task != null) {
                responseObserver.onNext(ToProto.task(task));
                responseObserver.onCompleted();
            } else {
                handleError(responseObserver, new TaskNotFoundError());
            }
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void createTaskPushNotificationConfig(io.a2a.grpc.CreateTaskPushNotificationConfigRequest request,
                                               StreamObserver<io.a2a.grpc.TaskPushNotificationConfig> responseObserver) {
        if (! agentCard.capabilities().pushNotifications()) {
            handleError(responseObserver, new PushNotificationNotSupportedError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            TaskPushNotificationConfig config = FromProto.taskPushNotificationConfig(request);
            TaskPushNotificationConfig responseConfig = requestHandler.onSetTaskPushNotificationConfig(config, context);
            responseObserver.onNext(ToProto.taskPushNotificationConfig(responseConfig));
            responseObserver.onCompleted();
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void getTaskPushNotificationConfig(io.a2a.grpc.GetTaskPushNotificationConfigRequest request,
                                            StreamObserver<io.a2a.grpc.TaskPushNotificationConfig> responseObserver) {
        if (! agentCard.capabilities().pushNotifications()) {
            handleError(responseObserver, new PushNotificationNotSupportedError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            GetTaskPushNotificationConfigParams params = FromProto.getTaskPushNotificationConfigParams(request);
            TaskPushNotificationConfig config = requestHandler.onGetTaskPushNotificationConfig(params, context);
            responseObserver.onNext(ToProto.taskPushNotificationConfig(config));
            responseObserver.onCompleted();
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void listTaskPushNotificationConfig(io.a2a.grpc.ListTaskPushNotificationConfigRequest request,
                                             StreamObserver<io.a2a.grpc.ListTaskPushNotificationConfigResponse> responseObserver) {
        if (! agentCard.capabilities().pushNotifications()) {
            handleError(responseObserver, new PushNotificationNotSupportedError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            ListTaskPushNotificationConfigParams params = FromProto.listTaskPushNotificationConfigParams(request);
            List<TaskPushNotificationConfig> configList = requestHandler.onListTaskPushNotificationConfig(params, context);
            io.a2a.grpc.ListTaskPushNotificationConfigResponse.Builder responseBuilder = 
                io.a2a.grpc.ListTaskPushNotificationConfigResponse.newBuilder();
            for (TaskPushNotificationConfig config : configList) {
                responseBuilder.addConfigs(ToProto.taskPushNotificationConfig(config));
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void sendStreamingMessage(io.a2a.grpc.SendMessageRequest request,
                                     StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
        if (! agentCard.capabilities().streaming()) {
            handleError(responseObserver, new InvalidRequestError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            MessageSendParams params = FromProto.messageSendParams(request);
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onMessageSendStream(params, context);
            convertToStreamResponse(publisher, responseObserver);
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void taskSubscription(io.a2a.grpc.TaskSubscriptionRequest request,
                                 StreamObserver<io.a2a.grpc.StreamResponse> responseObserver) {
        if (! agentCard.capabilities().streaming()) {
            handleError(responseObserver, new InvalidRequestError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            TaskIdParams params = FromProto.taskIdParams(request);
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onResubscribeToTask(params, context);
            convertToStreamResponse(publisher, responseObserver);
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
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
        });
    }

    @Override
    public void getAgentCard(io.a2a.grpc.GetAgentCardRequest request,
                           StreamObserver<io.a2a.grpc.AgentCard> responseObserver) {
        try {
            responseObserver.onNext(ToProto.agentCard(agentCard));
            responseObserver.onCompleted();
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void deleteTaskPushNotificationConfig(io.a2a.grpc.DeleteTaskPushNotificationConfigRequest request,
                                               StreamObserver<Empty> responseObserver) {
        if (! agentCard.capabilities().pushNotifications()) {
            handleError(responseObserver, new PushNotificationNotSupportedError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            DeleteTaskPushNotificationConfigParams params = FromProto.deleteTaskPushNotificationConfigParams(request);
            requestHandler.onDeleteTaskPushNotificationConfig(params, context);
            // void response
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    private <V> ServerCallContext createCallContext(StreamObserver<V> responseObserver) {
        if (callContextFactory == null || callContextFactory.isUnsatisfied()) {
            // Default implementation when no custom CallContextFactory is provided
            // This handles both CDI injection scenarios and test scenarios where callContextFactory is null
            User user = UnauthenticatedUser.INSTANCE;
            Map<String, Object> state = new HashMap<>();
            
            // TODO: ARCHITECTURAL LIMITATION - StreamObserver is NOT equivalent to Python's grpc.aio.ServicerContext
            // In Python: context parameter provides metadata, peer info, cancellation, etc.
            // In Java: the proper equivalent would be ServerCall<ReqT,RespT> + Metadata from ServerInterceptor
            // Current compromise: Store StreamObserver for basic functionality, but it lacks rich context
            // 
            // FUTURE ENHANCEMENT: Implement ServerInterceptor to capture ServerCall + Metadata,
            // store in gRPC Context using Context.Key, then access via Context.current().get(key)
            // This would provide proper equivalence to Python's ServicerContext
            state.put("grpc_response_observer", responseObserver);
            
            return new ServerCallContext(user, state);
        } else {
            CallContextFactory factory = callContextFactory.get();
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

    private <V> void handleInternalError(StreamObserver<V> responseObserver, Throwable t) {
        handleError(responseObserver, new InternalError(t.getMessage()));
    }

    public static void setStreamingSubscribedRunnable(Runnable runnable) {
        streamingSubscribedRunnable = runnable;
    }

}
