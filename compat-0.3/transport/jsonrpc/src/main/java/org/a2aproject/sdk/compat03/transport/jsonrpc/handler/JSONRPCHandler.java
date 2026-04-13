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
import mutiny.zero.ZeroPublisher;

@ApplicationScoped
public class JSONRPCHandler {

    private AgentCard agentCard;
    private Instance<AgentCard> extendedAgentCard;
    // TODO: Translation layer - translate from v0.3 types to current SDK types before calling requestHandler
    // private RequestHandler requestHandler;
    private final Executor executor;

    protected JSONRPCHandler() {
        this.executor = null;
    }

    @Inject
    public JSONRPCHandler(@PublicAgentCard AgentCard agentCard, @ExtendedAgentCard Instance<AgentCard> extendedAgentCard,
                          @Internal Executor executor) {
        this.agentCard = agentCard;
        this.extendedAgentCard = extendedAgentCard;
        // this.requestHandler = requestHandler;
        this.executor = executor;

        // TODO: Port AgentCardValidator for v0.3 AgentCard or skip validation in compat layer
        // AgentCardValidator.validateTransportConfiguration(agentCard);
    }

    public JSONRPCHandler(@PublicAgentCard AgentCard agentCard, Executor executor) {
        this(agentCard, null, executor);
    }

    public SendMessageResponse onMessageSend(SendMessageRequest request, ServerCallContext context) {
        // TODO: Translate v0.3 request.getParams() to current SDK types, call requestHandler, translate response back
        return new SendMessageResponse(request.getId(), new InternalError("Not yet implemented - translation layer pending"));
    }

    public Flow.Publisher<SendStreamingMessageResponse> onMessageSendStream(
            SendStreamingMessageRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().streaming()) {
            return ZeroPublisher.fromItems(
                    new SendStreamingMessageResponse(
                            request.getId(),
                            new InvalidRequestError("Streaming is not supported by the agent")));
        }

        // TODO: Translate v0.3 request.getParams() to current SDK types, call requestHandler.onMessageSendStream(),
        // translate StreamingEventKind responses back to v0.3 types
        return ZeroPublisher.fromItems(new SendStreamingMessageResponse(request.getId(),
                new InternalError("Not yet implemented - translation layer pending")));
    }

    public CancelTaskResponse onCancelTask(CancelTaskRequest request, ServerCallContext context) {
        // TODO: Translate v0.3 request.getParams() to current SDK types, call requestHandler, translate Task response back
        return new CancelTaskResponse(request.getId(), new InternalError("Not yet implemented - translation layer pending"));
    }

    public Flow.Publisher<SendStreamingMessageResponse> onResubscribeToTask(
            TaskResubscriptionRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().streaming()) {
            return ZeroPublisher.fromItems(
                    new SendStreamingMessageResponse(
                            request.getId(),
                            new InvalidRequestError("Streaming is not supported by the agent")));
        }

        // TODO: Translate v0.3 request.getParams() to current SDK types, call requestHandler.onResubscribeToTask(),
        // translate StreamingEventKind responses back to v0.3 types
        return ZeroPublisher.fromItems(new SendStreamingMessageResponse(request.getId(),
                new InternalError("Not yet implemented - translation layer pending")));
    }

    public GetTaskPushNotificationConfigResponse getPushNotificationConfig(
            GetTaskPushNotificationConfigRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new GetTaskPushNotificationConfigResponse(request.getId(),
                    new PushNotificationNotSupportedError());
        }
        // TODO: Translate v0.3 request.getParams() to current SDK types, call requestHandler, translate config back
        return new GetTaskPushNotificationConfigResponse(request.getId(),
                new InternalError("Not yet implemented - translation layer pending"));
    }

    public SetTaskPushNotificationConfigResponse setPushNotificationConfig(
            SetTaskPushNotificationConfigRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new SetTaskPushNotificationConfigResponse(request.getId(),
                    new PushNotificationNotSupportedError());
        }
        // TODO: Translate v0.3 request.getParams() to current SDK types, call requestHandler, translate config back
        return new SetTaskPushNotificationConfigResponse(request.getId(),
                new InternalError("Not yet implemented - translation layer pending"));
    }

    public GetTaskResponse onGetTask(GetTaskRequest request, ServerCallContext context) {
        // TODO: Translate v0.3 request.getParams() to current SDK types, call requestHandler, translate Task back
        return new GetTaskResponse(request.getId(), new InternalError("Not yet implemented - translation layer pending"));
    }

    public ListTaskPushNotificationConfigResponse listPushNotificationConfig(
            ListTaskPushNotificationConfigRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new ListTaskPushNotificationConfigResponse(request.getId(),
                    new PushNotificationNotSupportedError());
        }
        // TODO: Translate v0.3 request.getParams() to current SDK types, call requestHandler, translate configs back
        return new ListTaskPushNotificationConfigResponse(request.getId(),
                new InternalError("Not yet implemented - translation layer pending"));
    }

    public DeleteTaskPushNotificationConfigResponse deletePushNotificationConfig(
            DeleteTaskPushNotificationConfigRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new DeleteTaskPushNotificationConfigResponse(request.getId(),
                    new PushNotificationNotSupportedError());
        }
        // TODO: Translate v0.3 request.getParams() to current SDK types, call requestHandler
        return new DeleteTaskPushNotificationConfigResponse(request.getId(),
                new InternalError("Not yet implemented - translation layer pending"));
    }

    // TODO: Add authentication (https://github.com/a2aproject/a2a-java/issues/77)
    public GetAuthenticatedExtendedCardResponse onGetAuthenticatedExtendedCardRequest(
            GetAuthenticatedExtendedCardRequest request, ServerCallContext context) {
        if (!agentCard.supportsAuthenticatedExtendedCard() || !extendedAgentCard.isResolvable()) {
            return new GetAuthenticatedExtendedCardResponse(request.getId(),
                    new AuthenticatedExtendedCardNotConfiguredError());
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
