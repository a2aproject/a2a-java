package org.a2aproject.sdk.compat03.server.apps.quarkus;

import org.a2aproject.sdk.server.ServerCallContext;
import io.vertx.ext.web.RoutingContext;

/**
 * Factory interface for creating ServerCallContext from a Vert.x RoutingContext.
 *
 * <p>Implementations MUST pass {@link org.a2aproject.sdk.compat03.conversion.A2AProtocol_v0_3#PROTOCOL_VERSION}
 * as the protocol version when constructing {@link ServerCallContext} so that push notification
 * payloads are formatted correctly.</p>
 */
public interface CallContextFactory_v0_3 {
    ServerCallContext build(RoutingContext rc);
}
