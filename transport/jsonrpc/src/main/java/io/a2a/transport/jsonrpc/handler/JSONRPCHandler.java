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

/**
 * JSON-RPC 2.0 transport handler for processing A2A protocol requests over HTTP.
 *
 * <p>This handler converts JSON-RPC 2.0 requests into A2A protocol operations and
 * manages the lifecycle of agent interactions. It supports both blocking request/response
 * and streaming responses via Server-Sent Events.
 *
 * <h2>Request Flow</h2>
 * <p>JSON-RPC requests flow through this handler to the underlying {@link RequestHandler},
 * which coordinates with the agent executor and event queue system:
 * <pre>
 * JSON-RPC Request → JSONRPCHandler → RequestHandler → AgentExecutor
 *                         ↓               ↓
 *                   Validation      EventQueue → Response
 * </pre>
 *
 * <h2>JSON-RPC 2.0 Format</h2>
 * <p>Requests follow the JSON-RPC 2.0 specification:
 * <pre>{@code
 * {
 *   "jsonrpc": "2.0",
 *   "id": "request-123",
 *   "method": "sendMessage",
 *   "params": {
 *     "message": {...}
 *   }
 * }
 * }</pre>
 *
 * <p>Responses include the request ID for correlation:
 * <pre>{@code
 * {
 *   "jsonrpc": "2.0",
 *   "id": "request-123",
 *   "result": {
 *     "task": {...}
 *   }
 * }
 * }</pre>
 *
 * <h2>Supported Operations</h2>
 * <ul>
 *   <li>Message sending (blocking and streaming)</li>
 *   <li>Task management (get, list, cancel, subscribe)</li>
 *   <li>Push notification configurations (create, get, list, delete)</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>All A2A protocol errors are caught and converted to JSON-RPC error responses
 * with the error object embedded in the response. Protocol version and required
 * extensions are validated before processing requests.
 *
 * <h2>Streaming Support</h2>
 * <p>Streaming methods ({@code sendStreamingMessage}, {@code subscribeToTask}) return
 * a {@link Flow.Publisher} of JSON-RPC responses, allowing Server-Sent Events delivery
 * when used with Quarkus/Vert.x transport layers.
 *
 * <h2>CDI Integration</h2>
 * <p>This handler is an {@code @ApplicationScoped} CDI bean that requires:
 * <ul>
 *   <li>{@link AgentCard} qualified with {@code @PublicAgentCard}</li>
 *   <li>{@link RequestHandler} for processing A2A operations</li>
 *   <li>{@link Executor} qualified with {@code @Internal} for async operations</li>
 *   <li>Optional {@link AgentCard} qualified with {@code @ExtendedAgentCard}</li>
 * </ul>
 *
 * @see RequestHandler
 * @see io.a2a.server.requesthandlers.DefaultRequestHandler
 * @see io.a2a.spec.AgentCard
 * @see ServerCallContext
 */
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

    /**
     * Creates a JSON-RPC handler with full CDI injection support.
     *
     * @param agentCard the public agent card containing agent capabilities
     * @param extendedAgentCard optional extended agent card instance
     * @param requestHandler the handler for processing A2A requests
     * @param executor the executor for asynchronous operations
     */
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

    /**
     * Creates a JSON-RPC handler with basic dependencies.
     *
     * @param agentCard the agent card containing agent capabilities
     * @param requestHandler the handler for processing A2A requests
     * @param executor the executor for asynchronous operations
     */
    public JSONRPCHandler(@PublicAgentCard AgentCard agentCard, RequestHandler requestHandler, Executor executor) {
        this(agentCard, null, requestHandler, executor);
    }

    /**
     * Handles a blocking message send request.
     *
     * <p>This method processes a JSON-RPC {@code sendMessage} request containing a message
     * to be sent to the agent. The method blocks until the agent produces a terminal event
     * or requires authentication/input.
     *
     * <p><b>Example Request:</b>
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": "msg-123",
     *   "method": "sendMessage",
     *   "params": {
     *     "message": {
     *       "parts": [{"text": "What is the weather?"}]
     *     }
     *   }
     * }
     * }</pre>
     *
     * <p><b>Example Response:</b>
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": "msg-123",
     *   "result": {
     *     "task": {
     *       "id": "task-456",
     *       "status": {"state": "COMPLETED"},
     *       "artifacts": [...]
     *     }
     *   }
     * }
     * }</pre>
     *
     * @param request the JSON-RPC request containing message params
     * @param context the server call context containing authentication and metadata
     * @return JSON-RPC response with task or message result
     * @see #onMessageSendStream(SendStreamingMessageRequest, ServerCallContext)
     * @see RequestHandler#onMessageSend(io.a2a.spec.MessageSendParams, ServerCallContext)
     */
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

    /**
     * Handles a streaming message send request.
     *
     * <p>This method processes a JSON-RPC {@code sendStreamingMessage} request for streaming
     * responses from the agent. The response is returned as a {@link Flow.Publisher} of
     * JSON-RPC response objects, allowing Server-Sent Events delivery.
     *
     * <p>This method requires the agent card to have {@code capabilities.streaming = true}.
     *
     * <p><b>Example Request:</b>
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": "stream-123",
     *   "method": "sendStreamingMessage",
     *   "params": {
     *     "message": {
     *       "parts": [{"text": "Generate a story"}]
     *     }
     *   }
     * }
     * }</pre>
     *
     * <p><b>Example Streaming Responses:</b>
     * <pre>{@code
     * {"jsonrpc":"2.0","id":"stream-123","result":{"taskStatusUpdate":{...}}}
     * {"jsonrpc":"2.0","id":"stream-123","result":{"taskArtifactUpdate":{...}}}
     * {"jsonrpc":"2.0","id":"stream-123","result":{"taskStatusUpdate":{...}}}
     * }</pre>
     *
     * @param request the JSON-RPC request containing message params
     * @param context the server call context containing authentication and metadata
     * @return publisher of JSON-RPC response objects containing streaming events
     * @see #onMessageSend(SendMessageRequest, ServerCallContext)
     * @see RequestHandler#onMessageSendStream(io.a2a.spec.MessageSendParams, ServerCallContext)
     */
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

    /**
     * Handles a task cancellation request.
     *
     * <p>Attempts to cancel a running task identified by the task ID in the request params.
     * The cancellation request is forwarded to the {@link RequestHandler}, which signals the
     * agent executor to stop processing. The agent should transition the task to {@code CANCELED} state.
     *
     * <p><b>Example Request:</b>
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": "cancel-123",
     *   "method": "cancelTask",
     *   "params": {
     *     "taskId": "task-456"
     *   }
     * }
     * }</pre>
     *
     * @param request the JSON-RPC request containing task ID params
     * @param context the server call context containing authentication and metadata
     * @return JSON-RPC response with the cancelled task
     * @see RequestHandler#onCancelTask(io.a2a.spec.TaskIdParams, ServerCallContext)
     * @see io.a2a.server.agentexecution.AgentExecutor#cancel
     */
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

    /**
     * Subscribes to task updates via a streaming connection.
     *
     * <p>Creates a stream that delivers real-time updates for an existing task. This allows
     * clients to reconnect to ongoing or completed tasks and receive their event history
     * and future updates.
     *
     * <p>This method requires the agent card to have {@code capabilities.streaming = true}.
     *
     * <p><b>Example Request:</b>
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": "subscribe-123",
     *   "method": "subscribeToTask",
     *   "params": {
     *     "taskId": "task-456"
     *   }
     * }
     * }</pre>
     *
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>Reconnecting to a task after network interruption</li>
     *   <li>Monitoring long-running tasks from multiple clients</li>
     *   <li>Viewing historical events for completed tasks</li>
     * </ul>
     *
     * @param request the JSON-RPC request containing task ID params
     * @param context the server call context containing authentication and metadata
     * @return publisher of JSON-RPC response objects containing task updates
     * @see RequestHandler#onSubscribeToTask(io.a2a.spec.TaskIdParams, ServerCallContext)
     * @see #onMessageSendStream(SendStreamingMessageRequest, ServerCallContext)
     */
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

    /**
     * Retrieves a specific push notification configuration.
     *
     * <p>Returns the push notification configuration for a task by config ID.
     * This method requires the agent card to have {@code capabilities.pushNotifications = true}.
     *
     * <p><b>Example Request:</b>
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": "get-config-123",
     *   "method": "getTaskPushNotificationConfig",
     *   "params": {
     *     "taskId": "task-456",
     *     "configId": "config-789"
     *   }
     * }
     * }</pre>
     *
     * @param request the JSON-RPC request containing task and config ID params
     * @param context the server call context containing authentication and metadata
     * @return JSON-RPC response with the configuration
     * @see RequestHandler#onGetTaskPushNotificationConfig
     */
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

    /**
     * Creates a push notification configuration for a task.
     *
     * <p>Creates a new push notification configuration specifying webhook URL and event filters.
     * This method requires the agent card to have {@code capabilities.pushNotifications = true}.
     *
     * <p><b>Example Request:</b>
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": "create-config-123",
     *   "method": "setTaskPushNotificationConfig",
     *   "params": {
     *     "taskId": "task-456",
     *     "url": "https://webhook.example.com/notify",
     *     "events": ["taskStatusUpdate", "taskArtifactUpdate"]
     *   }
     * }
     * }</pre>
     *
     * @param request the JSON-RPC request containing push notification config params
     * @param context the server call context containing authentication and metadata
     * @return JSON-RPC response with the created configuration
     * @see RequestHandler#onCreateTaskPushNotificationConfig
     */
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

    /**
     * Retrieves a specific task by ID.
     *
     * <p>Returns the complete task object including status, artifacts, and optionally
     * task history based on the {@code historyLength} parameter.
     *
     * <p><b>Example Request:</b>
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": "get-123",
     *   "method": "getTask",
     *   "params": {
     *     "taskId": "task-456",
     *     "historyLength": 10
     *   }
     * }
     * }</pre>
     *
     * @param request the JSON-RPC request containing task query params
     * @param context the server call context containing authentication and metadata
     * @return JSON-RPC response with the task object
     * @see RequestHandler#onGetTask(io.a2a.spec.TaskQueryParams, ServerCallContext)
     */
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

    /**
     * Lists tasks with optional filtering and pagination.
     *
     * <p>Retrieves a list of tasks with support for filtering by context, status, and timestamp,
     * along with pagination controls.
     *
     * <p><b>Example Request:</b>
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": "list-123",
     *   "method": "listTasks",
     *   "params": {
     *     "status": "COMPLETED",
     *     "pageSize": 10,
     *     "includeArtifacts": true
     *   }
     * }
     * }</pre>
     *
     * <p><b>Query Parameters:</b>
     * <ul>
     *   <li>{@code contextId} - Filter tasks by conversation context</li>
     *   <li>{@code status} - Filter by task state</li>
     *   <li>{@code pageSize} - Maximum tasks to return</li>
     *   <li>{@code pageToken} - Token for pagination</li>
     *   <li>{@code historyLength} - Max history entries per task</li>
     *   <li>{@code statusTimestampAfter} - ISO-8601 timestamp filter</li>
     *   <li>{@code includeArtifacts} - Include task artifacts</li>
     * </ul>
     *
     * @param request the JSON-RPC request containing list tasks params
     * @param context the server call context containing authentication and metadata
     * @return JSON-RPC response with list of tasks
     * @see RequestHandler#onListTasks(io.a2a.spec.ListTasksParams, ServerCallContext)
     */
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

    /**
     * Lists push notification configurations for a task.
     *
     * <p>Returns a paginated list of push notification configurations associated with a task.
     * This method requires the agent card to have {@code capabilities.pushNotifications = true}.
     *
     * <p><b>Example Request:</b>
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": "list-config-123",
     *   "method": "listTaskPushNotificationConfig",
     *   "params": {
     *     "taskId": "task-456",
     *     "pageSize": 10
     *   }
     * }
     * }</pre>
     *
     * @param request the JSON-RPC request containing task ID and pagination params
     * @param context the server call context containing authentication and metadata
     * @return JSON-RPC response with list of configurations
     * @see RequestHandler#onListTaskPushNotificationConfig
     */
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

    /**
     * Deletes a push notification configuration.
     *
     * <p>Removes a push notification configuration by config ID, stopping notifications
     * for the associated task.
     * This method requires the agent card to have {@code capabilities.pushNotifications = true}.
     *
     * <p><b>Example Request:</b>
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": "delete-config-123",
     *   "method": "deleteTaskPushNotificationConfig",
     *   "params": {
     *     "taskId": "task-456",
     *     "configId": "config-789"
     *   }
     * }
     * }</pre>
     *
     * @param request the JSON-RPC request containing task and config ID params
     * @param context the server call context containing authentication and metadata
     * @return JSON-RPC response confirming deletion
     * @see RequestHandler#onDeleteTaskPushNotificationConfig
     */
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

    /**
     * Retrieves the extended agent card if configured.
     *
     * <p>The extended agent card provides additional metadata beyond the public agent card,
     * such as tenant-specific configurations or private capabilities. This endpoint requires
     * the agent card to have {@code capabilities.extendedAgentCard = true} and a CDI-produced
     * {@code @ExtendedAgentCard} instance.
     *
     * <p><b>Example Request:</b>
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": "ext-card-123",
     *   "method": "getExtendedAgentCard",
     *   "params": {}
     * }
     * }</pre>
     *
     * @param request the JSON-RPC request for extended agent card
     * @param context the server call context containing authentication and metadata
     * @return JSON-RPC response with the extended agent card
     * @see #getAgentCard()
     */
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

    /**
     * Returns the public agent card.
     *
     * <p>The agent card is a self-describing manifest that provides essential metadata about
     * the agent, including its capabilities, supported skills, communication methods, and
     * security requirements.
     *
     * @return the public agent card
     * @see AgentCard
     */
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
