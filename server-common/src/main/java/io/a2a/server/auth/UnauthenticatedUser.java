package io.a2a.server.auth;

public class UnauthenticatedUser implements User {

    public static UnauthenticatedUser INSTANCE = new UnauthenticatedUser();

    private UnauthenticatedUser() {
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public String getUsername() {
        return "";
    }
}
