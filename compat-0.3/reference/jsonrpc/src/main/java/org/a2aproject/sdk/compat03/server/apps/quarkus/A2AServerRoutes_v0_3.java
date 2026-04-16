package org.a2aproject.sdk.compat03.server.apps.quarkus;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.SERVER_SENT_EVENTS;
import static org.a2aproject.sdk.compat03.transport.jsonrpc.context.JSONRPCContextKeys_v0_3.HEADERS_KEY;
import static org.a2aproject.sdk.compat03.transport.jsonrpc.context.JSONRPCContextKeys_v0_3.METHOD_NAME_KEY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.quarkus.security.Authenticated;
import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.ReactiveRoutes;
import io.quarkus.vertx.web.Route;
import io.smallrye.mutiny.Multi;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.a2aproject.sdk.compat03.common.A2AHeaders_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.CancelTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetAuthenticatedExtendedCardRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.InternalError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONParseError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCErrorResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCMessage_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.MethodNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.NonStreamingJSONRPCRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingJSONRPCRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskResubscriptionRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.UnsupportedOperationError_v0_3;
import org.a2aproject.sdk.compat03.transport.jsonrpc.handler.JSONRPCHandler_v0_3;
import org.a2aproject.sdk.compat03.util.Utils_v0_3;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.auth.User;
import org.a2aproject.sdk.server.extensions.A2AExtensions;
import org.a2aproject.sdk.server.util.async.Internal;

@Singleton
public class A2AServerRoutes_v0_3 {

    @Inject
    JSONRPCHandler_v0_3 jsonRpcHandler;

    // Hook so testing can wait until the MultiSseSupport is subscribed.
    // Without this we get intermittent failures
    private static volatile Runnable streamingMultiSseSupportSubscribedRunnable;

    @Inject
    @Internal
    Executor executor;

    @Inject
    Instance<CallContextFactory_v0_3> callContextFactory;

