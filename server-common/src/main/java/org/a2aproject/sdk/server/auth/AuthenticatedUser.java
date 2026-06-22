package org.a2aproject.sdk.server.auth;

import org.a2aproject.sdk.util.Assert;

public record AuthenticatedUser(String username) implements User {
    public AuthenticatedUser {
        Assert.checkNotNullParam("username", username);
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
