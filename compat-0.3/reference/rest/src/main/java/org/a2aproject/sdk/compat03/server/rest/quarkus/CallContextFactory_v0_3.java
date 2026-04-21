package org.a2aproject.sdk.compat03.server.rest.quarkus;

import org.a2aproject.sdk.server.ServerCallContext;
import io.vertx.ext.web.RoutingContext;

public interface CallContextFactory_v0_3 {
    ServerCallContext build(RoutingContext rc);
}
