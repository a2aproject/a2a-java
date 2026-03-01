package io.a2a.server.apps.quarkus;

import static io.a2a.server.ServerCallContext.TRANSPORT_KEY;
import static io.a2a.transport.jsonrpc.context.JSONRPCContextKeys.HEADERS_KEY;
import static io.a2a.transport.jsonrpc.context.JSONRPCContextKeys.METHOD_NAME_KEY;
import static io.a2a.transport.jsonrpc.context.JSONRPCContextKeys.TENANT_KEY;
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

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.gson.JsonSyntaxException;
import io.a2a.common.A2AHeaders;
import io.a2a.server.util.sse.SseFormatter;
import io.a2a.grpc.utils.JSONRPCUtils;
import io.a2a.jsonrpc.common.json.IdJsonMappingException;
import io.a2a.jsonrpc.common.json.InvalidParamsJsonMappingException;
import io.a2a.jsonrpc.common.json.JsonMappingException;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.jsonrpc.common.json.MethodNotFoundJsonMappingException;
import io.a2a.jsonrpc.common.wrappers.A2AErrorResponse;
import io.a2a.jsonrpc.common.wrappers.A2ARequest;
import io.a2a.jsonrpc.common.wrappers.A2AResponse;
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
import io.a2a.jsonrpc.common.wrappers.NonStreamingJSONRPCRequest;
import io.a2a.jsonrpc.common.wrappers.SendMessageRequest;
import io.a2a.jsonrpc.common.wrappers.SendMessageResponse;
import io.a2a.jsonrpc.common.wrappers.SendStreamingMessageRequest;
import io.a2a.jsonrpc.common.wrappers.SendStreamingMessageResponse;
import io.a2a.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.SubscribeToTaskRequest;
import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.auth.User;
import io.a2a.server.extensions.A2AExtensions;
import io.a2a.server.util.async.Internal;
import io.a2a.spec.A2AError;
import io.a2a.spec.InternalError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.TransportProtocol;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *   <li>{@code listTaskPushNotificationConfig} - List push notification configs</li>
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
 *   <li>{@link InvalidParamsJsonMappingException} → {@link io.a2a.spec.InvalidParamsError}</li>
 *   <li>{@link MethodNotFoundJsonMappingException} → {@link io.a2a.spec.MethodNotFoundError}</li>
 *   <li>{@link IdJsonMappingException} → {@link io.a2a.spec.InvalidRequestError}</li>
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
 * @see MultiSseSupport
 */
@Singleton
public class A2AServerRoutes {

    @Inject
    JSONRPCHandler jsonRpcHandler;

    // Hook so testing can wait until the MultiSseSupport is subscribed.
    // Without this we get intermittent failures
    private static volatile Runnable streamingMultiSseSupportSubscribedRunnable;

    @Inject
    @Internal
    Executor executor;

