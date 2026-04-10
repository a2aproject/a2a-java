package org.a2aproject.sdk.compat03.server.rest.quarkus;

import static org.a2aproject.sdk.compat03.transport.rest.context.RestContextKeys_v0_3.HEADERS_KEY;
import static org.a2aproject.sdk.compat03.transport.rest.context.RestContextKeys_v0_3.METHOD_NAME_KEY;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.SERVER_SENT_EVENTS;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.a2aproject.sdk.compat03.common.A2AHeaders_v0_3;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.auth.User;
import org.a2aproject.sdk.server.util.async.Internal;
import org.a2aproject.sdk.server.extensions.A2AExtensions;
import org.a2aproject.sdk.compat03.spec.InternalError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.MethodNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.CancelTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskResubscriptionRequest_v0_3;
import org.a2aproject.sdk.compat03.transport.rest.handler.RestHandler_v0_3;
import org.a2aproject.sdk.compat03.transport.rest.handler.RestHandler_v0_3.HTTPRestResponse;
import org.a2aproject.sdk.compat03.transport.rest.handler.RestHandler_v0_3.HTTPRestStreamingResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

@Singleton
@Authenticated
public class A2AServerRoutes_v0_3 {

    @Inject
    RestHandler_v0_3 jsonRestHandler;

    // Hook so testing can wait until the MultiSseSupport is subscribed.
    // Without this we get intermittent failures
    private static volatile @Nullable Runnable streamingMultiSseSupportSubscribedRunnable;

    @Inject
    @Internal
    Executor executor;

    @Inject
    Instance<CallContextFactory_v0_3> callContextFactory;