    @Route(path = "/", methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    @Authenticated
    public void invokeJSONRPCHandler(@Body String body, RoutingContext rc) {
        boolean streaming = false;
        ServerCallContext context = createCallContext(rc);
        JSONRPCResponse_v0_3<?> nonStreamingResponse = null;
        Multi<? extends JSONRPCResponse_v0_3<?>> streamingResponse = null;
        JSONRPCErrorResponse_v0_3 error = null;
        Object requestId = null;
        try {
            com.google.gson.JsonObject node;
            try {
                node = JsonParser.parseString(body).getAsJsonObject();
            } catch (Exception e) {
                throw new JSONParseError_v0_3(e.getMessage());
            }

            // Validate jsonrpc field
            com.google.gson.JsonElement jsonrpcElement = node.get("jsonrpc");
            if (jsonrpcElement == null || !jsonrpcElement.isJsonPrimitive()
                    || !JSONRPCMessage_v0_3.JSONRPC_VERSION.equals(jsonrpcElement.getAsString())) {
                throw new InvalidRequestError_v0_3("Invalid JSON-RPC request: missing or invalid 'jsonrpc' field");
            }

            // Validate id field (must be string, number, or null — not an object or array)
            com.google.gson.JsonElement idElement = node.get("id");
            if (idElement != null && !idElement.isJsonNull() && !idElement.isJsonPrimitive()) {
                throw new InvalidRequestError_v0_3("Invalid JSON-RPC request: 'id' must be a string, number, or null");
            }
            if (idElement != null && !idElement.isJsonNull() && idElement.isJsonPrimitive()) {
                com.google.gson.JsonPrimitive idPrimitive = idElement.getAsJsonPrimitive();
                requestId = idPrimitive.isNumber() ? idPrimitive.getAsLong() : idPrimitive.getAsString();
            }

            // Validate method field
            com.google.gson.JsonElement methodElement = node.get("method");
            if (methodElement == null || !methodElement.isJsonPrimitive()) {
                throw new InvalidRequestError_v0_3("Invalid JSON-RPC request: missing or invalid 'method' field");
            }

            String methodName = methodElement.getAsString();
            context.getState().put(METHOD_NAME_KEY, methodName);

            streaming = SendStreamingMessageRequest_v0_3.METHOD.equals(methodName)
                    || TaskResubscriptionRequest_v0_3.METHOD.equals(methodName);

            if (streaming) {
                StreamingJSONRPCRequest_v0_3<?> request = deserializeStreamingRequest(body, methodName);
                streamingResponse = processStreamingRequest(request, context);
            } else {
                NonStreamingJSONRPCRequest_v0_3<?> request = deserializeNonStreamingRequest(body, methodName);
                nonStreamingResponse = processNonStreamingRequest(request, context);
            }
        } catch (JSONRPCError_v0_3 e) {
            error = new JSONRPCErrorResponse_v0_3(requestId, e);
        } catch (JsonSyntaxException e) {
            error = new JSONRPCErrorResponse_v0_3(requestId, new JSONParseError_v0_3(e.getMessage()));
        } catch (Throwable t) {
            error = new JSONRPCErrorResponse_v0_3(requestId, new InternalError_v0_3(t.getMessage()));
        } finally {
            if (error != null) {
                rc.response()
                        .setStatusCode(200)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(Utils_v0_3.toJsonString(error));
            } else if (streaming) {
                final Multi<? extends JSONRPCResponse_v0_3<?>> finalStreamingResponse = streamingResponse;
                executor.execute(() -> {
                    MultiSseSupport.subscribeObject(
                            finalStreamingResponse.map(i -> (Object) i), rc);
                });

            } else {
                rc.response()
                        .setStatusCode(200)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(Utils_v0_3.toJsonString(nonStreamingResponse));
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
    public AgentCard_v0_3 getAgentCard() {
        return jsonRpcHandler.getAgentCard();
    }

    private NonStreamingJSONRPCRequest_v0_3<?> deserializeNonStreamingRequest(String body, String methodName) {
        try {
            return switch (methodName) {
                case GetTaskRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, GetTaskRequest_v0_3.class);
                case CancelTaskRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, CancelTaskRequest_v0_3.class);
                case SendMessageRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, SendMessageRequest_v0_3.class);
                case SetTaskPushNotificationConfigRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, SetTaskPushNotificationConfigRequest_v0_3.class);
                case GetTaskPushNotificationConfigRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, GetTaskPushNotificationConfigRequest_v0_3.class);
                case ListTaskPushNotificationConfigRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, ListTaskPushNotificationConfigRequest_v0_3.class);
                case DeleteTaskPushNotificationConfigRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, DeleteTaskPushNotificationConfigRequest_v0_3.class);
                case GetAuthenticatedExtendedCardRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, GetAuthenticatedExtendedCardRequest_v0_3.class);
                default -> throw new MethodNotFoundError_v0_3();
            };
        } catch (JSONRPCError_v0_3 e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidParamsError_v0_3(e.getMessage());
        }
    }

    private StreamingJSONRPCRequest_v0_3<?> deserializeStreamingRequest(String body, String methodName) {
        try {
            return switch (methodName) {
                case SendStreamingMessageRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, SendStreamingMessageRequest_v0_3.class);
                case TaskResubscriptionRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, TaskResubscriptionRequest_v0_3.class);
                default -> throw new MethodNotFoundError_v0_3();
            };
        } catch (JSONRPCError_v0_3 e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidParamsError_v0_3(e.getMessage());
        }
    }

    private JSONRPCResponse_v0_3<?> processNonStreamingRequest(
            NonStreamingJSONRPCRequest_v0_3<?> request, ServerCallContext context) {
        if (request instanceof GetTaskRequest_v0_3 req) {
            return jsonRpcHandler.onGetTask(req, context);
        } else if (request instanceof CancelTaskRequest_v0_3 req) {
            return jsonRpcHandler.onCancelTask(req, context);
        } else if (request instanceof SetTaskPushNotificationConfigRequest_v0_3 req) {
            return jsonRpcHandler.setPushNotificationConfig(req, context);
        } else if (request instanceof GetTaskPushNotificationConfigRequest_v0_3 req) {
            return jsonRpcHandler.getPushNotificationConfig(req, context);
        } else if (request instanceof SendMessageRequest_v0_3 req) {
            return jsonRpcHandler.onMessageSend(req, context);
        } else if (request instanceof ListTaskPushNotificationConfigRequest_v0_3 req) {
            return jsonRpcHandler.listPushNotificationConfig(req, context);
        } else if (request instanceof DeleteTaskPushNotificationConfigRequest_v0_3 req) {
            return jsonRpcHandler.deletePushNotificationConfig(req, context);
        } else if (request instanceof GetAuthenticatedExtendedCardRequest_v0_3 req) {
            return jsonRpcHandler.onGetAuthenticatedExtendedCardRequest(req, context);
        } else {
            return generateErrorResponse(request, new UnsupportedOperationError_v0_3());
        }
    }

    private Multi<? extends JSONRPCResponse_v0_3<?>> processStreamingRequest(
            JSONRPCRequest_v0_3<?> request, ServerCallContext context) {
        Flow.Publisher<? extends JSONRPCResponse_v0_3<?>> publisher;
        if (request instanceof SendStreamingMessageRequest_v0_3 req) {
            publisher = jsonRpcHandler.onMessageSendStream(req, context);
        } else if (request instanceof TaskResubscriptionRequest_v0_3 req) {
            publisher = jsonRpcHandler.onResubscribeToTask(req, context);
        } else {
            return Multi.createFrom().item(generateErrorResponse(request, new UnsupportedOperationError_v0_3()));
        }
        return Multi.createFrom().publisher(publisher);
    }

    private JSONRPCResponse_v0_3<?> generateErrorResponse(JSONRPCRequest_v0_3<?> request, JSONRPCError_v0_3 error) {
        return new JSONRPCErrorResponse_v0_3(request.getId(), error);
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

            // Extract requested extensions from X-A2A-Extensions header
            List<String> extensionHeaderValues = rc.request().headers().getAll(A2AHeaders_v0_3.X_A2A_EXTENSIONS);
            Set<String> requestedExtensions = A2AExtensions.getRequestedExtensions(extensionHeaderValues);

            return new ServerCallContext(user, state, requestedExtensions);
        } else {
            CallContextFactory_v0_3 builder = callContextFactory.get();
            return builder.build(rc);
        }
    }

    // Port of import io.quarkus.vertx.web.runtime.MultiSseSupport, which is considered internal API
    private static class MultiSseSupport {

        private MultiSseSupport() {
            // Avoid direct instantiation.
        }

        private static void initialize(HttpServerResponse response) {
            if (response.bytesWritten() == 0) {
                MultiMap headers = response.headers();
                if (headers.get(CONTENT_TYPE) == null) {
                    headers.set(CONTENT_TYPE, SERVER_SENT_EVENTS);
                }
                response.setChunked(true);
            }
        }

        private static void onWriteDone(Flow.Subscription subscription, AsyncResult<Void> ar, RoutingContext rc) {
            if (ar.failed()) {
                rc.fail(ar.cause());
            } else {
                subscription.request(1);
            }
        }

        public static void write(Multi<Buffer> multi, RoutingContext rc) {
            HttpServerResponse response = rc.response();
            multi.subscribe().withSubscriber(new Flow.Subscriber<Buffer>() {
                Flow.Subscription upstream;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.upstream = subscription;
                    this.upstream.request(1);

                    // Notify tests that we are subscribed
                    Runnable runnable = streamingMultiSseSupportSubscribedRunnable;
                    if (runnable != null) {
                        runnable.run();
                    }
                }

                @Override
                public void onNext(Buffer item) {
                    initialize(response);
                    response.write(item, new Handler<AsyncResult<Void>>() {
                        @Override
                        public void handle(AsyncResult<Void> ar) {
                            onWriteDone(upstream, ar, rc);
                        }
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                    rc.fail(throwable);
                }

                @Override
                public void onComplete() {
                    endOfStream(response);
                }
            });
        }

        public static void subscribeObject(Multi<Object> multi, RoutingContext rc) {
            AtomicLong count = new AtomicLong();
            write(multi.map(new Function<Object, Buffer>() {
                @Override
                public Buffer apply(Object o) {
                    if (o instanceof ReactiveRoutes.ServerSentEvent) {
                        ReactiveRoutes.ServerSentEvent<?> ev = (ReactiveRoutes.ServerSentEvent<?>) o;
                        long id = ev.id() != -1 ? ev.id() : count.getAndIncrement();
                        String e = ev.event() == null ? "" : "event: " + ev.event() + "\n";
                        return Buffer.buffer(e + "data: " + Utils_v0_3.toJsonString(ev.data()) + "\nid: " + id + "\n\n");
                    }
                    return Buffer.buffer("data: " + Utils_v0_3.toJsonString(o) + "\nid: " + count.getAndIncrement() + "\n\n");
                }
            }), rc);
        }

        private static void endOfStream(HttpServerResponse response) {
            if (response.bytesWritten() == 0) { // No item
                MultiMap headers = response.headers();
                if (headers.get(CONTENT_TYPE) == null) {
                    headers.set(CONTENT_TYPE, SERVER_SENT_EVENTS);
                }
            }
            response.end();
        }
    }
}
