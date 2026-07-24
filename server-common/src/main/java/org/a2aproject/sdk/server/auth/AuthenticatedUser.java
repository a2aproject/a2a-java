package org.a2aproject.sdk.server.auth;

import org.jspecify.annotations.Nullable;
import org.a2aproject.sdk.util.Assert;

import java.util.Map;

public record AuthenticatedUser(
        String username,
        Map<String, Object> attributes
) implements User {

    public AuthenticatedUser(String username) {
        this(username, Map.of());
    }

    public AuthenticatedUser {
        Assert.checkNotNullParam("username", username);
        Assert.checkNotNullParam("attributes", attributes);
        attributes = Map.copyOf(attributes);
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public @Nullable Object getAttribute(String key) {
        return attributes.get(key);
    }
}
