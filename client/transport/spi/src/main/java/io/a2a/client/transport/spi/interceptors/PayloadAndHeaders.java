package io.a2a.client.transport.spi.interceptors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public class PayloadAndHeaders {

    private final  @Nullable Object payload;
    private final Map<String, String> headers;

    public PayloadAndHeaders(@Nullable Object payload, Map<String, String> headers) {
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
