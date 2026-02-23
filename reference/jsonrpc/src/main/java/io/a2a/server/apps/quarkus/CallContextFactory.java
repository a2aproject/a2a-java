package io.a2a.server.apps.quarkus;

import io.a2a.server.ServerCallContext;
import io.vertx.ext.web.RoutingContext;

/**
 * Factory interface for creating {@link ServerCallContext} from Vert.x routing context.
 *
 * <p>This interface provides an extension point for customizing how {@link ServerCallContext}
 * instances are created in Quarkus JSON-RPC applications. The default implementation in
 * {@link A2AServerRoutes} extracts standard information (user, headers, tenant, protocol version),
 * but applications can provide their own implementation to add custom context data.
 *
 * <h2>Default Behavior</h2>
 * <p>When no CDI bean implementing this interface is provided, {@link A2AServerRoutes}
 * creates contexts with:
 * <ul>
 *   <li>User authentication from Quarkus Security</li>
 *   <li>HTTP headers map</li>
 *   <li>Tenant ID from request path</li>
 *   <li>A2A protocol version from {@code X-A2A-Version} header</li>
 *   <li>Required extensions from {@code X-A2A-Extensions} header</li>
 * </ul>
 *
 * <h2>Custom Implementation Example</h2>
 * <pre>{@code
 * @ApplicationScoped
 * public class CustomCallContextFactory implements CallContextFactory {
 *     @Override
 *     public ServerCallContext build(RoutingContext rc) {
 *         // Extract user from Quarkus security context
 *         User user = (rc.user() == null) ? UnauthenticatedUser.INSTANCE :
 *             new User() {
 *                 public boolean isAuthenticated() { return rc.userContext().authenticated(); }
 *                 public String getUsername() { return rc.user().subject(); }
 *             };
 *
 *         // Extract custom data from routing context
 *         String orgId = rc.request().getHeader("X-Organization-ID");
 *
 *         Map<String, Object> state = new HashMap<>();
 *         state.put("organization", orgId);
 *         state.put("requestId", UUID.randomUUID().toString());
 *
 *         // Extract A2A protocol version from header
 *         String version = rc.request().getHeader(A2AHeaders.X_A2A_VERSION);
 *
 *         // Extract requested extensions from header
 *         List<String> extensionHeaders = rc.request().headers().getAll(A2AHeaders.X_A2A_EXTENSIONS);
 *         Set<String> extensions = A2AExtensions.getRequestedExtensions(extensionHeaders);
 *
 *         return new ServerCallContext(user, state, extensions, version);
 *     }
 * }
 * }</pre>
 *
 * @see ServerCallContext
 * @see A2AServerRoutes#createCallContext(RoutingContext)
 */
public interface CallContextFactory {
    /**
     * Builds a {@link ServerCallContext} from a Vert.x routing context.
     *
     * <p>This method is called for each incoming HTTP request to create the context
     * that will be passed to the {@link io.a2a.server.requesthandlers.RequestHandler}
     * and eventually to the {@link io.a2a.server.agentexecution.AgentExecutor}.
     *
     * @param rc the Vert.x routing context containing request data
     * @return a new ServerCallContext with extracted authentication, headers, and metadata
     */
    ServerCallContext build(RoutingContext rc);
}
