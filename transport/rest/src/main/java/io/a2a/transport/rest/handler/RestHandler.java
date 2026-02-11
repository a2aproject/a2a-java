package io.a2a.transport.rest.handler;

import static io.a2a.server.util.async.AsyncUtils.createTubeConfig;
import static io.a2a.spec.A2AErrorCodes.JSON_PARSE_ERROR_CODE;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.server.AgentCardValidator;
import io.a2a.server.ExtendedAgentCard;
import io.a2a.server.PublicAgentCard;
import io.a2a.server.ServerCallContext;
import io.a2a.server.extensions.A2AExtensions;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.version.A2AVersionValidator;
import io.a2a.server.util.async.Internal;
import io.a2a.spec.A2AError;
import io.a2a.spec.AgentCard;
import io.a2a.spec.ExtendedAgentCardNotConfiguredError;
import io.a2a.spec.ContentTypeNotSupportedError;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidAgentResponseError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.ListTasksParams;
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
import io.a2a.spec.ExtensionSupportRequiredError;
import io.a2a.spec.VersionNotSupportedError;
import mutiny.zero.ZeroPublisher;
import org.jspecify.annotations.Nullable;

@ApplicationScoped
public class RestHandler {

    private static final Logger log = Logger.getLogger(RestHandler.class.getName());
    private static final String TASK_STATE_PREFIX = "TASK_STATE_";

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

