package org.a2aproject.sdk.server.common.quarkus;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.ManagedContext;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.vertx.core.Context;
import io.vertx.ext.web.RoutingContext;

/**
 * CDI helper for integrating Quarkus security with Vert.x Web routes.
 *
 * <p>When using {@code @Observes Router} to register Vert.x routes (instead of Quarkus Reactive Routes),
 * the standard Quarkus HTTP authentication flow is bypassed. This helper provides utilities to manually
 * trigger authentication and activate the CDI request context so that {@code @Authenticated} interceptors
 * work correctly.
 *
 * <h2>Background</h2>
 * <p>Quarkus Reactive Routes (using {@code @Route}) automatically:
 * <ul>
 *   <li>Trigger HTTP authentication mechanisms (Basic, OAuth, etc.)</li>
 *   <li>Activate the CDI request context</li>
 *   <li>Populate {@code CurrentIdentityAssociation} with the authenticated identity</li>
 * </ul>
 *
 * <p>However, when using {@code @Observes Router} to register raw Vert.x Web routes, none of this happens
 * automatically. This helper bridges the gap by:
 * <ul>
 *   <li>Triggering authentication via {@link HttpAuthenticator}</li>
 *   <li>Activating the CDI request context</li>
 *   <li>Populating {@code CurrentIdentityAssociation} with the authenticated identity</li>
 *   <li>Executing {@code @Authenticated} methods within the active context</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * @Inject VertxSecurityHelper securityHelper;
 *
 * void setupRoutes(@Observes Router router) {
 *     router.post("/api/endpoint")
 *         .blockingHandler(ctx -> {
 *             try {
 *                 // Authenticate and execute within request context
 *                 securityHelper.runInRequestContext(ctx, () -> {
 *                     myAuthenticatedMethod(); // Has @Authenticated annotation
 *                 });
 *             } catch (UnauthorizedException | ForbiddenException e) {
 *                 securityHelper.handleAuthError(ctx, e);
 *             } catch (Exception e) {
 *                 VertxSecurityHelper.handleGenericError(ctx);
 *             }
 *         });
 * }
 * }</pre>
 *
 * @see HttpAuthenticator
 * @see CurrentIdentityAssociation
 */
@Singleton
public final class VertxSecurityHelper {

    @Inject
    Instance<HttpAuthenticator> httpAuthenticator;

    @Inject
    Instance<CurrentIdentityAssociation> currentIdentityAssociation;

    public VertxSecurityHelper() {
        // CDI-managed constructor
    }

    /**
     * Authenticates the request and executes a task within an active CDI request context.
     *
     * <p>This method performs the full authentication and context setup flow required for
     * Vert.x Web routes registered via {@code @Observes Router}:
     * <ol>
     *   <li>Triggers HTTP authentication via {@link HttpAuthenticator#attemptAuthentication}</li>
     *   <li>Activates the CDI request context if needed</li>
     *   <li>Sets the authenticated identity in {@link CurrentIdentityAssociation}</li>
     *   <li>Executes the task (which may call {@code @Authenticated} methods)</li>
     *   <li>Terminates the request context if it was activated by this method</li>
     * </ol>
     *
     * <p><b>Thread Safety:</b> This must be called on a Vert.x worker thread (e.g., from a
     * {@code blockingHandler}), not the event loop thread. The authentication call blocks
     * the worker thread using {@code await().indefinitely()}, which is safe on worker threads
     * but would block the event loop on event loop threads.
     *
     * <p><b>Context cleanup:</b> Unlike {@link #runInRequestContextDeferred}, this method calls
     * {@code terminate()} immediately in the {@code finally} block, so there is no need to register
     * {@code endHandler}/{@code closeHandler} callbacks — the context and its beans are destroyed
     * before this method returns. Use this for non-streaming requests where the task runs synchronously
     * on the calling thread.
     *
     * @param ctx the Vert.x routing context containing the HTTP request
     * @param task the code to execute within the authenticated request context
     * @throws io.quarkus.security.UnauthorizedException if authentication fails
     * @throws io.quarkus.security.ForbiddenException if authorization fails
     * @throws RuntimeException if the task throws an exception
     * @see #runInRequestContextDeferred(RoutingContext, Runnable)
     */
    public void runInRequestContext(RoutingContext ctx, Runnable task) {
        if (Context.isOnEventLoopThread()) {
            throw new IllegalStateException(
                    "Cannot perform blocking authentication on event loop thread. Use blockingHandler().");
        }
        ManagedContext requestContext = Arc.container().requestContext();
        boolean wasActive = requestContext.isActive();
        if (!wasActive) {
            requestContext.activate();
        }
        try {
            if (!httpAuthenticator.isUnsatisfied()) {
                var identity = httpAuthenticator.get().attemptAuthentication(ctx).await().indefinitely();
                currentIdentityAssociation.get().setIdentity(identity);
            }
            task.run();
        } finally {
            if (!wasActive) {
                requestContext.terminate();
            }
        }
    }

