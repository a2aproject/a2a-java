package io.a2a.transport.jsonrest.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.protobuf.util.JsonFormat;
import io.a2a.grpc.utils.ProtoUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.concurrent.Flow;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.a2a.server.PublicAgentCard;
import io.a2a.server.ServerCallContext;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.AgentCard;
import io.a2a.spec.ContentTypeNotSupportedError;
import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import io.a2a.spec.EventKind;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidAgentResponseError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.UnsupportedOperationError;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class JSONRestHandler {

    private static final Logger log = Logger.getLogger(JSONRestHandler.class.getName());
    private static final Pattern GET_TASK_PATTERN = Pattern.compile("^/v1/tasks/([^/]+)$");
    private static final Pattern CANCEL_TASK_PATTERN = Pattern.compile("^/v1/tasks/([^/]+):cancel$");
    private static final Pattern RESUBSCRIBE_TASK_PATTERN = Pattern.compile("^/v1/tasks/([^/]+):subscribe$");
    private static final Pattern GET_PUSH_NOTIFICATION_CONFIG_PATTERN = Pattern.compile("^/v1/tasks/([^/]+)/pushNotificationConfigs/([^/]+)$");
    private static final Pattern LIST_PUSH_NOTIFICATION_CONFIG_PATTERN = Pattern.compile("^/v1/tasks/([^/]+)/pushNotificationConfigs$");
    private static final Pattern DELETE_PUSH_NOTIFICATION_CONFIG_PATTERN = Pattern.compile("^/v1/tasks/([^/]+)/pushNotificationConfigs/([^/]+)$");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private AgentCard agentCard;
    private RequestHandler requestHandler;

    protected JSONRestHandler() {
        // For CDI
    }

    @Inject
    public JSONRestHandler(@PublicAgentCard AgentCard agentCard, RequestHandler requestHandler) {
        this.agentCard = agentCard;
        this.requestHandler = requestHandler;
    }

    public HTTPRestResponse handleRequest(String method, String path, String body, ServerCallContext context) {
        try {
            switch (method.toUpperCase(Locale.US)) {
                case "GET":
                    return handleGetRequest(path, context);
                case "POST":
                    return handlePostRequest(path, body, context);
                case "PUT":
                    return handlePutRequest(path, body, context);
                case "DELETE":
                    return handleDeleteRequest(path, context);
                default:
                    return createErrorResponse(405, new MethodNotFoundError());
            }
        } catch (JSONRPCError e) {
            e.printStackTrace();
            return createErrorResponse(mapErrorToHttpStatus(e), e);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse(500, new InternalError(e.getMessage()));
        }
    }

    private HTTPRestResponse handleGetRequest(String path, ServerCallContext context) throws JSONRPCError {
        if ("/v1/card".equals(path)) {
            return createSuccessResponse(200, io.a2a.grpc.AgentCard.newBuilder(ProtoUtils.ToProto.agentCard(agentCard)));
        }

        Matcher taskMatcher = GET_TASK_PATTERN.matcher(path);
        if (taskMatcher.matches()) {
            String taskId = taskMatcher.group(1);
            TaskQueryParams params = new TaskQueryParams(taskId);
            Task task = requestHandler.onGetTask(params, context);
            if (task != null) {
                return createSuccessResponse(200, io.a2a.grpc.Task.newBuilder(ProtoUtils.ToProto.task(task)));
            } else {
                throw new TaskNotFoundError();
            }
        }

        Matcher pushConfigMatcher = GET_PUSH_NOTIFICATION_CONFIG_PATTERN.matcher(path);
        if (pushConfigMatcher.matches()) {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            String taskId = pushConfigMatcher.group(1);
            String configId = pushConfigMatcher.group(2);
            GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams(taskId, configId);
            TaskPushNotificationConfig config = requestHandler.onGetTaskPushNotificationConfig(params, context);
            return createSuccessResponse(200, io.a2a.grpc.TaskPushNotificationConfig.newBuilder(ProtoUtils.ToProto.taskPushNotificationConfig(config)));
        }

        Matcher listPushConfigMatcher = LIST_PUSH_NOTIFICATION_CONFIG_PATTERN.matcher(path);
        if (listPushConfigMatcher.matches()) {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            String taskId = listPushConfigMatcher.group(1);
            ListTaskPushNotificationConfigParams params = new ListTaskPushNotificationConfigParams(taskId);
            List<TaskPushNotificationConfig> configs = requestHandler.onListTaskPushNotificationConfig(params, context);
            return createSuccessResponse(200, io.a2a.grpc.ListTaskPushNotificationConfigResponse.newBuilder(ProtoUtils.ToProto.listTaskPushNotificationConfigResponse(configs)));
        }

        throw new MethodNotFoundError();
    }

    private HTTPRestResponse handlePostRequest(String path, String body, ServerCallContext context) throws JSONRPCError {
        if ("/v1/message:send".equals(path)) {
            io.a2a.grpc.SendMessageRequest.Builder request= io.a2a.grpc.SendMessageRequest.newBuilder();
            parseRequestBody(body, request);
            EventKind result = requestHandler.onMessageSend(ProtoUtils.FromProto.messageSendParams(request), context);
            return createSuccessResponse(200, io.a2a.grpc.SendMessageResponse.newBuilder(ProtoUtils.ToProto.taskOrMessage(result)));
        }

        if ("/v1/message:stream".equals(path)) {
            if (!agentCard.capabilities().streaming()) {
                throw new InvalidRequestError("Streaming is not supported by the agent");
            }
            
            io.a2a.grpc.SendMessageRequest.Builder request = io.a2a.grpc.SendMessageRequest.newBuilder();
            parseRequestBody(body, request);
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onMessageSendStream(ProtoUtils.FromProto.messageSendParams(request), context);
            return createStreamingResponse(publisher);
        }

        Matcher cancelMatcher = CANCEL_TASK_PATTERN.matcher(path);
        if (cancelMatcher.matches()) {
            String taskId = cancelMatcher.group(1);
            TaskIdParams params = new TaskIdParams(taskId);
            Task task = requestHandler.onCancelTask(params, context);
            if (task != null) {
                return createSuccessResponse(200, io.a2a.grpc.Task.newBuilder(ProtoUtils.ToProto.task(task)));
            }
            throw new TaskNotFoundError();
        }

        Matcher resubscribeMatcher = RESUBSCRIBE_TASK_PATTERN.matcher(path);
        if (resubscribeMatcher.matches()) {
            if (!agentCard.capabilities().streaming()) {
                throw new InvalidRequestError("Streaming is not supported by the agent");
            }
            String taskId = resubscribeMatcher.group(1);
            TaskIdParams params = new TaskIdParams(taskId);
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onResubscribeToTask(params, context);
            return createStreamingResponse(publisher);
        }

        Matcher listPushConfigMatcher = LIST_PUSH_NOTIFICATION_CONFIG_PATTERN.matcher(path);
        if (listPushConfigMatcher.matches()) {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            String taskId = listPushConfigMatcher.group(1);
            io.a2a.grpc.CreateTaskPushNotificationConfigRequest.Builder builder = io.a2a.grpc. CreateTaskPushNotificationConfigRequest.newBuilder();
            parseRequestBody(body, builder);
            TaskPushNotificationConfig result = requestHandler.onSetTaskPushNotificationConfig(ProtoUtils.FromProto.taskPushNotificationConfig(builder), context);
            return createSuccessResponse(201, io.a2a.grpc.TaskPushNotificationConfig.newBuilder(ProtoUtils.ToProto.taskPushNotificationConfig(result)));
        }

        throw new MethodNotFoundError();
    }

    private HTTPRestResponse handlePutRequest(String path, String body, ServerCallContext context) throws JSONRPCError {
        throw new MethodNotFoundError();
    }

    private HTTPRestResponse handleDeleteRequest(String path, ServerCallContext context) throws JSONRPCError {
        Matcher deleteMatcher = DELETE_PUSH_NOTIFICATION_CONFIG_PATTERN.matcher(path);
        if (deleteMatcher.matches()) {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            String taskId = deleteMatcher.group(1);
            String configId = deleteMatcher.group(2);
            DeleteTaskPushNotificationConfigParams params = new DeleteTaskPushNotificationConfigParams(taskId, configId);
            requestHandler.onDeleteTaskPushNotificationConfig(params, context);
            return createSuccessResponse(204, null);
        }

        throw new MethodNotFoundError();
    }

    private void parseRequestBody(String body, com.google.protobuf.Message.Builder builder) throws JSONRPCError {
        try {
            if (body == null || body.trim().isEmpty()) {
                throw new InvalidParamsError("Request body is required");
            }
            JsonFormat.parser().merge(body, builder);
        } catch (Exception e) {
            log.log(Level.FINE, "Error parsing JSON request body: {0}", body);
            log.log(Level.FINE, "Parse error details", e);
            throw new InvalidParamsError("Failed to parse request body: " + e.getMessage());
        }
    }

    private HTTPRestResponse createSuccessResponse(int statusCode, com.google.protobuf.Message.Builder builder) {
        try {
            String jsonBody = builder != null ? JsonFormat.printer().print(builder) : null;
            return new HTTPRestResponse(statusCode, "application/json", jsonBody);
        } catch (Exception e) {
            return createErrorResponse(500, new InternalError("Failed to serialize response: " + e.getMessage()));
        }
    }

    private HTTPRestResponse createErrorResponse(int statusCode, JSONRPCError error) {
        try {
            HTTPRestErrorResponse errorResponse = new HTTPRestErrorResponse(error.getClass().getSimpleName(), error.getMessage());
            String jsonBody = OBJECT_MAPPER.writeValueAsString(errorResponse);
            return new HTTPRestResponse(statusCode, "application/json", jsonBody);
        } catch (Exception e) {
            String fallbackJson = "{\"error\":\"InternalError\",\"message\":\"Failed to serialize error response\"}";
            return new HTTPRestResponse(500, "application/json", fallbackJson);
        }
    }

    private HTTPRestResponse createStreamingResponse(Flow.Publisher<StreamingEventKind> publisher) {
        return new HTTPRestStreamingResponse(publisher);
    }

    private int mapErrorToHttpStatus(JSONRPCError error) {
        if (error instanceof InvalidRequestError || error instanceof InvalidParamsError) {
            return 400;
        }
        if (error instanceof MethodNotFoundError || error instanceof TaskNotFoundError) {
            return 404;
        }
        if (error instanceof TaskNotCancelableError || error instanceof PushNotificationNotSupportedError
                || error instanceof UnsupportedOperationError) {
            return 501;
        }
        if (error instanceof ContentTypeNotSupportedError) {
            return 415;
        }
        if (error instanceof InternalError || error instanceof InvalidAgentResponseError) {
            return 500;
        }
        return 500;
    }

    public AgentCard getAgentCard() {
        return agentCard;
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

        private final Flow.Publisher<StreamingEventKind> publisher;

        public HTTPRestStreamingResponse(Flow.Publisher<StreamingEventKind> publisher) {
            super(200, "text/event-stream", null);
            this.publisher = publisher;
        }

        public Flow.Publisher<StreamingEventKind> getPublisher() {
            return publisher;
        }

        public String generateSSEBody() {
            StringBuilder sseBody = new StringBuilder();
            // Note: In a real implementation, this would need to be handled asynchronously
            // This is a simplified representation for the structure
            sseBody.append("data: {\"id\": null, \"result\": null}\n\n");
            return sseBody.toString();
        }
    }

    private static class HTTPRestErrorResponse {

        private final String error;
        private final String message;

        public HTTPRestErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }
    }
}
