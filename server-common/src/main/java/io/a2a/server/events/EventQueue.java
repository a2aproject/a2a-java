package io.a2a.server.events;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.a2a.spec.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EventQueue implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventQueue.class);

    public static final int DEFAULT_QUEUE_SIZE = 1000;

    private final int queueSize;
    private final BlockingQueue<Event> queue = new LinkedBlockingDeque<>();
    private final Semaphore semaphore;
    private volatile boolean closed = false;



    protected EventQueue() {
        this(DEFAULT_QUEUE_SIZE);
    }

    protected EventQueue(int queueSize) {
        if (queueSize <= 0) {
            throw new IllegalArgumentException("Queue size must be greater than 0");
        }
        this.queueSize = queueSize;
        this.semaphore = new Semaphore(queueSize, true);
        LOGGER.trace("Creating {} with queue size: {}", this, queueSize);
    }

    protected EventQueue(EventQueue parent) {
        this(DEFAULT_QUEUE_SIZE);
        LOGGER.trace("Creating {}, parent: {}", this, parent);
    }

    public static EventQueue create() {
        return new MainQueue();
    }

    public static EventQueue create(int queueSize) {
        return new MainQueue(queueSize);
    }

    public int getQueueSize() {
        return queueSize;
    }

    public abstract void awaitQueuePollerStart() throws InterruptedException ;

    abstract void signalQueuePollerStarted();

    public void enqueueEvent(Event event) {
        if (closed) {
            LOGGER.warn("Queue is closed. Event will not be enqueued. {} {}", this, event);
            return;
        }
        // Call toString() since for errors we don't really want the full stacktrace
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Unable to acquire the semaphore to enqueue the event", e);
        }
        queue.add(event);
        LOGGER.debug("Enqueued event {} {}", event instanceof Throwable ? event.toString() : event, this);
    }

    abstract EventQueue tap();

    public Event dequeueEvent(int waitMilliSeconds) throws EventQueueClosedException {
        if (closed && queue.isEmpty()) {
            LOGGER.debug("Queue is closed, and empty. Sending termination message. {}", this);
            throw new EventQueueClosedException();
        }
        try {
            if (waitMilliSeconds <= 0) {
                Event event = queue.poll();
                if (event != null) {
                    // Call toString() since for errors we don't really want the full stacktrace
                    LOGGER.debug("Dequeued event (no wait) {} {}", this, event instanceof Throwable ? event.toString() : event);
                    semaphore.release();
                }
                return event;
            }
            try {
                Event event = queue.poll(waitMilliSeconds, TimeUnit.MILLISECONDS);
                if (event != null) {
                    // Call toString() since for errors we don't really want the full stacktrace
                    LOGGER.debug("Dequeued event (waiting) {} {}", this, event instanceof Throwable ? event.toString() : event);
                    semaphore.release();
                }
                return event;
            } catch (InterruptedException e) {
                LOGGER.debug("Interrupted dequeue (waiting) {}", this);
                Thread.currentThread().interrupt();
                return null;
            }
        } finally {
            signalQueuePollerStarted();
        }
    }

    public void taskDone() {
        // TODO Not sure if needed yet. BlockingQueue.poll()/.take() remove the events.
    }

    public abstract void close();

    public abstract void close(boolean immediate);

    public boolean isClosed() {
        return closed;
    }

    public void doClose() {
        doClose(false);
    }

    public void doClose(boolean immediate) {
        synchronized (this) {
            if (closed) {
                return;
            }
            LOGGER.debug("Closing {} (immediate={})", this, immediate);
            closed = true;
        }

        if (immediate) {
            // Immediate close: clear pending events
            queue.clear();
            LOGGER.debug("Cleared queue for immediate close: {}", this);
        }
        // For graceful close, let the queue drain naturally through normal consumption
    }

    static class MainQueue extends EventQueue {
        private final List<ChildQueue> children = new CopyOnWriteArrayList<>();
        private final CountDownLatch pollingStartedLatch = new CountDownLatch(1);
        private final AtomicBoolean pollingStarted = new AtomicBoolean(false);

        MainQueue() {
            super();
        }

        MainQueue(int queueSize) {
            super(queueSize);
        }

        EventQueue tap() {
            ChildQueue child = new ChildQueue(this);
            children.add(child);
            return child;
        }

        public void enqueueEvent(Event event) {
            super.enqueueEvent(event);
            children.forEach(eq -> eq.internalEnqueueEvent(event));
        }

        @Override
        public void awaitQueuePollerStart() throws InterruptedException {
            LOGGER.debug("Waiting for queue poller to start on {}", this);
            pollingStartedLatch.await(10, TimeUnit.SECONDS);
            LOGGER.debug("Queue poller started on {}", this);
        }

        @Override
        void signalQueuePollerStarted() {
            if (pollingStarted.get()) {
                return;
            }
            LOGGER.debug("Signalling that queue polling started {}", this);
            pollingStartedLatch.countDown();
            pollingStarted.set(true);
          }

        @Override
        public void close() {
            close(false);
        }

        @Override
        public void close(boolean immediate) {
            doClose(immediate);
            children.forEach(child -> child.doClose(immediate));
        }
    }

    static class ChildQueue extends EventQueue {
        private final MainQueue parent;

        public ChildQueue(MainQueue parent) {
            this.parent = parent;
        }

        @Override
        public void enqueueEvent(Event event) {
            parent.enqueueEvent(event);
        }

        private void internalEnqueueEvent(Event event) {
            super.enqueueEvent(event);
        }

        @Override
        EventQueue tap() {
            throw new IllegalStateException("Can only tap the main queue");
        }

        @Override
        public void awaitQueuePollerStart() throws InterruptedException {
            parent.awaitQueuePollerStart();
        }

        @Override
        void signalQueuePollerStarted() {
            parent.signalQueuePollerStarted();
        }

        @Override
        public void close() {
            parent.close();
        }

        @Override
        public void close(boolean immediate) {
            parent.close(immediate);
        }
    }
}
