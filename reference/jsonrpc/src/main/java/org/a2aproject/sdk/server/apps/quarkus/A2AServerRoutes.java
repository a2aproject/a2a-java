package org.a2aproject.sdk.server.apps.quarkus;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.a2aproject.sdk.server.ServerCallContext.TRANSPORT_KEY;
import static org.a2aproject.sdk.transport.jsonrpc.context.JSONRPCContextKeys.HEADERS_KEY;
import static org.a2aproject.sdk.transport.jsonrpc.context.JSONRPCContextKeys.METHOD_NAME_KEY;
import static org.a2aproject.sdk.transport.jsonrpc.context.JSONRPCContextKeys.TENANT_KEY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.gson.JsonSyntaxException;
import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.smallrye.mutiny.Multi;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.grpc.utils.JSONRPCUtils;
import org.a2aproject.sdk.jsonrpc.common.json.IdJsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.json.InvalidParamsJsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.jsonrpc.common.json.MethodNotFoundJsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2AErrorResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2ARequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2AResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CancelTaskRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CancelTaskResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetExtendedAgentCardRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetExtendedAgentCardResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTaskPushNotificationConfigsRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTaskPushNotificationConfigsResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.NonStreamingJSONRPCRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendMessageRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendMessageResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendStreamingMessageRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendStreamingMessageResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SubscribeToTaskRequest;
import org.a2aproject.sdk.server.AgentCardCacheMetadata;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.AuthenticatedUser;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.auth.User;
import org.a2aproject.sdk.server.common.quarkus.SseResponseWriter;
import org.a2aproject.sdk.server.common.quarkus.VertxSecurityHelper;
import org.a2aproject.sdk.server.extensions.A2AExtensions;
import org.a2aproject.sdk.server.util.async.Internal;
import org.a2aproject.sdk.server.util.sse.SseFormatter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.JSONParseError;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.a2aproject.sdk.transport.jsonrpc.handler.JSONRPCHandler;
import org.jspecify.annotations.Nullable;

/**
 * Quarkus routing configuration for JSON-RPC A2A protocol requests.
 *
 * <p>This class defines Vert.x Web routes for handling JSON-RPC 2.0 requests over HTTP,
 * processing them through the {@link JSONRPCHandler}, and returning responses in either
 * standard JSON or Server-Sent Events (SSE) format for streaming operations.
 *
 * <h2>Request Flow</h2>
 * <pre>
 * HTTP POST / → invokeJSONRPCHandler()
 *     ↓
 * Parse JSON-RPC request body
 *     ↓
 * Route to handler method (blocking or streaming)
 *     ↓
 * JSONRPCHandler → RequestHandler → AgentExecutor
 *     ↓
 * Response (JSON or SSE stream)
 * </pre>
 *
 * <h2>Supported Operations</h2>
 * <p><b>Non-Streaming (JSON responses):</b>
 * <ul>
 *   <li>{@code sendMessage} - Send message and wait for completion</li>
 *   <li>{@code getTask} - Retrieve task by ID</li>
 *   <li>{@code cancelTask} - Cancel task execution</li>
 *   <li>{@code listTasks} - List tasks with filtering</li>
 *   <li>{@code setTaskPushNotificationConfig} - Configure push notifications</li>
 *   <li>{@code getTaskPushNotificationConfig} - Get push notification config</li>
 *   <li>{@code listTaskPushNotificationConfigs} - List push notification configs</li>
 *   <li>{@code deleteTaskPushNotificationConfig} - Delete push notification config</li>
 *   <li>{@code getExtendedAgentCard} - Get extended agent capabilities</li>
 * </ul>
 *
 * <p><b>Streaming (SSE responses):</b>
 * <ul>
 *   <li>{@code sendStreamingMessage} - Send message with streaming response</li>
 *   <li>{@code subscribeToTask} - Subscribe to task events</li>
 * </ul>
 *
 * <h2>JSON-RPC Request Format</h2>
 * <pre>{@code
 * POST /
 * Content-Type: application/json
 *
 * {
 *   "jsonrpc": "2.0",
 *   "id": "req-123",
 *   "method": "sendMessage",
 *   "params": {
 *     "message": {
 *       "parts": [{"text": "Hello"}]
 *     }
 *   }
 * }
 * }</pre>
 *
 * <h2>Error Handling</h2>
 * <p>Errors are mapped to JSON-RPC 2.0 error responses:
 * <ul>
 *   <li>{@link JsonSyntaxException} → {@link JSONParseError}</li>
 *   <li>{@link InvalidParamsJsonMappingException} → {@link org.a2aproject.sdk.spec.InvalidParamsError}</li>
 *   <li>{@link MethodNotFoundJsonMappingException} → {@link org.a2aproject.sdk.spec.MethodNotFoundError}</li>
 *   <li>{@link IdJsonMappingException} → {@link org.a2aproject.sdk.spec.InvalidRequestError}</li>
 *   <li>{@link Throwable} → {@link InternalError}</li>
 * </ul>
 *
 * <h2>CDI Integration</h2>
 * <p>This class is a CDI {@code @Singleton} that automatically wires:
 * <ul>
 *   <li>{@link JSONRPCHandler} - Core protocol handler</li>
 *   <li>{@link Executor} - Async execution for streaming</li>
 *   <li>{@link CallContextFactory} (optional) - Custom context creation</li>
 * </ul>
 *
 * <h2>Multi-Tenancy Support</h2>
 * <p>Tenant identification is extracted from the request path:
 * <ul>
 *   <li>{@code POST /} → empty tenant</li>
 *   <li>{@code POST /tenant1} → tenant "tenant1"</li>
 *   <li>{@code POST /tenant1/} → tenant "tenant1" (trailing slash stripped)</li>
 * </ul>
 *
 * @see JSONRPCHandler
 * @see CallContextFactory
 * @see SseResponseWriter
 */
