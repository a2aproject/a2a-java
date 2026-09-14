package org.a2aproject.sdk.compat03.client.adapter;

import java.util.Map;

import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.ClientCallContext_v0_3;
import org.jspecify.annotations.Nullable;

/** Converts public 1.0 call contexts to the equivalent 0.3 context. */
public final class Compat03ClientCallContextMapper {

    private Compat03ClientCallContextMapper() {
    }

    public static ClientCallContext_v0_3 toV03(@Nullable ClientCallContext context) {
        if (context == null) {
            return null;
        }
        return new ClientCallContext_v0_3(Map.copyOf(context.getState()), Map.copyOf(context.getHeaders()));
    }
}
