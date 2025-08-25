package io.a2a.client.transport.spi.interceptors;

import java.util.Map;

public class PayloadAndHeaders {

    private final Object payload;
    private final Map<String, String> headers;

    public PayloadAndHeaders(Object payload, Map<String, String> headers) {
        this.payload = payload;
        this.headers = headers;
    }

    public Object getPayload() {
        return payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
