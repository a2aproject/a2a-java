package org.a2aproject.sdk.server.multiversion.jsonrpc;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.a2aproject.sdk.compat03.server.apps.quarkus.A2AServerRoutes_v0_3;
import org.a2aproject.sdk.grpc.utils.JSONRPCUtils;
import org.a2aproject.sdk.server.apps.quarkus.A2AServerRoutes;
import org.a2aproject.sdk.server.common.quarkus.VersionRouter;
import org.a2aproject.sdk.server.common.quarkus.VertxSecurityHelper;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.VersionNotSupportedError;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;

@Singleton
public class MultiVersionJSONRPCRoutes {

    @Inject
    A2AServerRoutes v10Routes;

    @Inject
    A2AServerRoutes_v0_3 v03Routes;

    @Inject
    VertxSecurityHelper vertxSecurityHelper;

    void setupRoutes(@Observes Router router) {
        router.post("/")
            .order(-1)
            .consumes("application/json")
            .handler(BodyHandler.create())
            .blockingHandler(ctx -> {
                try {
                    vertxSecurityHelper.runInRequestContext(ctx, () -> {
                        String version = VersionRouter.resolveVersion(ctx);
                        String body = ctx.body().asString();

                        if (VersionRouter.isV10(version)) {
                            v10Routes.invokeJSONRPCHandler(body, ctx);
                        } else if (VersionRouter.isV03(version)) {
                            v03Routes.invokeJSONRPCHandler(body, ctx);
                        } else {
                            throw new VersionNotSupportedError(
                                null,
                                "Protocol version '" + version + "' is not supported. Supported versions: [1.0, 0.3]",
                                null);
                        }
                    });
                } catch (UnauthorizedException | ForbiddenException e) {vertxSecurityHelper.handleAuthError(ctx, e);
                } catch (A2AError e) {
                    ctx.response()
                        .setStatusCode(200)
                        .putHeader(CONTENT_TYPE, "application/json")
                        .end(JSONRPCUtils.toJsonRPCErrorResponse(null, e));
                } catch (Exception e) {
                    VertxSecurityHelper.handleGenericError(ctx);
                }
            }, false);
    }
}