    @Route(regex = "^/v1/message:send$", order = 1, methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void sendMessage(@Body String body, RoutingContext rc) {
        ServerCallContext context = createCallContext(rc, SendMessageRequest_v0_3.METHOD);
        HTTPRestResponse response = null;
        try {
            response = jsonRestHandler.sendMessage(body, context);
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError_v0_3(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    @Route(regex = "^/v1/message:stream$", order = 1, methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void sendMessageStreaming(@Body String body, RoutingContext rc) {
        ServerCallContext context = createCallContext(rc, SendStreamingMessageRequest_v0_3.METHOD);
        HTTPRestStreamingResponse streamingResponse = null;
        HTTPRestResponse error = null;
        try {
            HTTPRestResponse response = jsonRestHandler.sendStreamingMessage(body, context);
            if (response instanceof HTTPRestStreamingResponse hTTPRestStreamingResponse) {
                streamingResponse = hTTPRestStreamingResponse;
            } else {
                error = response;
            }
        } finally {
            if (error != null) {
                sendResponse(rc, error);
            } else if (streamingResponse != null) {
                Multi<String> events = Multi.createFrom().publisher(streamingResponse.getPublisher());
                executor.execute(() -> {
                    MultiSseSupport.subscribeObject(
                            events.map(i -> (Object) i), rc);
                });
            }
        }
    }

    @Route(path = "/v1/tasks/:id", order = 1, methods = {Route.HttpMethod.GET}, type = Route.HandlerType.BLOCKING)
    public void getTask(RoutingContext rc) {
        String taskId = rc.pathParam("id");
        ServerCallContext context = createCallContext(rc, GetTaskRequest_v0_3.METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError_v0_3("bad task id"));
            } else {
                int historyLength = 0;
                boolean hasHistoryLength = rc.request().params().contains("history_length");
                boolean hasHistoryLengthCamel = rc.request().params().contains("historyLength");

                if (hasHistoryLength && hasHistoryLengthCamel) {
                    response = jsonRestHandler.createErrorResponse(
                        new InvalidParamsError_v0_3("Only one of 'history_length' or 'historyLength' may be specified"));
                } else if (hasHistoryLength) {
                    historyLength = Integer.parseInt(rc.request().params().get("history_length"));
                } else if (hasHistoryLengthCamel) {
                    historyLength = Integer.parseInt(rc.request().params().get("historyLength"));
                }

                if (response == null) {
                    response = jsonRestHandler.getTask(taskId, historyLength, context);
                }
            }
        } catch (NumberFormatException e) {
            response = jsonRestHandler.createErrorResponse(new InvalidParamsError_v0_3("bad history_length or historyLength"));
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError_v0_3(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    @Route(regex = "^/v1/tasks/([^/]+):cancel$", order = 1, methods = {Route.HttpMethod.POST}, type = Route.HandlerType.BLOCKING)
    public void cancelTask(RoutingContext rc) {
        String taskId = rc.pathParam("param0");
        ServerCallContext context = createCallContext(rc, CancelTaskRequest_v0_3.METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError_v0_3("bad task id"));
            } else {
                response = jsonRestHandler.cancelTask(taskId, context);
            }
        } catch (Throwable t) {
            if (t instanceof JSONRPCError_v0_3 error) {
                response = jsonRestHandler.createErrorResponse(error);
            } else {
                response = jsonRestHandler.createErrorResponse(new InternalError_v0_3(t.getMessage()));
            }
        } finally {
            sendResponse(rc, response);
        }
    }

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

    @Route(regex = "^/v1/tasks/([^/]+):subscribe$", order = 1, methods = {Route.HttpMethod.POST}, type = Route.HandlerType.BLOCKING)
    public void resubscribeTask(RoutingContext rc) {
        String taskId = rc.pathParam("param0");
        ServerCallContext context = createCallContext(rc, TaskResubscriptionRequest_v0_3.METHOD);
        HTTPRestStreamingResponse streamingResponse = null;
        HTTPRestResponse error = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                error = jsonRestHandler.createErrorResponse(new InvalidParamsError_v0_3("bad task id"));
            } else {
                HTTPRestResponse response = jsonRestHandler.resubscribeTask(taskId, context);
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
                Multi<String> events = Multi.createFrom().publisher(streamingResponse.getPublisher());
                executor.execute(() -> {
                    MultiSseSupport.subscribeObject(
                            events.map(i -> (Object) i), rc);
                });
            }
        }
    }

    @Route(path = "/v1/tasks/:id/pushNotificationConfigs", order = 1, methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void setTaskPushNotificationConfiguration(@Body String body, RoutingContext rc) {
        String taskId = rc.pathParam("id");
        ServerCallContext context = createCallContext(rc, SetTaskPushNotificationConfigRequest_v0_3.METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError_v0_3("bad task id"));
            } else {
                response = jsonRestHandler.setTaskPushNotificationConfiguration(taskId, body, context);
            }
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError_v0_3(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    @Route(path = "/v1/tasks/:id/pushNotificationConfigs/:configId", order = 1, methods = {Route.HttpMethod.GET}, type = Route.HandlerType.BLOCKING)
    public void getTaskPushNotificationConfiguration(RoutingContext rc) {
        String taskId = rc.pathParam("id");
        String configId = rc.pathParam("configId");
        ServerCallContext context = createCallContext(rc, GetTaskPushNotificationConfigRequest_v0_3.METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError_v0_3("bad task id"));
            } else {
                response = jsonRestHandler.getTaskPushNotificationConfiguration(taskId, configId, context);
            }
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError_v0_3(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    @Route(path = "/v1/tasks/:id/pushNotificationConfigs", order = 1, methods = {Route.HttpMethod.GET}, type = Route.HandlerType.BLOCKING)
    public void listTaskPushNotificationConfigurations(RoutingContext rc) {
        String taskId = rc.pathParam("id");
        ServerCallContext context = createCallContext(rc, ListTaskPushNotificationConfigRequest_v0_3.METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError_v0_3("bad task id"));
            } else {
                response = jsonRestHandler.listTaskPushNotificationConfigurations(taskId, context);
            }
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError_v0_3(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    @Route(path = "/v1/tasks/:id/pushNotificationConfigs/:configId", order = 1, methods = {Route.HttpMethod.DELETE}, type = Route.HandlerType.BLOCKING)
    public void deleteTaskPushNotificationConfiguration(RoutingContext rc) {
        String taskId = rc.pathParam("id");
        String configId = rc.pathParam("configId");
        ServerCallContext context = createCallContext(rc, DeleteTaskPushNotificationConfigRequest_v0_3.METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError_v0_3("bad task id"));
            } else if (configId == null || configId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError_v0_3("bad config id"));
            } else {
                response = jsonRestHandler.deleteTaskPushNotificationConfiguration(taskId, configId, context);
            }
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError_v0_3(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    /**
     * Handles incoming GET requests to the agent card endpoint.
     * Returns the agent card in JSON format.
     *
     * @param rc the routing context
     */
    @Route(path = "/.well-known/agent-card.json", order = 1, methods = Route.HttpMethod.GET, produces = APPLICATION_JSON)
    @PermitAll
    public void getAgentCard(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.getAgentCard();
        sendResponse(rc, response);
    }

    @Route(path = "/v1/card", order = 1, methods = Route.HttpMethod.GET, produces = APPLICATION_JSON)
    public void getAuthenticatedExtendedCard(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.getAuthenticatedExtendedCard();
        sendResponse(rc, response);
    }

    @Route(path = "^/v1/.*", order = 100, methods = {Route.HttpMethod.DELETE, Route.HttpMethod.GET, Route.HttpMethod.HEAD, Route.HttpMethod.OPTIONS, Route.HttpMethod.POST, Route.HttpMethod.PUT}, produces = APPLICATION_JSON)
    public void methodNotFoundMessage(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.createErrorResponse(new MethodNotFoundError_v0_3());
        sendResponse(rc, response);
    }

    static void setStreamingMultiSseSupportSubscribedRunnable(Runnable runnable) {
        streamingMultiSseSupportSubscribedRunnable = runnable;
    }

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
                        if (rc.user() != null) {
                            String subject = rc.user().subject();
                            return subject != null ? subject : "";
                        }
                        return "";
                    }
                };
            }
            Map<String, Object> state = new HashMap<>();

            Map<String, String> headers = new HashMap<>();
            Set<String> headerNames = rc.request().headers().names();
            headerNames.forEach(name -> headers.put(name, rc.request().getHeader(name)));
            state.put(HEADERS_KEY, headers);
            state.put(METHOD_NAME_KEY, jsonRpcMethodName);

            // Extract requested extensions from X-A2A-Extensions header (v0.3 header)
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

        private static void onWriteDone(Flow.@Nullable Subscription subscription, AsyncResult<Void> ar, RoutingContext rc) {
            if (ar.failed()) {
                rc.fail(ar.cause());
            } else if (subscription != null) {
                subscription.request(1);
            }
        }

        private static void write(Multi<Buffer> multi, RoutingContext rc) {
            HttpServerResponse response = rc.response();
            multi.subscribe().withSubscriber(new Flow.Subscriber<Buffer>() {
                Flow.@Nullable Subscription upstream;

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

        private static void subscribeObject(Multi<Object> multi, RoutingContext rc) {
            AtomicLong count = new AtomicLong();
            write(multi.map(new Function<Object, Buffer>() {
                @Override
                public Buffer apply(Object o) {
                    if (o instanceof ReactiveRoutes.ServerSentEvent) {
                        ReactiveRoutes.ServerSentEvent<?> ev = (ReactiveRoutes.ServerSentEvent<?>) o;
                        long id = ev.id() != -1 ? ev.id() : count.getAndIncrement();
                        String e = ev.event() == null ? "" : "event: " + ev.event() + "\n";
                        return Buffer.buffer(e + "data: " + ev.data() + "\nid: " + id + "\n\n");
                    } else {
                        return Buffer.buffer("data: " + o + "\nid: " + count.getAndIncrement() + "\n\n");
                    }
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
