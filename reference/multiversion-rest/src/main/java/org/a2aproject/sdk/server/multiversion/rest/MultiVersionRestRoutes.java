package org.a2aproject.sdk.server.multiversion.rest;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.google.gson.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.a2aproject.sdk.server.common.quarkus.VersionRouter;
import org.a2aproject.sdk.server.common.quarkus.VertxSecurityHelper;
import org.a2aproject.sdk.server.rest.quarkus.A2AServerRoutes;
import org.a2aproject.sdk.compat03.server.rest.quarkus.A2AServerRoutes_v0_3;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.A2AErrorCodes;
import org.a2aproject.sdk.spec.VersionNotSupportedError;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;

@Singleton
public class MultiVersionRestRoutes {

    @Inject
    A2AServerRoutes v10Routes;

    @Inject
    A2AServerRoutes_v0_3 v03Routes;

    @Inject
    VertxSecurityHelper vertxSecurityHelper;

    void setupRoutes(@Observes @Priority(5) Router router) {
        // POST /v1/message:send
        router.postWithRegex("^\\/v1\\/message:send$")
            .order(-1)
            .handler(BodyHandler.create())
            .blockingHandler(versionDispatch(false,
                MultiVersionRestRoutes::bridgeTenant,
                (body, ctx) -> v10Routes.sendMessage(body, ctx),
                (body, ctx) -> v03Routes.sendMessage(body, ctx)), false);

        // POST /v1/message:stream (deferred CDI context destruction)
        router.postWithRegex("^\\/v1\\/message:stream$")
            .order(-1)
            .handler(BodyHandler.create())
            .blockingHandler(versionDispatch(true,
                MultiVersionRestRoutes::bridgeTenant,
                (body, ctx) -> v10Routes.sendMessageStreaming(body, ctx),
                (body, ctx) -> v03Routes.sendMessageStreaming(body, ctx)), false);

        // GET /v1/tasks/{taskId}
        router.getWithRegex("^\\/v1\\/tasks\\/(?<taskId>[^:^/]+)$")
            .order(-1)
            .blockingHandler(versionDispatchNoBody(false,
                ctx -> { bridgeTenant(ctx); bridgeTaskId(ctx); },
                ctx -> v10Routes.getTask(ctx),
                ctx -> v03Routes.getTask(ctx)), false);

        // POST /v1/tasks/{taskId}:cancel
        router.postWithRegex("^\\/v1\\/tasks\\/(?<taskId>[^/]+):cancel$")
            .order(-1)
            .handler(BodyHandler.create())
            .blockingHandler(versionDispatch(false,
                ctx -> { bridgeTenant(ctx); bridgeTaskId(ctx); },
                (body, ctx) -> v10Routes.cancelTask(body, ctx),
                (body, ctx) -> v03Routes.cancelTask(ctx)), false);

        // POST /v1/tasks/{taskId}:subscribe (deferred CDI context destruction)
        router.postWithRegex("^\\/v1\\/tasks\\/(?<taskId>[^/]+):subscribe$")
            .order(-1)
            .blockingHandler(versionDispatchNoBody(true,
                ctx -> { bridgeTenant(ctx); bridgeTaskId(ctx); },
                ctx -> v10Routes.subscribeToTask(ctx),
                ctx -> v03Routes.resubscribeTask(ctx)), false);

        // POST /v1/tasks/{taskId}/pushNotificationConfigs
        router.postWithRegex("^\\/v1\\/tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs$")
            .order(-1)
            .handler(BodyHandler.create())
            .blockingHandler(versionDispatch(false,
                ctx -> { bridgeTenant(ctx); bridgeTaskId(ctx); },
                (body, ctx) -> v10Routes.createTaskPushNotificationConfiguration(body, ctx),
                (body, ctx) -> v03Routes.setTaskPushNotificationConfiguration(body, ctx)), false);

        // GET /v1/tasks/{taskId}/pushNotificationConfigs/{configId}
        router.getWithRegex("^\\/v1\\/tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs\\/(?<configId>[^\\/]+)")
            .order(-1)
            .blockingHandler(versionDispatchNoBody(false,
                ctx -> { bridgeTenant(ctx); bridgeTaskId(ctx); },
                ctx -> v10Routes.getTaskPushNotificationConfiguration(ctx),
                ctx -> v03Routes.getTaskPushNotificationConfiguration(ctx)), false);

        // GET /v1/tasks/{taskId}/pushNotificationConfigs
        router.getWithRegex("^\\/v1\\/tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs\\/?$")
            .order(-1)
            .blockingHandler(versionDispatchNoBody(false,
                ctx -> { bridgeTenant(ctx); bridgeTaskId(ctx); },
                ctx -> v10Routes.listTaskPushNotificationConfigurations(ctx),
                ctx -> v03Routes.listTaskPushNotificationConfigurations(ctx)), false);

        // DELETE /v1/tasks/{taskId}/pushNotificationConfigs/{configId}
        router.deleteWithRegex("^\\/v1\\/tasks\\/(?<taskId>[^/]+)\\/pushNotificationConfigs\\/(?<configId>[^/]+)")
            .order(-1)
            .blockingHandler(versionDispatchNoBody(false,
                ctx -> { bridgeTenant(ctx); bridgeTaskId(ctx); },
                ctx -> v10Routes.deleteTaskPushNotificationConfiguration(ctx),
                ctx -> v03Routes.deleteTaskPushNotificationConfiguration(ctx)), false);

        // GET /v1/card — v0.3 only (v1.0 uses /{tenant}/extendedAgentCard)
        router.get("/v1/card")
            .order(-1)
            .produces("application/json")
            .blockingHandler(ctx -> {
                try {
                    vertxSecurityHelper.runInRequestContext(ctx,
                        () -> v03Routes.getAuthenticatedExtendedCard(ctx));
                } catch (UnauthorizedException | ForbiddenException e) {
                    vertxSecurityHelper.handleAuthError(ctx, e);
                } catch (Exception e) {
                    VertxSecurityHelper.handleGenericError(ctx);
                }
            }, false);
    }

