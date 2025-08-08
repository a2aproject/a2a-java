package io.a2a.client;

import java.util.Map;

public class PayloadAndHeaders {

    private final Object payload;
    private final Map<String, String> httpHeaders;

    public PayloadAndHeaders(Object payload, Map<String, String> httpHeaders) {
        this.payload = payload;
        this.httpHeaders = httpHeaders;
    }

    public Object getPayload() {
        return payload;
    }

    public Map<String, String> getHttpHeaders() {
        return httpHeaders;
    }
}
