package org.a2aproject.sdk.compat03.client.transport.spi.interceptors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public class PayloadAndHeaders_v0_3 {

    private final  @Nullable Object payload;
    private final Map<String, String> headers;

    public PayloadAndHeaders_v0_3(@Nullable Object payload, @Nullable Map<String, String> headers) {
        this.payload = payload;
        this.headers = headers == null ? Collections.emptyMap() : new HashMap<>(headers);
    }

    public @Nullable Object getPayload() {
        return payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
