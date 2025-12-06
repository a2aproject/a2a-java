package org.springframework.a2a.server.autoconfigure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.a2a.common.A2AHeaders;
import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.auth.User;
import io.a2a.server.extensions.A2AExtensions;
import io.a2a.spec.*;
import io.a2a.spec.InternalError;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;
import io.a2a.util.Utils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import static io.a2a.transport.jsonrpc.context.JSONRPCContextKeys.HEADERS_KEY;
import static io.a2a.transport.jsonrpc.context.JSONRPCContextKeys.METHOD_NAME_KEY;

@Component
//@ConditionalOnMissingBean
public class A2AServerHandler {

    private final JSONRPCHandler jsonRpcHandler;
    private final Executor executor;

    public A2AServerHandler(JSONRPCHandler jsonRpcHandler, Executor executor) {
        this.jsonRpcHandler = jsonRpcHandler;
        this.executor = executor;
    }

    public Mono<ServerResponse> invokeJSONRPCHandler(ServerRequest request) {
        return request.bodyToMono(String.class)
                .flatMap(body -> Mono.fromCallable(() -> {
                            JsonNode node = Utils.OBJECT_MAPPER.readTree(body);
                            JsonNode method = node != null ? node.get("method") : null;
                            boolean streaming = method != null && (SendStreamingMessageRequest.METHOD.equals(method.asText())
                                    || TaskResubscriptionRequest.METHOD.equals(method.asText()));
                            return new RequestInfo(streaming, node);
                        }).flatMap(requestInfo -> createCallContext(request).flatMap(context -> {
                            String methodName = (requestInfo.node() != null && requestInfo.node().has("method") && requestInfo.node().get("method").isTextual()) ? requestInfo.node().get("method").asText() : null;
                            if (methodName != null) {
                                context.getState().put(METHOD_NAME_KEY, methodName);
                            }

                            if (requestInfo.isStreaming()) {
                                try {
                                    StreamingJSONRPCRequest<?> streamingRequest = Utils.OBJECT_MAPPER.treeToValue(requestInfo.node(), StreamingJSONRPCRequest.class);
                                    Flux<? extends JSONRPCResponse<?>> streamingResponse = processStreamingRequest(streamingRequest, context);
                                    Flux<ServerSentEvent<Object>> sseFlux = streamingResponse.map(response -> ServerSentEvent.builder().data(response).build());
                                    return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(sseFlux.subscribeOn(Schedulers.fromExecutor(executor)), ServerSentEvent.class);
                                } catch (JsonProcessingException e) {
                                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(handleError(e));
                                }
                            } else {
                                try {
                                    NonStreamingJSONRPCRequest<?> nonStreamingRequest = Utils.OBJECT_MAPPER.treeToValue(requestInfo.node(), NonStreamingJSONRPCRequest.class);
                                    JSONRPCResponse<?> nonStreamingResponse = processNonStreamingRequest(nonStreamingRequest, context);
                                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(nonStreamingResponse);
                                } catch (JsonProcessingException e) {
                                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(handleError(e));
                                }
                            }
                        }))
                ).onErrorResume(Throwable.class, t -> {
                    JSONRPCErrorResponse error;
                    if (t instanceof JsonProcessingException) {
                        error = handleError((JsonProcessingException) t);
                    } else {
                        error = new JSONRPCErrorResponse(new InternalError(t.getMessage()));
                    }
                    return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(error);
                });
    }

    private JSONRPCErrorResponse handleError(JsonProcessingException exception) {
        Object id = null;
        JSONRPCError jsonRpcError;
        if (exception.getCause() instanceof com.fasterxml.jackson.core.JsonParseException) {
            jsonRpcError = new JSONParseError();
        } else if (exception instanceof com.fasterxml.jackson.core.io.JsonEOFException) {
            jsonRpcError = new JSONParseError(exception.getMessage());
        } else if (exception instanceof MethodNotFoundJsonMappingException err) {
            id = err.getId();
            jsonRpcError = new MethodNotFoundError();
        } else if (exception instanceof InvalidParamsJsonMappingException err) {
            id = err.getId();
            jsonRpcError = new InvalidParamsError();
        } else if (exception instanceof IdJsonMappingException err) {
            id = err.getId();
            jsonRpcError = new InvalidRequestError();
        } else {
            jsonRpcError = new InvalidRequestError();
        }
        return new JSONRPCErrorResponse(id, jsonRpcError);
    }