    private static void bridgeTenant(RoutingContext ctx) {
        ctx.pathParams().put("tenant", "v1");
    }

    private static void bridgeTaskId(RoutingContext ctx) {
        String taskId = ctx.pathParam("taskId");
        if (taskId != null) {
            ctx.pathParams().put("id", taskId);
            ctx.pathParams().put("param0", taskId);
        }
    }

    /**
     * @param deferContextDestruction if true, defers CDI request context destruction until the SSE response completes
     */
    private io.vertx.core.Handler<RoutingContext> versionDispatch(
            boolean deferContextDestruction,
            Consumer<RoutingContext> paramBridger,
            BiConsumer<String, RoutingContext> v10Handler,
            BiConsumer<String, RoutingContext> v03Handler) {
        return ctx -> {
            try {
                Runnable task = () -> {
                    String version = VersionRouter.resolveVersion(ctx);
                    paramBridger.accept(ctx);
                    String body = ctx.body().asString();
                    if (body == null) {
                        body = "";
                    }
                    if (VersionRouter.isV10(version)) {
                        v10Handler.accept(body, ctx);
                    } else if (VersionRouter.isV03(version)) {
                        v03Handler.accept(body, ctx);
                    } else {
                        throw new VersionNotSupportedError(
                                null,
                                "Protocol version '" + version + "' is not supported. Supported versions: [1.0, 0.3]",
                                null);
                    }
                };
                runInContext(ctx, task, deferContextDestruction);
            } catch (UnauthorizedException | ForbiddenException e) {
                vertxSecurityHelper.handleAuthError(ctx, e);
            } catch (A2AError e) {
                sendA2AErrorResponse(ctx, e);
            } catch (Exception e) {
                VertxSecurityHelper.handleGenericError(ctx);
            }
        };
    }

    /**
     * @param deferContextDestruction if true, defers CDI request context destruction until the SSE response completes
     */
    private io.vertx.core.Handler<RoutingContext> versionDispatchNoBody(
            boolean deferContextDestruction,
            Consumer<RoutingContext> paramBridger,
            Consumer<RoutingContext> v10Handler,
            Consumer<RoutingContext> v03Handler) {
        return ctx -> {
            try {
                Runnable task = () -> {
                    String version = VersionRouter.resolveVersion(ctx);
                    paramBridger.accept(ctx);
                    if (VersionRouter.isV10(version)) {
                        v10Handler.accept(ctx);
                    } else if (VersionRouter.isV03(version)) {
                        v03Handler.accept(ctx);
                    } else {
                        throw new VersionNotSupportedError(
                                null,
                                "Protocol version '" + version + "' is not supported. Supported versions: [1.0, 0.3]",
                                null);
                    }
                };
                runInContext(ctx, task, deferContextDestruction);
            } catch (UnauthorizedException | ForbiddenException e) {
                vertxSecurityHelper.handleAuthError(ctx, e);
            } catch (A2AError e) {
                sendA2AErrorResponse(ctx, e);
            } catch (Exception e) {
                VertxSecurityHelper.handleGenericError(ctx);
            }
        };
    }

    private void runInContext(RoutingContext ctx, Runnable task, boolean deferContextDestruction) {
        if (deferContextDestruction) {
            vertxSecurityHelper.runInRequestContextDeferred(ctx, task);
        } else {
            vertxSecurityHelper.runInRequestContext(ctx, task);
        }
    }

    private static void sendA2AErrorResponse(RoutingContext ctx, A2AError error) {
        A2AErrorCodes errorCode = A2AErrorCodes.fromCode(error.getCode());
        int httpStatus = errorCode != null ? errorCode.httpCode() : 400;

        JsonObject errorObj = new JsonObject();
        errorObj.addProperty("code", error.getCode());
        errorObj.addProperty("message", error.getMessage());

        JsonObject response = new JsonObject();
        response.add("error", errorObj);

        ctx.response()
            .setStatusCode(httpStatus)
            .putHeader(CONTENT_TYPE, "application/json")
            .end(response.toString());
    }
}
