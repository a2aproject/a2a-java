package org.a2aproject.sdk.compat03.transport.grpc.handler;

import static org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils_v0_3.FromProto;
import static org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils_v0_3.ToProto;

import jakarta.enterprise.inject.Vetoed;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import com.google.protobuf.Empty;
import org.a2aproject.sdk.compat03.conversion.Convert_v0_3_To10RequestHandler;
import org.a2aproject.sdk.compat03.conversion.ErrorConverter_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.ContentTypeNotSupportedError_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.InternalError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidAgentResponseError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONParseError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigParams_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.MethodNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskNotCancelableError_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.compat03.spec.UnsupportedOperationError_v0_3;
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
public abstract class GrpcHandler_v0_3 extends org.a2aproject.sdk.compat03.grpc.A2AServiceGrpc.A2AServiceImplBase {

    private Convert_v0_3_To10RequestHandler requestHandler;

    // Hook so testing can wait until streaming is subscribed.
    // Without this we get intermittent failures
    private static volatile Runnable streamingSubscribedRunnable;

    protected abstract AgentCard_v0_3 getAgentCard();

    protected abstract CallContextFactory_v0_3 getCallContextFactory();

    protected abstract Executor getExecutor();

    protected Convert_v0_3_To10RequestHandler getRequestHandler() {
        return requestHandler;
    }

