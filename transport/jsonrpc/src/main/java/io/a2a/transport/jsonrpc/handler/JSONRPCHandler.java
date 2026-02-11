package io.a2a.transport.jsonrpc.handler;

import static io.a2a.server.util.async.AsyncUtils.createTubeConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.a2a.jsonrpc.common.wrappers.CancelTaskRequest;
import io.a2a.jsonrpc.common.wrappers.CancelTaskResponse;
import io.a2a.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.GetExtendedAgentCardRequest;
import io.a2a.jsonrpc.common.wrappers.GetExtendedAgentCardResponse;
import io.a2a.jsonrpc.common.wrappers.GetTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.GetTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.GetTaskRequest;
import io.a2a.jsonrpc.common.wrappers.GetTaskResponse;
import io.a2a.jsonrpc.common.wrappers.ListTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.ListTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.ListTasksRequest;
import io.a2a.jsonrpc.common.wrappers.ListTasksResponse;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.jsonrpc.common.wrappers.SendMessageRequest;
import io.a2a.jsonrpc.common.wrappers.SendMessageResponse;
import io.a2a.jsonrpc.common.wrappers.SendStreamingMessageRequest;
import io.a2a.jsonrpc.common.wrappers.SendStreamingMessageResponse;
import io.a2a.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.SubscribeToTaskRequest;
import io.a2a.server.AgentCardValidator;
import io.a2a.server.ExtendedAgentCard;
import io.a2a.server.PublicAgentCard;
import io.a2a.server.ServerCallContext;
import io.a2a.server.extensions.A2AExtensions;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.util.async.Internal;
import io.a2a.server.version.A2AVersionValidator;
import io.a2a.spec.A2AError;
import io.a2a.spec.AgentCard;
import io.a2a.spec.ExtendedAgentCardNotConfiguredError;
import io.a2a.spec.EventKind;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;

import mutiny.zero.ZeroPublisher;
import org.jspecify.annotations.Nullable;

@ApplicationScoped
public class JSONRPCHandler {

    // Fields set by constructor injection cannot be final. We need a noargs constructor for
    // Jakarta compatibility, and it seems that making fields set by constructor injection
    // final, is not proxyable in all runtimes
    private AgentCard agentCard;
    private @Nullable Instance<AgentCard> extendedAgentCard;
    private RequestHandler requestHandler;
    private Executor executor;

    /**
     * No-args constructor for CDI proxy creation.
     * CDI requires a non-private constructor to create proxies for @ApplicationScoped beans.
     * All fields are initialized by the @Inject constructor during actual bean creation.
     */
    @SuppressWarnings("NullAway")
    protected JSONRPCHandler() {
        // For CDI proxy creation
        this.agentCard = null;
        this.extendedAgentCard = null;
        this.requestHandler = null;
        this.executor = null;
    }

    @Inject
    public JSONRPCHandler(@PublicAgentCard AgentCard agentCard, @Nullable @ExtendedAgentCard Instance<AgentCard> extendedAgentCard,
                          RequestHandler requestHandler, @Internal Executor executor) {
        this.agentCard = agentCard;
        this.extendedAgentCard = extendedAgentCard;
        this.requestHandler = requestHandler;
        this.executor = executor;

        // Validate transport configuration
        AgentCardValidator.validateTransportConfiguration(agentCard);
    }

    public JSONRPCHandler(@PublicAgentCard AgentCard agentCard, RequestHandler requestHandler, Executor executor) {
        this(agentCard, null, requestHandler, executor);
    }

