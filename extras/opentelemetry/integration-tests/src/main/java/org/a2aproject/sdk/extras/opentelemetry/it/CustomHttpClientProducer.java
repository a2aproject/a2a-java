package org.a2aproject.sdk.extras.opentelemetry.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;

/**
 * Produces a custom {@link A2AHttpClient} CDI bean that adds a marker header to
 * every outbound POST request. Used by integration tests to verify that the
 * OpenTelemetry module wraps the application's client instead of replacing it.
 */
@ApplicationScoped
public class CustomHttpClientProducer {

    static final String CUSTOM_HEADER_NAME = "X-Custom-A2A-Client";
    static final String CUSTOM_HEADER_VALUE = "integration-test";

    @Produces
    @ApplicationScoped
    A2AHttpClient customClient() {
        return new MarkerHeaderA2AHttpClient(A2AHttpClientFactory.createExcluding("cdi"));
    }

    /**
     * Delegating {@link A2AHttpClient} that injects a marker header into every
     * POST request for test observability.
     */
    static class MarkerHeaderA2AHttpClient implements A2AHttpClient {

        private final A2AHttpClient delegate;

        MarkerHeaderA2AHttpClient(A2AHttpClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public GetBuilder createGet() {
            return delegate.createGet();
        }

        @Override
        public PostBuilder createPost() {
            PostBuilder builder = delegate.createPost();
            builder.addHeader(CUSTOM_HEADER_NAME, CUSTOM_HEADER_VALUE);
            return builder;
        }

        @Override
        public DeleteBuilder createDelete() {
            return delegate.createDelete();
        }
    }
}
