package io.a2a.client.transport.spi;

import io.a2a.client.transport.spi.interceptors.ClientCallContext;
import io.a2a.client.transport.spi.interceptors.ClientCallInterceptor;
import io.a2a.client.transport.spi.interceptors.PayloadAndHeaders;
import io.a2a.spec.AgentCard;
import org.jspecify.annotations.Nullable;

import java.util.List;

public abstract class AbstractClientTransport implements ClientTransport {

    private final @Nullable List<ClientCallInterceptor> interceptors;

    public AbstractClientTransport(@Nullable List<ClientCallInterceptor> interceptors) {
        this.interceptors = interceptors;
    }

    protected PayloadAndHeaders applyInterceptors(String methodName, @Nullable Object payload,
                                                  @Nullable AgentCard agentCard, @Nullable ClientCallContext clientCallContext) {
        PayloadAndHeaders payloadAndHeaders = new PayloadAndHeaders(payload,
                clientCallContext != null ? clientCallContext.getHeaders() : null);
        if (interceptors != null && ! interceptors.isEmpty()) {
            for (ClientCallInterceptor interceptor : interceptors) {
                payloadAndHeaders = interceptor.intercept(methodName, payloadAndHeaders.getPayload(),
                        payloadAndHeaders.getHeaders(), agentCard, clientCallContext);
            }
        }
        return payloadAndHeaders;
    }
}
