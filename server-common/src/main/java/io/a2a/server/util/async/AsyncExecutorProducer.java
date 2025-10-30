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

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AsyncExecutorProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncExecutorProducer.class);

    @Inject // Needed to work in standard Jakarta runtimes (Quarkus skips this)
    @ConfigProperty(name = "a2a.executor.core-pool-size", defaultValue = "5")
    int corePoolSize;

    @Inject // Needed to work in standard Jakarta runtimes (Quarkus skips this)
    @ConfigProperty(name = "a2a.executor.max-pool-size", defaultValue = "50")
    int maxPoolSize;

    @Inject // Needed to work in standard Jakarta runtimes (Quarkus skips this)
    @ConfigProperty(name = "a2a.executor.keep-alive-seconds", defaultValue = "60")
    long keepAliveSeconds;

    private ExecutorService executor;

    @PostConstruct
    public void init() {
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
