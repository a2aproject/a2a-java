package io.a2a.spec;

import io.a2a.util.Assert;

public class A2AClientHTTPError extends A2AClientError {
    private final int code;
    private final String message;

    public A2AClientHTTPError(int code, String message, Object data) {
        Assert.checkNotNullParam("code", code);
        Assert.checkNotNullParam("message", message);
        this.code = code;
        this.message = message;
    }

    /**
     * Gets the error code
     *
     * @return the error code
     */
    public int getCode() {
        return code;
    }

    /**
     * Gets the error message
     *
     * @return the error message
     */
    @Override
    public String getMessage() {
        return message;
    }
}
