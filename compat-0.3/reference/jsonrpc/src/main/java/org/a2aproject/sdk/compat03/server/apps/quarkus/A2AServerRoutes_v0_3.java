package org.a2aproject.sdk.compat03.server.apps.quarkus;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.a2aproject.sdk.compat03.transport.jsonrpc.context.JSONRPCContextKeys_v0_3.HEADERS_KEY;
import static org.a2aproject.sdk.compat03.transport.jsonrpc.context.JSONRPCContextKeys_v0_3.METHOD_NAME_KEY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.smallrye.mutiny.Multi;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.a2aproject.sdk.compat03.common.A2AHeaders_v0_3;
import org.a2aproject.sdk.compat03.conversion.A2AProtocol_v0_3;
import org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3;
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
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.AuthenticatedUser;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.auth.User;
import org.a2aproject.sdk.server.common.quarkus.SseResponseWriter;
import org.a2aproject.sdk.server.common.quarkus.VertxSecurityHelper;
import org.a2aproject.sdk.server.extensions.A2AExtensions;
import org.a2aproject.sdk.spec.AgentCard;
import org.jspecify.annotations.Nullable;

@Singleton
public class A2AServerRoutes_v0_3 {

    @Inject
    JSONRPCHandler_v0_3 jsonRpcHandler;

    // Hook so testing can wait until the SSE subscriber is attached.
    // Without this we get intermittent failures
    private static volatile @Nullable Runnable streamingMultiSseSupportSubscribedRunnable;

    @Inject
    Instance<CallContextFactory_v0_3> callContextFactory;

    @Inject
    VertxSecurityHelper vertxSecurityHelper;

    void setupRoutes(@Observes Router router) {
        // Main JSON-RPC endpoint: POST /
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

        // Only register v0.3 agent card if no real v1.0 agent card producer exists.
        // DefaultProducers provides a @DefaultBean AgentCard fallback that is always
        // present, so we must exclude it and only check for non-default beans.
        if (!hasNonDefaultV10AgentCard()) {
            router.get("/.well-known/agent-card.json")
                .produces(APPLICATION_JSON)
                .handler(ctx -> {
                    try {
                        String agentCard = JsonUtil_v0_3.toJson(jsonRpcHandler.getAgentCard());
                        ctx.response()
                            .setStatusCode(200)
                            .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                            .end(agentCard);
                    } catch (JsonProcessingException_v0_3 e) {
                        ctx.response().setStatusCode(500).end("Internal Server Error");
                    }
                });
        }
    }

    private static boolean hasNonDefaultV10AgentCard() {
        for (io.quarkus.arc.InstanceHandle<AgentCard> handle :
                io.quarkus.arc.Arc.container()
                        .select(AgentCard.class, PublicAgentCard.Literal.INSTANCE)
                        .handles()) {
            if (!handle.getBean().isDefaultBean()) {
                return true;
            }
        }
        return false;
    }

    @Authenticated
    public void invokeJSONRPCHandler(String body, RoutingContext rc) {
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

            // Extract id field early so error responses can include it
            com.google.gson.JsonElement idElement = node.get("id");
            if (idElement != null && !idElement.isJsonNull() && !idElement.isJsonPrimitive()) {
                throw new InvalidRequestError_v0_3("Invalid JSON-RPC request: 'id' must be a string, number, or null");
            }
            if (idElement != null && !idElement.isJsonNull() && idElement.isJsonPrimitive()) {
                com.google.gson.JsonPrimitive idPrimitive = idElement.getAsJsonPrimitive();
                requestId = idPrimitive.isNumber() ? idPrimitive.getAsLong() : idPrimitive.getAsString();
            }

            // Validate jsonrpc field
            com.google.gson.JsonElement jsonrpcElement = node.get("jsonrpc");
            if (jsonrpcElement == null || !jsonrpcElement.isJsonPrimitive()
                    || !JSONRPCMessage_v0_3.JSONRPC_VERSION.equals(jsonrpcElement.getAsString())) {
                throw new InvalidRequestError_v0_3("Invalid JSON-RPC request: missing or invalid 'jsonrpc' field");
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
                AtomicLong eventIdCounter = new AtomicLong(0);
                Multi<String> sseEvents = streamingResponse
                        .map(response -> formatSseEvent(response, eventIdCounter.getAndIncrement()));
                SseResponseWriter.writeSseStrings(sseEvents, rc, context, streamingMultiSseSupportSubscribedRunnable);
            } else {
                rc.response()
                        .setStatusCode(200)
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(Utils_v0_3.toJsonString(nonStreamingResponse));
            }
        }
    }

    private static String formatSseEvent(Object data, long id) {
        return "data: " + Utils_v0_3.toJsonString(data) + "\nid: " + id + "\n\n";
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

    public static void setStreamingMultiSseSupportSubscribedRunnable(Runnable runnable) {
        streamingMultiSseSupportSubscribedRunnable = runnable;
    }

    private ServerCallContext createCallContext(RoutingContext rc) {

        if (callContextFactory.isUnsatisfied()) {
            User user;
            if (rc.user() == null) {
                user = UnauthenticatedUser.INSTANCE;
            } else {
                String subject = rc.user().subject();
                user = new AuthenticatedUser(subject != null ? subject : "");
            }
            Map<String, Object> state = new HashMap<>();

            Map<String, String> headers = new HashMap<>();
            Set<String> headerNames = rc.request().headers().names();
            headerNames.forEach(name -> headers.put(name, rc.request().getHeader(name)));
            state.put(HEADERS_KEY, headers);

            // Extract requested extensions from X-A2A-Extensions header
            List<String> extensionHeaderValues = rc.request().headers().getAll(A2AHeaders_v0_3.X_A2A_EXTENSIONS);
            Set<String> requestedExtensions = A2AExtensions.getRequestedExtensions(extensionHeaderValues);

            return new ServerCallContext(user, state, requestedExtensions, A2AProtocol_v0_3.PROTOCOL_VERSION);
        } else {
            CallContextFactory_v0_3 builder = callContextFactory.get();
            return builder.build(rc);
        }
    }

}
