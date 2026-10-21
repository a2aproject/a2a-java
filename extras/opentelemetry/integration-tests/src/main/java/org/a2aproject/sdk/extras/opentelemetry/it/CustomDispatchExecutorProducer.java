package org.a2aproject.sdk.extras.opentelemetry.it;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.a2aproject.sdk.server.tasks.BasePushNotificationSender;

/**
 * Produces a custom {@link Named @Named} dispatch {@link Executor} CDI bean
 * that uses a distinctive thread name prefix. Used by integration tests to
 * verify that the OpenTelemetry module wraps the application's executor with
 * context propagation instead of replacing it.
 *
 * <p>An {@link AtomicInteger} counter tracks how many tasks have been
 * submitted, allowing tests to confirm the custom executor was actually used.
 */
@ApplicationScoped
public class CustomDispatchExecutorProducer {

    static final String CUSTOM_THREAD_PREFIX = "custom-dispatch-";

    private final AtomicInteger executionCount = new AtomicInteger();
    private final ExecutorService pool;

    public CustomDispatchExecutorProducer() {
        AtomicInteger threadCounter = new AtomicInteger();
        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, CUSTOM_THREAD_PREFIX + threadCounter.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
        this.pool = Executors.newFixedThreadPool(2, threadFactory);
    }

    @Produces
    @ApplicationScoped
    @Named(BasePushNotificationSender.DISPATCH_EXECUTOR_NAME)
    Executor customDispatchExecutor() {
        return command -> {
            executionCount.incrementAndGet();
            pool.execute(command);
        };
    }

    /**
     * Returns the number of tasks submitted through this executor.
     */
    public int getExecutionCount() {
        return executionCount.get();
    }

    /**
     * Resets the execution counter.
     */
    public void resetExecutionCount() {
        executionCount.set(0);
    }

    @PreDestroy
    void shutdown() {
        pool.shutdownNow();
    }
}
