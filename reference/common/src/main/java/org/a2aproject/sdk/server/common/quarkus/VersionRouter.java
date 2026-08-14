package org.a2aproject.sdk.server.common.quarkus;

import io.vertx.ext.web.RoutingContext;
import org.a2aproject.sdk.common.A2AHeaders;

public final class VersionRouter {

    public static final String VERSION_1_0 = "1.0";
    public static final String VERSION_0_3 = "0.3";

    /**
     * Vert.x route order for any JSON-RPC catch-all regex route.
     * Must be higher than all REST transport route orders (0–3) so that specific REST
     * routes are evaluated first when both transports share the same HTTP port.
     */
    public static final int JSONRPC_CATCH_ALL_ROUTE_ORDER = 100;

    private VersionRouter() {
    }

    /**
     * Resolves the protocol version from the request.
     *
     * @return {@link #VERSION_1_0}, {@link #VERSION_0_3}, or the raw version string
     *         if it matches neither (caller should reject as unsupported)
     */
    public static String resolveVersion(RoutingContext rc) {
        String version = rc.request().getHeader(A2AHeaders.A2A_VERSION);
        if (version == null || version.isBlank()) {
            version = rc.request().getParam(A2AHeaders.A2A_VERSION);
        }

        if (version == null || version.isBlank()) {
            return VERSION_0_3;
        }

        String trimmed = version.trim();
        if (trimmed.equals(VERSION_1_0)) {
            return VERSION_1_0;
        }
        if (trimmed.equals(VERSION_0_3)) {
            return VERSION_0_3;
        }
        return trimmed;
    }

    public static boolean isV10(String resolvedVersion) {
        return VERSION_1_0.equals(resolvedVersion);
    }

    public static boolean isV03(String resolvedVersion) {
        return VERSION_0_3.equals(resolvedVersion);
    }
}
