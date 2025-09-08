package io.a2a.client.transport.spi.interceptors;

import java.util.Map;

import io.a2a.spec.AgentCard;
import org.jspecify.annotations.Nullable;

/**
 * An abstract base class for client-side call interceptors.
 * Interceptors can inspect and modify requests before they are sent,
 * which is ideal for concerns like authentication, logging, or tracing.
 */
public abstract class ClientCallInterceptor {

    /**
     * Intercept a client call before the request is sent.
     *
     * @param methodName the name of the protocol method (e.g., 'message/send')
     * @param payload the request payload
     * @param headers the headers to use
     * @param agentCard the agent card (may be {@code null})
     * @param clientCallContext the {@code ClientCallContext} for this call (may be {@code null})
     * @return the potentially modified payload and headers
     */
    public abstract PayloadAndHeaders intercept(String methodName, @Nullable Object payload, Map<String, String> headers,
                                                @Nullable AgentCard agentCard, @Nullable ClientCallContext clientCallContext);
}
