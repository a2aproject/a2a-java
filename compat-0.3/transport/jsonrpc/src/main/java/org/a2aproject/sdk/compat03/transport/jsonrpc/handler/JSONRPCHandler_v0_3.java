package org.a2aproject.sdk.compat03.transport.jsonrpc.handler;

import static org.a2aproject.sdk.server.util.async.AsyncUtils.createTubeConfig;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AuthenticatedExtendedCardNotConfiguredError_v0_3;
import org.a2aproject.sdk.compat03.spec.CancelTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.CancelTaskResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.GetAuthenticatedExtendedCardRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetAuthenticatedExtendedCardResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.InternalError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError_v0_3;
import org.a2aproject.sdk.compat03.spec.SendMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendMessageResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskResubscriptionRequest_v0_3;
import org.a2aproject.sdk.server.util.async.Internal;
import org.a2aproject.sdk.compat03.conversion.Convert_v0_3_To10RequestHandler;
import org.a2aproject.sdk.compat03.conversion.ErrorConverter_v0_3;
import org.a2aproject.sdk.spec.A2AError;
import mutiny.zero.ZeroPublisher;

@ApplicationScoped
public class JSONRPCHandler_v0_3 {

    private AgentCard_v0_3 agentCard;
    private Instance<AgentCard_v0_3> extendedAgentCard;
    private Convert_v0_3_To10RequestHandler requestHandler;
    private final Executor executor;

    protected JSONRPCHandler_v0_3() {
        this.executor = null;
    }

    @Inject
    public JSONRPCHandler_v0_3(@PublicAgentCard AgentCard_v0_3 agentCard, @ExtendedAgentCard Instance<AgentCard_v0_3> extendedAgentCard,
                               @Internal Executor executor, Convert_v0_3_To10RequestHandler requestHandler) {
        this.agentCard = agentCard;
        this.extendedAgentCard = extendedAgentCard;
        this.requestHandler = requestHandler;
        this.executor = executor;

        // TODO: Port AgentCardValidator for v0.3 AgentCard or skip validation in compat layer
        // AgentCardValidator.validateTransportConfiguration(agentCard);
    }

    public JSONRPCHandler_v0_3(@PublicAgentCard AgentCard_v0_3 agentCard, Executor executor, Convert_v0_3_To10RequestHandler requestHandler) {
        this(agentCard, null, executor, requestHandler);
    }

    public SendMessageResponse_v0_3 onMessageSend(SendMessageRequest_v0_3 request, ServerCallContext context) {
        try {
            EventKind_v0_3 result = requestHandler.onMessageSend(request.getParams(), context);
            return new SendMessageResponse_v0_3(request.getId(), result);
        } catch (A2AError e) {
            return new SendMessageResponse_v0_3(request.getId(), ErrorConverter_v0_3.convertA2AError(e));
        } catch (Throwable t) {
            return new SendMessageResponse_v0_3(request.getId(), new InternalError_v0_3(t.getMessage()));
        }
    }

