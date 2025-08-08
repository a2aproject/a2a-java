package io.a2a.client.config;

import java.util.Map;

/**
 * A context passed with each client call, allowing for call-specific.
 * configuration and data passing. Such as authentication details or
 * request deadlines.
 */
public class ClientCallContext {

    private final Map<String, Object> state;
    private final Map<String, String> headers;

    public ClientCallContext(Map<String, Object> state, Map<String, String> headers) {
        this.state = state;
        this.headers = headers;
    }

    public Map<String, Object> getState() {
        return state;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
