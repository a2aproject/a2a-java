package org.a2aproject.sdk.transport.rest.handler;

import static org.a2aproject.sdk.common.MediaType.APPLICATION_JSON;
import static org.a2aproject.sdk.server.util.async.AsyncUtils.createTubeConfig;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
import mutiny.zero.ZeroPublisher;
import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.util.ErrorDetail;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.AgentCardCacheMetadata;
import org.a2aproject.sdk.server.AgentCardValidator;
import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.extensions.A2AExtensions;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.server.util.async.Internal;
import org.a2aproject.sdk.server.version.A2AVersionValidator;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.A2AErrorCodes;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.ExtendedAgentCardNotConfiguredError;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.JSONParseError;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.PushNotificationNotSupportedError;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.jspecify.annotations.Nullable;

/**
 * REST transport handler for processing A2A protocol requests over HTTP.
 *
 * <p>
 * This handler converts HTTP REST requests into A2A protocol operations and
 * manages the lifecycle of agent interactions including message sending, task
 * management, and push notification configurations.
 *
 * <h2>Request Flow</h2>
 * <p>
 * HTTP REST requests flow through this handler to the underlying {@link RequestHandler},
 * which coordinates with the agent executor and event queue system:
 * <pre>
 * HTTP Request → RestHandler → RequestHandler → AgentExecutor
 *                    ↓              ↓
 *              Validation    EventQueue → Response
 * </pre>
 *
 * <h2>Supported Operations</h2>
 * <ul>
 * <li>Message sending (blocking and streaming)</li>
 * <li>Task management (get, list, cancel, subscribe)</li>
 * <li>Push notification configurations (create, get, list, delete)</li>
 * <li>Agent card retrieval (public and extended)</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>
 * All A2A protocol errors are caught and converted to appropriate HTTP status codes
 * via {@link #mapErrorToHttpStatus(A2AError)}. Protocol version and required extensions
 * are validated before processing requests.
 *
 * <h2>CDI Integration</h2>
 * <p>
 * This handler is an {@code @ApplicationScoped} CDI bean that requires:
 * <ul>
 * <li>{@link AgentCard} qualified with {@code @PublicAgentCard}</li>
 * <li>{@link RequestHandler} for processing A2A operations</li>
 * <li>{@link Executor} qualified with {@code @Internal} for async operations</li>
 * <li>Optional {@link AgentCard} qualified with {@code @ExtendedAgentCard}</li>
 * </ul>
 *
 * @see RequestHandler
 * @see org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler
 * @see org.a2aproject.sdk.spec.AgentCard
 * @see ServerCallContext
 */
@ApplicationScoped
public class RestHandler {

    private static final Logger log = Logger.getLogger(RestHandler.class.getName());
    private static final String TASK_STATE_PREFIX = "TASK_STATE_";

    // Fields set by constructor injection cannot be final. We need a noargs constructor for
    // Jakarta compatibility, and it seems that making fields set by constructor injection
    // final, is not proxyable in all runtimes
    private AgentCard agentCard;
    private @Nullable Instance<AgentCard> extendedAgentCard;
    private AgentCardCacheMetadata cacheMetadata;
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

    /**
     * Creates a REST handler with full CDI injection support.
     *
     * @param agentCard the public agent card containing agent capabilities
     * @param extendedAgentCard optional extended agent card instance
     * @param cacheMetadata the agent card caching metadata
     * @param requestHandler the handler for processing A2A requests
     * @param executor the executor for asynchronous operations
     */
    @Inject
    public RestHandler(@PublicAgentCard AgentCard agentCard, @ExtendedAgentCard Instance<AgentCard> extendedAgentCard,
            AgentCardCacheMetadata cacheMetadata, RequestHandler requestHandler, @Internal Executor executor) {
        this.agentCard = agentCard;
        this.extendedAgentCard = extendedAgentCard;
        this.cacheMetadata = cacheMetadata;
        this.requestHandler = requestHandler;
        this.executor = executor;

        // Validate transport configuration
        AgentCardValidator.validateTransportConfiguration(agentCard);
    }