@Singleton
public class A2AServerRoutes {

    @Inject
    JSONRPCHandler jsonRpcHandler;

    @Inject
    AgentCardCacheMetadata cacheMetadata;

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
     * Configures Vert.x Web Router with JSON-RPC routes.
     *
     * <p>This method is invoked during application startup to register all HTTP routes
     * for the JSON-RPC transport. Routes are configured with direct Vert.x Web Router
     * instead of Quarkus Reactive Routes.
     *
     * @param router the Vert.x Web Router instance to configure
     */
    void setupRoutes(@Observes Router router) {
        // Main JSON-RPC endpoint: POST /
        // BodyHandler is per-route (not global) to avoid interfering with gRPC routes
        // ordered=false: delegation via Vert.x WebClient can share the same event loop context as the outer request; ordered=true would serialize them, causing a 30s deadlock.
        router.post("/")
            .consumes(APPLICATION_JSON)
            .handler(BodyHandler.create())
            .blockingHandler(ctx -> {
                try {
                    vertxSecurityHelper.runInRequestContextDeferred(ctx, () -> {
                        invokeJSONRPCHandler(ctx.body().asString(), ctx);
                    });
                } catch (UnauthorizedException | ForbiddenException e) {
                    vertxSecurityHelper.handleAuthError(ctx, e);
                } catch (Exception e) {
                    VertxSecurityHelper.handleGenericError(ctx);
                }
            }, false);

        // Agent card endpoint: GET /.well-known/agent-card.json
        router.get("/.well-known/agent-card.json")
            .produces(APPLICATION_JSON)
            .handler(ctx -> {
                try {
                    String agentCard = getAgentCard(ctx);
                    ctx.response()
                        .setStatusCode(200)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(agentCard);
                } catch (JsonProcessingException e) {
                    ctx.response().setStatusCode(500).end("Internal Server Error");
                }
            });
    }

