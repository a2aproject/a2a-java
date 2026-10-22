package org.a2aproject.sdk.compat03.client.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.spec.AgentCard;
import org.junit.jupiter.api.Test;

class Compat03ClientCallContextMapperTest {
    @Test
    void translatesExtensionHeadersWithoutChangingTheNativeContext() {
        for (String name : List.of("A2A-Extensions", "a2a-extensions", "A2A-EXTENSIONS")) {
            ClientCallContext context = new ClientCallContext(Map.of("request-id", "request-1"),
                    Map.of(name, "urn:example:extension", "Authorization", "Bearer token"));

            var legacy = Compat03ClientCallContextMapper.toV03(context);

            assertEquals(Map.of("X-A2A-Extensions", "urn:example:extension",
                    "Authorization", "Bearer token"), legacy.getHeaders());
            assertEquals(context.getState(), legacy.getState());
            assertEquals("urn:example:extension", context.getHeaders().get(name));
        }
    }

    @Test
    void translatesExtensionsAfterNativeInterceptorsAndPrefersTheNativeHeader() {
        ClientCallInterceptor interceptor = new ClientCallInterceptor() {
            @Override
            public PayloadAndHeaders intercept(String method, Object payload, Map<String, String> headers,
                    AgentCard card, ClientCallContext context) {
                assertEquals("urn:original", headers.get("A2A-Extensions"));
                return new PayloadAndHeaders(payload,
                        Map.of("a2a-extensions", "urn:replacement", "x-a2a-extensions", "urn:stale"));
            }
        };
        var result = Compat03InterceptorSupport.apply(List.of(interceptor), "message/send", "payload", null,
                new ClientCallContext(Map.of(), Map.of("A2A-Extensions", "urn:original")), String.class);

        var legacy = Compat03ClientCallContextMapper.toV03(new ClientCallContext(Map.of(), result.getHeaders()));

        assertEquals(Map.of("X-A2A-Extensions", "urn:replacement"), legacy.getHeaders());
    }
}
