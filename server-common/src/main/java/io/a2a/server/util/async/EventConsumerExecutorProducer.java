package io.a2a.server.util.async;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Qualifier;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Produces a dedicated executor for EventConsumer polling threads.
 * <p>
 * CRITICAL: EventConsumer polling must use a separate executor from AgentExecutor because:
 * <ul>
 * <li>EventConsumer threads are I/O-bound (blocking on queue.poll()), not CPU-bound</li>
 * <li>One EventConsumer thread needed per active queue (can be 100+ concurrent)</li>
 * <li>Threads are mostly idle, waiting for events</li>
 * <li>Using the same bounded pool as AgentExecutor causes deadlock when pool exhausted</li>
 * </ul>
 * <p>
 * Uses a cached thread pool (unbounded) with automatic thread reclamation:
 * <ul>
 * <li>Creates threads on demand as EventConsumers start</li>
 * <li>Idle threads automatically terminated after 10 seconds</li>
 * <li>No queue saturation since threads are created as needed</li>
 * </ul>
 */
@ApplicationScoped
public class EventConsumerExecutorProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventConsumerExecutorProducer.class);

    /**
     * Qualifier annotation for EventConsumer executor injection.
     */
    @Retention(RUNTIME)
    @Target({METHOD, FIELD, PARAMETER, TYPE})
    @Qualifier
    public @interface EventConsumerExecutor {
    }

    /**
     * Thread factory for EventConsumer threads.
     */
    private static class EventConsumerThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "a2a-event-consumer-" + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }

    private @Nullable ExecutorService executor;

    @Produces
    @EventConsumerExecutor
    @ApplicationScoped
    public Executor eventConsumerExecutor() {
        // Cached thread pool with 10s idle timeout (reduced from default 60s):
        // - Creates threads on demand as EventConsumers start
        // - Reclaims idle threads after 10s to prevent accumulation during fast test execution
        // - Perfect for I/O-bound EventConsumer polling which blocks on queue.poll()
        // - 10s timeout balances thread reuse (production) vs cleanup (testing)
        executor = new ThreadPoolExecutor(
                0,                              // corePoolSize - no core threads
                Integer.MAX_VALUE,              // maxPoolSize - unbounded
                10, TimeUnit.SECONDS,           // keepAliveTime - 10s idle timeout
                new SynchronousQueue<>(),       // queue - same as cached pool
                new EventConsumerThreadFactory()
        );

        LOGGER.info("Initialized EventConsumer executor: cached thread pool (unbounded, 10s idle timeout)");

        return executor;
    }
}
