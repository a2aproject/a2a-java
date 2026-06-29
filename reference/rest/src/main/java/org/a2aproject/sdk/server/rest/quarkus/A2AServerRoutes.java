package org.a2aproject.sdk.server.rest.quarkus;

import static org.a2aproject.sdk.server.ServerCallContext.TRANSPORT_KEY;
import static org.a2aproject.sdk.spec.A2AMethods.CANCEL_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_STREAMING_MESSAGE_METHOD;
import static org.a2aproject.sdk.transport.rest.context.RestContextKeys.HEADERS_KEY;
import static org.a2aproject.sdk.transport.rest.context.RestContextKeys.METHOD_NAME_KEY;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.a2aproject.sdk.server.util.sse.SseFormatter;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.AuthenticatedUser;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.auth.User;
import org.a2aproject.sdk.server.extensions.A2AExtensions;
import org.a2aproject.sdk.server.util.async.Internal;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.ContentTypeNotSupportedError;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.MethodNotFoundError;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.a2aproject.sdk.transport.rest.handler.RestHandler;
import org.a2aproject.sdk.transport.rest.handler.RestHandler.HTTPRestResponse;
import org.a2aproject.sdk.transport.rest.handler.RestHandler.HTTPRestStreamingResponse;
import org.a2aproject.sdk.server.common.quarkus.SseResponseWriter;
import org.a2aproject.sdk.server.common.quarkus.VertxSecurityHelper;
import org.a2aproject.sdk.util.Utils;
import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.smallrye.mutiny.Multi;
import jakarta.annotation.security.PermitAll;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.jspecify.annotations.Nullable;

import static org.a2aproject.sdk.spec.A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.LIST_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_MESSAGE_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SUBSCRIBE_TO_TASK_METHOD;

import static org.a2aproject.sdk.transport.rest.context.RestContextKeys.TENANT_KEY;

