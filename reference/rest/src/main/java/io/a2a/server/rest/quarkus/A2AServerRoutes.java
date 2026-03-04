package io.a2a.server.rest.quarkus;

import static io.a2a.server.ServerCallContext.TRANSPORT_KEY;
import static io.a2a.spec.A2AMethods.CANCEL_TASK_METHOD;
import static io.a2a.spec.A2AMethods.SEND_STREAMING_MESSAGE_METHOD;
import static io.a2a.transport.rest.context.RestContextKeys.HEADERS_KEY;
import static io.a2a.transport.rest.context.RestContextKeys.METHOD_NAME_KEY;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.SERVER_SENT_EVENTS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

import io.a2a.server.util.sse.SseFormatter;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.a2a.common.A2AHeaders;
import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.auth.User;
import io.a2a.server.extensions.A2AExtensions;
import io.a2a.server.util.async.Internal;
import io.a2a.spec.A2AError;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.TransportProtocol;
import io.a2a.transport.rest.handler.RestHandler;
import io.a2a.transport.rest.handler.RestHandler.HTTPRestResponse;
import io.a2a.transport.rest.handler.RestHandler.HTTPRestStreamingResponse;
import io.a2a.util.Utils;
import io.quarkus.security.Authenticated;
import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Multi;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.jspecify.annotations.Nullable;

import static io.a2a.spec.A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD;
import static io.a2a.spec.A2AMethods.GET_TASK_METHOD;
import static io.a2a.spec.A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.LIST_TASK_METHOD;
import static io.a2a.spec.A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.SEND_MESSAGE_METHOD;
import static io.a2a.spec.A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.SUBSCRIBE_TO_TASK_METHOD;

import static io.a2a.transport.rest.context.RestContextKeys.TENANT_KEY;

/**
 * Quarkus reactive routes for A2A protocol REST endpoints.
 *
 * <p>This class defines all HTTP routes for the A2A protocol using Quarkus Reactive Routes
 * (Vert.x web). Each method is annotated with {@code @Route} to map HTTP requests to
 * A2A operations, delegating to {@link RestHandler} for processing.
 *
 * <h2>Route Mapping</h2>
 * <p>Routes support optional tenant prefixing and use regex patterns for flexible matching:
 * <pre>
 * POST   /{tenant}/message:send           → sendMessage()
 * POST   /{tenant}/message:stream         → sendMessageStreaming()
 * GET    /{tenant}/tasks                  → listTasks()
 * GET    /{tenant}/tasks/{taskId}         → getTask()
 * POST   /{tenant}/tasks/{taskId}:cancel  → cancelTask()
 * POST   /{tenant}/tasks/{taskId}:subscribe → subscribeToTask()
 * GET    /.well-known/agent-card.json     → getAgentCard()
 * GET    /{tenant}/extendedAgentCard      → getExtendedAgentCard()
 * </pre>
 *
 * <h2>Authentication</h2>
 * <p>Most endpoints require authentication via {@code @Authenticated}, except:
 * <ul>
 *   <li>{@code /.well-known/agent-card.json} - Public agent discovery endpoint ({@code @PermitAll})</li>
 * </ul>
 *
 * <h2>Streaming Support</h2>
 * <p>Streaming endpoints ({@code message:stream}, {@code subscribe}) use Server-Sent Events (SSE)
 * via the inner {@link MultiSseSupport} class. SSE responses are handled by:
 * <ul>
 *   <li>Converting {@link Flow.Publisher} to Mutiny {@code Multi}</li>
 *   <li>Formatting events with {@link SseFormatter}</li>
 *   <li>Managing backpressure and client disconnection</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>All errors are caught and converted to HTTP responses via {@link RestHandler#createErrorResponse(A2AError)},
 * ensuring consistent error format and status codes across all endpoints.
 *
 * <h2>Context Creation</h2>
 * <p>Each request creates a {@link ServerCallContext} via {@link #createCallContext(RoutingContext, String)},
 * extracting:
 * <ul>
 *   <li>User authentication from Quarkus Security</li>
 *   <li>HTTP headers (including {@code X-A2A-Version}, {@code X-A2A-Extensions})</li>
 *   <li>Tenant ID from URL path</li>
 *   <li>Transport protocol metadata</li>
 * </ul>
 *
 * <p>Custom context creation is supported via CDI-provided {@link CallContextFactory}.
 *
 * @see RestHandler
 * @see ServerCallContext
 * @see CallContextFactory
 */