    protected void setRequestHandler(Convert_v0_3_To10RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    public void sendMessage(org.a2aproject.sdk.compat03.grpc.SendMessageRequest request,
                           StreamObserver<org.a2aproject.sdk.compat03.grpc.SendMessageResponse> responseObserver) {
        try {
            ServerCallContext context = createCallContext(responseObserver);
            MessageSendParams_v0_3 params = FromProto.messageSendParams(request);
            EventKind_v0_3 taskOrMessage = requestHandler.onMessageSend(params, context);
            org.a2aproject.sdk.compat03.grpc.SendMessageResponse response = ToProto.taskOrMessage(taskOrMessage);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
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
            TaskQueryParams_v0_3 params = FromProto.taskQueryParams(request);
            Task_v0_3 task = requestHandler.onGetTask(params, context);
            if (task != null) {
                responseObserver.onNext(ToProto.task(task));
                responseObserver.onCompleted();
            } else {
                handleError(responseObserver, new TaskNotFoundError_v0_3());
            }
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
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
            TaskIdParams_v0_3 params = FromProto.taskIdParams(request);
            Task_v0_3 task = requestHandler.onCancelTask(params, context);
            if (task != null) {
                responseObserver.onNext(ToProto.task(task));
                responseObserver.onCompleted();
            } else {
                handleError(responseObserver, new TaskNotFoundError_v0_3());
            }
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void createTaskPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.CreateTaskPushNotificationConfigRequest request,
                                               StreamObserver<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> responseObserver) {
        if (!getAgentCard().capabilities().pushNotifications()) {
            handleError(responseObserver, new PushNotificationNotSupportedError_v0_3());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            TaskPushNotificationConfig_v0_3 config = FromProto.taskPushNotificationConfig(request);
            TaskPushNotificationConfig_v0_3 responseConfig = requestHandler.onSetTaskPushNotificationConfig(config, context);
            responseObserver.onNext(ToProto.taskPushNotificationConfig(responseConfig));
            responseObserver.onCompleted();
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void getTaskPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.GetTaskPushNotificationConfigRequest request,
                                            StreamObserver<org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig> responseObserver) {
        if (!getAgentCard().capabilities().pushNotifications()) {
            handleError(responseObserver, new PushNotificationNotSupportedError_v0_3());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            GetTaskPushNotificationConfigParams_v0_3 params = FromProto.getTaskPushNotificationConfigParams(request);
            TaskPushNotificationConfig_v0_3 config = requestHandler.onGetTaskPushNotificationConfig(params, context);
            responseObserver.onNext(ToProto.taskPushNotificationConfig(config));
            responseObserver.onCompleted();
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void listTaskPushNotificationConfig(org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigRequest request,
                                             StreamObserver<org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse> responseObserver) {
        if (!getAgentCard().capabilities().pushNotifications()) {
            handleError(responseObserver, new PushNotificationNotSupportedError_v0_3());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            ListTaskPushNotificationConfigParams_v0_3 params = FromProto.listTaskPushNotificationConfigParams(request);
            List<TaskPushNotificationConfig_v0_3> configList = requestHandler.onListTaskPushNotificationConfig(params, context);
            org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse.Builder responseBuilder =
                org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse.newBuilder();
            for (TaskPushNotificationConfig_v0_3 config : configList) {
                responseBuilder.addConfigs(ToProto.taskPushNotificationConfig(config));
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void sendStreamingMessage(org.a2aproject.sdk.compat03.grpc.SendMessageRequest request,
                                     StreamObserver<org.a2aproject.sdk.compat03.grpc.StreamResponse> responseObserver) {
        if (!getAgentCard().capabilities().streaming()) {
            handleError(responseObserver, new InvalidRequestError_v0_3("Streaming is not supported by the agent"));
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            MessageSendParams_v0_3 params = FromProto.messageSendParams(request);
            Flow.Publisher<StreamingEventKind_v0_3> publisher = requestHandler.onMessageSendStream(params, context);
            convertToStreamResponse(publisher, responseObserver);
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    @Override
    public void taskSubscription(org.a2aproject.sdk.compat03.grpc.TaskSubscriptionRequest request,
                                 StreamObserver<org.a2aproject.sdk.compat03.grpc.StreamResponse> responseObserver) {
        if (!getAgentCard().capabilities().streaming()) {
            handleError(responseObserver, new InvalidRequestError_v0_3("Streaming is not supported by the agent"));
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            TaskIdParams_v0_3 params = FromProto.taskIdParams(request);
            Flow.Publisher<StreamingEventKind_v0_3> publisher = requestHandler.onResubscribeToTask(params, context);
            convertToStreamResponse(publisher, responseObserver);
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    private void convertToStreamResponse(Flow.Publisher<StreamingEventKind_v0_3> publisher,
                                         StreamObserver<org.a2aproject.sdk.compat03.grpc.StreamResponse> responseObserver) {
        CompletableFuture.runAsync(() -> {
            publisher.subscribe(new Flow.Subscriber<StreamingEventKind_v0_3>() {
                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    if (streamingSubscribedRunnable != null) {
                        streamingSubscribedRunnable.run();
                    }
                    subscription.request(1);
                }

                @Override
                public void onNext(StreamingEventKind_v0_3 event) {
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
                        handleError(responseObserver, ErrorConverter_v0_3.convertA2AError(a2aError));
                    } else if (throwable instanceof JSONRPCError_v0_3 jsonrpcError) {
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
            handleError(responseObserver, new PushNotificationNotSupportedError_v0_3());
            return;
        }

        try {
            ServerCallContext context = createCallContext(responseObserver);
            DeleteTaskPushNotificationConfigParams_v0_3 params = FromProto.deleteTaskPushNotificationConfigParams(request);
            requestHandler.onDeleteTaskPushNotificationConfig(params, context);
            // void response
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (A2AError e) {
            handleError(responseObserver, ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
            handleError(responseObserver, e);
        } catch (Throwable t) {
            handleInternalError(responseObserver, t);
        }
    }

    private <V> ServerCallContext createCallContext(StreamObserver<V> responseObserver) {
        CallContextFactory_v0_3 factory = getCallContextFactory();
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

    private <V> void handleError(StreamObserver<V> responseObserver, JSONRPCError_v0_3 error) {
        Status status;
        String description;
        if (error instanceof InvalidRequestError_v0_3) {
            status = Status.INVALID_ARGUMENT;
            description = "InvalidRequestError: " + error.getMessage();
        } else if (error instanceof MethodNotFoundError_v0_3) {
            status = Status.NOT_FOUND;
            description = "MethodNotFoundError: " + error.getMessage();
        } else if (error instanceof InvalidParamsError_v0_3) {
            status = Status.INVALID_ARGUMENT;
            description = "InvalidParamsError: " + error.getMessage();
        } else if (error instanceof InternalError_v0_3) {
            status = Status.INTERNAL;
            description = "InternalError: " + error.getMessage();
        } else if (error instanceof TaskNotFoundError_v0_3) {
            status = Status.NOT_FOUND;
            description = "TaskNotFoundError: " + error.getMessage();
        } else if (error instanceof TaskNotCancelableError_v0_3) {
            status = Status.UNIMPLEMENTED;
            description = "TaskNotCancelableError: " + error.getMessage();
        } else if (error instanceof PushNotificationNotSupportedError_v0_3) {
            status = Status.UNIMPLEMENTED;
            description = "PushNotificationNotSupportedError: " + error.getMessage();
        } else if (error instanceof UnsupportedOperationError_v0_3) {
            status = Status.UNIMPLEMENTED;
            description = "UnsupportedOperationError: " + error.getMessage();
        } else if (error instanceof JSONParseError_v0_3) {
            status = Status.INTERNAL;
            description = "JSONParseError: " + error.getMessage();
        } else if (error instanceof ContentTypeNotSupportedError_v0_3) {
            status = Status.UNIMPLEMENTED;
            description = "ContentTypeNotSupportedError: " + error.getMessage();
        } else if (error instanceof InvalidAgentResponseError_v0_3) {
            status = Status.INTERNAL;
            description = "InvalidAgentResponseError: " + error.getMessage();
        } else {
            status = Status.UNKNOWN;
            description = "Unknown error type: " + error.getMessage();
        }
        responseObserver.onError(status.withDescription(description).asRuntimeException());
    }

    private <V> void handleInternalError(StreamObserver<V> responseObserver, Throwable t) {
        handleError(responseObserver, new InternalError_v0_3(t.getMessage()));
    }

    public static void setStreamingSubscribedRunnable(Runnable runnable) {
        streamingSubscribedRunnable = runnable;
    }
}
