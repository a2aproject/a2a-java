package org.a2aproject.sdk.extras.opentelemetry.server;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.server.tasks.BasePushNotificationSender;

/**
 * CDI producer that creates an {@link OpenTelemetryA2AHttpClient} wrapping the
 * factory-resolved {@link A2AHttpClient} with W3C trace-context propagation.
 *
 * <p>When this module is on the classpath and CDI discovers this bean,
 * {@link org.a2aproject.sdk.server.tasks.BasePushNotificationSender} will use the
 * produced client instead of calling {@link A2AHttpClientFactory#create()} directly,
 * ensuring that outbound push-notification HTTP requests carry {@code traceparent}
 * and {@code tracestate} headers.
 *
 * <p><strong>CDI ambiguity:</strong> The {@link A2AHttpClient} bean produced by
 * {@link #createPropagatingClient()} has no qualifier. If the application also
 * produces an unqualified {@code A2AHttpClient} bean (e.g. a custom Vert.x or
 * Android client), CDI will report an {@code AmbiguousResolutionException} at
 * startup. In that case, use {@code @Alternative @Priority} on one of the
 * producers to resolve the conflict, or inject the custom client with a qualifier.
 */
@ApplicationScoped
public class OpenTelemetryHttpClientProducer {

    private final OpenTelemetry openTelemetry;

    @SuppressWarnings("NullAway")
    protected OpenTelemetryHttpClientProducer() {
        this.openTelemetry = null;
    }

    @Inject
    public OpenTelemetryHttpClientProducer(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    /**
     * Produces an {@link A2AHttpClient} that injects trace-context headers into
     * every outbound request.
     *
     * @return a propagating HTTP client wrapping the default factory client
     */
    @Produces
    @ApplicationScoped
    A2AHttpClient createPropagatingClient() {
        return new OpenTelemetryA2AHttpClient(A2AHttpClientFactory.create(), openTelemetry);
    }

    /**
     * Produces an {@link Executor} that propagates the current OpenTelemetry
     * {@link Context} to submitted tasks, ensuring trace context survives
     * {@code CompletableFuture.supplyAsync()} boundaries in push notification dispatch.
     *
     * @return a context-propagating executor wrapping the common ForkJoinPool
     */
    @Produces
    @ApplicationScoped
    @Named(BasePushNotificationSender.DISPATCH_EXECUTOR_NAME)
    Executor createContextPropagatingExecutor() {
        return Context.taskWrapping(ForkJoinPool.commonPool());
    }
}
