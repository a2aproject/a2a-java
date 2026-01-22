package io.a2a.server.rest.quarkus;

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
import java.util.function.Function;

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
import io.a2a.transport.rest.handler.RestHandler;
import io.a2a.transport.rest.handler.RestHandler.HTTPRestResponse;
import io.a2a.transport.rest.handler.RestHandler.HTTPRestStreamingResponse;
import io.a2a.util.Utils;
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
import org.jspecify.annotations.Nullable;

import static io.a2a.spec.A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.GET_TASK_METHOD;
import static io.a2a.spec.A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.LIST_TASK_METHOD;
import static io.a2a.spec.A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.SEND_MESSAGE_METHOD;
import static io.a2a.spec.A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.SUBSCRIBE_TO_TASK_METHOD;

@Singleton
@Authenticated
public class A2AServerRoutes {

    private static final String HISTORY_LENGTH_PARAM = "historyLength";
    private static final String PAGE_SIZE_PARAM = "pageSize";
    private static final String PAGE_TOKEN_PARAM = "pageToken";

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

    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)message:send$", order = 1, methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void sendMessage(@Body String body, RoutingContext rc) {
        ServerCallContext context = createCallContext(rc, SEND_MESSAGE_METHOD);
        HTTPRestResponse response = null;
        try {
            response = jsonRestHandler.sendMessage(body, extractTenant(rc), context);
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)message:stream$", order = 1, methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void sendMessageStreaming(@Body String body, RoutingContext rc) {
        ServerCallContext context = createCallContext(rc, SEND_STREAMING_MESSAGE_METHOD);
        HTTPRestStreamingResponse streamingResponse = null;
        HTTPRestResponse error = null;
        try {
            HTTPRestResponse response = jsonRestHandler.sendStreamingMessage(body, extractTenant(rc), context);
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
                            events.map(i -> (Object) i), rc, context);
                });
            }
        }
    }

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
            String lastUpdatedAfter = rc.request().params().get("lastUpdatedAfter");
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

            response = jsonRestHandler.listTasks(contextId, statusStr, pageSize, pageToken,
                    historyLength, lastUpdatedAfter, includeArtifacts, extractTenant(rc), context);
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
                response = jsonRestHandler.getTask(taskId, historyLength, extractTenant(rc), context);
            }
        } catch (NumberFormatException e) {
            response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad historyLength"));
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+):cancel$", order = 1, methods = {Route.HttpMethod.POST}, type = Route.HandlerType.BLOCKING)
    public void cancelTask(RoutingContext rc) {
        String taskId = rc.pathParam("taskId");
        ServerCallContext context = createCallContext(rc, CANCEL_TASK_METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad task id"));
            } else {
                response = jsonRestHandler.cancelTask(taskId, extractTenant(rc), context);
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
                HTTPRestResponse response = jsonRestHandler.subscribeToTask(taskId, extractTenant(rc), context);
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
                            events.map(i -> (Object) i), rc, context);
                });
            }
        }
    }

    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs$", order = 1, methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void setTaskPushNotificationConfiguration(@Body String body, RoutingContext rc) {
        String taskId = rc.pathParam("taskId");
        ServerCallContext context = createCallContext(rc, SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad task id"));
            } else {
                response = jsonRestHandler.setTaskPushNotificationConfiguration(taskId, body, extractTenant(rc), context);
            }
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs\\/(?<configId>[^\\/]+)", order = 2, methods = {Route.HttpMethod.GET}, type = Route.HandlerType.BLOCKING)
    public void getTaskPushNotificationConfiguration(RoutingContext rc) {
        String taskId = rc.pathParam("taskId");
        String configId = rc.pathParam("configId");
        ServerCallContext context = createCallContext(rc, GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad task id"));
            } else {
                response = jsonRestHandler.getTaskPushNotificationConfiguration(taskId, configId, extractTenant(rc), context);
            }
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs\\/$", order = 1, methods = {Route.HttpMethod.GET}, type = Route.HandlerType.BLOCKING)
    public void getTaskPushNotificationConfigurationWithoutId(RoutingContext rc) {
        String taskId = rc.pathParam("taskId");
        ServerCallContext context = createCallContext(rc, GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);
        HTTPRestResponse response = null;
        try {
            if (taskId == null || taskId.isEmpty()) {
                response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad task id"));
            } else {
                // Call get with null configId - trailing slash distinguishes this from list
                response = jsonRestHandler.getTaskPushNotificationConfiguration(taskId, null, extractTenant(rc), context);
            }
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs", order = 3, methods = {Route.HttpMethod.GET}, type = Route.HandlerType.BLOCKING)
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
                response = jsonRestHandler.listTaskPushNotificationConfigurations(taskId, pageSize, pageToken, extractTenant(rc), context);
            }
        } catch (NumberFormatException e) {
            response = jsonRestHandler.createErrorResponse(new InvalidParamsError("bad " + PAGE_SIZE_PARAM));
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

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
                response = jsonRestHandler.deleteTaskPushNotificationConfiguration(taskId, configId, extractTenant(rc), context);
            }
        } catch (Throwable t) {
            response = jsonRestHandler.createErrorResponse(new InternalError(t.getMessage()));
        } finally {
            sendResponse(rc, response);
        }
    }

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
     * /**
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

    @Route(regex = "^\\/(?<tenant>[^\\/]*\\/?)extendedAgentCard$", order = 1, methods = Route.HttpMethod.GET, produces = APPLICATION_JSON)
    public void getExtendedAgentCard(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.getExtendedAgentCard(extractTenant(rc));
        sendResponse(rc, response);
    }

    @Route(path = "^/.*", order = 100, methods = {Route.HttpMethod.DELETE, Route.HttpMethod.GET, Route.HttpMethod.HEAD, Route.HttpMethod.OPTIONS, Route.HttpMethod.POST, Route.HttpMethod.PUT}, produces = APPLICATION_JSON)
    public void methodNotFoundMessage(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.createErrorResponse(new MethodNotFoundError());
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

    // Port of import io.quarkus.vertx.web.runtime.MultiSseSupport, which is considered internal API
    private static class MultiSseSupport {
        private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MultiSseSupport.class);

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
                // Client disconnected or write failed - cancel upstream to stop EventConsumer
                subscription.cancel();
                rc.fail(ar.cause());
            } else {
                subscription.request(1);
            }
        }

        private static void write(Multi<Buffer> multi, RoutingContext rc, ServerCallContext context) {
            logger.info("REST MultiSseSupport.write() called - about to subscribe to Multi<Buffer>");
            HttpServerResponse response = rc.response();
            multi.subscribe().withSubscriber(new Flow.Subscriber<Buffer>() {
                Flow.@Nullable Subscription upstream;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    logger.info("REST MultiSseSupport.write.onSubscribe() called - subscribing to Multi<Buffer>");
                    this.upstream = subscription;
                    this.upstream.request(1);
                    logger.info("REST MultiSseSupport.write.onSubscribe() - requested first buffer");

                    // Detect client disconnect and call EventConsumer.cancel() directly
                    // This stops the polling loop without relying on subscription cancellation propagation
                    response.closeHandler(v -> {
                        logger.info("REST SSE connection closed by client, calling EventConsumer.cancel() to stop polling loop");
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
                public void onNext(Buffer item) {
                    logger.info("REST MultiSseSupport.write.onNext() called - buffer size: {} bytes", item.length());
                    initialize(response);
                    logger.info("REST MultiSseSupport.write.onNext() - calling response.write()");
                    response.write(item, new Handler<AsyncResult<Void>>() {
                        @Override
                        public void handle(AsyncResult<Void> ar) {
                            logger.info("REST MultiSseSupport.write.handle() - write {}", ar.succeeded() ? "succeeded" : "failed");
                            // NullAway: upstream is guaranteed non-null after onSubscribe
                            onWriteDone(java.util.Objects.requireNonNull(upstream), ar, rc);
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
                    endOfStream(response);
                }
            });
        }

        private static void subscribeObject(Multi<Object> multi, RoutingContext rc, ServerCallContext context) {
            logger.info("REST MultiSseSupport.subscribeObject called, creating map function");
            AtomicLong count = new AtomicLong();
            Multi<Buffer> bufferMulti = multi.map(new Function<Object, Buffer>() {
                @Override
                public Buffer apply(Object o) {
                    logger.info("REST MultiSseSupport.apply called with object: {}", o.getClass().getSimpleName());
                    if (o instanceof ReactiveRoutes.ServerSentEvent) {
                        ReactiveRoutes.ServerSentEvent<?> ev = (ReactiveRoutes.ServerSentEvent<?>) o;
                        long id = ev.id() != -1 ? ev.id() : count.getAndIncrement();
                        String e = ev.event() == null ? "" : "event: " + ev.event() + "\n";
                        String data = e + "data: " + ev.data() + "\nid: " + id + "\n\n";
                        logger.info("REST MultiSseSupport: Created SSE buffer ({}bytes): {}", data.length(), data.substring(0, Math.min(100, data.length())));
                        return Buffer.buffer(data);
                    } else {
                        String data = "data: " + o + "\nid: " + count.getAndIncrement() + "\n\n";
                        logger.info("REST MultiSseSupport: Created SSE buffer ({}bytes): {}", data.length(), data.substring(0, Math.min(100, data.length())));
                        return Buffer.buffer(data);
                    }
                }
            });
            logger.info("REST MultiSseSupport: Calling write() with Multi<Buffer>");
            write(bufferMulti, rc, context);
            logger.info("REST MultiSseSupport: write() call completed");
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