    /**
     * Main entry point for all JSON-RPC requests.
     *
     * <p>This route handler processes JSON-RPC 2.0 requests, dispatches them to the appropriate
     * handler method based on the request type (streaming vs non-streaming), and returns either
     * a JSON response or an SSE stream.
     *
     * <p><b>Request Format:</b>
     * <pre>{@code
     * POST /[tenant]
     * Content-Type: application/json
     *
     * {
     *   "jsonrpc": "2.0",
     *   "id": "req-123",
     *   "method": "sendMessage",
     *   "params": { ... }
     * }
     * }</pre>
     *
     * <p><b>Non-Streaming Response:</b>
     * <pre>{@code
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     *
     * {
     *   "jsonrpc": "2.0",
     *   "id": "req-123",
     *   "result": { ... }
     * }
     * }</pre>
     *
     * <p><b>Streaming Response (SSE):</b>
     * <pre>{@code
     * HTTP/1.1 200 OK
     * Content-Type: text/event-stream
     *
     * id: 0
     * data: {"jsonrpc":"2.0","id":"req-123","result":{...}}
     *
     * id: 1
     * data: {"jsonrpc":"2.0","id":"req-123","result":{...}}
     * }</pre>
     *
     * <p><b>Error Response:</b>
     * <pre>{@code
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     *
     * {
     *   "jsonrpc": "2.0",
     *   "id": "req-123",
     *   "error": {
     *     "code": -32602,
     *     "message": "Invalid params"
     *   }
     * }
     * }</pre>
     *
     * <p><b>Processing Flow:</b>
     * <ol>
     *   <li>Parse JSON-RPC request body using {@link JSONRPCUtils#parseRequestBody(String, String)}</li>
     *   <li>Create {@link ServerCallContext} from routing context</li>
     *   <li>Route to streaming or non-streaming handler</li>
     *   <li>Handle errors with appropriate JSON-RPC error codes</li>
     *   <li>Return JSON response or start SSE stream</li>
     * </ol>
     *
     * @param body the raw JSON-RPC request body
     * @param rc the Vert.x routing context containing HTTP request/response
     * @throws A2AError if request processing fails
     */
    @Authenticated
    public void invokeJSONRPCHandler(String body, RoutingContext rc) {
        boolean streaming = false;
        ServerCallContext context = createCallContext(rc);
        A2AResponse<?> nonStreamingResponse = null;
        Multi<? extends A2AResponse<?>> streamingResponse = null;
        A2AErrorResponse error = null;
        try {
            A2ARequest<?> request = JSONRPCUtils.parseRequestBody(body, extractTenant(rc));
            context.getState().put(METHOD_NAME_KEY, request.getMethod());
            if (request instanceof NonStreamingJSONRPCRequest nonStreamingRequest) {
                nonStreamingResponse = processNonStreamingRequest(nonStreamingRequest, context);
            } else {
                streaming = true;
                streamingResponse = processStreamingRequest(request, context);
            }
        } catch (A2AError e) {
            error = new A2AErrorResponse(e);
        } catch (InvalidParamsJsonMappingException e) {
            error = new A2AErrorResponse(e.getId(), new org.a2aproject.sdk.spec.InvalidParamsError(null, e.getMessage(), null));
        } catch (MethodNotFoundJsonMappingException e) {
            error = new A2AErrorResponse(e.getId(), new org.a2aproject.sdk.spec.MethodNotFoundError(null, e.getMessage(), null));
        } catch (IdJsonMappingException e) {
            error = new A2AErrorResponse(e.getId(), new org.a2aproject.sdk.spec.InvalidRequestError(null, e.getMessage(), null));
        } catch (JsonMappingException e) {
            // General JsonMappingException - treat as InvalidRequest
            error = new A2AErrorResponse(new org.a2aproject.sdk.spec.InvalidRequestError(null, e.getMessage(), null));
        } catch (JsonSyntaxException | JsonProcessingException e) {
            error = new A2AErrorResponse(new JSONParseError(e.getMessage()));
        } catch (Throwable t) {
            error = new A2AErrorResponse(new InternalError(t.getMessage()));
        } finally {
            if (error != null) {
                rc.response()
                        .setStatusCode(200)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(serializeResponse(error));
            } else if (streaming) {
                // Convert Multi<A2AResponse> to Multi<String> with SSE formatting
                // CRITICAL: Subscribe synchronously to avoid race condition where EventConsumer
                // starts emitting events before the SSE subscriber is attached. The executor.execute()
                // wrapper caused 100-600ms delays before subscription, causing events to be lost.
                AtomicLong eventIdCounter = new AtomicLong(0);
                Multi<String> sseEvents = streamingResponse
                        .map(response -> SseFormatter.formatResponseAsSSE(response, eventIdCounter.getAndIncrement()));
                // Write SSE-formatted strings to HTTP response
                SseResponseWriter.writeSseStrings(sseEvents, rc, context, streamingMultiSseSupportSubscribedRunnable);

            } else {
                rc.response()
                        .setStatusCode(200)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(serializeResponse(nonStreamingResponse));
            }
        }
    }

