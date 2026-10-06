package org.a2aproject.sdk.compat03.spec;

import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.util.Assert;
import org.a2aproject.sdk.util.HttpHeaderUtils;
import org.jspecify.annotations.Nullable;

public class A2AClientHTTPError_v0_3 extends A2AClientError_v0_3 {
    private final int code;
    private final String message;
    private final @Nullable String responseBody;
    private final Map<String, List<String>> responseHeaders;

    public A2AClientHTTPError_v0_3(int code, String message, Object data) {
        this(code, message, null, Map.of());
    }

    public A2AClientHTTPError_v0_3(int code, String message, @Nullable String responseBody,
            Map<String, List<String>> responseHeaders) {
        Assert.checkNotNullParam("message", message);
        Assert.checkNotNullParam("responseHeaders", responseHeaders);
        this.code = code;
        this.message = message;
        this.responseBody = responseBody;
        this.responseHeaders = HttpHeaderUtils.copyOfCaseInsensitive(responseHeaders);
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
    public @Nullable String getResponseBody() {
        return responseBody;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

}