    public SendMessageResponse onMessageSend(SendMessageRequest request, ServerCallContext context) {
        try {
            A2AVersionValidator.validateProtocolVersion(agentCard, context);
            A2AExtensions.validateRequiredExtensions(agentCard, context);
            EventKind taskOrMessage = requestHandler.onMessageSend(request.getParams(), context);
            return new SendMessageResponse(request.getId(), taskOrMessage);
        } catch (A2AError e) {
            return new SendMessageResponse(request.getId(), e);
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
            A2AVersionValidator.validateProtocolVersion(agentCard, context);
            A2AExtensions.validateRequiredExtensions(agentCard, context);
            Flow.Publisher<StreamingEventKind> publisher =
                    requestHandler.onMessageSendStream(request.getParams(), context);
            // We can't use the convertingProcessor convenience method since that propagates any errors as an error handled
            // via Subscriber.onError() rather than as part of the SendStreamingResponse payload
            return convertToSendStreamingMessageResponse(request.getId(), publisher);
        } catch (A2AError e) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse(request.getId(), e));
        } catch (Throwable throwable) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse(request.getId(), new InternalError(throwable.getMessage())));
        }
    }

    public CancelTaskResponse onCancelTask(CancelTaskRequest request, ServerCallContext context) {
        try {
            Task task = requestHandler.onCancelTask(request.getParams(), context);
            if (task != null) {
                return new CancelTaskResponse(request.getId(), task);
            }
            return new CancelTaskResponse(request.getId(), new TaskNotFoundError());
        } catch (A2AError e) {
            return new CancelTaskResponse(request.getId(), e);
        } catch (Throwable t) {
            return new CancelTaskResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public Flow.Publisher<SendStreamingMessageResponse> onSubscribeToTask(
            SubscribeToTaskRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().streaming()) {
            return ZeroPublisher.fromItems(
                    new SendStreamingMessageResponse(
                            request.getId(),
                            new InvalidRequestError("Streaming is not supported by the agent")));
        }

        try {
            Flow.Publisher<StreamingEventKind> publisher =
                    requestHandler.onSubscribeToTask(request.getParams(), context);
            // We can't use the convertingProcessor convenience method since that propagates any errors as an error handled
            // via Subscriber.onError() rather than as part of the SendStreamingResponse payload
            return convertToSendStreamingMessageResponse(request.getId(), publisher);
        } catch (A2AError e) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse(request.getId(), e));
        } catch (Throwable throwable) {
            return ZeroPublisher.fromItems(new SendStreamingMessageResponse(request.getId(), new InternalError(throwable.getMessage())));
        }
    }

    public GetTaskPushNotificationConfigResponse getPushNotificationConfig(
            GetTaskPushNotificationConfigRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new GetTaskPushNotificationConfigResponse(request.getId(),
                    new PushNotificationNotSupportedError());
        }
        try {
            TaskPushNotificationConfig config =
                    requestHandler.onGetTaskPushNotificationConfig(request.getParams(), context);
            return new GetTaskPushNotificationConfigResponse(request.getId(), config);
        } catch (A2AError e) {
            return new GetTaskPushNotificationConfigResponse(request.getId().toString(), e);
        } catch (Throwable t) {
            return new GetTaskPushNotificationConfigResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public CreateTaskPushNotificationConfigResponse setPushNotificationConfig(
            CreateTaskPushNotificationConfigRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().pushNotifications()) {
            return new CreateTaskPushNotificationConfigResponse(request.getId(),
                    new PushNotificationNotSupportedError());
        }
        try {
            TaskPushNotificationConfig config =
                    requestHandler.onCreateTaskPushNotificationConfig(request.getParams(), context);
            return new CreateTaskPushNotificationConfigResponse(request.getId().toString(), config);
        } catch (A2AError e) {
            return new CreateTaskPushNotificationConfigResponse(request.getId(), e);
        } catch (Throwable t) {
            return new CreateTaskPushNotificationConfigResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public GetTaskResponse onGetTask(GetTaskRequest request, ServerCallContext context) {
        try {
            Task task = requestHandler.onGetTask(request.getParams(), context);
            return new GetTaskResponse(request.getId(), task);
        } catch (A2AError e) {
            return new GetTaskResponse(request.getId(), e);
        } catch (Throwable t) {
            return new GetTaskResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public ListTasksResponse onListTasks(ListTasksRequest request, ServerCallContext context) {
        try {
            ListTasksResult result = requestHandler.onListTasks(request.getParams(), context);
            return new ListTasksResponse(request.getId(), result);
        } catch (A2AError e) {
            return new ListTasksResponse(request.getId(), e);
        } catch (Throwable t) {
            return new ListTasksResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public ListTaskPushNotificationConfigResponse listPushNotificationConfig(
            ListTaskPushNotificationConfigRequest request, ServerCallContext context) {
        if ( !agentCard.capabilities().pushNotifications()) {
            return new ListTaskPushNotificationConfigResponse(request.getId(),
                    new PushNotificationNotSupportedError());
        }
        try {
            ListTaskPushNotificationConfigResult result =
                    requestHandler.onListTaskPushNotificationConfig(request.getParams(), context);
            return new ListTaskPushNotificationConfigResponse(request.getId(), result);
        } catch (A2AError e) {
            return new ListTaskPushNotificationConfigResponse(request.getId(), e);
        } catch (Throwable t) {
            return new ListTaskPushNotificationConfigResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public DeleteTaskPushNotificationConfigResponse deletePushNotificationConfig(
            DeleteTaskPushNotificationConfigRequest request, ServerCallContext context) {
        if ( !agentCard.capabilities().pushNotifications()) {
            return new DeleteTaskPushNotificationConfigResponse(request.getId(),
                    new PushNotificationNotSupportedError());
        }
        try {
            requestHandler.onDeleteTaskPushNotificationConfig(request.getParams(), context);
            return new DeleteTaskPushNotificationConfigResponse(request.getId());
        } catch (A2AError e) {
            return new DeleteTaskPushNotificationConfigResponse(request.getId(), e);
        } catch (Throwable t) {
            return new DeleteTaskPushNotificationConfigResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    // TODO: Add authentication (https://github.com/a2aproject/a2a-java/issues/77)
    public GetExtendedAgentCardResponse onGetExtendedCardRequest(
            GetExtendedAgentCardRequest request, ServerCallContext context) {
        if (!agentCard.capabilities().extendedAgentCard() || extendedAgentCard == null || !extendedAgentCard.isResolvable()) {
            return new GetExtendedAgentCardResponse(request.getId(),
                    new ExtendedAgentCardNotConfiguredError(null, "Extended Card not configured", null));
        }
        try {
            return new GetExtendedAgentCardResponse(request.getId(), extendedAgentCard.get());
        } catch (A2AError e) {
            return new GetExtendedAgentCardResponse(request.getId(), e);
        } catch (Throwable t) {
            return new GetExtendedAgentCardResponse(request.getId(), new InternalError(t.getMessage()));
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
                        @SuppressWarnings("NullAway")
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
                            if (throwable instanceof A2AError jsonrpcError) {
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
