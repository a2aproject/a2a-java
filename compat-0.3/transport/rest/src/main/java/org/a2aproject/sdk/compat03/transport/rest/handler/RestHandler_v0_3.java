package org.a2aproject.sdk.compat03.transport.rest.handler;

import static org.a2aproject.sdk.server.util.async.AsyncUtils.createTubeConfig;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.a2aproject.sdk.compat03.grpc.utils.ProtoUtils_v0_3;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.concurrent.Flow;

import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AuthenticatedExtendedCardNotConfiguredError_v0_3;
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
import org.a2aproject.sdk.server.util.async.Internal;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.conversion.Convert_v0_3_To10RequestHandler;
import org.a2aproject.sdk.compat03.conversion.ErrorConverter_v0_3;
import org.a2aproject.sdk.spec.A2AError;
import jakarta.enterprise.inject.Instance;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import mutiny.zero.ZeroPublisher;
import org.jspecify.annotations.Nullable;

@ApplicationScoped
public class RestHandler_v0_3 {

    private static final Logger log = Logger.getLogger(RestHandler_v0_3.class.getName());
    private AgentCard_v0_3 agentCard;
    private @Nullable
    Instance<AgentCard_v0_3> extendedAgentCard;
    private Convert_v0_3_To10RequestHandler requestHandler;
    private final Executor executor;

    @SuppressWarnings("NullAway")
    protected RestHandler_v0_3() {
        // For CDI
        this.executor = null;
    }

    @Inject
    public RestHandler_v0_3(@PublicAgentCard AgentCard_v0_3 agentCard, @ExtendedAgentCard Instance<AgentCard_v0_3> extendedAgentCard,
                            @Internal Executor executor, Convert_v0_3_To10RequestHandler requestHandler) {
        this.agentCard = agentCard;
        this.extendedAgentCard = extendedAgentCard;
        this.requestHandler = requestHandler;
        this.executor = executor;

        // TODO: Port AgentCardValidator for v0.3 AgentCard or skip validation in compat layer
        // AgentCardValidator.validateTransportConfiguration(agentCard);
    }

    public RestHandler_v0_3(AgentCard_v0_3 agentCard, Executor executor, Convert_v0_3_To10RequestHandler requestHandler) {
        this.agentCard = agentCard;
        this.executor = executor;
        this.requestHandler = requestHandler;
    }

    public HTTPRestResponse sendMessage(String body, ServerCallContext context) {
        try {
            org.a2aproject.sdk.compat03.grpc.SendMessageRequest.Builder request = org.a2aproject.sdk.compat03.grpc.SendMessageRequest.newBuilder();
            parseRequestBody(body, request);
            EventKind_v0_3 result = requestHandler.onMessageSend(ProtoUtils_v0_3.FromProto.messageSendParams(request), context);
            return createSuccessResponse(200, org.a2aproject.sdk.compat03.grpc.SendMessageResponse.newBuilder(ProtoUtils_v0_3.ToProto.taskOrMessage(result)));
        } catch (A2AError e) {
            return createErrorResponse(ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError_v0_3(throwable.getMessage()));
        }
    }