    public HTTPRestResponse sendMessage(ServerCallContext context, String tenant, String body) {

        try {
            A2AVersionValidator.validateProtocolVersion(agentCard, context);
            A2AExtensions.validateRequiredExtensions(agentCard, context);
            io.a2a.grpc.SendMessageRequest.Builder request = io.a2a.grpc.SendMessageRequest.newBuilder();
            parseRequestBody(body, request);
            request.setTenant(tenant);
            EventKind result = requestHandler.onMessageSend(ProtoUtils.FromProto.messageSendParams(request), context);
            return createSuccessResponse(200, io.a2a.grpc.SendMessageResponse.newBuilder(ProtoUtils.ToProto.taskOrMessage(result)));
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    public HTTPRestResponse sendStreamingMessage(ServerCallContext context, String tenant, String body) {
        try {
            if (!agentCard.capabilities().streaming()) {
                return createErrorResponse(new InvalidRequestError("Streaming is not supported by the agent"));
            }
            A2AVersionValidator.validateProtocolVersion(agentCard, context);
            A2AExtensions.validateRequiredExtensions(agentCard, context);
            io.a2a.grpc.SendMessageRequest.Builder request = io.a2a.grpc.SendMessageRequest.newBuilder();
            parseRequestBody(body, request);
            request.setTenant(tenant);
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onMessageSendStream(ProtoUtils.FromProto.messageSendParams(request), context);
            return createStreamingResponse(publisher);
        } catch (A2AError e) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(e).toJson()));
        } catch (Throwable throwable) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(new InternalError(throwable.getMessage())).toJson()));
        }
    }

    public HTTPRestResponse cancelTask(ServerCallContext context, String tenant, String taskId) {
        try {
            if (taskId == null || taskId.isEmpty()) {
                throw new InvalidParamsError();
            }
            TaskIdParams params = new TaskIdParams(taskId, tenant);
            Task task = requestHandler.onCancelTask(params, context);
            if (task != null) {
                return createSuccessResponse(200, io.a2a.grpc.Task.newBuilder(ProtoUtils.ToProto.task(task)));
            }
            throw new UnsupportedOperationError();
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    public HTTPRestResponse createTaskPushNotificationConfiguration(ServerCallContext context, String tenant, String body, String taskId) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            io.a2a.grpc.CreateTaskPushNotificationConfigRequest.Builder builder = io.a2a.grpc.CreateTaskPushNotificationConfigRequest.newBuilder();
            parseRequestBody(body, builder);
            builder.setTenant(tenant);
            TaskPushNotificationConfig result = requestHandler.onCreateTaskPushNotificationConfig(ProtoUtils.FromProto.CreateTaskPushNotificationConfig(builder), context);
            return createSuccessResponse(201, io.a2a.grpc.TaskPushNotificationConfig.newBuilder(ProtoUtils.ToProto.taskPushNotificationConfig(result)));
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    public HTTPRestResponse subscribeToTask(ServerCallContext context, String tenant, String taskId) {
        try {
            if (!agentCard.capabilities().streaming()) {
                return createErrorResponse(new InvalidRequestError("Streaming is not supported by the agent"));
            }
            TaskIdParams params = new TaskIdParams(taskId, tenant);
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onSubscribeToTask(params, context);
            return createStreamingResponse(publisher);
        } catch (A2AError e) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(e).toJson()));
        } catch (Throwable throwable) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(new InternalError(throwable.getMessage())).toJson()));
        }
    }

    public HTTPRestResponse getTask(ServerCallContext context, String tenant, String taskId, @Nullable Integer historyLength) {
        try {
            TaskQueryParams params = new TaskQueryParams(taskId, historyLength, tenant);
            Task task = requestHandler.onGetTask(params, context);
            if (task != null) {
                return createSuccessResponse(200, io.a2a.grpc.Task.newBuilder(ProtoUtils.ToProto.task(task)));
            }
            throw new TaskNotFoundError();
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    public HTTPRestResponse listTasks(ServerCallContext context, String tenant,
                                       @Nullable String contextId, @Nullable String status,
                                       @Nullable Integer pageSize, @Nullable String pageToken,
                                       @Nullable Integer historyLength, @Nullable String statusTimestampAfter,
                                       @Nullable Boolean includeArtifacts) {
        try {
            // Build params
            ListTasksParams.Builder paramsBuilder = ListTasksParams.builder();
            if (contextId != null) {
                paramsBuilder.contextId(contextId);
            }
            if (status != null) {
                TaskState taskState = null;

                // Try JSON format first (e.g., "working", "completed")
                try {
                    taskState = TaskState.fromString(status);
                } catch (IllegalArgumentException e) {
                    // Try protobuf enum format (e.g., "TASK_STATE_WORKING")
                    if (status.startsWith(TASK_STATE_PREFIX)) {
                        String enumName = status.substring(TASK_STATE_PREFIX.length());
                        try {
                            taskState = TaskState.valueOf(enumName);
                        } catch (IllegalArgumentException protoError) {
                            // Fall through to error handling below
                        }
                    } else {
                        // Try enum constant name directly (e.g., "WORKING")
                        try {
                            taskState = TaskState.valueOf(status);
                        } catch (IllegalArgumentException valueOfError) {
                            // Fall through to error handling below
                        }
                    }
                }

                if (taskState == null) {
                    String validStates = Arrays.stream(TaskState.values())
                            .map(TaskState::asString)
                            .collect(Collectors.joining(", "));
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("parameter", "status");
                    errorData.put("reason", "Must be one of: " + validStates);
                    throw new InvalidParamsError(null, "Invalid params", errorData);
                }

                paramsBuilder.status(taskState);
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
            paramsBuilder.tenant(tenant);
            if (statusTimestampAfter != null) {
                try {
                    paramsBuilder.statusTimestampAfter(Instant.parse(statusTimestampAfter));
                } catch (DateTimeParseException e) {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("parameter", "statusTimestampAfter");
                    errorData.put("reason", "Must be an ISO-8601 timestamp");
                    throw new InvalidParamsError(null, "Invalid params", errorData);
                }
            }
            if (includeArtifacts != null) {
                paramsBuilder.includeArtifacts(includeArtifacts);
            }
            ListTasksParams params = paramsBuilder.build();

            ListTasksResult result = requestHandler.onListTasks(params, context);
            return createSuccessResponse(200, io.a2a.grpc.ListTasksResponse.newBuilder(ProtoUtils.ToProto.listTasksResult(result)));
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    public HTTPRestResponse getTaskPushNotificationConfiguration(ServerCallContext context, String tenant, String taskId, String configId) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams(taskId, configId, tenant);
            TaskPushNotificationConfig config = requestHandler.onGetTaskPushNotificationConfig(params, context);
            return createSuccessResponse(200, io.a2a.grpc.TaskPushNotificationConfig.newBuilder(ProtoUtils.ToProto.taskPushNotificationConfig(config)));
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    public HTTPRestResponse listTaskPushNotificationConfigurations(ServerCallContext context, String tenant, String taskId, int pageSize, String pageToken) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId, pageSize, pageToken, tenant);
            ListTaskPushNotificationConfigResult result = requestHandler.onListTaskPushNotificationConfig(params, context);
            return createSuccessResponse(200, io.a2a.grpc.ListTaskPushNotificationConfigResponse.newBuilder(ProtoUtils.ToProto.listTaskPushNotificationConfigResponse(result)));
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    public HTTPRestResponse deleteTaskPushNotificationConfiguration(ServerCallContext context, String tenant, String taskId, String configId) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            DeleteTaskPushNotificationConfigParams params = new DeleteTaskPushNotificationConfigParams(taskId, configId, tenant);
            requestHandler.onDeleteTaskPushNotificationConfig(params, context);
            return new HTTPRestResponse(204, "application/json", "");
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    private void parseRequestBody(String body, com.google.protobuf.Message.Builder builder) throws A2AError {
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
            JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            throw new JSONParseError(JSON_PARSE_ERROR_CODE, "Failed to parse json", e.getMessage());
        }
    }

    private HTTPRestResponse createSuccessResponse(int statusCode, com.google.protobuf.Message.Builder builder) {
        try {
            // Include default value fields to ensure empty arrays, zeros, etc. are present in JSON
            String jsonBody = JsonFormat.printer().includingDefaultValueFields().print(builder);
            return new HTTPRestResponse(statusCode, "application/json", jsonBody);
        } catch (InvalidProtocolBufferException e) {
            return createErrorResponse(new InternalError("Failed to serialize response: " + e.getMessage()));
        }
    }

    public HTTPRestResponse createErrorResponse(A2AError error) {
        int statusCode = mapErrorToHttpStatus(error);
        return createErrorResponse(statusCode, error);
    }

    private HTTPRestResponse createErrorResponse(int statusCode, A2AError error) {
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
        log.log(Level.FINE, "REST: convertToSendStreamingMessageResponse called, creating ZeroPublisher");
        return ZeroPublisher.create(createTubeConfig(), tube -> {
            log.log(Level.FINE, "REST: ZeroPublisher tube created, starting CompletableFuture.runAsync");
            CompletableFuture.runAsync(() -> {
                log.log(Level.FINE, "REST: Inside CompletableFuture, subscribing to EventKind publisher");
                publisher.subscribe(new Flow.Subscriber<StreamingEventKind>() {
                    Flow.@Nullable Subscription subscription;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        log.log(Level.FINE, "REST: onSubscribe called, storing subscription and requesting first event");
                        this.subscription = subscription;
                        subscription.request(1);
                    }

                    @Override
                    public void onNext(StreamingEventKind item) {
                        log.log(Level.FINE, "REST: onNext called with event: {0}", item.getClass().getSimpleName());
                        try {
                            String payload = JsonFormat.printer().omittingInsignificantWhitespace().print(ProtoUtils.ToProto.taskOrMessageStream(item));
                            log.log(Level.FINE, "REST: Converted to JSON, sending via tube: {0}", payload.substring(0, Math.min(100, payload.length())));
                            tube.send(payload);
                            log.log(Level.FINE, "REST: tube.send() completed, requesting next event from EventConsumer");
                            // Request next event from EventConsumer (Chain 1: EventConsumer → RestHandler)
                            // This is safe because ZeroPublisher buffers items
                            // Chain 2 (ZeroPublisher → MultiSseSupport) controls actual delivery via request(1) in onWriteDone()
                            if (subscription != null) {
                                subscription.request(1);
                            } else {
                                log.log(Level.WARNING, "REST: subscription is null in onNext!");
                            }
                        } catch (InvalidProtocolBufferException ex) {
                            log.log(Level.SEVERE, "REST: JSON conversion failed", ex);
                            onError(ex);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        log.log(Level.SEVERE, "REST: onError called", throwable);
                        if (throwable instanceof A2AError jsonrpcError) {
                            tube.send(new HTTPRestErrorResponse(jsonrpcError).toJson());
                        } else {
                            tube.send(new HTTPRestErrorResponse(new InternalError(throwable.getMessage())).toJson());
                        }
                        onComplete();
                    }

                    @Override
                    public void onComplete() {
                        log.log(Level.FINE, "REST: onComplete called, calling tube.complete()");
                        tube.complete();
                    }
                });
            }, executor);
        });
    }

    private int mapErrorToHttpStatus(A2AError error) {
        if (error instanceof InvalidRequestError || error instanceof JSONParseError) {
            return 400;
        }
        if (error instanceof InvalidParamsError) {
            return 422;
        }
        if (error instanceof MethodNotFoundError || error instanceof TaskNotFoundError) {
            return 404;
        }
        if (error instanceof TaskNotCancelableError) {
            return 409;
        }
        if (error instanceof PushNotificationNotSupportedError
                || error instanceof UnsupportedOperationError
                || error instanceof VersionNotSupportedError) {
            return 501;
        }
        if (error instanceof ContentTypeNotSupportedError) {
            return 415;
        }
        if (error instanceof InvalidAgentResponseError) {
            return 502;
        }
        if (error instanceof ExtendedAgentCardNotConfiguredError
                || error instanceof ExtensionSupportRequiredError) {
            return 400;
        }
        if (error instanceof InternalError) {
            return 500;
        }
        return 500;
    }

    public HTTPRestResponse getExtendedAgentCard(ServerCallContext context, String tenant) {
        try {
            if (!agentCard.capabilities().extendedAgentCard() || extendedAgentCard == null || !extendedAgentCard.isResolvable()) {
                throw new ExtendedAgentCardNotConfiguredError(null, "Extended Card not configured", null);
            }
            return new HTTPRestResponse(200, "application/json", JsonUtil.toJson(extendedAgentCard.get()));
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable t) {
            return createErrorResponse(500, new InternalError(t.getMessage()));
        }
    }

    public HTTPRestResponse getAgentCard() {
        try {
            return new HTTPRestResponse(200, "application/json", JsonUtil.toJson(agentCard));
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

        private HTTPRestErrorResponse(A2AError jsonRpcError) {
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