    /**
     * Handles GET requests to the agent card endpoint.
     *
     * <p>Returns the agent's capabilities and metadata in JSON format according to the
     * A2A protocol specification. This endpoint is publicly accessible (no authentication).
     *
     * <p>Includes HTTP caching headers per A2A specification section 8.6:
     * <ul>
     *   <li>{@code Cache-Control} - with max-age directive</li>
     *   <li>{@code ETag} - content hash for validation</li>
     *   <li>{@code Last-Modified} - timestamp when agent card was initialized</li>
     * </ul>
     *
     * <p><b>Request:</b>
     * <pre>{@code
     * GET /.well-known/agent-card.json
     * }</pre>
     *
     * <p><b>Response:</b>
     * <pre>{@code
     * HTTP/1.1 200 OK
     * Content-Type: application/json
     * Cache-Control: public, max-age=3600
     * ETag: "a1b2c3d4..."
     * Last-Modified: Mon, 17 Mar 2025 10:00:00 GMT
     *
     * {
     *   "name": "My Agent",
     *   "description": "Agent description",
     *   "capabilities": {
     *     "streaming": true,
     *     "pushNotifications": false
     *   },
     *   ...
     * }
     * }</pre>
     *
     * @param rc the Vert.x routing context
     * @return the agent card as a JSON string
     * @throws JsonProcessingException if serialization fails
     * @see JSONRPCHandler#getAgentCard()
     */
    public String getAgentCard(RoutingContext rc) throws JsonProcessingException {
        // Add caching headers per A2A specification section 8.6
        cacheMetadata.getHttpHeadersMap().forEach((k, v) -> rc.response().putHeader(k, v));
        return JsonUtil.toJson(jsonRpcHandler.getAgentCard());
    }

    /**
     * Routes non-streaming JSON-RPC requests to the appropriate handler method.
     *
     * <p>This method uses pattern matching to dispatch requests based on their type,
     * invoking the corresponding handler method in {@link JSONRPCHandler}.
     *
     * <p><b>Supported Request Types:</b>
     * <ul>
     *   <li>{@link GetTaskRequest} → {@link JSONRPCHandler#onGetTask}</li>
     *   <li>{@link CancelTaskRequest} → {@link JSONRPCHandler#onCancelTask}</li>
     *   <li>{@link ListTasksRequest} → {@link JSONRPCHandler#onListTasks}</li>
     *   <li>{@link SendMessageRequest} → {@link JSONRPCHandler#onMessageSend}</li>
     *   <li>{@link CreateTaskPushNotificationConfigRequest} → {@link JSONRPCHandler#setPushNotificationConfig}</li>
     *   <li>{@link GetTaskPushNotificationConfigRequest} → {@link JSONRPCHandler#getPushNotificationConfig}</li>
     *   <li>{@link ListTaskPushNotificationConfigsRequest} → {@link JSONRPCHandler#listPushNotificationConfigs}</li>
     *   <li>{@link DeleteTaskPushNotificationConfigRequest} → {@link JSONRPCHandler#deletePushNotificationConfig}</li>
     *   <li>{@link GetExtendedAgentCardRequest} → {@link JSONRPCHandler#onGetExtendedCardRequest}</li>
     * </ul>
     *
     * @param request the non-streaming JSON-RPC request
     * @param context the server call context
     * @return the JSON-RPC response
     */
    private A2AResponse<?> processNonStreamingRequest(NonStreamingJSONRPCRequest<?> request, ServerCallContext context) {
        if (request instanceof GetTaskRequest req) {
            return jsonRpcHandler.onGetTask(req, context);
        }
        if (request instanceof CancelTaskRequest req) {
            return jsonRpcHandler.onCancelTask(req, context);
        }
        if (request instanceof ListTasksRequest req) {
            return jsonRpcHandler.onListTasks(req, context);
        }
        if (request instanceof CreateTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.setPushNotificationConfig(req, context);
        }
        if (request instanceof GetTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.getPushNotificationConfig(req, context);
        }
        if (request instanceof SendMessageRequest req) {
            return jsonRpcHandler.onMessageSend(req, context);
        }
        if (request instanceof ListTaskPushNotificationConfigsRequest req) {
            return jsonRpcHandler.listPushNotificationConfigs(req, context);
        }
        if (request instanceof DeleteTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.deletePushNotificationConfig(req, context);
        }
        if (request instanceof GetExtendedAgentCardRequest req) {
            return jsonRpcHandler.onGetExtendedCardRequest(req, context);
        }
        return generateErrorResponse(request, new UnsupportedOperationError());
    }

