package io.a2a.client;

import java.util.Map;

public class ClientCallContext {

    private final Map<String, Object> state;
    private final Map<String, String> httpHeaders;

    public ClientCallContext(Map<String, Object> state, Map<String, String> httpHeaders) {
        this.state = state;
        this.httpHeaders = httpHeaders;
    }

    public Map<String, Object> getState() {
        return state;
    }

    public Map<String, String> getHttpHeaders() {
        return httpHeaders;
    }
}