@Singleton
@Authenticated
public class A2AServerRoutes {

    private static final String HISTORY_LENGTH_PARAM = "historyLength";
    private static final String PAGE_SIZE_PARAM = "pageSize";
    private static final String PAGE_TOKEN_PARAM = "pageToken";
    private static final String STATUS_TIMESTAMP_AFTER = "statusTimestampAfter";

    @Inject
    RestHandler jsonRestHandler;

    // Hook so testing can wait until the MultiSseSupport is subscribed.
    // Without this we get intermittent failures
    private static volatile @Nullable
    Runnable streamingMultiSseSupportSubscribedRunnable;

    @Inject
    @Internal
    Executor executor;

    @Inject
    Instance<CallContextFactory> callContextFactory;

    /**
     * Handles blocking message send requests.
     *
     * <p>Maps {@code POST /{tenant}/message:send} to {@link RestHandler#sendMessage}.
     * The request body must be JSON containing a message with parts.
     *
     * <p><b>URL Pattern:</b> {@code /message:send} or {@code /{tenant}/message:send}
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * POST /message:send
     * Content-Type: application/json
     *
     * {
     *   "message": {
     *     "parts": [{"text": "Hello"}]
     *   }
     * }
     * }</pre>
     *
     * @param body the JSON request body
     * @param rc the Vert.x routing context
     */
    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)message:send$", order = 1, methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void sendMessage(@Body String body, RoutingContext rc) {
        ServerCallContext context = createCallContext(rc, SEND_MESSAGE_METHOD);
        HTTPRestResponse response = null;
        try {
            response = jsonRestHandler.sendMessage(context, extractTenant(rc), body);
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    /**
     * Handles streaming message send requests with Server-Sent Events.
     *
     * <p>Maps {@code POST /{tenant}/message:stream} to {@link RestHandler#sendStreamingMessage}.
     * Returns a stream of task updates and artifacts as SSE events.
     *
     * <p><b>URL Pattern:</b> {@code /message:stream} or {@code /{tenant}/message:stream}
     *
     * <p><b>Response Format:</b> {@code text/event-stream} with JSON events:
     * <pre>{@code
     * data: {"taskStatusUpdate":{"task":{"status":{"state":"WORKING"}}}}
     *
     * data: {"taskArtifactUpdate":{"artifacts":[...]}}
     *
     * data: {"taskStatusUpdate":{"task":{"status":{"state":"COMPLETED"}}}}
     * }</pre>
     *
     * @param body the JSON request body
     * @param rc the Vert.x routing context
     */
    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)message:stream$", order = 1, methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void sendMessageStreaming(@Body String body, RoutingContext rc) {
        ServerCallContext context = createCallContext(rc, SEND_STREAMING_MESSAGE_METHOD);
        HTTPRestStreamingResponse streamingResponse = null;
        HTTPRestResponse error = null;
        try {
            HTTPRestResponse response = jsonRestHandler.sendStreamingMessage(context, extractTenant(rc), body);
            if (response instanceof HTTPRestStreamingResponse hTTPRestStreamingResponse) {
                streamingResponse = hTTPRestStreamingResponse;
            } else {
                error = response;
            }
        } finally {
            if (error != null) {
                sendResponse(rc, error);
            } else if (streamingResponse != null) {
                final HTTPRestStreamingResponse finalStreamingResponse = streamingResponse;
                executor.execute(() -> {
                    // Convert Flow.Publisher<String> (JSON) to Multi<String> (SSE-formatted)
                    AtomicLong eventIdCounter = new AtomicLong(0);
                    Multi<String> sseEvents = Multi.createFrom().publisher(finalStreamingResponse.getPublisher())
                            .map(json -> SseFormatter.formatJsonAsSSE(json, eventIdCounter.getAndIncrement()));
                    // Write SSE-formatted strings to HTTP response
                    MultiSseSupport.writeSseStrings(sseEvents, rc, context);
                });
            }
        }
    }

    /**
     * Lists tasks with optional filtering and pagination.
     *
     * <p>Maps {@code GET /{tenant}/tasks} to {@link RestHandler#listTasks}.
     * Supports query parameters for filtering and pagination.
     *
     * <p><b>URL Pattern:</b> {@code /tasks?status=COMPLETED&pageSize=10}
     *
     * <p><b>Query Parameters:</b>
     * <ul>
     *   <li>{@code contextId} - Filter by conversation context</li>
     *   <li>{@code status} - Filter by task state (SUBMITTED, WORKING, COMPLETED, etc.)</li>
     *   <li>{@code pageSize} - Maximum tasks to return</li>
     *   <li>{@code pageToken} - Pagination token</li>
     *   <li>{@code historyLength} - Max history entries per task</li>
     *   <li>{@code statusTimestampAfter} - ISO-8601 timestamp filter</li>
     *   <li>{@code includeArtifacts} - Include artifacts in response (boolean)</li>
     * </ul>
     *
     * @param rc the Vert.x routing context
     */
    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)tasks\\??", order = 0, methods = {Route.HttpMethod.GET}, type = Route.HandlerType.BLOCKING)
    public void listTasks(RoutingContext rc) {
        ServerCallContext context = createCallContext(rc, LIST_TASK_METHOD);
        HTTPRestResponse response = null;
        try {
            // Extract query parameters
            String contextId = rc.request().params().get("contextId");
            String statusStr = rc.request().params().get("status");
            if (statusStr != null && !statusStr.isEmpty()) {
                statusStr = statusStr.toUpperCase();
            }
            String pageSizeStr = rc.request().params().get(PAGE_SIZE_PARAM);
            String pageToken = rc.request().params().get(PAGE_TOKEN_PARAM);
            String historyLengthStr = rc.request().params().get(HISTORY_LENGTH_PARAM);
            String statusTimestampAfter = rc.request().params().get(STATUS_TIMESTAMP_AFTER);
            String includeArtifactsStr = rc.request().params().get("includeArtifacts");

            // Parse optional parameters
            Integer pageSize = null;
            if (pageSizeStr != null && !pageSizeStr.isEmpty()) {
                pageSize = Integer.valueOf(pageSizeStr);
            }

            Integer historyLength = null;
            if (historyLengthStr != null && !historyLengthStr.isEmpty()) {
                historyLength = Integer.valueOf(historyLengthStr);
            }

            Boolean includeArtifacts = null;
            if (includeArtifactsStr != null && !includeArtifactsStr.isEmpty()) {
                includeArtifacts = Boolean.valueOf(includeArtifactsStr);
            }
            response = jsonRestHandler.listTasks(context, extractTenant(rc), contextId, statusStr, pageSize, pageToken,
                    historyLength, statusTimestampAfter, includeArtifacts);
        } catch (NumberFormatException e) {
            response = jsonRestHandler.createErrorResponse(new InvalidParamsError("Invalid number format in parameters"));
        } catch (IllegalArgumentException e) {
            response = jsonRestHandler.createErrorResponse(new InvalidParamsError("Invalid parameter value: " + e.getMessage()));
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    /**
     * Retrieves a specific task by ID.
     *
     * <p>Maps {@code GET /{tenant}/tasks/{taskId}} to {@link RestHandler#getTask}.
     * Optionally includes task history via query parameter.
     *
     * <p><b>URL Pattern:</b> {@code /tasks/{taskId}?historyLength=10}
     *
     * @param rc the Vert.x routing context (taskId extracted from path)
     */
    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^:^/]+)$", order = 1, methods = {Route.HttpMethod.GET}, type = Route.HandlerType.BLOCKING)
    public void getTask(RoutingContext rc) {
        String taskId = rc.pathParam("taskId");
        ServerCallContext context = createCallContext(rc, GET_TASK_METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad task id"));
            } else {
                Integer historyLength = null;
                if (rc.request().params().contains(HISTORY_LENGTH_PARAM)) {
                    historyLength = Integer.valueOf(rc.request().params().get(HISTORY_LENGTH_PARAM));
                }
                response = jsonRestHandler.getTask(context, extractTenant(rc), taskId, historyLength);
            }
        } catch (NumberFormatException e) {
            response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad historyLength"));
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    /**
     * Cancels a running task.
     *
     * <p>Maps {@code POST /{tenant}/tasks/{taskId}:cancel} to {@link RestHandler#cancelTask}.
     * Signals the agent executor to stop processing and transition to CANCELED state.
     *
     * <p><b>URL Pattern:</b> {@code /tasks/{taskId}:cancel}
     *
     * @param rc the Vert.x routing context (taskId extracted from path)
     */
    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+):cancel$", order = 1, methods = {Route.HttpMethod.POST}, type = Route.HandlerType.BLOCKING)
    public void cancelTask(RoutingContext rc) {
        String taskId = rc.pathParam("taskId");
        ServerCallContext context = createCallContext(rc, CANCEL_TASK_METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad task id"));
            } else {
                response = jsonRestHandler.cancelTask(context, extractTenant(rc), taskId);
            }
        } catch (Throwable t) {
            if (t instanceof A2AError error) {
                response = jsonRestHandler.createErrorResponse(error);
            } else {
                response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
            }
        } finally {
            sendResponse(rc, response);
        }
    }

    /**
     * Sends an HTTP response from a {@link HTTPRestResponse} object.
     *
     * <p>Helper method that sets status code, content type header, and body
     * from the response object. Used by all blocking endpoints.
     *
     * @param rc the Vert.x routing context
     * @param response the response to send, or null to end without body
     */
    private void sendResponse(RoutingContext rc, @Nullable HTTPRestResponse response) {
        if (response != null) {
            rc.response()
                    .setStatusCode(response.getStatusCode())
                    .putHeader(CONTENT_TYPE, response.getContentType())
                    .end(response.getBody());
        } else {
            rc.response().end();
        }
    }

    /**
     * Subscribes to task updates via Server-Sent Events.
     *
     * <p>Maps {@code POST /{tenant}/tasks/{taskId}:subscribe} to {@link RestHandler#subscribeToTask}.
     * Returns a stream of task events allowing clients to reconnect to ongoing tasks.
     *
     * <p><b>URL Pattern:</b> {@code /tasks/{taskId}:subscribe}
     *
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>Reconnecting after network interruption</li>
     *   <li>Monitoring long-running tasks</li>
     *   <li>Multiple clients observing same task</li>
     * </ul>
     *
     * @param rc the Vert.x routing context (taskId extracted from path)
     */
    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+):subscribe$", order = 1, methods = {Route.HttpMethod.POST}, type = Route.HandlerType.BLOCKING)
    public void subscribeToTask(RoutingContext rc) {
        String taskId = rc.pathParam("taskId");
        ServerCallContext context = createCallContext(rc, SUBSCRIBE_TO_TASK_METHOD);
        HTTPRestStreamingResponse streamingResponse = null;
        HTTPRestResponse error = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                error = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad task id"));
            } else {
                HTTPRestResponse response = jsonRestHandler.subscribeToTask(context, extractTenant(rc), taskId);
                if (response instanceof HTTPRestStreamingResponse hTTPRestStreamingResponse) {
                    streamingResponse = hTTPRestStreamingResponse;
                } else {
                    error = response;
                }
            }
        } finally {
            if (error != null) {
                sendResponse(rc, error);
            } else if (streamingResponse != null) {
                final HTTPRestStreamingResponse finalStreamingResponse = streamingResponse;
                executor.execute(() -> {
                    // Convert Flow.Publisher<String> (JSON) to Multi<String> (SSE-formatted)
                    AtomicLong eventIdCounter = new AtomicLong(0);
                    Multi<String> sseEvents = Multi.createFrom().publisher(finalStreamingResponse.getPublisher())
                            .map(json -> SseFormatter.formatJsonAsSSE(json, eventIdCounter.getAndIncrement()));
                    // Write SSE-formatted strings to HTTP response
                    MultiSseSupport.writeSseStrings(sseEvents, rc, context);
                });
            }
        }
    }

    /**
     * Creates a push notification configuration for a task.
     *
     * <p>Maps {@code POST /{tenant}/tasks/{taskId}/pushNotificationConfigs} to
     * {@link RestHandler#createTaskPushNotificationConfiguration}.
     *
     * <p><b>URL Pattern:</b> {@code /tasks/{taskId}/pushNotificationConfigs}
     *
     * <p><b>Request Body:</b> JSON containing webhook URL and event filters
     *
     * @param body the JSON request body with notification configuration
     * @param rc the Vert.x routing context (taskId extracted from path)
     */
    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs$", order = 1, methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void CreateTaskPushNotificationConfiguration(@Body String body, RoutingContext rc) {
        String taskId = rc.pathParam("taskId");
        ServerCallContext context = createCallContext(rc, SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad task id"));
            } else {
                response = jsonRestHandler.createTaskPushNotificationConfiguration(context, extractTenant(rc), body, taskId);
            }
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    /**
     * Retrieves a specific push notification configuration.
     *
     * <p>Maps {@code GET /{tenant}/tasks/{taskId}/pushNotificationConfigs/{configId}} to
     * {@link RestHandler#getTaskPushNotificationConfiguration}.
     *
     * <p><b>URL Pattern:</b> {@code /tasks/{taskId}/pushNotificationConfigs/{configId}}
     *
     * @param rc the Vert.x routing context (taskId and configId extracted from path)
     */
    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs\\/(?<configId>[^\\/]+)", order = 2, methods = {Route.HttpMethod.GET}, type = Route.HandlerType.BLOCKING)
    public void getTaskPushNotificationConfiguration(RoutingContext rc) {
        String taskId = rc.pathParam("taskId");
        String configId = rc.pathParam("configId");
        ServerCallContext context = createCallContext(rc, GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad task id"));
            } else if (configId == null || configId.isEmpty()) { 
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad configuration id"));
            }else {
                response = jsonRestHandler.getTaskPushNotificationConfiguration(context, extractTenant(rc), taskId, configId);
            }
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    /**
     * Lists push notification configurations for a task.
     *
     * <p>Maps {@code GET /{tenant}/tasks/{taskId}/pushNotificationConfigs} to
     * {@link RestHandler#listTaskPushNotificationConfigurations}.
     * Supports pagination via query parameters.
     *
     * <p><b>URL Pattern:</b> {@code /tasks/{taskId}/pushNotificationConfigs?pageSize=10}
     *
     * <p><b>Query Parameters:</b>
     * <ul>
     *   <li>{@code pageSize} - Maximum configurations to return</li>
     *   <li>{@code pageToken} - Pagination token for next page</li>
     * </ul>
     *
     * @param rc the Vert.x routing context (taskId extracted from path)
     */
    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs\\/?$", order = 3, methods = {Route.HttpMethod.GET}, type = Route.HandlerType.BLOCKING)
    public void listTaskPushNotificationConfigurations(RoutingContext rc) {
        String taskId = rc.pathParam("taskId");
        ServerCallContext context = createCallContext(rc, LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad task id"));
            } else {
                 int pageSize = 0;
                if (rc.request().params().contains(PAGE_SIZE_PARAM)) {
                    pageSize = Integer.parseInt(rc.request().params().get(PAGE_SIZE_PARAM));
                }
                String pageToken = "";
                if (rc.request().params().contains(PAGE_TOKEN_PARAM)) {
                    pageToken = Utils.defaultIfNull(rc.request().params().get(PAGE_TOKEN_PARAM), "");
                }
                response = jsonRestHandler.listTaskPushNotificationConfigurations(context, extractTenant(rc), taskId, pageSize, pageToken);
            }
        } catch (NumberFormatException e) {
            response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad " + PAGE_SIZE_PARAM));
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    /**
     * Deletes a push notification configuration.
     *
     * <p>Maps {@code DELETE /{tenant}/tasks/{taskId}/pushNotificationConfigs/{configId}} to
     * {@link RestHandler#deleteTaskPushNotificationConfiguration}.
     *
     * <p><b>URL Pattern:</b> {@code /tasks/{taskId}/pushNotificationConfigs/{configId}}
     *
     * <p><b>Response:</b> HTTP 204 No Content on success
     *
     * @param rc the Vert.x routing context (taskId and configId extracted from path)
     */
    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs\\/(?<configId>[^/]+)", order = 1, methods = {Route.HttpMethod.DELETE}, type = Route.HandlerType.BLOCKING)
    public void deleteTaskPushNotificationConfiguration(RoutingContext rc) {
        String taskId = rc.pathParam("taskId");
        String configId = rc.pathParam("configId");
        ServerCallContext context = createCallContext(rc, DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad task id"));
            } else if (configId == null || configId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad config id"));
            } else {
                response = jsonRestHandler.deleteTaskPushNotificationConfiguration(context, extractTenant(rc), taskId, configId);
            }
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    /**
     * Extracts tenant ID from the routing context path parameter.
     *
     * <p>Handles optional tenant prefixes in URL paths, normalizing:
     * <ul>
     *   <li>{@code /message:send} → empty string (default tenant)</li>
     *   <li>{@code /tenant-123/message:send} → "tenant-123"</li>
     *   <li>{@code /tenant-123} → "tenant-123" (strips leading/trailing slashes)</li>
     * </ul>
     *
     * @param rc the Vert.x routing context
     * @return the extracted tenant ID, or empty string if not specified
     */
    private String extractTenant(RoutingContext rc) {
        String tenantPath = rc.pathParam("tenant");
        if (tenantPath == null || tenantPath.isBlank()) {
            return "";
        }
        if (tenantPath.startsWith("/")) {
            tenantPath = tenantPath.substring(1);
        }
        if (tenantPath.endsWith("/")) {
            tenantPath = tenantPath.substring(0, tenantPath.length() - 1);
        }
        return tenantPath;
    }

    /**
     * Retrieves the public agent card for agent discovery.
     *
     * <p>Maps {@code GET /.well-known/agent-card.json} to {@link RestHandler#getAgentCard}.
     * This is the primary discovery endpoint that clients use to understand agent capabilities,
     * supported skills, and communication methods.
     *
     * <p><b>URL Pattern:</b> {@code /.well-known/agent-card.json} (well-known URI)
     *
     * <p><b>Authentication:</b> {@code @PermitAll} - Public endpoint requiring no authentication
     *
     * <p><b>Response:</b> JSON containing {@link io.a2a.spec.AgentCard} with:
     * <ul>
     *   <li>Agent name, description, version</li>
     *   <li>Capabilities (streaming, push notifications)</li>
     *   <li>Supported skills</li>
     *   <li>Communication interfaces and protocols</li>
     * </ul>
     *
     * @param rc the Vert.x routing context
     */
    @Route(path = "/.well-known/agent-card.json", order = 1, methods = Route.HttpMethod.GET, produces = APPLICATION_JSON)
    @PermitAll
    public void getAgentCard(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.getAgentCard();
        sendResponse(rc, response);
    }

    /**
     * Retrieves the extended agent card with additional metadata.
     *
     * <p>Maps {@code GET /{tenant}/extendedAgentCard} to {@link RestHandler#getExtendedAgentCard}.
     * Provides tenant-specific or private capabilities beyond the public agent card.
     *
     * <p><b>URL Pattern:</b> {@code /extendedAgentCard} or {@code /{tenant}/extendedAgentCard}
     *
     * <p><b>Authentication:</b> Required (inherits {@code @Authenticated} from class)
     *
     * @param rc the Vert.x routing context
     */
    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)extendedAgentCard$", order = 1, methods = Route.HttpMethod.GET, produces = APPLICATION_JSON)
    public void getExtendedAgentCard(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.getExtendedAgentCard(createCallContext(rc, GET_EXTENDED_AGENT_CARD_METHOD), extractTenant(rc));
        sendResponse(rc, response);
    }

    /**
     * Catch-all route for undefined endpoints.
     *
     * <p>Handles all HTTP methods on unmatched paths with order=100 (lowest priority).
     * Returns a {@link io.a2a.spec.MethodNotFoundError} with HTTP 404 status.
     *
     * <p><b>Purpose:</b> Provides consistent error responses for invalid API calls
     * instead of generic 404 HTML pages.
     *
     * @param rc the Vert.x routing context
     */
    @Route(path = "^/.*", order = 100, methods = {Route.HttpMethod.DELETE, Route.HttpMethod.GET, Route.HttpMethod.HEAD, Route.HttpMethod.OPTIONS, Route.HttpMethod.POST, Route.HttpMethod.PUT}, produces = APPLICATION_JSON)
    public void methodNotFoundMessage(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.createErrorResponse(new MethodNotFoundError());
        sendResponse(rc, response);
    }

    static void setStreamingMultiSseSupportSubscribedRunnable(Runnable runnable) {
        streamingMultiSseSupportSubscribedRunnable = runnable;
    }

    /**
     * Creates a {@link ServerCallContext} from Vert.x routing context.
     *
     * <p>This method extracts authentication, headers, and protocol metadata from the
     * HTTP request and builds a context object that flows through the request processing
     * pipeline to the agent executor.
     *
     * <h2>Context Contents</h2>
     * <p>The created context includes:
     * <ul>
     *   <li><b>User:</b> From Quarkus Security (or {@link UnauthenticatedUser} if absent)</li>
     *   <li><b>Headers:</b> All HTTP headers as a map</li>
     *   <li><b>Tenant:</b> Extracted from URL path parameter</li>
     *   <li><b>Method Name:</b> A2A method being invoked</li>
     *   <li><b>Transport:</b> {@link TransportProtocol#HTTP_JSON}</li>
     *   <li><b>Protocol Version:</b> From {@code X-A2A-Version} header</li>
     *   <li><b>Extensions:</b> From {@code X-A2A-Extensions} header</li>
     * </ul>
     *
     * <h2>Custom Context Factory</h2>
     * <p>If a CDI bean implementing {@link CallContextFactory} is provided, it will be
     * used instead of the default implementation. This allows applications to add custom
     * context data or modify the extraction logic.
     *
     * @param rc the Vert.x routing context containing request data
     * @param jsonRpcMethodName the A2A method name (e.g., "sendMessage", "cancelTask")
     * @return a new ServerCallContext with extracted request metadata
     * @see CallContextFactory
     * @see ServerCallContext
     */
    private ServerCallContext createCallContext(RoutingContext rc, String jsonRpcMethodName) {
        if (callContextFactory.isUnsatisfied()) {
            User user;
            if (rc.user() == null) {
                user = UnauthenticatedUser.INSTANCE;
            } else {
                user = new User() {
                    @Override
                    public boolean isAuthenticated() {
                        if (rc.userContext() != null) {
                            return rc.userContext().authenticated();
                        }
                        return false;
                    }

                    @Override
                    public String getUsername() {
                        if (rc.user() != null && rc.user().subject() != null) {
                            return rc.user().subject();
                        }
                        return "";
                    }
                };
            }
            Map<String, Object> state = new HashMap<>();
            // TODO Python's impl has
            //    state['auth'] = request.auth
            //  in jsonrpc_app.py. Figure out what this maps to in what Vert.X gives us

            Map<String, String> headers = new HashMap<>();
            Set<String> headerNames = rc.request().headers().names();
            headerNames.forEach(name -> headers.put(name, rc.request().getHeader(name)));
            state.put(HEADERS_KEY, headers);
            state.put(METHOD_NAME_KEY, jsonRpcMethodName);
            state.put(TENANT_KEY, extractTenant(rc));
            state.put(TRANSPORT_KEY, TransportProtocol.HTTP_JSON);

            // Extract requested protocol version from X-A2A-Version header
            String requestedVersion = rc.request().getHeader(A2AHeaders.X_A2A_VERSION);

            // Extract requested extensions from X-A2A-Extensions header
            List<String> extensionHeaderValues = rc.request().headers().getAll(A2AHeaders.X_A2A_EXTENSIONS);
            Set<String> requestedExtensions = A2AExtensions.getRequestedExtensions(extensionHeaderValues);

            return new ServerCallContext(user, state, requestedExtensions, requestedVersion);
        } else {
            CallContextFactory builder = callContextFactory.get();
            return builder.build(rc);
        }
    }

    /**
     * Server-Sent Events (SSE) streaming support for Vert.x/Quarkus.
     *
     * <p>This inner class handles the HTTP-specific aspects of SSE streaming:
     * <ul>
     *   <li>Writing SSE-formatted events to the HTTP response</li>
     *   <li>Managing backpressure via {@link Flow.Subscription#request(long)}</li>
     *   <li>Detecting client disconnection and canceling upstream</li>
     *   <li>Setting appropriate SSE headers and chunked encoding</li>
     * </ul>
     *
     * <h2>SSE Format</h2>
     * <p>Events are formatted by {@link SseFormatter} before being passed to this class.
     * Each event follows the SSE specification:
     * <pre>
     * id: 0
     * data: {"taskStatusUpdate":{...}}
     *
     * id: 1
     * data: {"taskArtifactUpdate":{...}}
     * </pre>
     *
     * <h2>Backpressure Handling</h2>
     * <p>The subscriber requests one event at a time ({@code request(1)}) and only
     * requests the next event after the previous write completes. This ensures the
     * HTTP connection doesn't buffer excessive data if the client is slow.
     *
     * <h2>Disconnect Detection</h2>
     * <p>When the client closes the connection, this class:
     * <ol>
     *   <li>Calls {@link ServerCallContext#invokeEventConsumerCancelCallback()} to stop the event producer</li>
     *   <li>Cancels the upstream subscription to stop event generation</li>
     * </ol>
     *
     * <h2>Write Queue Configuration</h2>
     * <p>Critical: Sets {@code setWriteQueueMaxSize(1)} to force immediate flushing
     * of each event. Without this, Vert.x buffers writes, causing delays in SSE delivery.
     *
     * @see SseFormatter
     * @see ServerCallContext#invokeEventConsumerCancelCallback()
     */
    private static class MultiSseSupport {
        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MultiSseSupport.class);

        private MultiSseSupport() {
            // Avoid direct instantiation.
        }

        /**
         * Writes SSE-formatted event strings to the HTTP response with backpressure control.
         *
         * <p>This method subscribes to the event stream and writes each SSE-formatted string
         * to the Vert.x HTTP response. It implements reactive backpressure by requesting
         * events one at a time and only requesting the next after the previous write completes.
         *
         * <h2>Execution Flow</h2>
         * <ol>
         *   <li>Subscribe to upstream {@code Multi<String>} (SSE-formatted events)</li>
         *   <li>On first event: set SSE headers, disable buffering, write kickstart comment</li>
         *   <li>For each event: write to HTTP response asynchronously</li>
         *   <li>After write completes: request next event (backpressure control)</li>
         *   <li>On client disconnect or error: cancel upstream to stop event production</li>
         * </ol>
         *
         * <h2>Headers Set</h2>
         * <ul>
         *   <li>{@code Content-Type: text/event-stream}</li>
         *   <li>{@code Cache-Control: no-cache}</li>
         *   <li>{@code X-Accel-Buffering: no} (disable nginx buffering)</li>
         *   <li>Chunked encoding enabled</li>
         * </ul>
         *
         * @param sseStrings Multi stream of SSE-formatted strings (from {@link SseFormatter})
         * @param rc Vert.x routing context providing HTTP response
         * @param context A2A server call context (for EventConsumer cancellation on disconnect)
         */
        public static void writeSseStrings(Multi<String> sseStrings, RoutingContext rc, ServerCallContext context) {
            HttpServerResponse response = rc.response();

            sseStrings.subscribe().withSubscriber(new Flow.Subscriber<String>() {
                Flow.@Nullable Subscription upstream;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.upstream = subscription;
                    this.upstream.request(1);

                    // Detect client disconnect and call EventConsumer.cancel() directly
                    response.closeHandler(v -> {
                        logger.debug("REST SSE connection closed by client, calling EventConsumer.cancel() to stop polling loop");
                        context.invokeEventConsumerCancelCallback();
                        subscription.cancel();
                    });

                    // Notify tests that we are subscribed
                    Runnable runnable = streamingMultiSseSupportSubscribedRunnable;
                    if (runnable != null) {
                        runnable.run();
                    }
                }

                @Override
                public void onNext(String sseEvent) {
                    // Set SSE headers on first event
                    if (response.bytesWritten() == 0) {
                        MultiMap headers = response.headers();
                        if (headers.get(CONTENT_TYPE) == null) {
                            headers.set(CONTENT_TYPE, SERVER_SENT_EVENTS);
                        }
                        // Additional SSE headers to prevent buffering
                        headers.set("Cache-Control", "no-cache");
                        headers.set("X-Accel-Buffering", "no");  // Disable nginx buffering
                        response.setChunked(true);

                        // CRITICAL: Disable write queue max size to prevent buffering
                        // Vert.x buffers writes by default - we need immediate flushing for SSE
                        response.setWriteQueueMaxSize(1);  // Force immediate flush

                        // Send initial SSE comment to kickstart the stream
                        // This forces Vert.x to send headers and start the stream immediately
                        response.write(": SSE stream started\n\n");
                    }

                    // Write SSE-formatted string to response
                    response.write(Buffer.buffer(sseEvent), new Handler<AsyncResult<Void>>() {
                        @Override
                        public void handle(AsyncResult<Void> ar) {
                            if (ar.failed()) {
                                // Client disconnected or write failed - cancel upstream to stop EventConsumer
                                // NullAway: upstream is guaranteed non-null after onSubscribe
                                java.util.Objects.requireNonNull(upstream).cancel();
                                rc.fail(ar.cause());
                            } else {
                                // NullAway: upstream is guaranteed non-null after onSubscribe
                                java.util.Objects.requireNonNull(upstream).request(1);
                            }
                        }
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                    // Cancel upstream to stop EventConsumer when error occurs
                    // NullAway: upstream is guaranteed non-null after onSubscribe
                    java.util.Objects.requireNonNull(upstream).cancel();
                    rc.fail(throwable);
                }

                @Override
                public void onComplete() {
                    if (response.bytesWritten() == 0) {
                        // No events written - still set SSE content type
                        MultiMap headers = response.headers();
                        if (headers.get(CONTENT_TYPE) == null) {
                            headers.set(CONTENT_TYPE, SERVER_SENT_EVENTS);
                        }
                    }
                    response.end();
                }
            });
        }
    }

}
