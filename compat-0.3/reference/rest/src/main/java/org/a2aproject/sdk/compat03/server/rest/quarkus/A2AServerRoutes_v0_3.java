package org.a2aproject.sdk.compat03.server.rest.quarkus;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.SERVER_SENT_EVENTS;
import static org.a2aproject.sdk.compat03.transport.rest.context.RestContextKeys_v0_3.HEADERS_KEY;
import static org.a2aproject.sdk.compat03.transport.rest.context.RestContextKeys_v0_3.METHOD_NAME_KEY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.security.Authenticated;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.smallrye.mutiny.Multi;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.a2aproject.sdk.compat03.common.A2AHeaders_v0_3;
import org.a2aproject.sdk.compat03.conversion.A2AProtocol_v0_3;
import org.a2aproject.sdk.compat03.spec.CancelTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.InternalError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskResubscriptionRequest_v0_3;
import org.a2aproject.sdk.compat03.transport.rest.handler.RestHandler_v0_3;
import org.a2aproject.sdk.compat03.transport.rest.handler.RestHandler_v0_3.HTTPRestResponse;
import org.a2aproject.sdk.compat03.transport.rest.handler.RestHandler_v0_3.HTTPRestStreamingResponse;
import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.auth.User;
import org.a2aproject.sdk.server.common.quarkus.VertxSecurityHelper;
import org.a2aproject.sdk.server.extensions.A2AExtensions;
import org.a2aproject.sdk.spec.AgentCard;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class A2AServerRoutes_v0_3 {

    @Inject
    RestHandler_v0_3 jsonRestHandler;

    // Hook so testing can wait until the MultiSseSupport is subscribed.
    // Without this we get intermittent failures
    private static volatile @Nullable Runnable streamingMultiSseSupportSubscribedRunnable;

    @Inject
    Instance<CallContextFactory_v0_3> callContextFactory;

    @Inject
    VertxSecurityHelper vertxSecurityHelper;

    void setupRouter(@Observes @Priority(10) Router router) {
        // POST /v1/message:send
        router.postWithRegex("^\\/v1\\/message:send$")
            .handler(BodyHandler.create())
            .blockingHandler(authenticated(ctx -> {
                sendMessage(extractBody(ctx), ctx);
            }));

        // POST /v1/message:stream
        router.postWithRegex("^\\/v1\\/message:stream$")
            .handler(BodyHandler.create())
            .blockingHandler(authenticated(ctx -> {
                sendMessageStreaming(extractBody(ctx), ctx);
            }));

        // GET /v1/tasks/:id
        router.get("/v1/tasks/:id")
            .order(1)
            .blockingHandler(authenticated(this::getTask));

        // POST /v1/tasks/{id}:cancel
        router.postWithRegex("^\\/v1\\/tasks\\/([^/]+):cancel$")
            .order(1)
            .blockingHandler(authenticated(this::cancelTask));

        // POST /v1/tasks/{id}:subscribe
        router.postWithRegex("^\\/v1\\/tasks\\/([^/]+):subscribe$")
            .order(1)
            .blockingHandler(authenticated(this::resubscribeTask));

        // POST /v1/tasks/:id/pushNotificationConfigs
        router.post("/v1/tasks/:id/pushNotificationConfigs")
            .order(1)
            .handler(BodyHandler.create())
            .blockingHandler(authenticated(ctx -> {
                setTaskPushNotificationConfiguration(extractBody(ctx), ctx);
            }));

        // GET /v1/tasks/:id/pushNotificationConfigs/:configId
        router.get("/v1/tasks/:id/pushNotificationConfigs/:configId")
            .order(1)
            .blockingHandler(authenticated(this::getTaskPushNotificationConfiguration));

        // GET /v1/tasks/:id/pushNotificationConfigs
        router.get("/v1/tasks/:id/pushNotificationConfigs")
            .order(2)
            .blockingHandler(authenticated(this::listTaskPushNotificationConfigurations));

        // DELETE /v1/tasks/:id/pushNotificationConfigs/:configId
        router.delete("/v1/tasks/:id/pushNotificationConfigs/:configId")
            .order(1)
            .blockingHandler(authenticated(this::deleteTaskPushNotificationConfiguration));

        // Only register v0.3 agent card if no real v1.0 agent card producer exists.
        // DefaultProducers provides a @DefaultBean AgentCard fallback that is always
        // present, so we must exclude it and only check for non-default beans.
        if (!hasNonDefaultV10AgentCard()) {
            router.get("/.well-known/agent-card.json")
                .order(1)
                .produces(APPLICATION_JSON)
                .handler(this::getAgentCard);
        }

        // GET /v1/card - authenticated
        router.get("/v1/card")
            .order(1)
            .produces(APPLICATION_JSON)
            .blockingHandler(authenticated(this::getAuthenticatedExtendedCard));
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

    @Authenticated
    public void sendMessage(String body, RoutingContext rc) {
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

    @Authenticated
    public void sendMessageStreaming(String body, RoutingContext rc) {
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
                AtomicLong eventIdCounter = new AtomicLong(0);
                Multi<String> sseEvents = Multi.createFrom().publisher(streamingResponse.getPublisher())
                        .map(json -> formatSseEvent(json, eventIdCounter.getAndIncrement()));
                MultiSseSupport.writeSseStrings(sseEvents, rc, context);
            }
        }
    }

    @Authenticated
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

    @Authenticated
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
            var httpResponse = rc.response()
                    .setStatusCode(response.getStatusCode())
                    .putHeader(CONTENT_TYPE, response.getContentType());

            response.getHeaders().forEach(httpResponse::putHeader);

            httpResponse.end(response.getBody());
        } else {
            rc.response().end();
        }
    }

    @Authenticated
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
                AtomicLong eventIdCounter = new AtomicLong(0);
                Multi<String> sseEvents = Multi.createFrom().publisher(streamingResponse.getPublisher())
                        .map(json -> formatSseEvent(json, eventIdCounter.getAndIncrement()));
                MultiSseSupport.writeSseStrings(sseEvents, rc, context);
            }
        }
    }

    @Authenticated
    public void setTaskPushNotificationConfiguration(String body, RoutingContext rc) {
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

    @Authenticated
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

    @Authenticated
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

    @Authenticated
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

    public void getAgentCard(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.getAgentCard();
        sendResponse(rc, response);
    }

    @Authenticated
    public void getAuthenticatedExtendedCard(RoutingContext rc) {
        HTTPRestResponse response = jsonRestHandler.getAuthenticatedExtendedCard();
        sendResponse(rc, response);
    }

    private static String extractBody(RoutingContext rc) {
        String body = rc.body().asString();
        return body != null ? body : "";
    }

    private static String formatSseEvent(String data, long id) {
        return "data: " + data + "\nid: " + id + "\n\n";
    }

    public static void setStreamingMultiSseSupportSubscribedRunnable(Runnable runnable) {
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

            return new ServerCallContext(user, state, requestedExtensions, A2AProtocol_v0_3.PROTOCOL_VERSION);
        } else {
            CallContextFactory_v0_3 builder = callContextFactory.get();
            return builder.build(rc);
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

    private static class MultiSseSupport {
        private static final Logger logger = LoggerFactory.getLogger(MultiSseSupport.class);

        private MultiSseSupport() {
            // Avoid direct instantiation.
        }

        private static final int IDLE = 0;
        private static final int WRITE_PENDING = 1;
        private static final int COMPLETE_DEFERRED = 2;

        public static void writeSseStrings(Multi<String> sseStrings, RoutingContext rc, ServerCallContext context) {
            HttpServerResponse response = rc.response();

            sseStrings.subscribe().withSubscriber(new Flow.Subscriber<String>() {
                Flow.@Nullable Subscription upstream;
                final java.util.concurrent.atomic.AtomicInteger writeState = new java.util.concurrent.atomic.AtomicInteger(IDLE);

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.upstream = subscription;
                    this.upstream.request(1);

                    response.closeHandler(v -> {
                        logger.debug("REST SSE connection closed by client, calling EventConsumer.cancel() to stop polling loop");
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
                    if (response.bytesWritten() == 0) {
                        MultiMap headers = response.headers();
                        if (headers.get(CONTENT_TYPE) == null) {
                            headers.set(CONTENT_TYPE, SERVER_SENT_EVENTS);
                        }
                        headers.set("Cache-Control", "no-cache");
                        headers.set("X-Accel-Buffering", "no");
                        response.setChunked(true);
                        response.setWriteQueueMaxSize(1);
                        response.write(": SSE stream started\n\n");
                    }

                    writeState.set(WRITE_PENDING);
                    response.write(Buffer.buffer(sseEvent), new Handler<AsyncResult<Void>>() {
                        @Override
                        public void handle(AsyncResult<Void> ar) {
                            if (ar.failed()) {
                                java.util.Objects.requireNonNull(upstream).cancel();
                                rc.fail(ar.cause());
                            } else if (writeState.compareAndSet(WRITE_PENDING, IDLE)) {
                                java.util.Objects.requireNonNull(upstream).request(1);
                            } else {
                                endResponse(response);
                            }
                        }
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                    java.util.Objects.requireNonNull(upstream).cancel();
                    rc.fail(throwable);
                }

                @Override
                public void onComplete() {
                    if (!writeState.compareAndSet(WRITE_PENDING, COMPLETE_DEFERRED)) {
                        endResponse(response);
                    }
                }

                private void endResponse(HttpServerResponse resp) {
                    Runnable doEnd = () -> {
                        if (resp.ended()) return;
                        if (resp.bytesWritten() == 0) {
                            MultiMap headers = resp.headers();
                            if (headers.get(CONTENT_TYPE) == null) {
                                headers.set(CONTENT_TYPE, SERVER_SENT_EVENTS);
                            }
                        }
                        resp.end();
                    };
                    if (io.vertx.core.Context.isOnEventLoopThread()) {
                        doEnd.run();
                    } else {
                        rc.vertx().runOnContext(v -> doEnd.run());
                    }
                }
            });
        }
    }
}
