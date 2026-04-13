package org.a2aproject.sdk.compat03.transport.rest.handler;

import static org.a2aproject.sdk.server.util.async.AsyncUtils.createTubeConfig;

import com.fasterxml.jackson.core.JacksonException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.concurrent.Flow;

import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.compat03.spec.AgentCard;
import org.a2aproject.sdk.compat03.spec.AuthenticatedExtendedCardNotConfiguredError;
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
import org.a2aproject.sdk.server.util.async.Internal;
import org.a2aproject.sdk.compat03.util.Utils;
import jakarta.enterprise.inject.Instance;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import mutiny.zero.ZeroPublisher;
import org.jspecify.annotations.Nullable;

@ApplicationScoped
public class RestHandler {

    private static final Logger log = Logger.getLogger(RestHandler.class.getName());
    private AgentCard agentCard;
    private @Nullable
    Instance<AgentCard> extendedAgentCard;
    // TODO: Translation layer - translate from v0.3 types to current SDK types before calling requestHandler
    // private RequestHandler requestHandler;
    private final Executor executor;

    @SuppressWarnings("NullAway")
    protected RestHandler() {
        // For CDI
        this.executor = null;
    }

    @Inject
    public RestHandler(@PublicAgentCard AgentCard agentCard, @ExtendedAgentCard Instance<AgentCard> extendedAgentCard,
            @Internal Executor executor) {
        this.agentCard = agentCard;
        this.extendedAgentCard = extendedAgentCard;
        // this.requestHandler = requestHandler;
        this.executor = executor;

        // TODO: Port AgentCardValidator for v0.3 AgentCard or skip validation in compat layer
        // AgentCardValidator.validateTransportConfiguration(agentCard);
    }

    public RestHandler(AgentCard agentCard, Executor executor) {
        this.agentCard = agentCard;
        this.executor = executor;
    }

