package org.a2aproject.sdk.integrations.springboot.server.autoconfigure;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class A2AEventConsumerThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, "a2a-event-consumer-" + threadNumber.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }
}
