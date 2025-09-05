package io.a2a.server.rest.quarkus;

import io.a2a.server.ServerCallContext;
import io.vertx.ext.web.RoutingContext;

public interface CallContextFactory {
    ServerCallContext build(RoutingContext rc);
}
