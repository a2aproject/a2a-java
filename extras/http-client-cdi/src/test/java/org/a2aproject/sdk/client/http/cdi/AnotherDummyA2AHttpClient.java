package org.a2aproject.sdk.client.http.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import org.a2aproject.sdk.client.http.A2AHttpClient;

// Second distinct A2AHttpClient type registered alongside DummyA2AHttpClient to produce an
// ambiguous CDI resolution — two beans of the same type with no qualifier to disambiguate.
@ApplicationScoped
class AnotherDummyA2AHttpClient implements A2AHttpClient {

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