    public Flow.Publisher<SendStreamingMessageResponse_v0_3> onMessageSendStream(
            SendStreamingMessageRequest_v0_3 request, ServerCallContext context) {
        if (!agentCard.capabilities().streaming()) {
            return ZeroPublisher.fromItems(
                    new SendStreamingMessageResponse_v0_3(
                            request.getId(),
                            new InvalidRequestError_v0_3("Streaming is not supported by the agent")));
        }

        try {
            Flow.Publisher<StreamingEventKind_v0_3> publisher = requestHandler.onMessageSendStream(request.getParams(), context);
            return convertToSendStreamingMessageResponse(request.getId(), publisher);
        } catch (A2AError e) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse_v0_3(request.getId(), ErrorConverter_v0_3.convertA2AError(e)));
        } catch (Throwable t) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse_v0_3(request.getId(), new InternalError_v0_3(t.getMessage())));
        }
    }

    public CancelTaskResponse_v0_3 onCancelTask(CancelTaskRequest_v0_3 request, ServerCallContext context) {
        try {
            Task_v0_3 result = requestHandler.onCancelTask(request.getParams(), context);
            return new CancelTaskResponse_v0_3(request.getId(), result);
        } catch (A2AError e) {
            return new CancelTaskResponse_v0_3(request.getId(), ErrorConverter_v0_3.convertA2AError(e));
        } catch (Throwable t) {
            return new CancelTaskResponse_v0_3(request.getId(), new InternalError_v0_3(t.getMessage()));
        }
    }

    public Flow.Publisher<SendStreamingMessageResponse_v0_3> onResubscribeToTask(
            TaskResubscriptionRequest_v0_3 request, ServerCallContext context) {
        if (!agentCard.capabilities().streaming()) {
            return ZeroPublisher.fromItems(
                    new SendStreamingMessageResponse_v0_3(
                            request.getId(),
                            new InvalidRequestError_v0_3("Streaming is not supported by the agent")));
        }

        try {
            Flow.Publisher<StreamingEventKind_v0_3> publisher = requestHandler.onResubscribeToTask(request.getParams(), context);
            return convertToSendStreamingMessageResponse(request.getId(), publisher);
        } catch (A2AError e) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse_v0_3(request.getId(), ErrorConverter_v0_3.convertA2AError(e)));
        } catch (Throwable t) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse_v0_3(request.getId(), new InternalError_v0_3(t.getMessage())));
        }
    }

    public GetTaskPushNotificationConfigResponse_v0_3 getPushNotificationConfig(
            GetTaskPushNotificationConfigRequest_v0_3 request, ServerCallContext context) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new GetTaskPushNotificationConfigResponse_v0_3(request.getId(),
                    new PushNotificationNotSupportedError_v0_3());
        }
        try {
            TaskPushNotificationConfig_v0_3 result = requestHandler.onGetTaskPushNotificationConfig(request.getParams(), context);
            return new GetTaskPushNotificationConfigResponse_v0_3(request.getId(), result);
        } catch (A2AError e) {
            return new GetTaskPushNotificationConfigResponse_v0_3(request.getId(), ErrorConverter_v0_3.convertA2AError(e));
        } catch (Throwable t) {
            return new GetTaskPushNotificationConfigResponse_v0_3(request.getId(), new InternalError_v0_3(t.getMessage()));
        }
    }

    public SetTaskPushNotificationConfigResponse_v0_3 setPushNotificationConfig(
            SetTaskPushNotificationConfigRequest_v0_3 request, ServerCallContext context) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new SetTaskPushNotificationConfigResponse_v0_3(request.getId(),
                    new PushNotificationNotSupportedError_v0_3());
        }
        try {
            TaskPushNotificationConfig_v0_3 result = requestHandler.onSetTaskPushNotificationConfig(request.getParams(), context);
            return new SetTaskPushNotificationConfigResponse_v0_3(request.getId(), result);
        } catch (A2AError e) {
            return new SetTaskPushNotificationConfigResponse_v0_3(request.getId(), ErrorConverter_v0_3.convertA2AError(e));
        } catch (Throwable t) {
            return new SetTaskPushNotificationConfigResponse_v0_3(request.getId(), new InternalError_v0_3(t.getMessage()));
        }
    }

    public GetTaskResponse_v0_3 onGetTask(GetTaskRequest_v0_3 request, ServerCallContext context) {
        try {
            Task_v0_3 result = requestHandler.onGetTask(request.getParams(), context);
            return new GetTaskResponse_v0_3(request.getId(), result);
        } catch (A2AError e) {
            return new GetTaskResponse_v0_3(request.getId(), ErrorConverter_v0_3.convertA2AError(e));
        } catch (Throwable t) {
            return new GetTaskResponse_v0_3(request.getId(), new InternalError_v0_3(t.getMessage()));
        }
    }

    public ListTaskPushNotificationConfigResponse_v0_3 listPushNotificationConfig(
            ListTaskPushNotificationConfigRequest_v0_3 request, ServerCallContext context) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new ListTaskPushNotificationConfigResponse_v0_3(request.getId(),
                    new PushNotificationNotSupportedError_v0_3());
        }
        try {
            List<TaskPushNotificationConfig_v0_3> pushNotificationConfigList =
                    requestHandler.onListTaskPushNotificationConfig(request.getParams(), context);
            return new ListTaskPushNotificationConfigResponse_v0_3(request.getId(), pushNotificationConfigList);
        } catch (A2AError e) {
            return new ListTaskPushNotificationConfigResponse_v0_3(request.getId(), ErrorConverter_v0_3.convertA2AError(e));
        } catch (Throwable t) {
            return new ListTaskPushNotificationConfigResponse_v0_3(request.getId(), new InternalError_v0_3(t.getMessage()));
        }
    }

    public DeleteTaskPushNotificationConfigResponse_v0_3 deletePushNotificationConfig(
            DeleteTaskPushNotificationConfigRequest_v0_3 request, ServerCallContext context) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new DeleteTaskPushNotificationConfigResponse_v0_3(request.getId(),
                    new PushNotificationNotSupportedError_v0_3());
        }
        try {
            requestHandler.onDeleteTaskPushNotificationConfig(request.getParams(), context);
            return new DeleteTaskPushNotificationConfigResponse_v0_3(request.getId());
        } catch (A2AError e) {
            return new DeleteTaskPushNotificationConfigResponse_v0_3(request.getId(), ErrorConverter_v0_3.convertA2AError(e));
        } catch (Throwable t) {
            return new DeleteTaskPushNotificationConfigResponse_v0_3(request.getId(), new InternalError_v0_3(t.getMessage()));
        }
    }

    // TODO: Add authentication (https://github.com/a2aproject/a2a-java/issues/77)
    public GetAuthenticatedExtendedCardResponse_v0_3 onGetAuthenticatedExtendedCardRequest(
            GetAuthenticatedExtendedCardRequest_v0_3 request, ServerCallContext context) {
        if (!agentCard.supportsAuthenticatedExtendedCard() || !extendedAgentCard.isResolvable()) {
            return new GetAuthenticatedExtendedCardResponse_v0_3(request.getId(),
                    new AuthenticatedExtendedCardNotConfiguredError_v0_3(null, "Authenticated Extended Card not configured", null));
        }
        try {
            return new GetAuthenticatedExtendedCardResponse_v0_3(request.getId(), extendedAgentCard.get());
        } catch (JSONRPCError_v0_3 e) {
            return new GetAuthenticatedExtendedCardResponse_v0_3(request.getId(), e);
        } catch (Throwable t) {
            return new GetAuthenticatedExtendedCardResponse_v0_3(request.getId(), new InternalError_v0_3(t.getMessage()));
        }
    }

    public AgentCard_v0_3 getAgentCard() {
        return agentCard;
    }

    private Flow.Publisher<SendStreamingMessageResponse_v0_3> convertToSendStreamingMessageResponse(
            Object requestId,
            Flow.Publisher<StreamingEventKind_v0_3> publisher) {
        // We can't use the normal convertingProcessor since that propagates any errors as an error handled
        // via Subscriber.onError() rather than as part of the SendStreamingResponse payload
        return ZeroPublisher.create(createTubeConfig(), tube -> {
            CompletableFuture.runAsync(() -> {
                publisher.subscribe(new Flow.Subscriber<StreamingEventKind_v0_3>() {
                    Flow.Subscription subscription;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        this.subscription = subscription;
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(StreamingEventKind_v0_3 item) {
                        tube.send(new SendStreamingMessageResponse_v0_3(requestId, item));
                        subscription.request(1);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if (throwable instanceof JSONRPCError_v0_3 jsonrpcError) {
                            tube.send(new SendStreamingMessageResponse_v0_3(requestId, jsonrpcError));
                        } else {
                            tube.send(
                                    new SendStreamingMessageResponse_v0_3(
                                            requestId, new
                                            InternalError_v0_3(throwable.getMessage())));
                        }
                        onComplete();
                    }

                    @Override
                    public void onComplete() {
                        tube.complete();
                    }
                });
            }, executor);
        });
    }
}
