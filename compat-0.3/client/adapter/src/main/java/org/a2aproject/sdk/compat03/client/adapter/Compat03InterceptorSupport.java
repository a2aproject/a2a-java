package org.a2aproject.sdk.compat03.client.adapter;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.jspecify.annotations.Nullable;

/** Applies ordinary 1.0 interceptors while enforcing the 0.3 routing contract. */
public final class Compat03InterceptorSupport {
    private Compat03InterceptorSupport() {
    }

    public static PayloadAndHeaders apply(List<ClientCallInterceptor> interceptors, String method,
            Object payload, AgentCard card, @Nullable ClientCallContext context, Class<?> expectedType) {
        Map<String, String> headers = context == null ? Map.of() : context.getHeaders();
        PayloadAndHeaders result = new PayloadAndHeaders(payload, headers);
        validateVersionHeader(result.getHeaders());
        for (ClientCallInterceptor interceptor : interceptors) {
            result = interceptor.intercept(method, result.getPayload(), result.getHeaders(), card, context);
            if (result == null || result.getPayload() == null) {
                throw new A2AClientException("0.3 interceptor returned a forbidden null payload for " + method);
            }
            if (!expectedType.equals(result.getPayload().getClass())) {
                throw new A2AClientException("0.3 interceptor returned " + result.getPayload().getClass().getName()
                        + "; expected " + expectedType.getName() + " for " + method);
            }
            validateVersionHeader(result.getHeaders());
        }
        return result;
    }

    private static void validateVersionHeader(Map<String, String> headers) {
        for (String name : headers.keySet()) {
            if (A2AHeaders.A2A_VERSION.equalsIgnoreCase(name)) {
                throw new A2AClientException("0.3 client interceptors may not override A2A-Version");
            }
        }
    }
}
