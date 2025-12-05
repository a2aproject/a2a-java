package io.a2a.transport.rest.handler;

import static io.a2a.server.util.async.AsyncUtils.createTubeConfig;
import static io.a2a.spec.A2AErrorCodes.JSON_PARSE_ERROR_CODE;

import com.fasterxml.jackson.core.JacksonException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.server.AgentCardValidator;
import io.a2a.server.ExtendedAgentCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

import io.a2a.server.PublicAgentCard;
import io.a2a.server.ServerCallContext;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AuthenticatedExtendedCardNotConfiguredError;
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
import io.a2a.spec.ListTasksParams;
import io.a2a.spec.ListTasksResult;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.server.util.async.Internal;
import io.a2a.util.Utils;
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
    private @Nullable Instance<AgentCard> extendedAgentCard;
    private RequestHandler requestHandler;
    private final Executor executor;

    @SuppressWarnings("NullAway")
    protected RestHandler() {
        // For CDI
        this.executor = null;
    }

    @Inject
    public RestHandler(@PublicAgentCard AgentCard agentCard, @ExtendedAgentCard Instance<AgentCard> extendedAgentCard,
            RequestHandler requestHandler, @Internal Executor executor) {
        this.agentCard = agentCard;
        this.extendedAgentCard = extendedAgentCard;
        this.requestHandler = requestHandler;
        this.executor = executor;

        // Validate transport configuration
        AgentCardValidator.validateTransportConfiguration(agentCard);
    }

    public RestHandler(AgentCard agentCard, RequestHandler requestHandler, Executor executor) {
        this.agentCard = agentCard;
        this.requestHandler = requestHandler;
        this.executor = executor;
    }

    public HTTPRestResponse sendMessage(String body, ServerCallContext context) {
        try {
            io.a2a.grpc.SendMessageRequest.Builder request = io.a2a.grpc.SendMessageRequest.newBuilder();
            parseRequestBody(body, request);
            EventKind result = requestHandler.onMessageSend(ProtoUtils.FromProto.messageSendParams(request), context);
            return createSuccessResponse(200, io.a2a.grpc.SendMessageResponse.newBuilder(ProtoUtils.ToProto.taskOrMessage(result)));
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
            io.a2a.grpc.SendMessageRequest.Builder request = io.a2a.grpc.SendMessageRequest.newBuilder();
            parseRequestBody(body, request);
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onMessageSendStream(ProtoUtils.FromProto.messageSendParams(request), context);
            return createStreamingResponse(publisher);
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
            Task task = requestHandler.onCancelTask(params, context);
            if (task != null) {
                return createSuccessResponse(200, io.a2a.grpc.Task.newBuilder(ProtoUtils.ToProto.task(task)));
            }
            throw new UnsupportedOperationError();
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
            io.a2a.grpc.SetTaskPushNotificationConfigRequest.Builder builder = io.a2a.grpc.SetTaskPushNotificationConfigRequest.newBuilder();
            parseRequestBody(body, builder);
            TaskPushNotificationConfig result = requestHandler.onSetTaskPushNotificationConfig(ProtoUtils.FromProto.setTaskPushNotificationConfig(builder), context);
            return createSuccessResponse(201, io.a2a.grpc.TaskPushNotificationConfig.newBuilder(ProtoUtils.ToProto.taskPushNotificationConfig(result)));
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
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onResubscribeToTask(params, context);
            return createStreamingResponse(publisher);
        } catch (JSONRPCError e) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(e).toJson()));
        } catch (Throwable throwable) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(new InternalError(throwable.getMessage())).toJson()));
        }
    }

    public HTTPRestResponse getTask(String taskId, int historyLength, ServerCallContext context) {
        try {
            TaskQueryParams params = new TaskQueryParams(taskId, historyLength);
            Task task = requestHandler.onGetTask(params, context);
            if (task != null) {
                return createSuccessResponse(200, io.a2a.grpc.Task.newBuilder(ProtoUtils.ToProto.task(task)));
            }
            throw new TaskNotFoundError();
        } catch (JSONRPCError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    public HTTPRestResponse listTasks(@Nullable String contextId, @Nullable String status,
                                       @Nullable Integer pageSize, @Nullable String pageToken,
                                       @Nullable Integer historyLength, @Nullable String lastUpdatedAfter,
                                       @Nullable Boolean includeArtifacts,
                                       ServerCallContext context) {
        try {
            // Build params
            ListTasksParams.Builder paramsBuilder = new ListTasksParams.Builder();
            if (contextId != null) {
                paramsBuilder.contextId(contextId);
            }
            if (status != null) {
                paramsBuilder.status(TaskState.valueOf(status));
            }
            if (pageSize != null) {
                paramsBuilder.pageSize(pageSize);
            }
            if (pageToken != null) {
                paramsBuilder.pageToken(pageToken);
            }
            if (historyLength != null) {
                paramsBuilder.historyLength(historyLength);
            }
            if (lastUpdatedAfter != null) {
                try {
                    paramsBuilder.lastUpdatedAfter(Instant.parse(lastUpdatedAfter));
                } catch (DateTimeParseException e) {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("parameter", "lastUpdatedAfter");
                    errorData.put("reason", "Must be valid ISO-8601 timestamp");
                    throw new InvalidParamsError(null, "Invalid params", errorData);
                }
            }
            if (includeArtifacts != null) {
                paramsBuilder.includeArtifacts(includeArtifacts);
            }
            ListTasksParams params = paramsBuilder.build();

            ListTasksResult result = requestHandler.onListTasks(params, context);
            return createSuccessResponse(200, io.a2a.grpc.ListTasksResponse.newBuilder(ProtoUtils.ToProto.listTasksResult(result)));
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
            TaskPushNotificationConfig config = requestHandler.onGetTaskPushNotificationConfig(params, context);
            return createSuccessResponse(200, io.a2a.grpc.TaskPushNotificationConfig.newBuilder(ProtoUtils.ToProto.taskPushNotificationConfig(config)));
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
            List<TaskPushNotificationConfig> configs = requestHandler.onListTaskPushNotificationConfig(params, context);
            return createSuccessResponse(200, io.a2a.grpc.ListTaskPushNotificationConfigResponse.newBuilder(ProtoUtils.ToProto.listTaskPushNotificationConfigResponse(configs)));
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
            requestHandler.onDeleteTaskPushNotificationConfig(params, context);
            return new HTTPRestResponse(204, "application/json", "");
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
            throw new JSONParseError(JSON_PARSE_ERROR_CODE, "Failed to parse json", e.getMessage());
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