    @Inject
    Instance<CallContextFactory> callContextFactory;

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
     *   <li>Parse JSON-RPC request body using {@link JSONRPCUtils#parseRequestBody}</li>
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
    @Route(path = "/", methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    @Authenticated
    public void invokeJSONRPCHandler(@Body String body, RoutingContext rc) {
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
            error = new A2AErrorResponse(e.getId(), new io.a2a.spec.InvalidParamsError(null, e.getMessage(), null));
        } catch (MethodNotFoundJsonMappingException e) {
            error = new A2AErrorResponse(e.getId(), new io.a2a.spec.MethodNotFoundError(null, e.getMessage(), null));
        } catch (IdJsonMappingException e) {
            error = new A2AErrorResponse(e.getId(), new io.a2a.spec.InvalidRequestError(null, e.getMessage(), null));
        } catch (JsonMappingException e) {
            // General JsonMappingException - treat as InvalidRequest
            error = new A2AErrorResponse(new io.a2a.spec.InvalidRequestError(null, e.getMessage(), null));
        } catch (JsonSyntaxException e) {
            error = new A2AErrorResponse(new JSONParseError(e.getMessage()));
        } catch (JsonProcessingException e) {
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
                final Multi<? extends A2AResponse<?>> finalStreamingResponse = streamingResponse;
                executor.execute(() -> {
                    // Convert Multi<A2AResponse> to Multi<String> with SSE formatting
                    AtomicLong eventIdCounter = new AtomicLong(0);
                    Multi<String> sseEvents = finalStreamingResponse
                            .map(response -> SseFormatter.formatResponseAsSSE(response, eventIdCounter.getAndIncrement()));
                    // Write SSE-formatted strings to HTTP response
                    MultiSseSupport.writeSseStrings(sseEvents, rc, context);
                });

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
     * <p><b>Request:</b>
     * <pre>{@code
     * GET /.well-known/agent-card.json
     * }</pre>
     *
     * <p><b>Response:</b>
     * <pre>{@code
     * HTTP/1.1 200 OK
     * Content-Type: application/json
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
     * @return the agent card as a JSON string
     * @throws JsonProcessingException if serialization fails
     * @see JSONRPCHandler#getAgentCard()
     */
    @Route(path = "/.well-known/agent-card.json", methods = Route.HttpMethod.GET, produces = APPLICATION_JSON)
    public String getAgentCard() throws JsonProcessingException {
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
     *   <li>{@link ListTaskPushNotificationConfigRequest} → {@link JSONRPCHandler#listPushNotificationConfig}</li>
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
        if (request instanceof ListTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.listPushNotificationConfig(req, context);
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
            A2ARequest<?> request, ServerCallContext context) {
        Flow.Publisher<? extends A2AResponse<?>> publisher;
        if (request instanceof SendStreamingMessageRequest req) {
            publisher = jsonRpcHandler.onMessageSendStream(req, context);
        } else if (request instanceof SubscribeToTaskRequest req) {
            publisher = jsonRpcHandler.onSubscribeToTask(req, context);
        } else {
            return Multi.createFrom().item(generateErrorResponse(request, new UnsupportedOperationError()));
        }
        return Multi.createFrom().publisher(publisher);
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
    static void setStreamingMultiSseSupportSubscribedRunnable(Runnable runnable) {
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
                user = new User() {
                    @Override
                    public boolean isAuthenticated() {
                        return rc.userContext().authenticated();
                    }

                    @Override
                    public String getUsername() {
                        return rc.user().subject();
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
            state.put(TENANT_KEY, extractTenant(rc));
            state.put(TRANSPORT_KEY, TransportProtocol.JSONRPC);

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
     *     "data": { ... }
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
     * using {@link io.a2a.grpc.utils.ProtoUtils}, ensuring consistent serialization
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
     *   <li>{@link ListTaskPushNotificationConfigResponse} → ListTaskPushNotificationConfigResponse protobuf message</li>
     *   <li>{@link DeleteTaskPushNotificationConfigResponse} → Empty protobuf message</li>
     *   <li>{@link GetExtendedAgentCardResponse} → GetExtendedCardResponse protobuf message</li>
     *   <li>{@link SendStreamingMessageResponse} → TaskOrMessageStream protobuf message</li>
     * </ul>
     *
     * @param response the domain response object
     * @return the protobuf message representation
     * @throws IllegalArgumentException if the response type is unknown
     * @see io.a2a.grpc.utils.ProtoUtils
     */
    private static com.google.protobuf.MessageOrBuilder convertToProto(A2AResponse<?> response) {
        if (response instanceof GetTaskResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.task(r.getResult());
        } else if (response instanceof CancelTaskResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.task(r.getResult());
        } else if (response instanceof SendMessageResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.taskOrMessage(r.getResult());
        } else if (response instanceof ListTasksResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.listTasksResult(r.getResult());
        } else if (response instanceof CreateTaskPushNotificationConfigResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.createTaskPushNotificationConfigResponse(r.getResult());
        } else if (response instanceof GetTaskPushNotificationConfigResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.getTaskPushNotificationConfigResponse(r.getResult());
        } else if (response instanceof ListTaskPushNotificationConfigResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.listTaskPushNotificationConfigResponse(r.getResult());
        } else if (response instanceof DeleteTaskPushNotificationConfigResponse) {
            // DeleteTaskPushNotificationConfig has no result body, just return empty message
            return com.google.protobuf.Empty.getDefaultInstance();
        } else if (response instanceof GetExtendedAgentCardResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.getExtendedCardResponse(r.getResult());
        } else if (response instanceof SendStreamingMessageResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.taskOrMessageStream(r.getResult());
        } else {
            throw new IllegalArgumentException("Unknown response type: " + response.getClass().getName());
        }
    }

    /**
     * Server-Sent Events (SSE) support for streaming JSON-RPC responses.
     *
     * <p>This class handles the HTTP-specific aspects of SSE streaming, including:
     * <ul>
     *   <li>Writing SSE-formatted events to the HTTP response</li>
     *   <li>Managing backpressure through reactive streams</li>
     *   <li>Detecting client disconnections and canceling upstream</li>
     *   <li>Setting appropriate HTTP headers for SSE</li>
     * </ul>
     *
     * <h2>SSE Format</h2>
     * <p>Events are written in Server-Sent Events format:
     * <pre>
     * id: 0
     * data: {"jsonrpc":"2.0","id":"req-123","result":{...}}
     *
     * id: 1
     * data: {"jsonrpc":"2.0","id":"req-123","result":{...}}
     * </pre>
     *
     * <h2>Backpressure Handling</h2>
     * <p>Uses reactive streams subscription to handle backpressure:
     * <ol>
     *   <li>Request 1 event from upstream</li>
     *   <li>Write event to HTTP response</li>
     *   <li>Wait for write completion</li>
     *   <li>Request next event (backpressure)</li>
     * </ol>
     *
     * <h2>Disconnect Handling</h2>
     * <p>When the client disconnects:
     * <ol>
     *   <li>Vert.x closeHandler fires</li>
     *   <li>Invokes {@link ServerCallContext#invokeEventConsumerCancelCallback()}</li>
     *   <li>Cancels upstream subscription</li>
     *   <li>Stops event polling</li>
     * </ol>
     *
     * @see SseFormatter
     */
    private static class MultiSseSupport {
        private static final Logger logger = LoggerFactory.getLogger(MultiSseSupport.class);

        private MultiSseSupport() {
            // Avoid direct instantiation.
        }

        /**
         * Writes SSE-formatted strings to the HTTP response with backpressure handling.
         *
         * <p>This method subscribes to the SSE event stream and writes each event to the
         * HTTP response, managing backpressure through the reactive streams subscription.
         *
         * <p><b>Subscription Flow:</b>
         * <ol>
         *   <li>Subscribe to SSE stream</li>
         *   <li>Register disconnect handler</li>
         *   <li>Request first event</li>
         *   <li>Write event to response</li>
         *   <li>Wait for write completion</li>
         *   <li>Request next event (backpressure)</li>
         * </ol>
         *
         * <p><b>HTTP Headers:</b>
         * <p>Sets {@code Content-Type: text/event-stream} on first write
         *
         * <p><b>Error Handling:</b>
         * <ul>
         *   <li>Client disconnect → cancel upstream, stop polling</li>
         *   <li>Write failure → cancel upstream, fail routing context</li>
         *   <li>Stream error → cancel upstream, fail routing context</li>
         * </ul>
         *
         * @param sseStrings Multi stream of SSE-formatted strings from {@link SseFormatter}
         * @param rc the Vert.x routing context
         * @param context the A2A server call context (for EventConsumer cancellation)
         * @see SseFormatter#formatResponseAsSSE
         */
        public static void writeSseStrings(Multi<String> sseStrings, RoutingContext rc, ServerCallContext context) {
            HttpServerResponse response = rc.response();

            sseStrings.subscribe().withSubscriber(new Flow.Subscriber<String>() {
                Flow.Subscription upstream;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.upstream = subscription;
                    this.upstream.request(1);

                    // Detect client disconnect and call EventConsumer.cancel() directly
                    response.closeHandler(v -> {
                        logger.info("SSE connection closed by client, calling EventConsumer.cancel() to stop polling loop");
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
                        response.setChunked(true);
                    }

                    // Write SSE-formatted string to response
                    response.write(Buffer.buffer(sseEvent), new Handler<AsyncResult<Void>>() {
                        @Override
                        public void handle(AsyncResult<Void> ar) {
                            if (ar.failed()) {
                                // Client disconnected or write failed - cancel upstream to stop EventConsumer
                                upstream.cancel();
                                rc.fail(ar.cause());
                            } else {
                                upstream.request(1);
                            }
                        }
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                    // Cancel upstream to stop EventConsumer when error occurs
                    upstream.cancel();
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
