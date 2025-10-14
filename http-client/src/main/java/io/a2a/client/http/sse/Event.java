package io.a2a.client.http.sse;

public abstract class Event {

    enum Type {
        COMMENT,
        DATA,
    }

    abstract Type getType();
}