/**
 * Vert.x Web Router routes for A2A protocol REST endpoints.
 *
 * <p>This class defines all HTTP routes for the A2A protocol using the Vert.x Web Router API
 * ({@code @Observes Router}). Routes are registered programmatically to map HTTP requests to
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
 * via {@link SseResponseWriter}. SSE responses are handled by:
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
public class A2AServerRoutes {

    private static final String HISTORY_LENGTH_PARAM = "historyLength";
    private static final String PAGE_SIZE_PARAM = "pageSize";
    private static final String PAGE_TOKEN_PARAM = "pageToken";
    private static final String STATUS_TIMESTAMP_AFTER = "statusTimestampAfter";

    @Inject
    RestHandler jsonRestHandler;

    // Hook so testing can wait until the SSE subscriber is attached.
    // Without this we get intermittent failures
    private static volatile @Nullable
    Runnable streamingMultiSseSupportSubscribedRunnable;

    @Inject
    @Internal
    Executor executor;

    @Inject
    Instance<CallContextFactory> callContextFactory;

    @Inject
    VertxSecurityHelper vertxSecurityHelper;

    /**
     * Initializes Vert.x Web Router with all A2A REST endpoints.
     *
     * <p>This method observes the Router bean creation and configures all routes.
     * Replaces the deprecated @Route annotation-based routing with direct Router API.
     *
     * @param router the Vert.x router to configure
     */
    void setupRouter(@Observes @Priority(10) Router router) {
        // Don't add a global BodyHandler - it interferes with gRPC routes
        // Instead, BodyHandler is added per-route below

        // Message Routes

        // ordered=false on all blocking handlers: A2A requests are independent, and agent
        // delegation via Vert.x WebClient can land on the same event loop context as the outer
        // request. ordered=true would serialize them, causing a 30s deadlock.

        // POST /{tenant}/message:send - Non-streaming message send
        router.postWithRegex("^\\/(?<tenant>[^\\/]*\\/?)message:send$")
            .handler(BodyHandler.create())
            .blockingHandler(authenticated(ctx -> {
                String body = extractBody(ctx);
                sendMessage(body, ctx);
            }), false);

        // POST /{tenant}/message:stream - Streaming message with SSE
        router.postWithRegex("^\\/(?<tenant>[^\\/]*\\/?)message:stream$")
            .handler(BodyHandler.create())
            .blockingHandler(authenticatedStreaming(ctx -> {
                String body = extractBody(ctx);
                sendMessageStreaming(body, ctx);
            }), false);

        // Task Routes

        // GET /{tenant}/tasks - List tasks with query params
        router.getWithRegex("^\\/(?<tenant>[^\\/]*\\/?)tasks\\??")
            .order(0)
            .blockingHandler(authenticated(this::listTasks), false);

        // GET /{tenant}/tasks/{taskId} - Get specific task
        router.getWithRegex("^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^:^/]+)$")
            .order(1)
            .blockingHandler(authenticated(this::getTask), false);

        // POST /{tenant}/tasks/{taskId}:cancel - Cancel task
        router.postWithRegex("^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+):cancel$")
            .order(1)
            .handler(BodyHandler.create())
            .blockingHandler(authenticated(ctx -> {
                String body = extractBody(ctx);
                cancelTask(body, ctx);
            }), false);

        // POST /{tenant}/tasks/{taskId}:subscribe - Subscribe to task updates (SSE)
        router.postWithRegex("^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+):subscribe$")
            .order(1)
            .blockingHandler(authenticatedStreaming(this::subscribeToTask), false);

        // Push Notification Routes

        // POST /{tenant}/tasks/{taskId}/pushNotificationConfigs
        router.postWithRegex("^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs$")
            .order(1)
            .handler(BodyHandler.create())
            .blockingHandler(authenticated(ctx -> {
                String body = extractBody(ctx);
                createTaskPushNotificationConfiguration(body, ctx);
            }), false);

        // GET /{tenant}/tasks/{taskId}/pushNotificationConfigs/{configId}
        router.getWithRegex("^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs\\/(?<configId>[^\\/]+)")
            .order(2)
            .blockingHandler(authenticated(this::getTaskPushNotificationConfiguration), false);

        // GET /{tenant}/tasks/{taskId}/pushNotificationConfigs
        router.getWithRegex("^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs\\/?$")
            .order(3)
            .blockingHandler(authenticated(this::listTaskPushNotificationConfigurations), false);

        // DELETE /{tenant}/tasks/{taskId}/pushNotificationConfigs/{configId}
        router.deleteWithRegex("^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs\\/(?<configId>[^/]+)")
            .order(1)
            .blockingHandler(authenticated(this::deleteTaskPushNotificationConfiguration), false);

        // Discovery Routes

        // GET /.well-known/agent-card.json - Public agent card (no auth required)
        router.get("/.well-known/agent-card.json")
            .order(1)
            .produces(APPLICATION_JSON)
            .handler(this::getAgentCard);

        // GET /{tenant}/extendedAgentCard - Extended agent card (auth required)
        router.getWithRegex("^\\/(?<tenant>[^\\/]*\\/?)extendedAgentCard$")
            .order(1)
            .produces(APPLICATION_JSON)
            .blockingHandler(authenticated(this::getExtendedAgentCard), false);
    }

    private Handler<RoutingContext> authenticated(Consumer<RoutingContext> action) {
        return ctx -> {
            try {
                vertxSecurityHelper.runInRequestContext(ctx, () -> action.accept(ctx));
            } catch (UnauthorizedException | ForbiddenException e) {
                vertxSecurityHelper.handleAuthError(ctx, e);
            } catch (Exception e) {
                VertxSecurityHelper.handleGenericError(ctx);
            }
        };
    }

    private Handler<RoutingContext> authenticatedStreaming(Consumer<RoutingContext> action) {
        return ctx -> {
            try {
                vertxSecurityHelper.runInRequestContextDeferred(ctx, () -> action.accept(ctx));
            } catch (UnauthorizedException | ForbiddenException e) {
                vertxSecurityHelper.handleAuthError(ctx, e);
            } catch (Exception e) {
                VertxSecurityHelper.handleGenericError(ctx);
            }
        };
    }

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
    @Authenticated
    public void sendMessage(String body, RoutingContext rc) {
        if(!validateContentType(rc)) {
            return;
        }
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
    @Authenticated
    public void sendMessageStreaming(String body, RoutingContext rc) {
        if(!validateContentType(rc)) {
            return;
        }
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
                // Convert Flow.Publisher<String> (JSON) to Multi<String> (SSE-formatted)
                // CRITICAL: Subscribe synchronously to avoid race condition where EventConsumer
                // starts emitting events before the SSE subscriber is attached. The executor.execute()
                // wrapper caused 100-600ms delays before subscription, causing events to be lost.
                AtomicLong eventIdCounter = new AtomicLong(0);
                Multi<String> sseEvents = Multi.createFrom().publisher(streamingResponse.getPublisher())
                        .map(json -> SseFormatter.formatJsonAsSSE(json, eventIdCounter.getAndIncrement()));
                // Write SSE-formatted strings to HTTP response
                SseResponseWriter.writeSseStrings(sseEvents, rc, context, streamingMultiSseSupportSubscribedRunnable);
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
    @Authenticated
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
    @Authenticated
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
    @Authenticated
    public void cancelTask(String body, RoutingContext rc) {
        if (!validateContentTypeForOptionalBody(rc, body)) {
            return;
        }
        String taskId = rc.pathParam("taskId");
        ServerCallContext context = createCallContext(rc, CANCEL_TASK_METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad task id"));
            } else {
                response = jsonRestHandler.cancelTask(context, extractTenant(rc), body, taskId);
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
            var httpResponse = rc.response()
                    .setStatusCode(response.getStatusCode())
                    .putHeader(CONTENT_TYPE, response.getContentType());

            // Add any additional headers from the response
            response.getHeaders().forEach(httpResponse::putHeader);

            httpResponse.end(response.getBody());
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
    @Authenticated
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
                // Convert Flow.Publisher<String> (JSON) to Multi<String> (SSE-formatted)
                // CRITICAL: Subscribe synchronously to avoid race condition where EventConsumer
                // starts emitting events before the SSE subscriber is attached. The executor.execute()
                // wrapper caused 100-600ms delays before subscription, causing events to be lost.
                AtomicLong eventIdCounter = new AtomicLong(0);
                Multi<String> sseEvents = Multi.createFrom().publisher(streamingResponse.getPublisher())
                        .map(json -> SseFormatter.formatJsonAsSSE(json, eventIdCounter.getAndIncrement()));
                // Write SSE-formatted strings to HTTP response
                SseResponseWriter.writeSseStrings(sseEvents, rc, context, streamingMultiSseSupportSubscribedRunnable);
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
    @Authenticated
    public void createTaskPushNotificationConfiguration(String body, RoutingContext rc) {
        if(!validateContentType(rc)) {
            return;
        }
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
    @Authenticated
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
    @Authenticated
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
    @Authenticated
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
     * Extracts request body from routing context, returning empty string if null.
     *
     * @param rc the routing context
     * @return the request body, or empty string if null
     */
    private static String extractBody(RoutingContext rc) {
        String body = rc.body().asString();
        return body != null ? body : "";
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
     * Check if the request content type is application/json.
     * @param rc the routing context
     * @return true if the content type is application/json - false otherwise.
     */
    private boolean validateContentType(RoutingContext rc) {
        String contentType = rc.request().getHeader(CONTENT_TYPE);
        if (contentType == null || !contentType.trim().startsWith(APPLICATION_JSON)) {
            sendResponse(rc, jsonRestHandler.createErrorResponse(new ContentTypeNotSupportedError(null, null, null)));
            return false;
        }
        return true;
    }

    /**
     * Check if the request content type is application/json when body content is present.
     * Per RFC 9110 §8.3, Content-Type is only meaningful when a message body is present.
     * Use this for endpoints where the body is optional.
     *
     * @param rc the routing context
     * @param body the request body (may be null or empty)
     * @return true if validation passes, false if Content-Type error should be returned
     */
    private boolean validateContentTypeForOptionalBody(RoutingContext rc, @Nullable String body) {
        // If body is null or empty, Content-Type is not required
        if (body == null || body.isBlank()) {
            return true;
        }

        // Body has content - validate Content-Type
        return validateContentType(rc);
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
     * <p><b>Response:</b> JSON containing {@link org.a2aproject.sdk.spec.AgentCard} with:
     * <ul>
     *   <li>Agent name, description, version</li>
     *   <li>Capabilities (streaming, push notifications)</li>
     *   <li>Supported skills</li>
     *   <li>Communication interfaces and protocols</li>
     * </ul>
     *
     * @param rc the Vert.x routing context
     */
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
    @Authenticated
    public void getExtendedAgentCard(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.getExtendedAgentCard(createCallContext(rc, GET_EXTENDED_AGENT_CARD_METHOD), extractTenant(rc));
        sendResponse(rc, response);
    }

    /**
     * Catch-all route for undefined endpoints.
     *
     * <p>Handles all HTTP methods on unmatched paths with order=100 (lowest priority).
     * Returns a {@link org.a2aproject.sdk.spec.MethodNotFoundError} with HTTP 404 status.
     *
     * <p><b>Purpose:</b> Provides consistent error responses for invalid API calls
     * instead of generic 404 HTML pages.
     *
     * @param rc the Vert.x routing context
     */
    public void methodNotFoundMessage(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.createErrorResponse(new MethodNotFoundError());
        sendResponse(rc, response);
    }

    public static void setStreamingMultiSseSupportSubscribedRunnable(Runnable runnable) {
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
                String subject = rc.user().subject();
                user = subject != null ? new AuthenticatedUser(subject) : UnauthenticatedUser.INSTANCE;
            }
            Map<String, Object> state = new HashMap<>();

            Map<String, String> headers = new HashMap<>();
            Set<String> headerNames = rc.request().headers().names();
            headerNames.forEach(name -> headers.put(name, rc.request().getHeader(name)));
            state.put(HEADERS_KEY, headers);
            state.put(METHOD_NAME_KEY, jsonRpcMethodName);
            state.put(TENANT_KEY, extractTenant(rc));
            state.put(TRANSPORT_KEY, TransportProtocol.HTTP_JSON);

            // Extract requested protocol version from A2A-Version header
            String requestedVersion = rc.request().getHeader(A2AHeaders.A2A_VERSION);

            // Extract requested extensions from A2A-Extensions header
            List<String> extensionHeaderValues = rc.request().headers().getAll(A2AHeaders.A2A_EXTENSIONS);
            Set<String> requestedExtensions = A2AExtensions.getRequestedExtensions(extensionHeaderValues);

            return new ServerCallContext(user, state, requestedExtensions, requestedVersion);
        } else {
            CallContextFactory builder = callContextFactory.get();
            return builder.build(rc);
        }
    }


}
