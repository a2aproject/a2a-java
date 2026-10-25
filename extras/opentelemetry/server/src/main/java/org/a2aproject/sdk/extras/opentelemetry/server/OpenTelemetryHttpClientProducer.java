package org.a2aproject.sdk.extras.opentelemetry.server;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpClientFactory;
import org.a2aproject.sdk.server.Instrumented;
import org.a2aproject.sdk.server.tasks.BasePushNotificationSender;
import org.jspecify.annotations.Nullable;

/**
 * CDI producer that creates an {@link OpenTelemetryA2AHttpClient} wrapping
 * the application's HTTP client delegate with W3C trace-context propagation.
 *
 * <p>When this module is on the classpath and CDI discovers this bean,
 * {@link BasePushNotificationSender} will use the produced
 * {@link Instrumented @Instrumented} client instead of calling
 * {@link A2AHttpClientFactory#create()} directly, ensuring that outbound
 * push-notification HTTP requests carry {@code traceparent} and
 * {@code tracestate} headers.
 *
 * <p>The delegate is resolved from CDI first: if the application provides a
 * custom unqualified {@link A2AHttpClient} bean (e.g. one configured with
 * mTLS, a proxy, or custom timeouts), this producer wraps that bean so the
 * application's HTTP configuration is preserved. When no CDI-provided bean
 * exists, the delegate falls back to
 * {@link A2AHttpClientFactory#createExcluding(String...)} with the
 * {@code "cdi"} provider excluded (to avoid the self-delegation cycle
 * that would otherwise occur when the CDI provider re-enters the container).
 *
 * <p>The produced client is qualified with {@link Instrumented} so it
 * does not compete with the application's own {@link A2AHttpClient} bean in
 * default CDI resolution. {@link BasePushNotificationSender} prefers a
 * {@code @Instrumented} client when one is available.
 */
@ApplicationScoped
public class OpenTelemetryHttpClientProducer {

    /**
     * Name of the CDI HTTP client provider to exclude when falling back to the factory,
     * preventing the recursive self-delegation cycle described in the class Javadoc.
     */
    static final String CDI_PROVIDER_NAME = "cdi";

    private final OpenTelemetry openTelemetry;
    private volatile @Nullable ExecutorService dispatchPool;

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
     * <p>The delegate is resolved in order of preference:
     * <ol>
     *   <li>An application-provided unqualified {@link A2AHttpClient} CDI bean
     *       (custom mTLS, proxy, auth, etc.)</li>
     *   <li>A factory-created client via
     *       {@link A2AHttpClientFactory#createExcluding(String...)} with the
     *       {@code "cdi"} provider excluded to prevent recursive CDI
     *       self-delegation</li>
     * </ol>
     *
     * @param httpClient CDI lookup handle for the application's unqualified
     *                   {@link A2AHttpClient} bean (injected by the container)
     * @return a propagating HTTP client wrapping the resolved delegate
     */
    @Produces
    @ApplicationScoped
    @Instrumented
    A2AHttpClient createPropagatingClient(Instance<A2AHttpClient> httpClient) {
        A2AHttpClient delegate = httpClient.isResolvable()
                ? httpClient.get()
                : A2AHttpClientFactory.createExcluding(CDI_PROVIDER_NAME);
        return new OpenTelemetryA2AHttpClient(delegate, openTelemetry);
    }

    /**
     * Produces an {@link Executor} that propagates the current OpenTelemetry
     * {@link Context} to submitted tasks, ensuring trace context survives
     * {@code CompletableFuture.supplyAsync()} boundaries in push notification dispatch.
     *
     * <p>The delegate is resolved in order of preference:
     * <ol>
     *   <li>An application-provided
     *       {@link Named @Named}{@code ("a2a.pushNotificationDispatch")}
     *       {@link Executor} CDI bean (custom thread pool, sizing, etc.)</li>
     *   <li>A dedicated fixed thread pool sized at
     *       {@code max(4, 2 * availableProcessors)} to isolate I/O-bound webhook
     *       HTTP calls from {@code ForkJoinPool.commonPool()}</li>
     * </ol>
     *
     * <p>In both cases the delegate is wrapped with
     * {@link Context#taskWrapping(Executor)} so that OpenTelemetry context
     * propagation is transparent to the application.
     *
     * @param userExecutor CDI lookup handle for the application's
     *                     {@code @Named("a2a.pushNotificationDispatch")} executor
     * @return a context-propagating executor wrapping the resolved delegate
     */
    @Produces
    @ApplicationScoped
    @Instrumented
    Executor createContextPropagatingExecutor(
            @Named(BasePushNotificationSender.DISPATCH_EXECUTOR_NAME) Instance<Executor> userExecutor) {
        if (userExecutor.isResolvable()) {
            return Context.taskWrapping(userExecutor.get());
        }
        int threads = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "a2a-push-dispatch-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
        ExecutorService pool = Executors.newFixedThreadPool(threads, threadFactory);
        dispatchPool = pool;
        return Context.taskWrapping(pool);
    }

    @PreDestroy
    void shutdownDispatchPool() {
        ExecutorService pool = dispatchPool;
        if (pool != null) {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
