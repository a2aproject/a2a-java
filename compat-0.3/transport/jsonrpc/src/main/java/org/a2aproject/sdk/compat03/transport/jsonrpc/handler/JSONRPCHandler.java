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
import org.a2aproject.sdk.compat03.spec.AgentCard;
import org.a2aproject.sdk.compat03.spec.AuthenticatedExtendedCardNotConfiguredError;
import org.a2aproject.sdk.compat03.spec.CancelTaskRequest;
import org.a2aproject.sdk.compat03.spec.CancelTaskResponse;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.compat03.spec.EventKind;
import org.a2aproject.sdk.compat03.spec.GetAuthenticatedExtendedCardRequest;
import org.a2aproject.sdk.compat03.spec.GetAuthenticatedExtendedCardResponse;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.compat03.spec.GetTaskRequest;
import org.a2aproject.sdk.compat03.spec.GetTaskResponse;
import org.a2aproject.sdk.compat03.spec.InternalError;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError;
import org.a2aproject.sdk.compat03.spec.JSONRPCError;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.compat03.spec.PushNotificationNotSupportedError;
import org.a2aproject.sdk.compat03.spec.SendMessageRequest;
import org.a2aproject.sdk.compat03.spec.SendMessageResponse;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageRequest;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageResponse;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.compat03.spec.StreamingEventKind;
import org.a2aproject.sdk.compat03.spec.Task;
import org.a2aproject.sdk.compat03.spec.TaskNotFoundError;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.compat03.spec.TaskResubscriptionRequest;
import org.a2aproject.sdk.server.util.async.Internal;
import org.a2aproject.sdk.compat03.conversion.Convert03To10RequestHandler;
import org.a2aproject.sdk.compat03.conversion.ErrorConverter;
import org.a2aproject.sdk.spec.A2AError;
import mutiny.zero.ZeroPublisher;

@ApplicationScoped
public class JSONRPCHandler {

    private AgentCard agentCard;
    private Instance<AgentCard> extendedAgentCard;
    private Convert03To10RequestHandler requestHandler;
    private final Executor executor;

    protected JSONRPCHandler() {
        this.executor = null;
    }

    @Inject
    public JSONRPCHandler(@PublicAgentCard AgentCard agentCard, @ExtendedAgentCard Instance<AgentCard> extendedAgentCard,
                          @Internal Executor executor, Convert03To10RequestHandler requestHandler) {
        this.agentCard = agentCard;
        this.extendedAgentCard = extendedAgentCard;
        this.requestHandler = requestHandler;
        this.executor = executor;

        // TODO: Port AgentCardValidator for v0.3 AgentCard or skip validation in compat layer
        // AgentCardValidator.validateTransportConfiguration(agentCard);
    }

    public JSONRPCHandler(@PublicAgentCard AgentCard agentCard, Executor executor, Convert03To10RequestHandler requestHandler) {
        this(agentCard, null, executor, requestHandler);
    }

