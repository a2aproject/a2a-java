package io.a2a.spec;

public class A2AProtocolError extends A2AError {

    private final String url;

    public A2AProtocolError(Integer code, String message, Object data, String url) {
        super(code, message, data);
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