    public HTTPRestResponse sendStreamingMessage(String body, ServerCallContext context) {
        try {
            if (!agentCard.capabilities().streaming()) {
                return createErrorResponse(new InvalidRequestError_v0_3("Streaming is not supported by the agent"));
            }
            org.a2aproject.sdk.compat03.grpc.SendMessageRequest.Builder request = org.a2aproject.sdk.compat03.grpc.SendMessageRequest.newBuilder();
            parseRequestBody(body, request);
            Flow.Publisher<StreamingEventKind_v0_3> publisher = requestHandler.onMessageSendStream(ProtoUtils_v0_3.FromProto.messageSendParams(request), context);
            return createStreamingResponse(publisher);
        } catch (A2AError e) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(ErrorConverter_v0_3.convertA2AError(e)).toJson()));
        } catch (JSONRPCError_v0_3 e) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(e).toJson()));
        } catch (Throwable throwable) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(new InternalError_v0_3(throwable.getMessage())).toJson()));
        }
    }

    public HTTPRestResponse cancelTask(String taskId, ServerCallContext context) {
        try {
            if (taskId == null || taskId.isEmpty()) {
                throw new InvalidParamsError_v0_3();
            }
            TaskIdParams_v0_3 params = new TaskIdParams_v0_3(taskId);
            Task_v0_3 task = requestHandler.onCancelTask(params, context);
            if (task != null) {
                return createSuccessResponse(200, org.a2aproject.sdk.compat03.grpc.Task.newBuilder(ProtoUtils_v0_3.ToProto.task(task)));
            }
            throw new UnsupportedOperationError_v0_3();
        } catch (A2AError e) {
            return createErrorResponse(ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError_v0_3(throwable.getMessage()));
        }
    }

    public HTTPRestResponse setTaskPushNotificationConfiguration(String taskId, String body, ServerCallContext context) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError_v0_3();
            }
            org.a2aproject.sdk.compat03.grpc.CreateTaskPushNotificationConfigRequest.Builder builder = org.a2aproject.sdk.compat03.grpc.CreateTaskPushNotificationConfigRequest.newBuilder();
            parseRequestBody(body, builder);
            TaskPushNotificationConfig_v0_3 result = requestHandler.onSetTaskPushNotificationConfig(ProtoUtils_v0_3.FromProto.taskPushNotificationConfig(builder), context);
            return createSuccessResponse(201, org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig.newBuilder(ProtoUtils_v0_3.ToProto.taskPushNotificationConfig(result)));
        } catch (A2AError e) {
            return createErrorResponse(ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError_v0_3(throwable.getMessage()));
        }
    }

    public HTTPRestResponse resubscribeTask(String taskId, ServerCallContext context) {
        try {
            if (!agentCard.capabilities().streaming()) {
                return createErrorResponse(new InvalidRequestError_v0_3("Streaming is not supported by the agent"));
            }
            TaskIdParams_v0_3 params = new TaskIdParams_v0_3(taskId);
            Flow.Publisher<StreamingEventKind_v0_3> publisher = requestHandler.onResubscribeToTask(params, context);
            return createStreamingResponse(publisher);
        } catch (A2AError e) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(ErrorConverter_v0_3.convertA2AError(e)).toJson()));
        } catch (JSONRPCError_v0_3 e) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(e).toJson()));
        } catch (Throwable throwable) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(new InternalError_v0_3(throwable.getMessage())).toJson()));
        }
    }

    public HTTPRestResponse getTask(String taskId, int historyLength, ServerCallContext context) {
        try {
            TaskQueryParams_v0_3 params = new TaskQueryParams_v0_3(taskId, historyLength);
            Task_v0_3 task = requestHandler.onGetTask(params, context);
            if (task != null) {
                return createSuccessResponse(200, org.a2aproject.sdk.compat03.grpc.Task.newBuilder(ProtoUtils_v0_3.ToProto.task(task)));
            }
            throw new TaskNotFoundError_v0_3();
        } catch (A2AError e) {
            return createErrorResponse(ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError_v0_3(throwable.getMessage()));
        }
    }

    public HTTPRestResponse getTaskPushNotificationConfiguration(String taskId, @Nullable String configId, ServerCallContext context) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError_v0_3();
            }
            GetTaskPushNotificationConfigParams_v0_3 params = new GetTaskPushNotificationConfigParams_v0_3(taskId, configId);
            TaskPushNotificationConfig_v0_3 config = requestHandler.onGetTaskPushNotificationConfig(params, context);
            return createSuccessResponse(200, org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig.newBuilder(ProtoUtils_v0_3.ToProto.taskPushNotificationConfig(config)));
        } catch (A2AError e) {
            return createErrorResponse(ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError_v0_3(throwable.getMessage()));
        }
    }

    public HTTPRestResponse listTaskPushNotificationConfigurations(String taskId, ServerCallContext context) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError_v0_3();
            }
            ListTaskPushNotificationConfigParams_v0_3 params = new ListTaskPushNotificationConfigParams_v0_3(taskId);
            List<TaskPushNotificationConfig_v0_3> configs = requestHandler.onListTaskPushNotificationConfig(params, context);
            return createSuccessResponse(200, org.a2aproject.sdk.compat03.grpc.ListTaskPushNotificationConfigResponse.newBuilder(ProtoUtils_v0_3.ToProto.listTaskPushNotificationConfigResponse(configs)));
        } catch (A2AError e) {
            return createErrorResponse(ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError_v0_3(throwable.getMessage()));
        }
    }

    public HTTPRestResponse deleteTaskPushNotificationConfiguration(String taskId, String configId, ServerCallContext context) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError_v0_3();
            }
            DeleteTaskPushNotificationConfigParams_v0_3 params = new DeleteTaskPushNotificationConfigParams_v0_3(taskId, configId);
            requestHandler.onDeleteTaskPushNotificationConfig(params, context);
            return new HTTPRestResponse(204, "application/json", "");
        } catch (A2AError e) {
            return createErrorResponse(ErrorConverter_v0_3.convertA2AError(e));
        } catch (JSONRPCError_v0_3 e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError_v0_3(throwable.getMessage()));
        }
    }

    private void parseRequestBody(String body, com.google.protobuf.Message.Builder builder) throws JSONRPCError_v0_3 {
        try {
            if (body == null || body.trim().isEmpty()) {
                throw new InvalidRequestError_v0_3("Request body is required");
            }
            validate(body);
            JsonFormat.parser().merge(body, builder);
        } catch (InvalidProtocolBufferException e) {
            log.log(Level.SEVERE, "Error parsing JSON request body: {0}", body);
            log.log(Level.SEVERE, "Parse error details", e);
            throw new InvalidParamsError_v0_3("Failed to parse request body: " + e.getMessage());
        }
    }

    private void validate(String json) {
        try {
            JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            throw new JSONParseError_v0_3(JSONParseError_v0_3.DEFAULT_CODE, "Failed to parse json", e.getMessage());
        }
    }

    private HTTPRestResponse createSuccessResponse(int statusCode, com.google.protobuf.Message.Builder builder) {
        try {
            String jsonBody = JsonFormat.printer().print(builder);
            return new HTTPRestResponse(statusCode, "application/json", jsonBody);
        } catch (InvalidProtocolBufferException e) {
            return createErrorResponse(new InternalError_v0_3("Failed to serialize response: " + e.getMessage()));
        }
    }

    public HTTPRestResponse createErrorResponse(JSONRPCError_v0_3 error) {
        int statusCode = mapErrorToHttpStatus(error);
        return createErrorResponse(statusCode, error);
    }

    private HTTPRestResponse createErrorResponse(int statusCode, JSONRPCError_v0_3 error) {
        String jsonBody = new HTTPRestErrorResponse(error).toJson();
        return new HTTPRestResponse(statusCode, "application/json", jsonBody);
    }

    private HTTPRestStreamingResponse createStreamingResponse(Flow.Publisher<StreamingEventKind_v0_3> publisher) {
        return new HTTPRestStreamingResponse(convertToSendStreamingMessageResponse(publisher));
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    private Flow.Publisher<String> convertToSendStreamingMessageResponse(
            Flow.Publisher<StreamingEventKind_v0_3> publisher) {
        // We can't use the normal convertingProcessor since that propagates any errors as an error handled
        // via Subscriber.onError() rather than as part of the SendStreamingResponse payload
        return ZeroPublisher.create(createTubeConfig(), tube -> {
            CompletableFuture.runAsync(() -> {
                publisher.subscribe(new Flow.Subscriber<StreamingEventKind_v0_3>() {
                    Flow.@Nullable Subscription subscription;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        this.subscription = subscription;
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(StreamingEventKind_v0_3 item) {
                        try {
                            String payload = JsonFormat.printer().omittingInsignificantWhitespace().print(ProtoUtils_v0_3.ToProto.taskOrMessageStream(item));
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
                        if (throwable instanceof JSONRPCError_v0_3 jsonrpcError) {
                            tube.send(new HTTPRestErrorResponse(jsonrpcError).toJson());
                        } else {
                            tube.send(new HTTPRestErrorResponse(new InternalError_v0_3(throwable.getMessage())).toJson());
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

    private int mapErrorToHttpStatus(JSONRPCError_v0_3 error) {
        if (error instanceof InvalidRequestError_v0_3 || error instanceof JSONParseError_v0_3) {
            return 400;
        }
        if (error instanceof InvalidParamsError_v0_3) {
            return 422;
        }
        if (error instanceof MethodNotFoundError_v0_3 || error instanceof TaskNotFoundError_v0_3 || error instanceof AuthenticatedExtendedCardNotConfiguredError_v0_3) {
            return 404;
        }
        if (error instanceof TaskNotCancelableError_v0_3) {
            return 409;
        }
        if (error instanceof PushNotificationNotSupportedError_v0_3 || error instanceof UnsupportedOperationError_v0_3) {
            return 501;
        }
        if (error instanceof ContentTypeNotSupportedError_v0_3) {
            return 415;
        }
        if (error instanceof InvalidAgentResponseError_v0_3) {
            return 502;
        }
        if (error instanceof InternalError_v0_3) {
            return 500;
        }
        return 500;
    }

    public HTTPRestResponse getAuthenticatedExtendedCard() {
        try {
            if (!agentCard.supportsAuthenticatedExtendedCard() || extendedAgentCard == null || !extendedAgentCard.isResolvable()) {
                throw new AuthenticatedExtendedCardNotConfiguredError_v0_3(null, "Authenticated Extended Card not configured", null);
            }
            return new HTTPRestResponse(200, "application/json", JsonUtil_v0_3.toJson(extendedAgentCard.get()));
        } catch (JSONRPCError_v0_3 e) {
            return createErrorResponse(e);
        } catch (Throwable t) {
            return createErrorResponse(500, new InternalError_v0_3(t.getMessage()));
        }
    }

    public HTTPRestResponse getAgentCard() {
        try {
            return new HTTPRestResponse(200, "application/json", JsonUtil_v0_3.toJson(agentCard));
        } catch (Throwable t) {
            return createErrorResponse(500, new InternalError_v0_3(t.getMessage()));
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

        private HTTPRestErrorResponse(JSONRPCError_v0_3 jsonRpcError) {
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
