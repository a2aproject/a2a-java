package io.a2a.server.rest.quarkus;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.auth.User;
import io.a2a.server.util.async.Internal;
import io.a2a.spec.InternalError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.transport.rest.handler.RestHandler;
import io.a2a.transport.rest.handler.RestHandler.HTTPRestResponse;
import io.a2a.transport.rest.handler.RestHandler.HTTPRestStreamingResponse;
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
import java.util.Map;
import java.util.Set;

@Singleton
public class A2AServerRoutes {

    @Inject
    RestHandler jsonRestHandler;

    // Hook so testing can wait until the MultiSseSupport is subscribed.
    // Without this we get intermittent failures
    private static volatile Runnable streamingMultiSseSupportSubscribedRunnable;

    @Inject
    @Internal
    Executor executor;

    @Inject
    Instance<CallContextFactory> callContextFactory;

    @Route(regex = "^/v1/message:send$", order = 1, methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void sendMessage(@Body String body, RoutingContext rc) {
        ServerCallContext context = createCallContext(rc);
        HTTPRestResponse response = null;
        try {
            response = jsonRestHandler.sendMessage(body, context);
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            rc.response()
                    .setStatusCode(response.getStatusCode())
                    .putHeader(CONTENT_TYPE, response.getContentType())
                    .end(response.getBody());
        }
    }

    @Route(regex = "^/v1/message:stream$", order = 1, methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void sendMessageStreaming(@Body String body, RoutingContext rc) {
        ServerCallContext context = createCallContext(rc);
        HTTPRestStreamingResponse streamingResponse = null;
        HTTPRestResponse error = null;
        try {
            HTTPRestResponse response = jsonRestHandler.sendStreamingMessage(body, context);
            if (response instanceof HTTPRestStreamingResponse) {
                streamingResponse = (HTTPRestStreamingResponse) response;
            } else {
                error = response;
            }
        } finally {
            if (error != null) {
                rc.response()
                        .setStatusCode(error.getStatusCode())
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(error.getBody());
            } else {
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
        ServerCallContext context = createCallContext(rc);
        HTTPRestResponse response = null;
        try {
            Integer historyLength = null;
            if (rc.request().params().contains("history_length")) {
                historyLength = Integer.valueOf(rc.request().params().get("history_length"));
            }
            response = jsonRestHandler.getTask(taskId, historyLength, context);
        } catch (NumberFormatException e) {
            response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad history_length"));
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            rc.response()
                    .setStatusCode(response.getStatusCode())
                    .putHeader(CONTENT_TYPE, response.getContentType())
                    .end(response.getBody());
        }
    }

    @Route(regex = "^/v1/tasks/([^/]+):cancel$", order = 1, methods = {Route.HttpMethod.POST}, type = Route.HandlerType.BLOCKING)
    public void cancelTask(RoutingContext rc) {
        String taskId = rc.pathParam("param0");
        ServerCallContext context = createCallContext(rc);
        HTTPRestResponse response = null;
        try {
            response = jsonRestHandler.cancelTask(taskId, context);
        } catch (Throwable t) {
            if (t instanceof JSONRPCError error) {
                response = jsonRestHandler.createErrorResponse(error);
            } else {
                response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
            }
        } finally {
            rc.response()
                    .setStatusCode(response.getStatusCode())
                    .putHeader(CONTENT_TYPE, response.getContentType())
                    .end(response.getBody());
        }
    }

    @Route(regex = "^/v1/tasks/([^/]+):subscribe$", order = 1, methods = {Route.HttpMethod.POST}, type = Route.HandlerType.BLOCKING)
    public void resubscribeTask(RoutingContext rc) {
        String taskId = rc.pathParam("param0");
        ServerCallContext context = createCallContext(rc);
        HTTPRestStreamingResponse streamingResponse = null;
        HTTPRestResponse error = null;
        try {
            HTTPRestResponse response = jsonRestHandler.resubscribeTask(taskId, context);
            if (response instanceof HTTPRestStreamingResponse) {
                streamingResponse = (HTTPRestStreamingResponse) response;
            } else {
                error = response;
            }
        } finally {
            if (error != null) {
                rc.response()
                        .setStatusCode(error.getStatusCode())
                        .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .end(error.getBody());
            } else {
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
        ServerCallContext context = createCallContext(rc);
        HTTPRestResponse response = null;
        try {
            response = jsonRestHandler.setTaskPushNotificationConfiguration(taskId, body, context);
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            rc.response()
                    .setStatusCode(response.getStatusCode())
                    .putHeader(CONTENT_TYPE, response.getContentType())
                    .end(response.getBody());
        }
    }

    @Route(path = "/v1/tasks/:id/pushNotificationConfigs/:configId", order = 1, methods = {Route.HttpMethod.GET}, type = Route.HandlerType.BLOCKING)
    public void getTaskPushNotificationConfiguration(RoutingContext rc) {
        String taskId = rc.pathParam("id");
        String configId = rc.pathParam("configId");
        ServerCallContext context = createCallContext(rc);
        HTTPRestResponse response = null;
        try {
            response = jsonRestHandler.getTaskPushNotificationConfiguration(taskId, configId, context);
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            rc.response()
                    .setStatusCode(response.getStatusCode())
                    .putHeader(CONTENT_TYPE, response.getContentType())
                    .end(response.getBody());
        }
    }

    @Route(path = "/v1/tasks/:id/pushNotificationConfigs", order = 1, methods = {Route.HttpMethod.GET}, type = Route.HandlerType.BLOCKING)
    public void listTaskPushNotificationConfigurations(RoutingContext rc) {
        String taskId = rc.pathParam("id");
        ServerCallContext context = createCallContext(rc);
        HTTPRestResponse response = null;
        try {
            response = jsonRestHandler.listTaskPushNotificationConfigurations(taskId, context);
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            rc.response()
                    .setStatusCode(response.getStatusCode())
                    .putHeader(CONTENT_TYPE, response.getContentType())
                    .end(response.getBody());
        }
    }

    @Route(path = "/v1/tasks/:id/pushNotificationConfigs/:configId", order = 1, methods = {Route.HttpMethod.DELETE}, type = Route.HandlerType.BLOCKING)
    public void deleteTaskPushNotificationConfiguration(RoutingContext rc) {
        String taskId = rc.pathParam("id");
        String configId = rc.pathParam("configId");
        ServerCallContext context = createCallContext(rc);
        HTTPRestResponse response = null;
        try {
            response = jsonRestHandler.deleteTaskPushNotificationConfiguration(taskId, configId, context);
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            rc.response()
                    .setStatusCode(response.getStatusCode())
                    .putHeader(CONTENT_TYPE, response.getContentType())
                    .end(response.getBody());
        }
    }

    /**
     * /**
     * Handles incoming GET requests to the agent card endpoint.
     * Returns the agent card in JSON format.
     *
     * @param rc
     */
    @Route(path = "/.well-known/agent-card.json", order = 1, methods = Route.HttpMethod.GET, produces = APPLICATION_JSON)
    public void getAgentCard(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.getAgentCard();
        rc.response()
                .setStatusCode(response.getStatusCode())
                .putHeader(CONTENT_TYPE, response.getContentType())
                .end(response.getBody());
    }

    @Route(path = "/v1/card", order = 1, methods = Route.HttpMethod.GET, produces = APPLICATION_JSON)
    public void getAuthenticatedExtendedCard(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.getAuthenticatedExtendedCard();
        rc.response()
                .setStatusCode(response.getStatusCode())
                .putHeader(CONTENT_TYPE, response.getContentType())
                .end(response.getBody());
    }

    @Route(path = "^/v1/.*", order = 100, methods = {Route.HttpMethod.DELETE, Route.HttpMethod.GET, Route.HttpMethod.HEAD, Route.HttpMethod.OPTIONS, Route.HttpMethod.POST, Route.HttpMethod.PUT}, produces = APPLICATION_JSON)
    public void methodNotFoundMessage(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.createErrorResponse(new MethodNotFoundError());
        rc.response()
                .setStatusCode(response.getStatusCode())
                .putHeader(CONTENT_TYPE, response.getContentType())
                .end(response.getBody());
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
            state.put("headers", headers);

            return new ServerCallContext(user, state);
        } else {
            CallContextFactory builder = callContextFactory.get();
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
                if (headers.get("content-type") == null) {
                    headers.set("content-type", "text/event-stream");
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
                if (headers.get("content-type") == null) {
                    headers.set("content-type", "text/event-stream");
                }
            }
            response.end();
        }
    }

}
