package io.a2a.server.util.async;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AsyncExecutorProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncExecutorProducer.class);

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

    private ExecutorService executor;

    @PostConstruct
    public void init() {
        corePoolSize = Integer.parseInt(configProvider.getValue("a2a.executor.core-pool-size"));
        maxPoolSize = Integer.parseInt(configProvider.getValue("a2a.executor.max-pool-size"));
        keepAliveSeconds = Long.parseLong(configProvider.getValue("a2a.executor.keep-alive-seconds"));

        LOGGER.info("Initializing async executor: corePoolSize={}, maxPoolSize={}, keepAliveSeconds={}",
                corePoolSize, maxPoolSize, keepAliveSeconds);

        executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new A2AThreadFactory()
        );
    }

    @PreDestroy
    public void close() {
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
        return executor;
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
