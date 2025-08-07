package io.a2a.server;

import io.a2a.spec.JSONRPCError;

public class JSONRPCException extends Exception{
    private final JSONRPCError error;

    public JSONRPCException(JSONRPCError error) {
        this.error = error;
    }

    public JSONRPCError getError() {
        return error;
    }
}
