package io.a2a.server.util.async;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.a2a.server.config.A2AConfigProvider;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AsyncExecutorProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncExecutorProducer.class);
    private static final String A2A_EXECUTOR_CORE_POOL_SIZE = "a2a.executor.core-pool-size";
    private static final String A2A_EXECUTOR_MAX_POOL_SIZE = "a2a.executor.max-pool-size";
    private static final String A2A_EXECUTOR_KEEP_ALIVE_SECONDS = "a2a.executor.keep-alive-seconds";
    private static final String A2A_EXECUTOR_QUEUE_CAPACITY = "a2a.executor.queue-capacity";

    @Inject
    A2AConfigProvider configProvider;

    /**
     * Core pool size for async agent execution thread pool.
     * <p>
     * Property: {@code a2a.executor.core-pool-size}<br>
     * Default: 5<br>
     * Note: Property override requires a configurable {@link A2AConfigProvider} on the classpath.
     */
    int corePoolSize;

    /**
     * Maximum pool size for async agent execution thread pool.
     * <p>
     * Property: {@code a2a.executor.max-pool-size}<br>
     * Default: 50<br>
     * Note: Property override requires a configurable {@link A2AConfigProvider} on the classpath.
     */
    int maxPoolSize;

    /**
     * Keep-alive time for idle threads (seconds).
     * <p>
     * Property: {@code a2a.executor.keep-alive-seconds}<br>
     * Default: 60<br>
     * Note: Property override requires a configurable {@link A2AConfigProvider} on the classpath.
     */
    long keepAliveSeconds;

    /**
     * Queue capacity for pending tasks.
     * <p>
     * Property: {@code a2a.executor.queue-capacity}<br>
     * Default: 100<br>
     * Note: Must be bounded to allow pool growth to maxPoolSize.
     * When queue is full, new threads are created up to maxPoolSize.
     */
    int queueCapacity;

    private @Nullable ExecutorService executor;

    @PostConstruct
    public void init() {
        corePoolSize = Integer.parseInt(configProvider.getValue(A2A_EXECUTOR_CORE_POOL_SIZE));
        maxPoolSize = Integer.parseInt(configProvider.getValue(A2A_EXECUTOR_MAX_POOL_SIZE));
        keepAliveSeconds = Long.parseLong(configProvider.getValue(A2A_EXECUTOR_KEEP_ALIVE_SECONDS));
        queueCapacity = Integer.parseInt(configProvider.getValue(A2A_EXECUTOR_QUEUE_CAPACITY));

        LOGGER.info("Initializing async executor: corePoolSize={}, maxPoolSize={}, keepAliveSeconds={}, queueCapacity={}",
                corePoolSize, maxPoolSize, keepAliveSeconds, queueCapacity);

        // CRITICAL: Use ArrayBlockingQueue (bounded) instead of LinkedBlockingQueue (unbounded).
        // With unbounded queue, ThreadPoolExecutor NEVER grows beyond corePoolSize because the
        // queue never fills. This causes executor pool exhaustion during concurrent requests when
        // EventConsumer polling threads hold all core threads and agent tasks queue indefinitely.
        // Bounded queue enables pool growth: when queue is full, new threads are created up to
        // maxPoolSize, preventing agent execution starvation.
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new A2AThreadFactory()
        );

        // CRITICAL: Allow core threads to timeout after keepAliveSeconds when idle.
        // By default, ThreadPoolExecutor only times out threads above corePoolSize.
        // Without this, core threads accumulate during testing and never clean up.
        // This is essential for streaming scenarios where many short-lived tasks create threads
        // for agent execution and cleanup callbacks, but those threads remain idle afterward.
        tpe.allowCoreThreadTimeOut(true);

        executor = tpe;
    }

    @PreDestroy
    public void close() {
        if (executor == null) {
            return;
        }
        LOGGER.info("Shutting down async executor");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                LOGGER.warn("Executor did not terminate in 10 seconds, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for executor shutdown", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Produces
    @Internal
    public Executor produce() {
        if (executor == null) {
            throw new IllegalStateException("Executor not initialized - @PostConstruct not called");
        }
        return executor;
    }

    /**
     * Log current executor pool statistics for diagnostics.
     * Useful for debugging pool exhaustion or sizing issues.
     */
    public void logPoolStats() {
        if (executor instanceof ThreadPoolExecutor tpe) {
            LOGGER.info("Executor pool stats: active={}/{}, queued={}/{}, completed={}, total={}",
                    tpe.getActiveCount(),
                    tpe.getPoolSize(),
                    tpe.getQueue().size(),
                    queueCapacity,
                    tpe.getCompletedTaskCount(),
                    tpe.getTaskCount());
        }
    }

    private static class A2AThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix = "a2a-agent-executor-";

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(false);
            return t;
        }
    }

}
