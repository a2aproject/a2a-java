package io.a2a.jsonrpc.handler;

import static io.a2a.server.util.async.AsyncUtils.createTubeConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.concurrent.Flow;

import io.a2a.server.PublicAgentCard;
import io.a2a.server.ServerCallContext;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.AgentCard;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigResponse;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.ListTaskPushNotificationConfigResponse;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskResubscriptionRequest;
import mutiny.zero.ZeroPublisher;

@ApplicationScoped
public class JSONRPCHandler {

    private AgentCard agentCard;
    private RequestHandler requestHandler;

    protected JSONRPCHandler() {
    }

    @Inject
    public JSONRPCHandler(@PublicAgentCard AgentCard agentCard, RequestHandler requestHandler) {
        this.agentCard = agentCard;
        this.requestHandler = requestHandler;
    }

    public SendMessageResponse onMessageSend(SendMessageRequest request, ServerCallContext context) {
        try {
            EventKind taskOrMessage = requestHandler.onMessageSend(request.getParams(), context);
            return new SendMessageResponse(request.getId(), taskOrMessage);
        } catch (JSONRPCError e) {
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
            Flow.Publisher<StreamingEventKind> publisher =
                    requestHandler.onMessageSendStream(request.getParams(), context);
            // We can't use the convertingProcessor convenience method since that propagates any errors as an error handled
            // via Subscriber.onError() rather than as part of the SendStreamingResponse payload
            return convertToSendStreamingMessageResponse(request.getId(), publisher);
        } catch (JSONRPCError e) {
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
        } catch (JSONRPCError e) {
            return new CancelTaskResponse(request.getId(), e);
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
            Flow.Publisher<StreamingEventKind> publisher =
                    requestHandler.onResubscribeToTask(request.getParams(), context);
            // We can't use the convertingProcessor convenience method since that propagates any errors as an error handled
            // via Subscriber.onError() rather than as part of the SendStreamingResponse payload
            return convertToSendStreamingMessageResponse(request.getId(), publisher);
        } catch (JSONRPCError e) {
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
        } catch (JSONRPCError e) {
            return new GetTaskPushNotificationConfigResponse(request.getId().toString(), e);
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
            TaskPushNotificationConfig config =
                    requestHandler.onSetTaskPushNotificationConfig(request.getParams(), context);
            return new SetTaskPushNotificationConfigResponse(request.getId().toString(), config);
        } catch (JSONRPCError e) {
            return new SetTaskPushNotificationConfigResponse(request.getId(), e);
        } catch (Throwable t) {
            return new SetTaskPushNotificationConfigResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public GetTaskResponse onGetTask(GetTaskRequest request, ServerCallContext context) {
        try {
            Task task = requestHandler.onGetTask(request.getParams(), context);
            return new GetTaskResponse(request.getId(), task);
        } catch (JSONRPCError e) {
            return new GetTaskResponse(request.getId(), e);
        } catch (Throwable t) {
            return new GetTaskResponse(request.getId(), new InternalError(t.getMessage()));
        }
    }

    public ListTaskPushNotificationConfigResponse listPushNotificationConfig(
            ListTaskPushNotificationConfigRequest request, ServerCallContext context) {
        if ( !agentCard.capabilities().pushNotifications()) {
            return new ListTaskPushNotificationConfigResponse(request.getId(),
                    new PushNotificationNotSupportedError());
        }
        try {
            List<TaskPushNotificationConfig> pushNotificationConfigList =
                    requestHandler.onListTaskPushNotificationConfig(request.getParams(), context);
            return new ListTaskPushNotificationConfigResponse(request.getId(), pushNotificationConfigList);
        } catch (JSONRPCError e) {
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
        } catch (JSONRPCError e) {
            return new DeleteTaskPushNotificationConfigResponse(request.getId(), e);
        } catch (Throwable t) {
            return new DeleteTaskPushNotificationConfigResponse(request.getId(), new InternalError(t.getMessage()));
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
            });
    }
}
