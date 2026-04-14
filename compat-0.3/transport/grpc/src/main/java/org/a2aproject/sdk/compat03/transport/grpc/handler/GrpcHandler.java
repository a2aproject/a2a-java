package org.a2aproject.sdk.compat03.transport.grpc.handler;

import static org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils.FromProto;
import static org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils.ToProto;

import jakarta.enterprise.inject.Vetoed;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import com.google.protobuf.Empty;
import org.a2aproject.sdk.compat03.conversion.Convert03To10RequestHandler;
import org.a2aproject.sdk.compat03.conversion.ErrorConverter;
import org.a2aproject.sdk.compat03.spec.AgentCard;
import org.a2aproject.sdk.compat03.spec.ContentTypeNotSupportedError;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.compat03.spec.EventKind;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.compat03.spec.InternalError;
import org.a2aproject.sdk.compat03.spec.InvalidAgentResponseError;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError;
import org.a2aproject.sdk.compat03.spec.JSONParseError;
import org.a2aproject.sdk.compat03.spec.JSONRPCError;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams;
import org.a2aproject.sdk.compat03.spec.MessageSendParams;
import org.a2aproject.sdk.compat03.spec.MethodNotFoundError;
import org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind;
import org.a2aproject.sdk.compat03.spec.Task;
import org.a2aproject.sdk.compat03.spec.TaskIdParams;
import org.a2aproject.sdk.compat03.spec.TaskNotCancelableError;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams;
import org.a2aproject.sdk.compat03.spec.UnsupportedOperationError;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.auth.User;
import org.a2aproject.sdk.spec.A2AError;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract gRPC handler for v0.3 protocol with translation layer to v1.0.
 */
@Vetoed
public abstract class GrpcHandler extends org.a2aproject.sdk.compat03.grpc.A2AServiceGrpc.A2AServiceImplBase {

    private Convert03To10RequestHandler requestHandler;

    protected abstract AgentCard getAgentCard();

    protected abstract CallContextFactory getCallContextFactory();

    protected abstract Executor getExecutor();

    protected Convert03To10RequestHandler getRequestHandler() {
        return requestHandler;
    }