    public Mono<ServerResponse> getAgentCard(ServerRequest request) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(jsonRpcHandler.getAgentCard());
    }

    private JSONRPCResponse<?> processNonStreamingRequest(
            NonStreamingJSONRPCRequest<?> request, ServerCallContext context) {
        if (request instanceof GetTaskRequest req) {
            return jsonRpcHandler.onGetTask(req, context);
        } else if (request instanceof CancelTaskRequest req) {
            return jsonRpcHandler.onCancelTask(req, context);
        } else if (request instanceof SetTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.setPushNotificationConfig(req, context);
        } else if (request instanceof GetTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.getPushNotificationConfig(req, context);
        } else if (request instanceof SendMessageRequest req) {
            return jsonRpcHandler.onMessageSend(req, context);
        } else if (request instanceof ListTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.listPushNotificationConfig(req, context);
        } else if (request instanceof DeleteTaskPushNotificationConfigRequest req) {
            return jsonRpcHandler.deletePushNotificationConfig(req, context);
        } else if (request instanceof GetAuthenticatedExtendedCardRequest req) {
            return jsonRpcHandler.onGetAuthenticatedExtendedCardRequest(req, context);
        } else {
            return generateErrorResponse(request, new UnsupportedOperationError());
        }
    }

    private Flux<? extends JSONRPCResponse<?>> processStreamingRequest(
            JSONRPCRequest<?> request, ServerCallContext context) {
        if (request instanceof SendStreamingMessageRequest req) {
            return JdkFlowAdapter.flowPublisherToFlux(jsonRpcHandler.onMessageSendStream(req, context));
        } else if (request instanceof TaskResubscriptionRequest req) {
            return JdkFlowAdapter.flowPublisherToFlux(jsonRpcHandler.onResubscribeToTask(req, context));
        } else {
            return Flux.just(generateErrorResponse(request, new UnsupportedOperationError()));
        }
    }

    private JSONRPCResponse<?> generateErrorResponse(JSONRPCRequest<?> request, JSONRPCError error) {
        return new JSONRPCErrorResponse(request.getId(), error);
    }

    private Mono<ServerCallContext> createCallContext(ServerRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .map(auth -> {
                    User user;
                    if (auth == null || !auth.isAuthenticated()) {
                        user = UnauthenticatedUser.INSTANCE;
                    } else {
                        Object principal = auth.getPrincipal();
                        user = new User() {
                            @Override
                            public boolean isAuthenticated() {
                                return auth.isAuthenticated();
                            }

                            @Override
                            public String getUsername() {
                                if (principal instanceof UserDetails) {
                                    return ((UserDetails) principal).getUsername();
                                }
                                if (principal instanceof Principal) {
                                    return ((Principal) principal).getName();
                                }
                                return principal.toString();
                            }
                        };
                    }
                    return user;
                })
                .defaultIfEmpty(UnauthenticatedUser.INSTANCE)
                .map(user -> {
                    Map<String, Object> state = new HashMap<>();
                    Map<String, String> headers = new HashMap<>();
                    request.headers().asHttpHeaders().forEach((name, values) -> {
                        if (!values.isEmpty()) {
                            headers.put(name, values.get(0));
                        }
                    });
                    state.put(HEADERS_KEY, headers);

                    List<String> extensionHeaderValues = request.headers().header(A2AHeaders.X_A2A_EXTENSIONS);
                    Set<String> requestedExtensions = A2AExtensions.getRequestedExtensions(extensionHeaderValues);

                    return new ServerCallContext(user, state, requestedExtensions);
                });
    }

    private record RequestInfo(boolean isStreaming, JsonNode node) {}
}
