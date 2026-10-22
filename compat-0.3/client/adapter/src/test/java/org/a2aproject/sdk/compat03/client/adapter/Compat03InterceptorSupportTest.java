package org.a2aproject.sdk.compat03.client.adapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.junit.jupiter.api.Test;

class Compat03InterceptorSupportTest {

    @Test
    void stripsVersionHeaderFromInitialContextBeforeInterceptors() {
        AtomicReference<Map<String, String>> interceptedHeaders = new AtomicReference<>();
        AtomicReference<ClientCallContext> interceptedContext = new AtomicReference<>();
        Map<String, Object> state = Map.of("request-id", "request-1");
        ClientCallInterceptor interceptor = new ClientCallInterceptor() {
            @Override
            public PayloadAndHeaders intercept(String methodName, Object payload, Map<String, String> headers,
                    org.a2aproject.sdk.spec.AgentCard agentCard, ClientCallContext clientCallContext) {
                interceptedHeaders.set(headers);
                interceptedContext.set(clientCallContext);
                return new PayloadAndHeaders(payload, headers);
            }
        };

        Compat03InterceptorSupport.apply(
                java.util.List.of(interceptor), "message/send", "payload", null,
                new ClientCallContext(state, Map.of("a2a-version", "1.0", "Authorization", "Bearer token")),
                String.class);

        assertFalse(interceptedHeaders.get().keySet().stream()
                .anyMatch(name -> name.equalsIgnoreCase("A2A-Version")));
        assertEquals("Bearer token", interceptedHeaders.get().get("Authorization"));
        assertSame(state, interceptedContext.get().getState());
        assertFalse(interceptedContext.get().getHeaders().keySet().stream()
                .anyMatch(name -> name.equalsIgnoreCase("A2A-Version")));
        assertEquals("Bearer token", interceptedContext.get().getHeaders().get("Authorization"));
    }
}
