package org.a2aproject.sdk.client.transport.spi;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.a2aproject.sdk.client.http.HttpEndpoint;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.auth.AuthInterceptor;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.spec.AgentInterface;
import org.jspecify.annotations.Nullable;

/** Shared HTTP transport endpoint and redirect policy. */
public final class HttpTransportSecurity {
    private static final Logger LOGGER = Logger.getLogger(HttpTransportSecurity.class.getName());

    private HttpTransportSecurity() {
    }

    /** Validates and canonicalizes a JSON-RPC interface endpoint for transport and credential use.
     * @param endpoint configured interface
     * @return interface with the canonical endpoint
     */
    public static AgentInterface normalizeJsonRpcEndpoint(AgentInterface endpoint) {
        return new AgentInterface(endpoint.protocolBinding(), HttpEndpoint.normalizeJsonRpc(endpoint.url()),
                endpoint.tenant(), endpoint.protocolVersion());
    }

    /** Applies the POST redirect policy and reports its first suppression per transport. */
    public static final class PostRedirectPolicy {
        private static final String X_REQUEST_ID = "X-Request-Id";

        private final boolean requested;
        private final AtomicBoolean warned = new AtomicBoolean();

        /** @param requested whether the transport is configured to follow POST redirects */
        public PostRedirectPolicy(boolean requested) {
            this.requested = requested;
        }

        /**
         * Tests whether a non-streaming request may follow a redirect.
         *
         * @param headers caller and interceptor headers
         * @param interceptors configured interceptors, if any
         * @return whether following a redirect is permitted
         */
        public boolean allows(Map<String, String> headers, @Nullable List<ClientCallInterceptor> interceptors) {
            if (!requested) {
                return false;
            }
            for (String header : headers.keySet()) {
                if (!A2AHeaders.A2A_EXTENSIONS.equalsIgnoreCase(header) && !X_REQUEST_ID.equalsIgnoreCase(header)) {
                    if (warned.compareAndSet(false, true)) {
                        LOGGER.warning(() -> "Requested POST redirects disabled by request header "
                                + header.replaceAll("[^A-Za-z0-9!#$%&'*+.^_`|~-]", "?"));
                    }
                    return false;
                }
            }
            if (interceptors != null && interceptors.stream().anyMatch(AuthInterceptor.class::isInstance)) {
                if (warned.compareAndSet(false, true)) {
                    LOGGER.warning("Requested POST redirects disabled by an authentication interceptor");
                }
                return false;
            }
            return true;
        }
    }
}
