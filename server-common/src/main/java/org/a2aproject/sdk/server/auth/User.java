package org.a2aproject.sdk.server.auth;

public interface User {
    boolean isAuthenticated();
    String getUsername();
}