    /**
     * Routes streaming JSON-RPC requests to the appropriate handler method.
     *
     * <p>This method dispatches streaming requests to handlers that return
     * {@link Flow.Publisher} of responses, which are then converted to SSE streams.
     *
     * <p><b>Supported Request Types:</b>
     * <ul>
     *   <li>{@link SendStreamingMessageRequest} → {@link JSONRPCHandler#onMessageSendStream}</li>
     *   <li>{@link SubscribeToTaskRequest} → {@link JSONRPCHandler#onSubscribeToTask}</li>
     * </ul>
     *
     * @param request the streaming JSON-RPC request
     * @param context the server call context
     * @return a Multi stream of JSON-RPC responses
     */
    private Multi<? extends A2AResponse<?>> processStreamingRequest(
            A2ARequest<?> request, ServerCallContext context) throws A2AError {
        if (request instanceof SendStreamingMessageRequest req) {
            jsonRpcHandler.validateRequestedTask(req.getParams().message().taskId());
        } else if (request instanceof SubscribeToTaskRequest req) {
            jsonRpcHandler.validateRequestedTask(req.getParams().id());
        }
        try {
            Flow.Publisher<? extends A2AResponse<?>> publisher;
            if (request instanceof SendStreamingMessageRequest req) {
                publisher = jsonRpcHandler.onMessageSendStream(req, context);
            } else if (request instanceof SubscribeToTaskRequest req) {
                publisher = jsonRpcHandler.onSubscribeToTask(req, context);
            } else {
                return Multi.createFrom().item(generateErrorResponse(request, new UnsupportedOperationError()));
            }
            return Multi.createFrom().publisher(publisher);
        } catch (A2AError error) {
            return Multi.createFrom().item(generateErrorResponse(request, error));
        }
    }

    /**
     * Generates a JSON-RPC error response for the given request and error.
     *
     * @param request the original request
     * @param error the A2A error to include in the response
     * @return a JSON-RPC error response
     */
    private A2AResponse<?> generateErrorResponse(A2ARequest<?> request, A2AError error) {
        return new A2AErrorResponse(request.getId(), error);
    }

    /**
     * Sets a callback to be invoked when SSE streaming subscription starts.
     *
     * <p>This is a testing hook used to synchronize test execution with streaming setup.
     * In production, this remains null.
     *
     * @param runnable the callback to invoke on subscription
     */
    public static void setStreamingMultiSseSupportSubscribedRunnable(Runnable runnable) {
        streamingMultiSseSupportSubscribedRunnable = runnable;
    }

