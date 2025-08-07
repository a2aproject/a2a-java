package io.a2a.server.auth;

public interface User {
    boolean isAuthenticated();
    String getUsername();
}