    /**
     * Creates a REST handler with basic dependencies.
     *
     * @param agentCard the agent card containing agent capabilities
     * @param cacheMetadata the agent card caching metadata
     * @param requestHandler the handler for processing A2A requests
     * @param executor the executor for asynchronous operations
     */
    public RestHandler(AgentCard agentCard, AgentCardCacheMetadata cacheMetadata,
            RequestHandler requestHandler, Executor executor) {
        this.agentCard = agentCard;
        this.cacheMetadata = cacheMetadata;
        this.requestHandler = requestHandler;
        this.executor = executor;
    }

    /**
     * Handles a blocking message send request.
     *
     * <p>
     * This method processes an HTTP POST request containing a message to be sent to the agent.
     * The request is validated for protocol version and required extensions before being forwarded
     * to the {@link RequestHandler}. The method blocks until the agent produces a terminal event
     * or requires authentication/input.
     *
     * <p>
     * <b>Example Request:</b></p>
     * <pre>{@code
     * POST /v1/tenants/{tenant}/messages
     * Content-Type: application/json
     *
     * {
     *   "message": {
     *     "parts": [
     *       {"text": "What is the weather in San Francisco?"}
     *     ]
     *   }
     * }
     * }</pre>
     *
     * <p>
     * <b>Example Response:</b></p>
     * <pre>{@code
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     *
     * {
     *   "task": {
     *     "id": "task-123",
     *     "status": {"state": "COMPLETED"},
     *     "artifacts": [...]
     *   }
     * }
     * }</pre>
     *
     * @param context the server call context containing authentication and metadata
     * @param tenant the tenant identifier
     * @param body the JSON request body containing the message to send
     * @return the HTTP response containing the task or message result
     * @see #sendStreamingMessage(ServerCallContext, String, String)
     * @see RequestHandler#onMessageSend(org.a2aproject.sdk.spec.MessageSendParams, ServerCallContext)
     */
    public HTTPRestResponse sendMessage(ServerCallContext context, String tenant, String body) {
        log.warning("DEBUG: Received request body: " + body);
        try {
            A2AVersionValidator.validateProtocolVersion(agentCard, context);
            A2AExtensions.validateRequiredExtensions(agentCard, context);
            org.a2aproject.sdk.grpc.SendMessageRequest.Builder request = org.a2aproject.sdk.grpc.SendMessageRequest.newBuilder();
            parseRequestBody(body, request);
            request.setTenant(tenant);
            EventKind result = requestHandler.onMessageSend(ProtoUtils.FromProto.messageSendParams(request), context);
            return createSuccessResponse(200, org.a2aproject.sdk.grpc.SendMessageResponse.newBuilder(ProtoUtils.ToProto.taskOrMessage(result)));
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    /**
     * Handles a streaming message send request.
     *
     * <p>
     * This method processes an HTTP POST request for streaming responses from the agent.
     * The response is returned as Server-Sent Events (SSE) via {@link HTTPRestStreamingResponse},
     * allowing clients to receive task updates and artifacts as they are produced by the agent.
     *
     * <p>
     * This method requires the agent card to have {@code capabilities.streaming = true}.
     *
     * <p>
     * <b>Example Request:</b></p>
     * <pre>{@code
     * POST /v1/tenants/{tenant}/messages/stream
     * Content-Type: application/json
     *
     * {
     *   "message": {
     *     "parts": [
     *       {"text": "Generate a long story"}
     *     ]
     *   }
     * }
     * }</pre>
     *
     * <p>
     * <b>Example Streaming Response:</b></p>
     * <pre>{@code
     * HTTP/1.1 200 OK
     * Content-Type: text/event-stream
     *
     * data: {"taskStatusUpdate":{"task":{"id":"task-123","status":{"state":"WORKING"}}}}
     *
     * data: {"taskArtifactUpdate":{"taskId":"task-123","artifacts":[{"parts":[{"text":"Once upon"}]}]}}
     *
     * data: {"taskArtifactUpdate":{"taskId":"task-123","artifacts":[{"parts":[{"text":" a time..."}]}]}}
     *
     * data: {"taskStatusUpdate":{"task":{"id":"task-123","status":{"state":"COMPLETED"}}}}
     * }</pre>
     *
     * @param context the server call context containing authentication and metadata
     * @param tenant the tenant identifier
     * @param body the JSON request body containing the message to send
     * @return the streaming HTTP response containing a publisher of events
     * @see #sendMessage(ServerCallContext, String, String)
     * @see RequestHandler#onMessageSendStream(org.a2aproject.sdk.spec.MessageSendParams, ServerCallContext)
     * @see HTTPRestStreamingResponse
     */
    public HTTPRestResponse sendStreamingMessage(ServerCallContext context, String tenant, String body) {
        try {
            if (!agentCard.capabilities().streaming()) {
                return createErrorResponse(new InvalidRequestError("Streaming is not supported by the agent"));
            }
            A2AVersionValidator.validateProtocolVersion(agentCard, context);
            A2AExtensions.validateRequiredExtensions(agentCard, context);
            org.a2aproject.sdk.grpc.SendMessageRequest.Builder request = org.a2aproject.sdk.grpc.SendMessageRequest.newBuilder();
            parseRequestBody(body, request);
            request.setTenant(tenant);
            MessageSendParams params = ProtoUtils.FromProto.messageSendParams(request);
            try {
                requestHandler.validateRequestedTask(params.message().taskId());
            } catch (A2AError e) {
                return createErrorResponse(e);
            }
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onMessageSendStream(params, context);
            return createStreamingResponse(publisher);
        } catch (A2AError e) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(e).toJson()));
        } catch (Throwable throwable) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(new InternalError(throwable.getMessage())).toJson()));
        }
    }

    /**
     * Handles a task cancellation request.
     *
     * <p>
     * Attempts to cancel a running task identified by the task ID. The cancellation
     * request is forwarded to the {@link RequestHandler}, which signals the agent executor
     * to stop processing. The agent should transition the task to {@code CANCELED} state.
     *
     * <p>
     * <b>Example Request:</b></p>
     * <pre>{@code
     * POST /v1/tenants/{tenant}/tasks/{taskId}/cancel
     * }</pre>
     *
     * @param context the server call context containing authentication and metadata
     * @param tenant the tenant identifier
     * @param body the JSON request body
     * @param taskId the ID of the task to cancel
     * @return the HTTP response containing the cancelled task
     * @throws InvalidParamsError if taskId is null or empty
     * @see RequestHandler#onCancelTask(CancelTaskParams, ServerCallContext)
     * @see org.a2aproject.sdk.server.agentexecution.AgentExecutor#cancel
     */
    @SuppressWarnings("unchecked")
    public HTTPRestResponse cancelTask(ServerCallContext context, String tenant, String body, String taskId) {
        try {
            if (taskId == null || taskId.isEmpty()) {
                throw new InvalidParamsError();
            }
            Map<String, Object> metadata = JsonUtil.readMetadata(body);
            CancelTaskParams params = CancelTaskParams.builder().id(taskId).tenant(tenant).metadata(metadata).build();
            Task task = requestHandler.onCancelTask(params, context);
            if (task != null) {
                return createSuccessResponse(200, org.a2aproject.sdk.grpc.Task.newBuilder(ProtoUtils.ToProto.task(task)));
            }
            throw new UnsupportedOperationError();
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    /**
     * Creates a push notification configuration for a task.
     *
     * @param context the server call context containing authentication and metadata
     * @param tenant the tenant identifier
     * @param body the JSON request body containing the configuration
     * @param taskId the ID of the task
     * @return the HTTP response containing the created configuration
     */
    public HTTPRestResponse createTaskPushNotificationConfiguration(ServerCallContext context, String tenant, String body, String taskId) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder builder = org.a2aproject.sdk.grpc.TaskPushNotificationConfig.newBuilder();
            parseRequestBody(body, builder);

            String taskIdFromBody = builder.getTaskId();
            if (!taskIdFromBody.isEmpty() && !taskIdFromBody.equals(taskId)) {
                throw new InvalidParamsError("Task ID in request body (" + taskIdFromBody + ") does not match task ID in URL path (" + taskId + ").");
            }
            builder.setTenant(tenant);
            builder.setTaskId(taskId);
            TaskPushNotificationConfig result = requestHandler.onCreateTaskPushNotificationConfig(ProtoUtils.FromProto.createTaskPushNotificationConfig(builder), context);
            return createSuccessResponse(201, org.a2aproject.sdk.grpc.TaskPushNotificationConfig.newBuilder(ProtoUtils.ToProto.taskPushNotificationConfig(result)));
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    /**
     * Subscribes to task updates via a streaming connection.
     *
     * <p>
     * Creates a Server-Sent Events (SSE) stream that delivers real-time updates for an
     * existing task. This allows clients to reconnect to ongoing or completed tasks and
     * receive their event history and future updates.
     *
     * <p>
     * This method requires the agent card to have {@code capabilities.streaming = true}.
     *
     * <p>
     * <b>Example Request:</b></p>
     * <pre>{@code
     * GET /v1/tenants/{tenant}/tasks/{taskId}/subscribe
     * }</pre>
     *
     * <p>
     * <b>Use Cases:</b></p>
     * <ul>
     * <li>Reconnecting to a task after network interruption</li>
     * <li>Monitoring long-running tasks from multiple clients</li>
     * <li>Viewing historical events for completed tasks</li>
     * </ul>
     *
     * @param context the server call context containing authentication and metadata
     * @param tenant the tenant identifier
     * @param taskId the ID of the task to subscribe to
     * @return the streaming HTTP response containing task updates
     * @see RequestHandler#onSubscribeToTask(TaskIdParams, ServerCallContext)
     * @see #sendStreamingMessage(ServerCallContext, String, String)
     */
    public HTTPRestResponse subscribeToTask(ServerCallContext context, String tenant, String taskId) {
        try {
            if (!agentCard.capabilities().streaming()) {
                return createErrorResponse(new InvalidRequestError("Streaming is not supported by the agent"));
            }
            TaskIdParams params = TaskIdParams.builder().id(taskId).tenant(tenant).build();
            try {
                requestHandler.validateRequestedTask(params.id());
            } catch (A2AError e) {
                return createErrorResponse(e);
            }
            Flow.Publisher<StreamingEventKind> publisher = requestHandler.onSubscribeToTask(params, context);
            return createStreamingResponse(publisher);
        } catch (A2AError e) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(e).toJson()));
        } catch (Throwable throwable) {
            return new HTTPRestStreamingResponse(ZeroPublisher.fromItems(new HTTPRestErrorResponse(new InternalError(throwable.getMessage())).toJson()));
        }
    }

    /**
     * Retrieves a task by ID.
     *
     * @param context the server call context containing authentication and metadata
     * @param tenant the tenant identifier
     * @param taskId the ID of the task to retrieve
     * @param historyLength the maximum number of history entries to include
     * @return the HTTP response containing the task
     */
    public HTTPRestResponse getTask(ServerCallContext context, String tenant, String taskId, @Nullable Integer historyLength) {
        try {
            TaskQueryParams params = new TaskQueryParams(taskId, historyLength, tenant);
            Task task = requestHandler.onGetTask(params, context);
            if (task != null) {
                return createSuccessResponse(200, org.a2aproject.sdk.grpc.Task.newBuilder(ProtoUtils.ToProto.task(task)));
            }
            throw new TaskNotFoundError();
        } catch (IllegalArgumentException e) {
            return createErrorResponse(new InvalidParamsError(e.getMessage()));
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    /**
     * Lists tasks with optional filtering and pagination.
     *
     * <p>
     * Retrieves a list of tasks with support for filtering by context, status, and timestamp,
     * along with pagination controls. This method is useful for task management dashboards,
     * monitoring systems, and task history retrieval.
     *
     * <p>
     * <b>Example Request:</b></p>
     * <pre>{@code
     * GET /v1/tenants/{tenant}/tasks?status=COMPLETED&pageSize=10&includeArtifacts=true
     * }</pre>
     *
     * <p>
     * <b>Query Parameters:</b></p>
     * <ul>
     * <li>{@code contextId} - Filter tasks by conversation context</li>
     * <li>{@code status} - Filter by task state (SUBMITTED, WORKING, COMPLETED, etc.)</li>
     * <li>{@code pageSize} - Maximum tasks to return (for pagination)</li>
     * <li>{@code pageToken} - Token for retrieving next page of results</li>
     * <li>{@code historyLength} - Maximum history entries to include per task</li>
     * <li>{@code statusTimestampAfter} - ISO-8601 timestamp for filtering recent tasks</li>
     * <li>{@code includeArtifacts} - Whether to include task artifacts in response</li>
     * </ul>
     *
     * @param context the server call context containing authentication and metadata
     * @param tenant the tenant identifier
     * @param contextId optional context ID to filter by
     * @param status optional task status to filter by (must be valid {@link TaskState} value)
     * @param pageSize optional maximum number of tasks to return
     * @param pageToken optional token for pagination
     * @param historyLength optional maximum number of history entries per task
     * @param statusTimestampAfter optional ISO-8601 timestamp to filter tasks updated after
     * @param includeArtifacts optional flag to include task artifacts
     * @return the HTTP response containing the list of tasks
     * @throws InvalidParamsError if status is not a valid TaskState or timestamp is malformed
     * @see RequestHandler#onListTasks(ListTasksParams, ServerCallContext)
     * @see TaskState
     */
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
                TaskState taskState;

                try {
                    taskState = TaskState.valueOf(status);
                } catch (IllegalArgumentException e) {
                    String validStates = Arrays.stream(TaskState.values())
                            .map(TaskState::name)
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
            return createSuccessResponse(200, org.a2aproject.sdk.grpc.ListTasksResponse.newBuilder(ProtoUtils.ToProto.listTasksResult(result)));
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    /**
     * Retrieves a specific push notification configuration for a task.
     *
     * @param context the server call context containing authentication and metadata
     * @param tenant the tenant identifier
     * @param taskId the ID of the task
     * @param configId the ID of the configuration to retrieve
     * @return the HTTP response containing the configuration
     */
    public HTTPRestResponse getTaskPushNotificationConfiguration(ServerCallContext context, String tenant, String taskId, String configId) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams(taskId, configId, tenant);
            TaskPushNotificationConfig config = requestHandler.onGetTaskPushNotificationConfig(params, context);
            return createSuccessResponse(200, org.a2aproject.sdk.grpc.TaskPushNotificationConfig.newBuilder(ProtoUtils.ToProto.taskPushNotificationConfig(config)));
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    /**
     * Lists push notification configurations for a task.
     *
     * @param context the server call context containing authentication and metadata
     * @param tenant the tenant identifier
     * @param taskId the ID of the task
     * @param pageSize the maximum number of configurations to return
     * @param pageToken the token for pagination
     * @return the HTTP response containing the list of configurations
     */
    public HTTPRestResponse listTaskPushNotificationConfigurations(ServerCallContext context, String tenant, String taskId, int pageSize, String pageToken) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            ListTaskPushNotificationConfigsParams params = new ListTaskPushNotificationConfigsParams(taskId, pageSize, pageToken, tenant);
            ListTaskPushNotificationConfigsResult result = requestHandler.onListTaskPushNotificationConfigs(params, context);
            return createSuccessResponse(200, org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsResponse.newBuilder(ProtoUtils.ToProto.listTaskPushNotificationConfigsResponse(result)));
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable throwable) {
            return createErrorResponse(new InternalError(throwable.getMessage()));
        }
    }

    /**
     * Deletes a push notification configuration for a task.
     *
     * @param context the server call context containing authentication and metadata
     * @param tenant the tenant identifier
     * @param taskId the ID of the task
     * @param configId the ID of the configuration to delete
     * @return the HTTP response with no content on success
     */
    public HTTPRestResponse deleteTaskPushNotificationConfiguration(ServerCallContext context, String tenant, String taskId, String configId) {
        try {
            if (!agentCard.capabilities().pushNotifications()) {
                throw new PushNotificationNotSupportedError();
            }
            DeleteTaskPushNotificationConfigParams params = new DeleteTaskPushNotificationConfigParams(taskId, configId, tenant);
            requestHandler.onDeleteTaskPushNotificationConfig(params, context);
            return new HTTPRestResponse(204, APPLICATION_JSON, "");
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
            throw new JSONParseError(A2AErrorCodes.JSON_PARSE.code(), "Failed to parse json", null);
        }
    }

    private HTTPRestResponse createSuccessResponse(int statusCode, com.google.protobuf.Message.Builder builder) {
        try {
            // Include default value fields to ensure empty arrays, zeros, etc. are present in JSON
            String jsonBody = JsonFormat.printer().alwaysPrintFieldsWithNoPresence().print(builder);
            return new HTTPRestResponse(statusCode, APPLICATION_JSON, jsonBody);
        } catch (InvalidProtocolBufferException e) {
            return createErrorResponse(new InternalError("Failed to serialize response: " + e.getMessage()));
        }
    }

    /**
     * Creates an HTTP error response from an A2A error.
     *
     * @param error the A2A error to convert
     * @return the HTTP response with appropriate status code and error details
     */
    public HTTPRestResponse createErrorResponse(A2AError error) {
        int statusCode = mapErrorToHttpStatus(error);
        return createErrorResponse(statusCode, error);
    }

    private HTTPRestResponse createErrorResponse(int statusCode, A2AError error) {
        String jsonBody = new HTTPRestErrorResponse(error).toJson();
        return new HTTPRestResponse(statusCode, APPLICATION_JSON, jsonBody);
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

    /**
     * Maps A2A protocol errors to HTTP status codes using {@link A2AErrorCodes}.
     *
     * @param error the A2A error to map
     * @return the corresponding HTTP status code
     */
    private static int mapErrorToHttpStatus(A2AError error) {
        A2AErrorCodes errorCode = A2AErrorCodes.fromCode(error.getCode());
        if (errorCode != null) {
            return errorCode.httpCode();
        }
        return A2AErrorCodes.INTERNAL.httpCode();
    }

    /**
     * Retrieves the extended agent card if configured.
     *
     * <p>
     * The extended agent card provides additional metadata beyond the public agent card,
     * such as tenant-specific configurations or private capabilities. This endpoint requires
     * the agent card to have {@code capabilities.extendedAgentCard = true} and a CDI-produced
     * {@code @ExtendedAgentCard} instance.
     *
     * <p>
     * <b>Example Request:</b></p>
     * <pre>{@code
     * GET /v1/tenants/{tenant}/extended-agent-card
     * }</pre>
     *
     * @param context the server call context containing authentication and metadata
     * @param tenant the tenant identifier
     * @return the HTTP response containing the extended agent card
     * @throws ExtendedAgentCardNotConfiguredError if extended agent card is not available
     * @see #getAgentCard()
     * @see AgentCard
     */
    public HTTPRestResponse getExtendedAgentCard(ServerCallContext context, String tenant) {
        try {
            if (!agentCard.capabilities().extendedAgentCard()) {
                throw new UnsupportedOperationError();
            }
            if (extendedAgentCard == null || !extendedAgentCard.isResolvable()) {
                throw new ExtendedAgentCardNotConfiguredError(null, "Extended Card not configured", null);
            }
            return new HTTPRestResponse(200, APPLICATION_JSON, JsonUtil.toJson(extendedAgentCard.get()));
        } catch (A2AError e) {
            return createErrorResponse(e);
        } catch (Throwable t) {
            return createErrorResponse(500, new InternalError(t.getMessage()));
        }
    }

    /**
     * Retrieves the public agent card.
     *
     * <p>
     * The agent card is a self-describing manifest that provides essential metadata about
     * the agent, including its capabilities, supported skills, communication methods, and
     * security requirements. This is the primary discovery endpoint for clients to understand
     * what the agent can do and how to interact with it.
     *
     * <p>
     * <b>Example Request:</b></p>
     * <pre>{@code
     * GET /v1/agent-card
     * }</pre>
     *
     * <p>
     * <b>Example Response:</b></p>
     * <pre>{@code
     * {
     *   "name": "Weather Agent",
     *   "description": "Provides weather information",
     *   "version": "1.0.0",
     *   "capabilities": {
     *     "streaming": true,
     *     "pushNotifications": false
     *   },
     *   "skills": [...],
     *   "supportedInterfaces": [...]
     * }
     * }</pre>
     *
     * @return the HTTP response containing the agent card
     * @see AgentCard
     * @see #getExtendedAgentCard(ServerCallContext, String)
     */
    public HTTPRestResponse getAgentCard() {
        try {
            return new HTTPRestResponse(200, APPLICATION_JSON, JsonUtil.toJson(agentCard),
                    cacheMetadata.getHttpHeadersMap());
        } catch (Throwable t) {
            return createErrorResponse(500, new InternalError(t.getMessage()));
        }
    }

    /**
     * Represents an HTTP REST response with status code, content type, and body.
     */
    public static class HTTPRestResponse {

        private final int statusCode;
        private final String contentType;
        private final String body;
        private final Map<String, String> headers;

        /**
         * Creates an HTTP REST response.
         *
         * @param statusCode the HTTP status code
         * @param contentType the content type of the response
         * @param body the response body
         */
        public HTTPRestResponse(int statusCode, String contentType, String body) {
            this(statusCode, contentType, body, Map.of());
        }

        /**
         * Creates an HTTP REST response with custom headers.
         *
         * @param statusCode the HTTP status code
         * @param contentType the content type of the response
         * @param body the response body
         * @param headers additional HTTP headers
         */
        public HTTPRestResponse(int statusCode, String contentType, String body, Map<String, String> headers) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.body = body;
            this.headers = Map.copyOf(headers);
        }

        /**
         * Returns the HTTP status code.
         *
         * @return the status code
         */
        public int getStatusCode() {
            return statusCode;
        }

        /**
         * Returns the content type.
         *
         * @return the content type
         */
        public String getContentType() {
            return contentType;
        }

        /**
         * Returns the response body.
         *
         * @return the body
         */
        public String getBody() {
            return body;
        }

        /**
         * Returns additional HTTP headers.
         *
         * @return the headers map
         */
        public Map<String, String> getHeaders() {
            return headers;
        }

        @Override
        public String toString() {
            return "HTTPRestResponse{" + "statusCode=" + statusCode + ", contentType=" + contentType + ", body=" + body + ", headers=" + headers + '}';
        }
    }

    /**
     * Represents an HTTP streaming response with Server-Sent Events.
     */
    public static class HTTPRestStreamingResponse extends HTTPRestResponse {

        private final Flow.Publisher<String> publisher;

        /**
         * Creates an HTTP streaming response.
         *
         * @param publisher the publisher of streaming events
         */
        public HTTPRestStreamingResponse(Flow.Publisher<String> publisher) {
            super(200, "text/event-stream", "");
            this.publisher = publisher;
        }

        /**
         * Returns the publisher for streaming events.
         *
         * @return the publisher
         */
        public Flow.Publisher<String> getPublisher() {
            return publisher;
        }
    }

    /**
     * Represents an HTTP error response containing A2A error details in the Google Cloud API error format.
     * <p>
     * Produces JSON of the form:
     * <pre>{@code
     * {
     *   "error": {
     *     "code": 404,
     *     "status": "NOT_FOUND",
     *     "message": "Task not found",
     *     "details": [
     *       {
     *         "@type": "type.googleapis.com/google.rpc.ErrorInfo",
     *         "reason": "TASK_NOT_FOUND",
     *         "domain": "a2a-protocol.org",
     *         "metadata": { ... }
     *       }
     *     ]
     *   }
     * }
     * }</pre>
     */
    private static class HTTPRestErrorResponse {

        private final ErrorBody error;

        private HTTPRestErrorResponse(A2AError a2aError) {
            A2AErrorCodes errorCode = A2AErrorCodes.fromCode(a2aError.getCode());
            int httpCode = mapErrorToHttpStatus(a2aError);
            String status = errorCode != null
                    ? errorCode.grpcStatus()
                    : A2AErrorCodes.INTERNAL.grpcStatus();
            String reason = errorCode != null ? errorCode.name() : "INTERNAL";
            String message = a2aError.getMessage() == null ? a2aError.getClass().getName() : a2aError.getMessage();

            ErrorDetail detail = ErrorDetail.of(reason, a2aError.getDetails());
            this.error = new ErrorBody(httpCode, status, message, List.of(detail));
        }

        private String toJson() {
            try {
                return JsonUtil.toJson(this);
            } catch (JsonProcessingException ex) {
                log.log(Level.SEVERE, "Failed to serialize HTTPRestErrorResponse to JSON", ex);
                return "{\"error\":{\"code\":500,\"status\":\"INTERNAL\",\"message\":\"Internal Server Error\",\"details\":[]}}";
            }
        }

        @Override
        public String toString() {
            return "HTTPRestErrorResponse{error=" + error + '}';
        }

        private record ErrorBody(int code, String status, String message, List<ErrorDetail> details) {}
    }
}
