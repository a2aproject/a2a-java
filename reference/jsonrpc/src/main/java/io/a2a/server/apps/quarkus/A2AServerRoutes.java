package io.a2a.server.apps.quarkus;

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
     * /**
     * Handles incoming GET requests to the agent card endpoint.
     * Returns the agent card in JSON format.
     *
     * @return the agent card
     */
    @Route(path = "/.well-known/agent-card.json", methods = Route.HttpMethod.GET, produces = APPLICATION_JSON)
    public String getAgentCard() throws JsonProcessingException {
        return JsonUtil.toJson(jsonRpcHandler.getAgentCard());
    }

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

    private A2AResponse<?> generateErrorResponse(A2ARequest<?> request, A2AError error) {
        return new A2AErrorResponse(request.getId(), error);
    }

    static void setStreamingMultiSseSupportSubscribedRunnable(Runnable runnable) {
        streamingMultiSseSupportSubscribedRunnable = runnable;
    }

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
     * Simplified SSE support for Vert.x/Quarkus.
     * <p>
     * This class only handles HTTP-specific concerns (writing to response, backpressure, disconnect).
     * SSE formatting and JSON serialization are handled by {@link SseFormatter}.
     */
    private static class MultiSseSupport {
        private static final Logger logger = LoggerFactory.getLogger(MultiSseSupport.class);

        private MultiSseSupport() {
            // Avoid direct instantiation.
        }

        /**
         * Write SSE-formatted strings to HTTP response.
         *
         * @param sseStrings Multi stream of SSE-formatted strings (from SseFormatter)
         * @param rc         Vert.x routing context
         * @param context    A2A server call context (for EventConsumer cancellation)
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
