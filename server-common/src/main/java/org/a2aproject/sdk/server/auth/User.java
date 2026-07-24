package org.a2aproject.sdk.server.auth;

import org.jspecify.annotations.Nullable;

public interface User {
    boolean isAuthenticated();
    String getUsername();

    default @Nullable Object getAttribute(String key) {
        return null;
    }
}
