package org.a2aproject.sdk.compat03.client.adapter;

import java.util.HashMap;
import java.util.Map;

import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.compat03.common.A2AHeaders_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.jspecify.annotations.Nullable;

/** Converts public 1.0 call contexts to the equivalent 0.3 context. */
public final class Compat03ClientCallContextMapper {

    private Compat03ClientCallContextMapper() {
    }

    public static @Nullable ClientCallContext_v0_3 toV03(@Nullable ClientCallContext context) {
        if (context == null) {
            return null;
        }
        Map<String, String> headers = new HashMap<>();
        context.getHeaders().forEach((name, value) -> {
            if (!A2AHeaders.A2A_EXTENSIONS.equalsIgnoreCase(name)) {
                headers.put(A2AHeaders_v0_3.X_A2A_EXTENSIONS.equalsIgnoreCase(name)
                        ? A2AHeaders_v0_3.X_A2A_EXTENSIONS : name, value);
            }
        });
        // The native header, including interceptor replacements, takes precedence over a legacy alias.
        context.getHeaders().forEach((name, value) -> {
            if (A2AHeaders.A2A_EXTENSIONS.equalsIgnoreCase(name)) {
                headers.put(A2AHeaders_v0_3.X_A2A_EXTENSIONS, value);
            }
        });
        return new ClientCallContext_v0_3(Map.copyOf(context.getState()), Map.copyOf(headers));
    }
}
