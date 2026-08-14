package org.a2aproject.sdk.client.http.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import org.a2aproject.sdk.client.http.A2AHttpClient;

@ApplicationScoped
class DummyA2AHttpClient implements A2AHttpClient {

    @Override
    public GetBuilder createGet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PostBuilder createPost() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DeleteBuilder createDelete() {
        throw new UnsupportedOperationException();
    }
}