    /**
     * Creates a {@link ServerCallContext} from the Vert.x routing context.
     *
     * <p>This method extracts authentication, headers, tenant, and protocol information
     * from the HTTP request and packages them into a context object for use by the
     * request handler and agent executor.
     *
     * <p><b>Default Context Creation:</b>
     * <p>If no {@link CallContextFactory} CDI bean is provided, creates a context with:
     * <ul>
     *   <li>User authentication from Quarkus Security</li>
     *   <li>HTTP headers map</li>
     *   <li>Tenant ID from request path</li>
     *   <li>Transport protocol ({@link TransportProtocol#JSONRPC})</li>
     *   <li>A2A protocol version from {@code X-A2A-Version} header</li>
     *   <li>Required extensions from {@code X-A2A-Extensions} header</li>
     * </ul>
     *
     * <p><b>Custom Context Creation:</b>
     * <p>If a {@link CallContextFactory} bean is present, delegates to
     * {@link CallContextFactory#build(RoutingContext)} for custom context creation.
     *
     * @param rc the Vert.x routing context
     * @return the server call context
     * @see CallContextFactory
     */
    private ServerCallContext createCallContext(RoutingContext rc) {
        if (callContextFactory.isUnsatisfied()) {
            User user;
            if (rc.user() == null) {
                user = UnauthenticatedUser.INSTANCE;
            } else {
                String subject = rc.user().subject();
                user = subject != null ? new AuthenticatedUser(subject) : UnauthenticatedUser.INSTANCE;
            }
            Map<String, Object> state = new HashMap<>();
            // TODO Python's impl has
            //    state['auth'] = request.auth
            //  in jsonrpc_app.py. Figure out what this maps to in what Vert.X gives us

            Map<String, String> headers = new HashMap<>();
            Set<String> headerNames = rc.request().headers().names();
            headerNames.forEach(name -> headers.put(name, rc.request().getHeader(name)));
            state.put(HEADERS_KEY, headers);
            state.put(TENANT_KEY, extractTenant(rc));
            state.put(TRANSPORT_KEY, TransportProtocol.JSONRPC);

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

    /**
     * Extracts the tenant identifier from the request path.
     *
     * <p>The tenant is determined by the normalized path, with leading and trailing
     * slashes stripped:
     * <ul>
     *   <li>{@code /} → empty tenant</li>
     *   <li>{@code /tenant1} → "tenant1"</li>
     *   <li>{@code /tenant1/} → "tenant1"</li>
     *   <li>{@code /org/team} → "org/team"</li>
     * </ul>
     *
     * @param rc the routing context
     * @return the tenant identifier, or empty string if no tenant in path
     */
    private String extractTenant(RoutingContext rc) {
        String tenantPath = rc.normalizedPath();
        if (tenantPath == null || tenantPath.isBlank()) {
            return "";
        }
        if (tenantPath.startsWith("/")) {
            tenantPath = tenantPath.substring(1);
        }
        if(tenantPath.endsWith("/")) {
            tenantPath = tenantPath.substring(0, tenantPath.length() -1);
        }
        return tenantPath;
    }

    /**
     * Serializes a JSON-RPC response to a JSON string.
     *
     * <p>This method handles both success and error responses, converting domain objects
     * to protobuf messages before JSON serialization for consistency with the gRPC transport.
     *
     * <p><b>Success Response Format:</b>
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": "req-123",
     *   "result": { ... }
     * }
     * }</pre>
     *
     * <p><b>Error Response Format:</b>
     * <pre>{@code
     * {
     *   "jsonrpc": "2.0",
     *   "id": "req-123",
     *   "error": {
     *     "code": -32602,
     *     "message": "Invalid params",
     *     "data": [ ... ]
     *   }
     * }
     * }</pre>
     *
     * @param response the response to serialize
     * @return the JSON string
     * @see JSONRPCUtils#toJsonRPCResultResponse
     * @see JSONRPCUtils#toJsonRPCErrorResponse
     */
    private static String serializeResponse(A2AResponse<?> response) {
        // For error responses, use Jackson serialization (errors are standardized)
        if (response instanceof A2AErrorResponse error) {
            return JSONRPCUtils.toJsonRPCErrorResponse(error.getId(), error.getError());
        }
        if (response.getError() != null) {
            return JSONRPCUtils.toJsonRPCErrorResponse(response.getId(), response.getError());
        }
        // Convert domain response to protobuf message and serialize
        com.google.protobuf.MessageOrBuilder protoMessage = convertToProto(response);
        return JSONRPCUtils.toJsonRPCResultResponse(response.getId(), protoMessage);
    }

    /**
     * Converts a domain response object to its protobuf representation.
     *
     * <p>This method maps response types to their corresponding protobuf messages
     * using {@link org.a2aproject.sdk.grpc.utils.ProtoUtils}, ensuring consistent serialization
     * across all transports (JSON-RPC, gRPC, REST).
     *
     * <p><b>Supported Response Types:</b>
     * <ul>
     *   <li>{@link GetTaskResponse} → Task protobuf message</li>
     *   <li>{@link CancelTaskResponse} → Task protobuf message</li>
     *   <li>{@link SendMessageResponse} → TaskOrMessage protobuf message</li>
     *   <li>{@link ListTasksResponse} → ListTasksResult protobuf message</li>
     *   <li>{@link CreateTaskPushNotificationConfigResponse} → CreateTaskPushNotificationConfigResponse protobuf message</li>
     *   <li>{@link GetTaskPushNotificationConfigResponse} → GetTaskPushNotificationConfigResponse protobuf message</li>
     *   <li>{@link ListTaskPushNotificationConfigsResponse} → ListTaskPushNotificationConfigsResponse protobuf message</li>
     *   <li>{@link DeleteTaskPushNotificationConfigResponse} → Empty protobuf message</li>
     *   <li>{@link GetExtendedAgentCardResponse} → GetExtendedCardResponse protobuf message</li>
     *   <li>{@link SendStreamingMessageResponse} → TaskOrMessageStream protobuf message</li>
     * </ul>
     *
     * @param response the domain response object
     * @return the protobuf message representation
     * @throws IllegalArgumentException if the response type is unknown
     * @see org.a2aproject.sdk.grpc.utils.ProtoUtils
     */
    private static com.google.protobuf.MessageOrBuilder convertToProto(A2AResponse<?> response) {
        if (response instanceof GetTaskResponse r) {
            return org.a2aproject.sdk.grpc.utils.ProtoUtils.ToProto.task(r.getResult());
        } else if (response instanceof CancelTaskResponse r) {
            return org.a2aproject.sdk.grpc.utils.ProtoUtils.ToProto.task(r.getResult());
        } else if (response instanceof SendMessageResponse r) {
            return org.a2aproject.sdk.grpc.utils.ProtoUtils.ToProto.taskOrMessage(r.getResult());
        } else if (response instanceof ListTasksResponse r) {
            return org.a2aproject.sdk.grpc.utils.ProtoUtils.ToProto.listTasksResult(r.getResult());
        } else if (response instanceof CreateTaskPushNotificationConfigResponse r) {
            return org.a2aproject.sdk.grpc.utils.ProtoUtils.ToProto.createTaskPushNotificationConfigResponse(r.getResult());
        } else if (response instanceof GetTaskPushNotificationConfigResponse r) {
            return org.a2aproject.sdk.grpc.utils.ProtoUtils.ToProto.getTaskPushNotificationConfigResponse(r.getResult());
        } else if (response instanceof ListTaskPushNotificationConfigsResponse r) {
            return org.a2aproject.sdk.grpc.utils.ProtoUtils.ToProto.listTaskPushNotificationConfigsResponse(r.getResult());
        } else if (response instanceof DeleteTaskPushNotificationConfigResponse) {
            // DeleteTaskPushNotificationConfig has no result body, just return empty message
            return com.google.protobuf.Empty.getDefaultInstance();
        } else if (response instanceof GetExtendedAgentCardResponse r) {
            return org.a2aproject.sdk.grpc.utils.ProtoUtils.ToProto.getExtendedCardResponse(r.getResult());
        } else if (response instanceof SendStreamingMessageResponse r) {
            return org.a2aproject.sdk.grpc.utils.ProtoUtils.ToProto.taskOrMessageStream(r.getResult());
        } else {
            throw new IllegalArgumentException("Unknown response type: " + response.getClass().getName());
        }
    }

}