    /**
     * Authenticates the request and executes a task, deferring CDI context destruction to the HTTP
     * response lifecycle.
     *
     * <p>Unlike {@link #runInRequestContext}, this method does <b>not</b> terminate the CDI request
     * context when the task completes. Instead, it:
     * <ol>
     *   <li>Activates the CDI request context (or captures the existing active state)</li>
     *   <li>Triggers HTTP authentication</li>
     *   <li>Executes the task</li>
     *   <li><b>Deactivates</b> the context on the calling thread (without destroying beans) — this
     *       prevents any external lifecycle manager (e.g. Quarkus's request filter) from calling
     *       {@code terminate()} and destroying beans while the agent thread is still running</li>
     *   <li>Destroys the context state when the HTTP response ends or the connection closes</li>
     * </ol>
     *
     * <p>This is required for streaming (SSE) requests where the task dispatches work to a background
     * thread (via {@code CompletableFuture.runAsync} with a {@code ManagedExecutor}) and returns
     * immediately. The {@code ManagedExecutor} captures the CDI context at submit time and propagates
     * it to the agent thread via {@link AsyncManagedExecutorProducer} — but only if the context
     * hasn't been destroyed yet. By deactivating (not destroying) in the finally block and deferring
     * destruction to the response lifecycle, the agent thread can access all {@code @RequestScoped}
     * beans (including OIDC token credentials for token propagation).
     *
     * <p>When the CDI context is already active (e.g. activated by a Quarkus request filter), this
     * method takes ownership of its destruction lifecycle to ensure it is not prematurely terminated
     * when the blocking handler thread returns. The context is destroyed by the SSE response
     * {@code endHandler} / {@code closeHandler} instead.
     *
     * <p>This method is also safe for non-streaming requests: {@code response.end()} is called
     * synchronously inside the task, and the {@code endHandler} fires the cleanup shortly after.
     *
     * @param ctx the Vert.x routing context containing the HTTP request
     * @param task the code to execute within the authenticated request context
     * @throws io.quarkus.security.UnauthorizedException if authentication fails
     * @throws io.quarkus.security.ForbiddenException if authorization fails
     * @throws RuntimeException if the task throws an exception
     * @see #runInRequestContext(RoutingContext, Runnable)
     * @see AsyncManagedExecutorProducer
     */
    public void runInRequestContextDeferred(RoutingContext ctx, Runnable task) {
        if (Context.isOnEventLoopThread()) {
            throw new IllegalStateException(
                    "Cannot perform blocking authentication on event loop thread. Use blockingHandler().");
        }
        ManagedContext requestContext = Arc.container().requestContext();
        // If already active, capture the existing state; otherwise activate a fresh context.
        // Either way we take ownership of destruction — deactivate() in finally prevents any
        // external manager (Quarkus request filter) from calling terminate() and destroying beans
        // while the ManagedExecutor agent thread is still running. Actual destruction is deferred
        // to the SSE response endHandler / closeHandler.
        InjectableContext.ContextState state = requestContext.isActive()
                ? requestContext.getState()
                : requestContext.activate();

        AtomicBoolean destroyed = new AtomicBoolean(false);
        Runnable cleanup = () -> {
            if (destroyed.compareAndSet(false, true)) {
                requestContext.destroy(state);
            }
        };

        ctx.response().endHandler(v -> cleanup.run());
        ctx.response().closeHandler(v -> cleanup.run());

        try {
            if (!httpAuthenticator.isUnsatisfied()) {
                var identity = httpAuthenticator.get().attemptAuthentication(ctx).await().indefinitely();
                currentIdentityAssociation.get().setIdentity(identity);
            }
            task.run();
        } catch (Throwable t) {
            // Destroy bean instances immediately on error. The subsequent deactivate() in the
            // finally block is still needed to detach the (now-empty) context from the worker
            // thread. destroy(state) only destroys beans — it does not deactivate the context.
            cleanup.run();
            throw t;
        } finally {
            // Deactivate on the calling thread so that Arc's terminate() (called by any external
            // lifecycle manager when this thread returns) sees isActive() == false and skips its
            // own destroy(). Destruction is handled exclusively by the cleanup registered above.
            requestContext.deactivate();
        }
    }

    /**
     * Handles authentication or authorization errors by sending the appropriate HTTP error response.
     *
     * <p>This should be called when catching {@code UnauthorizedException} or {@code ForbiddenException}
     * thrown by {@code @Authenticated} interceptors or authentication mechanisms.
     *
     * <ul>
     *   <li>{@code ForbiddenException} → HTTP 403 Forbidden</li>
     *   <li>All other auth errors → delegates to {@link HttpAuthenticator#getChallenge} to obtain
     *       the correct {@code WWW-Authenticate} header for the configured auth mechanism
     *       (Basic, Bearer, etc.) and sends HTTP 401 with the challenge header</li>
     * </ul>
     *
     * @param ctx the routing context
     * @param e the authentication or authorization exception
     */
    public void handleAuthError(RoutingContext ctx, Exception e) {
        if (!ctx.response().ended()) {
            if (e instanceof io.quarkus.security.ForbiddenException) {
                ctx.response()
                    .setStatusCode(403)
                    .end();
            } else {
                int status = 401;
                if (!httpAuthenticator.isUnsatisfied()) {
                    ChallengeData challenge = httpAuthenticator.get().getChallenge(ctx).await().indefinitely();
                    if (challenge != null) {
                        status = challenge.status;
                        for(Map.Entry<CharSequence, String> header : challenge.getHeaders().entrySet()) {
                            ctx.response().putHeader(header.getKey(), header.getValue());
                        }
                    }
                }
                ctx.response()
                    .setStatusCode(status)
                    .end();
            }
        }
    }

    /**
     * Handles generic errors by sending a 500 Internal Server Error response.
     *
     * @param ctx the routing context
     */
    public static void handleGenericError(RoutingContext ctx) {
        if (!ctx.response().ended()) {
            ctx.response()
                .setStatusCode(500)
                .end("Internal Server Error");
        }
    }

}
