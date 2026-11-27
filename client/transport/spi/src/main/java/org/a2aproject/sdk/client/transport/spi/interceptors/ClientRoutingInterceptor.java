package org.a2aproject.sdk.client.transport.spi.interceptors;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.client.transport.spi.ClientTransportProvider;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.util.Utils;
import org.jspecify.annotations.Nullable;

/**
 * Selects the tenant for a current-protocol client request. REST, JSON-RPC and gRPC run routing
 * interceptors in registration order before the ordinary interceptor chain and credential lookup.
 * The transport writes the final tenant into the request and uses it for credentials.
 * Providers implementing this routing phase declare it through
 * {@link ClientTransportProvider#supportsTenantRouting()}.
 */
public abstract class ClientRoutingInterceptor extends ClientCallInterceptor {

    /**
     * Selects the tenant without changing the configured recipient or other request fields.
     *
     * @param destination configured recipient identity, if available
     * @param methodName protocol method
     * @param tenant tenant selected so far, including the interface default
     * @param agentCard agent card, if available
     * @param context call context, if available
     * @return selected tenant, or null to address the unscoped agent
     */
    public abstract @Nullable String routeTenant(@Nullable String destination, String methodName,
            @Nullable String tenant, @Nullable AgentCard agentCard, @Nullable ClientCallContext context);

    /**
     * Runs the tenant routing phase for a transport call.
     *
     * @param interceptors configured interceptors, if present
     * @param destination configured recipient identity, if available
     * @param methodName protocol method
     * @param tenant effective tenant before routing
     * @param agentCard agent card, if available
     * @param context call context, if available
     * @return final tenant, or null for an unscoped request
     */
    public static @Nullable String selectTenant(@Nullable List<ClientCallInterceptor> interceptors,
            @Nullable String destination, String methodName, @Nullable String tenant,
            @Nullable AgentCard agentCard, @Nullable ClientCallContext context) {
        tenant = Utils.canonicalizeTenant(tenant);
        if (interceptors != null) {
            for (var interceptor : interceptors) {
                if (interceptor instanceof ClientRoutingInterceptor routing) {
                    tenant = Utils.canonicalizeTenant(routing.routeTenant(destination, methodName, tenant, agentCard, context));
                }
            }
        }
        return tenant;
    }

    @Override
    public final PayloadAndHeaders intercept(String methodName, Object payload, Map<String, String> headers,
            @Nullable AgentCard agentCard, @Nullable ClientCallContext context) {
        throw new UnsupportedOperationException("This transport does not support tenant routing");
    }
}