    public SendMessageResponse onMessageSend(SendMessageRequest request, ServerCallContext context) {
        try {
            EventKind result = requestHandler.onMessageSend(request.getParams(), context);
            return new SendMessageResponse(request.getId(), result);
        } catch (A2AError e) {
            return new SendMessageResponse(request.getId(), ErrorConverter.convertA2AError(e));
        } catch (Throwable t) {
            return new SendMessageResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public Flow.Publisher<SendStreamingMessageResponse> onMessageSendStream(
            SendStreamingMessageRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().streaming()) {
            return ZeroPublisher.fromItems(
                    new SendStreamingMessageResponse(
                            request.getId(),
                            new InvalidRequestError("Streaming is not supported by the agent")));
        }

        try {
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onMessageSendStream(request.getParams(), context);
            return convertToSendStreamingMessageResponse(request.getId(), publisher);
        } catch (A2AError e) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse(request.getId(), ErrorConverter.convertA2AError(e)));
        } catch (Throwable t) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse(request.getId(), new InternalError(t.getMessage())));
        }
    }

    public CancelTaskResponse onCancelTask(CancelTaskRequest request, ServerCallContext context) {
        try {
            Task result = requestHandler.onCancelTask(request.getParams(), context);
            return new CancelTaskResponse(request.getId(), result);
        } catch (A2AError e) {
            return new CancelTaskResponse(request.getId(), ErrorConverter.convertA2AError(e));
        } catch (Throwable t) {
            return new CancelTaskResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public Flow.Publisher<SendStreamingMessageResponse> onResubscribeToTask(
            TaskResubscriptionRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().streaming()) {
            return ZeroPublisher.fromItems(
                    new SendStreamingMessageResponse(
                            request.getId(),
                            new InvalidRequestError("Streaming is not supported by the agent")));
        }

        try {
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onResubscribeToTask(request.getParams(), context);
            return convertToSendStreamingMessageResponse(request.getId(), publisher);
        } catch (A2AError e) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse(request.getId(), ErrorConverter.convertA2AError(e)));
        } catch (Throwable t) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse(request.getId(), new InternalError(t.getMessage())));
        }
    }

    public GetTaskPushNotificationConfigResponse getPushNotificationConfig(
            GetTaskPushNotificationConfigRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new GetTaskPushNotificationConfigResponse(request.getId(),
                    new PushNotificationNotSupportedError());
        }
        try {
            TaskPushNotificationConfig result = requestHandler.onGetTaskPushNotificationConfig(request.getParams(), context);
            return new GetTaskPushNotificationConfigResponse(request.getId(), result);
        } catch (A2AError e) {
            return new GetTaskPushNotificationConfigResponse(request.getId(), ErrorConverter.convertA2AError(e));
        } catch (Throwable t) {
            return new GetTaskPushNotificationConfigResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public SetTaskPushNotificationConfigResponse setPushNotificationConfig(
            SetTaskPushNotificationConfigRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new SetTaskPushNotificationConfigResponse(request.getId(),
                    new PushNotificationNotSupportedError());
        }
        try {
            TaskPushNotificationConfig result = requestHandler.onSetTaskPushNotificationConfig(request.getParams(), context);
            return new SetTaskPushNotificationConfigResponse(request.getId(), result);
        } catch (A2AError e) {
            return new SetTaskPushNotificationConfigResponse(request.getId(), ErrorConverter.convertA2AError(e));
        } catch (Throwable t) {
            return new SetTaskPushNotificationConfigResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public GetTaskResponse onGetTask(GetTaskRequest request, ServerCallContext context) {
        try {
            Task result = requestHandler.onGetTask(request.getParams(), context);
            return new GetTaskResponse(request.getId(), result);
        } catch (A2AError e) {
            return new GetTaskResponse(request.getId(), ErrorConverter.convertA2AError(e));
        } catch (Throwable t) {
            return new GetTaskResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public ListTaskPushNotificationConfigResponse listPushNotificationConfig(
            ListTaskPushNotificationConfigRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new ListTaskPushNotificationConfigResponse(request.getId(),
                    new PushNotificationNotSupportedError());
        }
        try {
            List<TaskPushNotificationConfig> pushNotificationConfigList =
                    requestHandler.onListTaskPushNotificationConfig(request.getParams(), context);
            return new ListTaskPushNotificationConfigResponse(request.getId(), pushNotificationConfigList);
        } catch (A2AError e) {
            return new ListTaskPushNotificationConfigResponse(request.getId(), ErrorConverter.convertA2AError(e));
        } catch (Throwable t) {
            return new ListTaskPushNotificationConfigResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public DeleteTaskPushNotificationConfigResponse deletePushNotificationConfig(
            DeleteTaskPushNotificationConfigRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new DeleteTaskPushNotificationConfigResponse(request.getId(),
                    new PushNotificationNotSupportedError());
        }
        try {
            requestHandler.onDeleteTaskPushNotificationConfig(request.getParams(), context);
            return new DeleteTaskPushNotificationConfigResponse(request.getId());
        } catch (A2AError e) {
            return new DeleteTaskPushNotificationConfigResponse(request.getId(), ErrorConverter.convertA2AError(e));
        } catch (Throwable t) {
            return new DeleteTaskPushNotificationConfigResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    // TODO: Add authentication (https://github.com/a2aproject/a2a-java/issues/77)
    public GetAuthenticatedExtendedCardResponse onGetAuthenticatedExtendedCardRequest(
            GetAuthenticatedExtendedCardRequest request, ServerCallContext context) {
        if (!agentCard.supportsAuthenticatedExtendedCard() || !extendedAgentCard.isResolvable()) {
            return new GetAuthenticatedExtendedCardResponse(request.getId(),
                    new AuthenticatedExtendedCardNotConfiguredError(null, "Authenticated Extended Card not configured", null));
        }
        try {
            return new GetAuthenticatedExtendedCardResponse(request.getId(), extendedAgentCard.get());
        } catch (JSONRPCError e) {
            return new GetAuthenticatedExtendedCardResponse(request.getId(), e);
        } catch (Throwable t) {
            return new GetAuthenticatedExtendedCardResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public AgentCard getAgentCard() {
        return agentCard;
    }

    private Flow.Publisher<SendStreamingMessageResponse> convertToSendStreamingMessageResponse(
            Object requestId,
            Flow.Publisher<StreamingEventKind> publisher) {
        // We can't use the normal convertingProcessor since that propagates any errors as an error handled
        // via Subscriber.onError() rather than as part of the SendStreamingResponse payload
        return ZeroPublisher.create(createTubeConfig(), tube -> {
            CompletableFuture.runAsync(() -> {
                publisher.subscribe(new Flow.Subscriber<StreamingEventKind>() {
                    Flow.Subscription subscription;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        this.subscription = subscription;
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(StreamingEventKind item) {
                        tube.send(new SendStreamingMessageResponse(requestId, item));
                        subscription.request(1);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if (throwable instanceof JSONRPCError jsonrpcError) {
                            tube.send(new SendStreamingMessageResponse(requestId, jsonrpcError));
                        } else {
                            tube.send(
                                    new SendStreamingMessageResponse(
                                            requestId, new
                                            InternalError(throwable.getMessage())));
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
