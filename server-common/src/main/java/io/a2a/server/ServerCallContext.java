package io.a2a.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.a2a.server.auth.User;

public class ServerCallContext {
    // TODO Not totally sure yet about these field types
    private final Map<Object, Object> modelConfig = new ConcurrentHashMap<>();
    private final Map<String, Object> state;
    private final User user;

    public ServerCallContext(User user, Map<String, Object> state) {
        this.user = user;
        this.state = state;
    }

    public Map<String, Object> getState() {
        return state;
    }

    public User getUser() {
        return user;
    }
}