    protected void setRequestHandler(Convert03To10RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    public void sendMessage(org.a2aproject.sdk.compat03.grpc.SendMessageRequest request,
                           StreamObserver<org.a2aproject.sdk.compat03.grpc.SendMessageResponse> responseObserver) {
        try {
            ServerCallContext context = createCallContext(responseObserver);
            MessageSendParams params = FromProto.messageSendParams(request);
            EventKind taskOrMessage = requestHandler.onMessageSend(params, context);
            org.a2aproject.sdk.compat03.grpc.SendMessageResponse response = ToProto.taskOrMessage(taskOrMessage);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter.convertA2AError(e));
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void getTask(org.a2aproject.sdk.compat03.grpc.GetTaskRequest request,
                       StreamObserver<org.a2aproject.sdk.compat03.grpc.Task> responseObserver) {
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
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter.convertA2AError(e));
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void cancelTask(org.a2aproject.sdk.compat03.grpc.CancelTaskRequest request,
                          StreamObserver<org.a2aproject.sdk.compat03.grpc.Task> responseObserver) {
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
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter.convertA2AError(e));
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void createTaskPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.CreateTaskPushNotificationConfigRequest request,
                                               StreamObserver<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> responseObserver) {
        if (!getAgentCard().capabilities().pushNotifications()) {
            handleError(responseObserver, new PushNotificationNotSupportedError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            TaskPushNotificationConfig config = FromProto.taskPushNotificationConfig(request);
            TaskPushNotificationConfig responseConfig = requestHandler.onSetTaskPushNotificationConfig(config, context);
            responseObserver.onNext(ToProto.taskPushNotificationConfig(responseConfig));
            responseObserver.onCompleted();
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter.convertA2AError(e));
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void getTaskPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.GetTaskPushNotificationConfigRequest request,
                                            StreamObserver<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> responseObserver) {
        if (!getAgentCard().capabilities().pushNotifications()) {
            handleError(responseObserver, new PushNotificationNotSupportedError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            GetTaskPushNotificationConfigParams params = FromProto.getTaskPushNotificationConfigParams(request);
            TaskPushNotificationConfig config = requestHandler.onGetTaskPushNotificationConfig(params, context);
            responseObserver.onNext(ToProto.taskPushNotificationConfig(config));
            responseObserver.onCompleted();
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter.convertA2AError(e));
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void listTaskPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigRequest request,
                                             StreamObserver<org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse> responseObserver) {
        if (!getAgentCard().capabilities().pushNotifications()) {
            handleError(responseObserver, new PushNotificationNotSupportedError());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            ListTaskPushNotificationConfigParams params = FromProto.listTaskPushNotificationConfigParams(request);
            List<TaskPushNotificationConfig> configList = requestHandler.onListTaskPushNotificationConfig(params, context);
            org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse.Builder responseBuilder =
                org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse.newBuilder();
            for (TaskPushNotificationConfig config : configList) {
                responseBuilder.addConfigs(ToProto.taskPushNotificationConfig(config));
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter.convertA2AError(e));
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void sendStreamingMessage(org.a2aproject.sdk.compat03.grpc.SendMessageRequest request,
                                     StreamObserver<org.a2aproject.sdk.compat03.grpc.StreamResponse> responseObserver) {
        if (!getAgentCard().capabilities().streaming()) {
            handleError(responseObserver, new InvalidRequestError("Streaming is not supported by the agent"));
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            MessageSendParams params = FromProto.messageSendParams(request);
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onMessageSendStream(params, context);
            convertToStreamResponse(publisher, responseObserver);
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter.convertA2AError(e));
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void taskSubscription(org.a2aproject.sdk.compat03.grpc.TaskSubscriptionRequest request,
                                 StreamObserver<org.a2aproject.sdk.compat03.grpc.StreamResponse> responseObserver) {
        if (!getAgentCard().capabilities().streaming()) {
            handleError(responseObserver, new InvalidRequestError("Streaming is not supported by the agent"));
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            TaskIdParams params = FromProto.taskIdParams(request);
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onResubscribeToTask(params, context);
            convertToStreamResponse(publisher, responseObserver);
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter.convertA2AError(e));
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    private void convertToStreamResponse(Flow.Publisher<StreamingEventKind> publisher,
                                         StreamObserver<org.a2aproject.sdk.compat03.grpc.StreamResponse> responseObserver) {
        CompletableFuture.runAsync(() -> {
            publisher.subscribe(new Flow.Subscriber<StreamingEventKind>() {
                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    subscription.request(1);
                }

                @Override
                public void onNext(StreamingEventKind event) {
                    org.a2aproject.sdk.compat03.grpc.StreamResponse response = ToProto.streamResponse(event);
                    responseObserver.onNext(response);
                    if (response.hasStatusUpdate() && response.getStatusUpdate().getFinal()) {
                        responseObserver.onCompleted();
                    } else {
                        subscription.request(1);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    if (throwable instanceof A2AError a2aError) {
                        handleError(responseObserver, ErrorConverter.convertA2AError(a2aError));
                    } else if (throwable instanceof JSONRPCError jsonrpcError) {
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
    public void getAgentCard(org.a2aproject.sdk.compat03.grpc.GetAgentCardRequest request,
                           StreamObserver<org.a2aproject.sdk.compat03.grpc.AgentCard> responseObserver) {
        try {
            responseObserver.onNext(ToProto.agentCard(getAgentCard()));
            responseObserver.onCompleted();
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void deleteTaskPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.DeleteTaskPushNotificationConfigRequest request,
                                               StreamObserver<Empty> responseObserver) {
        if (!getAgentCard().capabilities().pushNotifications()) {
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
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter.convertA2AError(e));
        } catch (JSONRPCError e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    private <V> ServerCallContext createCallContext(StreamObserver<V> responseObserver) {
        CallContextFactory factory = getCallContextFactory();
        if (factory == null) {
            // Default implementation when no custom CallContextFactory is provided
            User user = UnauthenticatedUser.INSTANCE;
            Map<String, Object> state = new HashMap<>();
            state.put("grpc_response_observer", responseObserver);
            Set<String> requestedExtensions = new HashSet<>();
            return new ServerCallContext(user, state, requestedExtensions);
        } else {
            return factory.create(responseObserver);
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
}