    public HTTPRestResponse sendMessage(String body, ServerCallContext context) {
        try {
            org.a2aproject.sdk.compat03.grpc.SendMessageRequest.Builder request = org.a2aproject.sdk.compat03.grpc.SendMessageRequest.newBuilder();
            parseRequestBody(body, request);
            // TODO: Translate v0.3 request to current SDK types, call requestHandler.onMessageSend(), translate response back
            // EventKind result = requestHandler.onMessageSend(ProtoUtils.FromProto.messageSendParams(request), context);
            // return createSuccessResponse(200, org.a2aproject.sdk.compat03.grpc.SendMessageResponse.newBuilder(ProtoUtils.ToProto.taskOrMessage(result)));
            return createErrorResponse(new InternalError("Not yet implemented - translation layer pending"));
        } catch (JSONRPCError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    public HTTPRestResponse sendStreamingMessage(String body, ServerCallContext context) {
        try {
            if (!agentCard.capabilities().streaming()) {
                return createErrorResponse(new InvalidRequestError("Streaming is not supported by the agent"));
            }
            org.a2aproject.sdk.compat03.grpc.SendMessageRequest.Builder request = org.a2aproject.sdk.compat03.grpc.SendMessageRequest.newBuilder();
            parseRequestBody(body, request);
            // TODO: Translate v0.3 request to current SDK types, call requestHandler.onMessageSendStream(), translate response back
            // Flow.Publisher<StreamingEventKind> publisher = requestHandler.onMessageSendStream(ProtoUtils.FromProto.messageSendParams(request), context);
            // return createStreamingResponse(publisher);
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(new InternalError("Not yet implemented - translation layer pending")).toJson()));
        } catch (JSONRPCError e) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(e).toJson()));
        } catch (Throwable throwable) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(new InternalError(throwable.getMessage())).toJson()));
        }
    }

    public HTTPRestResponse cancelTask(String taskId, ServerCallContext context) {
        try {
            if (taskId == null || taskId.isEmpty()) {
                throw new InvalidParamsError();
            }
            TaskIdParams params = new TaskIdParams(taskId);
            // TODO: Translate v0.3 params to current SDK types, call requestHandler.onCancelTask(), translate Task response back
            // Task task = requestHandler.onCancelTask(params, context);
            // if (task != null) {
            //     return createSuccessResponse(200, org.a2aproject.sdk.compat03.grpc.Task.newBuilder(ProtoUtils.ToProto.task(task)));
            // }
            // throw new UnsupportedOperationError();
            return createErrorResponse(new InternalError("Not yet implemented - translation layer pending"));
        } catch (JSONRPCError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    public HTTPRestResponse setTaskPushNotificationConfiguration(String taskId, String body, ServerCallContext context) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            org.a2aproject.sdk.compat03.grpc.CreateTaskPushNotificationConfigRequest.Builder builder = org.a2aproject.sdk.compat03.grpc.CreateTaskPushNotificationConfigRequest.newBuilder();
            parseRequestBody(body, builder);
            // TODO: Translate v0.3 request to current SDK types, call requestHandler.onSetTaskPushNotificationConfig(), translate response back
            // TaskPushNotificationConfig result = requestHandler.onSetTaskPushNotificationConfig(ProtoUtils.FromProto.taskPushNotificationConfig(builder), context);
            // return createSuccessResponse(201, org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig.newBuilder(ProtoUtils.ToProto.taskPushNotificationConfig(result)));
            return createErrorResponse(new InternalError("Not yet implemented - translation layer pending"));
        } catch (JSONRPCError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    public HTTPRestResponse resubscribeTask(String taskId, ServerCallContext context) {
        try {
            if (!agentCard.capabilities().streaming()) {
                return createErrorResponse(new InvalidRequestError("Streaming is not supported by the agent"));
            }
            TaskIdParams params = new TaskIdParams(taskId);
            // TODO: Translate v0.3 params to current SDK types, call requestHandler.onResubscribeToTask(), translate response back
            // Flow.Publisher<StreamingEventKind> publisher = requestHandler.onResubscribeToTask(params, context);
            // return createStreamingResponse(publisher);
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(new InternalError("Not yet implemented - translation layer pending")).toJson()));
        } catch (JSONRPCError e) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(e).toJson()));
        } catch (Throwable throwable) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(new InternalError(throwable.getMessage())).toJson()));
        }
    }

    public HTTPRestResponse getTask(String taskId, int historyLength, ServerCallContext context) {
        try {
            TaskQueryParams params = new TaskQueryParams(taskId, historyLength);
            // TODO: Translate v0.3 params to current SDK types, call requestHandler.onGetTask(), translate Task response back
            // Task task = requestHandler.onGetTask(params, context);
            // if (task != null) {
            //     return createSuccessResponse(200, org.a2aproject.sdk.compat03.grpc.Task.newBuilder(ProtoUtils.ToProto.task(task)));
            // }
            // throw new TaskNotFoundError();
            return createErrorResponse(new InternalError("Not yet implemented - translation layer pending"));
        } catch (JSONRPCError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    public HTTPRestResponse getTaskPushNotificationConfiguration(String taskId, @Nullable String configId, ServerCallContext context) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams(taskId, configId);
            // TODO: Translate v0.3 params to current SDK types, call requestHandler.onGetTaskPushNotificationConfig(), translate response back
            // TaskPushNotificationConfig config = requestHandler.onGetTaskPushNotificationConfig(params, context);
            // return createSuccessResponse(200, org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig.newBuilder(ProtoUtils.ToProto.taskPushNotificationConfig(config)));
            return createErrorResponse(new InternalError("Not yet implemented - translation layer pending"));
        } catch (JSONRPCError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    public HTTPRestResponse listTaskPushNotificationConfigurations(String taskId, ServerCallContext context) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId);
            // TODO: Translate v0.3 params to current SDK types, call requestHandler.onListTaskPushNotificationConfig(), translate response back
            // List<TaskPushNotificationConfig> configs = requestHandler.onListTaskPushNotificationConfig(params, context);
            // return createSuccessResponse(200, org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse.newBuilder(ProtoUtils.ToProto.listTaskPushNotificationConfigResponse(configs)));
            return createErrorResponse(new InternalError("Not yet implemented - translation layer pending"));
        } catch (JSONRPCError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    public HTTPRestResponse deleteTaskPushNotificationConfiguration(String taskId, String configId, ServerCallContext context) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            DeleteTaskPushNotificationConfigParams params = new DeleteTaskPushNotificationConfigParams(taskId, configId);
            // TODO: Translate v0.3 params to current SDK types, call requestHandler.onDeleteTaskPushNotificationConfig()
            // requestHandler.onDeleteTaskPushNotificationConfig(params, context);
            // return new HTTPRestResponse(204, "application/json", "");
            return createErrorResponse(new InternalError("Not yet implemented - translation layer pending"));
        } catch (JSONRPCError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    private void parseRequestBody(String body, com.google.protobuf.Message.Builder builder) throws JSONRPCError {
        try {
            if (body == null || body.trim().isEmpty()) {
                throw new InvalidRequestError("Request body is required");
            }
            validate(body);
            JsonFormat.parser().merge(body, builder);
        } catch (InvalidProtocolBufferException e) {
            log.log(Level.SEVERE, "Error parsing JSON request body: {0}", body);
            log.log(Level.SEVERE, "Parse error details", e);
            throw new InvalidParamsError("Failed to parse request body: " + e.getMessage());
        }
    }

    private void validate(String json) {
        try {
            Utils.OBJECT_MAPPER.readTree(json);
        } catch (JacksonException e) {
            throw new JSONParseError(JSONParseError.DEFAULT_CODE, "Failed to parse json", e.getMessage());
        }
    }

    private HTTPRestResponse createSuccessResponse(int statusCode, com.google.protobuf.Message.Builder builder) {
        try {
            String jsonBody = JsonFormat.printer().print(builder);
            return new HTTPRestResponse(statusCode, "application/json", jsonBody);
        } catch (InvalidProtocolBufferException e) {
            return createErrorResponse(new InternalError("Failed to serialize response: " + e.getMessage()));
        }
    }

    public HTTPRestResponse createErrorResponse(JSONRPCError error) {
        int statusCode = mapErrorToHttpStatus(error);
        return createErrorResponse(statusCode, error);
    }

    private HTTPRestResponse createErrorResponse(int statusCode, JSONRPCError error) {
        String jsonBody = new HTTPRestErrorResponse(error).toJson();
        return new HTTPRestResponse(statusCode, "application/json", jsonBody);
    }

    private HTTPRestStreamingResponse createStreamingResponse(Flow.Publisher<StreamingEventKind> publisher) {
        return new HTTPRestStreamingResponse(convertToSendStreamingMessageResponse(publisher));
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private Flow.Publisher<String> convertToSendStreamingMessageResponse(
            Flow.Publisher<StreamingEventKind> publisher) {
        // We can't use the normal convertingProcessor since that propagates any errors as an error handled
        // via Subscriber.onError() rather than as part of the SendStreamingResponse payload
        return ZeroPublisher.create(createTubeConfig(), tube -> {
            CompletableFuture.runAsync(() -> {
                publisher.subscribe(new Flow.Subscriber<StreamingEventKind>() {
                    Flow.@Nullable Subscription subscription;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        this.subscription = subscription;
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(StreamingEventKind item) {
                        try {
                            String payload = JsonFormat.printer().omittingInsignificantWhitespace().print(ProtoUtils.ToProto.taskOrMessageStream(item));
                            tube.send(payload);
                            if (subscription != null) {
                                subscription.request(1);
                            }
                        } catch (InvalidProtocolBufferException ex) {
                            onError(ex);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        if (throwable instanceof JSONRPCError jsonrpcError) {
                            tube.send(new HTTPRestErrorResponse(jsonrpcError).toJson());
                        } else {
                            tube.send(new HTTPRestErrorResponse(new InternalError(throwable.getMessage())).toJson());
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

    private int mapErrorToHttpStatus(JSONRPCError error) {
        if (error instanceof InvalidRequestError || error instanceof JSONParseError) {
            return 400;
        }
        if (error instanceof InvalidParamsError) {
            return 422;
        }
        if (error instanceof MethodNotFoundError || error instanceof TaskNotFoundError || error instanceof AuthenticatedExtendedCardNotConfiguredError) {
            return 404;
        }
        if (error instanceof TaskNotCancelableError) {
            return 409;
        }
        if (error instanceof PushNotificationNotSupportedError || error instanceof UnsupportedOperationError) {
            return 501;
        }
        if (error instanceof ContentTypeNotSupportedError) {
            return 415;
        }
        if (error instanceof InvalidAgentResponseError) {
            return 502;
        }
        if (error instanceof InternalError) {
            return 500;
        }
        return 500;
    }

    public HTTPRestResponse getAuthenticatedExtendedCard() {
        try {
            if (!agentCard.supportsAuthenticatedExtendedCard() || extendedAgentCard == null || !extendedAgentCard.isResolvable()) {
                throw new AuthenticatedExtendedCardNotConfiguredError();
            }
            return new HTTPRestResponse(200, "application/json", Utils.OBJECT_MAPPER.writeValueAsString(extendedAgentCard.get()));
        } catch (JSONRPCError e) {
            return createErrorResponse(e);
        } catch (Throwable t) {
            return createErrorResponse(500, new InternalError(t.getMessage()));
        }
    }

    public HTTPRestResponse getAgentCard() {
        try {
            return new HTTPRestResponse(200, "application/json", Utils.OBJECT_MAPPER.writeValueAsString(agentCard));
        } catch (Throwable t) {
            return createErrorResponse(500, new InternalError(t.getMessage()));
        }
    }

    public static class HTTPRestResponse {

        private final int statusCode;
        private final String contentType;
        private final String body;

        public HTTPRestResponse(int statusCode, String contentType, String body) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.body = body;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getContentType() {
            return contentType;
        }

        public String getBody() {
            return body;
        }

        @Override
        public String toString() {
            return "HTTPRestResponse{" + "statusCode=" + statusCode + ", contentType=" + contentType + ", body=" + body + '}';
        }
    }

    public static class HTTPRestStreamingResponse extends HTTPRestResponse {

        private final Flow.Publisher<String> publisher;

        public HTTPRestStreamingResponse(Flow.Publisher<String> publisher) {
            super(200, "text/event-stream", "");
            this.publisher = publisher;
        }

        public Flow.Publisher<String> getPublisher() {
            return publisher;
        }
    }

    private static class HTTPRestErrorResponse {

        private final String error;
        private final @Nullable
        String message;

        private HTTPRestErrorResponse(JSONRPCError jsonRpcError) {
            this.error = jsonRpcError.getClass().getName();
            this.message = jsonRpcError.getMessage();
        }

        private String toJson() {
            return "{\"error\": \"" + error + "\", \"message\": \"" + message + "\"}";
        }

        @Override
        public String toString() {
            return "HTTPRestErrorResponse{" + "error=" + error + ", message=" + message + '}';
        }
    }
}
