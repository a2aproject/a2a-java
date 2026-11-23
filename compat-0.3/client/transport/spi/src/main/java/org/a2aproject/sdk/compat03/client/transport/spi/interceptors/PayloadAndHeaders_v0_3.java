package org.a2aproject.sdk.compat03.client.transport.spi.interceptors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

public class PayloadAndHeaders_v0_3 {

    private final Object payload;
    private final Map<String, String> headers;

    /**
     * Creates an interceptor result.
     *
     * @param payload the non-null request payload
     * @param headers the request headers, or null for an empty map
     * @throws IllegalArgumentException if payload is null
     */
    public PayloadAndHeaders_v0_3(Object payload, @Nullable Map<String, String> headers) {
        this.payload = Assert.checkNotNullParam("payload", payload);
        this.headers = headers == null ? Collections.emptyMap() : new HashMap<>(headers);
    }

    public Object getPayload() {
        return payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
