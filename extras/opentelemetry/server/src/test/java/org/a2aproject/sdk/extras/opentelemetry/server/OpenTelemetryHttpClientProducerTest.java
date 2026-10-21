package org.a2aproject.sdk.extras.opentelemetry.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.OpenTelemetry;
import jakarta.enterprise.inject.Instance;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.junit.jupiter.api.Test;

class OpenTelemetryHttpClientProducerTest {

    /**
     * Verifies that when no application-provided {@link A2AHttpClient} bean
     * exists, the producer falls back to a factory-created delegate.
     *
     * <p>Note: this test calls the real {@link org.a2aproject.sdk.client.http.A2AHttpClientFactory}
     * via {@code ServiceLoader}. The JDK HTTP client provider ({@code http-client} module) must be
     * on this module's test classpath for the factory to resolve a delegate.
     */
    @Test
    @SuppressWarnings("unchecked")
    void createPropagatingClient_noUserBean_fallsBackToFactory() {
        OpenTelemetry mockOTel = mock(OpenTelemetry.class);
        OpenTelemetryHttpClientProducer producer = new OpenTelemetryHttpClientProducer(mockOTel);

        Instance<A2AHttpClient> emptyInstance = mock(Instance.class);
        when(emptyInstance.isResolvable()).thenReturn(false);

        A2AHttpClient client = producer.createPropagatingClient(emptyInstance);

        assertNotNull(client);
        assertInstanceOf(OpenTelemetryA2AHttpClient.class, client,
                "Producer must wrap a factory-created delegate in OpenTelemetryA2AHttpClient");
    }

    /**
     * Verifies that when an application-provided {@link A2AHttpClient} bean
     * exists, the producer wraps it instead of creating a new one from the factory.
     */
    @Test
    @SuppressWarnings("unchecked")
    void createPropagatingClient_withUserBean_wrapsUserClient() {
        OpenTelemetry mockOTel = mock(OpenTelemetry.class);
        OpenTelemetryHttpClientProducer producer = new OpenTelemetryHttpClientProducer(mockOTel);

        A2AHttpClient userClient = mock(A2AHttpClient.class);
        Instance<A2AHttpClient> userInstance = mock(Instance.class);
        when(userInstance.isResolvable()).thenReturn(true);
        when(userInstance.get()).thenReturn(userClient);

        A2AHttpClient client = producer.createPropagatingClient(userInstance);

        assertNotNull(client);
        assertInstanceOf(OpenTelemetryA2AHttpClient.class, client,
                "Producer must wrap the user's client in OpenTelemetryA2AHttpClient");
        verify(userInstance).get();
    }

    @Test
    @SuppressWarnings("unchecked")
    void createContextPropagatingExecutor_noUserBean_createsDefaultPool() {
        OpenTelemetry mockOTel = mock(OpenTelemetry.class);
        OpenTelemetryHttpClientProducer producer = new OpenTelemetryHttpClientProducer(mockOTel);

        Instance<Executor> emptyInstance = mock(Instance.class);
        when(emptyInstance.isResolvable()).thenReturn(false);

        Executor executor = producer.createContextPropagatingExecutor(emptyInstance);

        try {
            assertNotNull(executor, "Produced executor must not be null");
        } finally {
            producer.shutdownDispatchPool();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void createContextPropagatingExecutor_withUserBean_wrapsUserExecutor() {
        OpenTelemetry mockOTel = mock(OpenTelemetry.class);
        OpenTelemetryHttpClientProducer producer = new OpenTelemetryHttpClientProducer(mockOTel);

        AtomicInteger executionCount = new AtomicInteger();
        Executor userExecutor = command -> {
            executionCount.incrementAndGet();
            command.run();
        };
        Instance<Executor> userInstance = mock(Instance.class);
        when(userInstance.isResolvable()).thenReturn(true);
        when(userInstance.get()).thenReturn(userExecutor);

        Executor wrappedExecutor = producer.createContextPropagatingExecutor(userInstance);

        assertNotNull(wrappedExecutor, "Produced executor must not be null");
        verify(userInstance).get();

        wrappedExecutor.execute(() -> {});
        assertEquals(1, executionCount.get(),
                "Wrapped executor must delegate to the user-provided executor");
    }
}
