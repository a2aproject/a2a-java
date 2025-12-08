package io.a2a.server.events;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.a2a.server.tasks.TaskStateProvider;
import io.a2a.spec.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EventQueue implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventQueue.class);

    public static final int DEFAULT_QUEUE_SIZE = 1000;

    private final int queueSize;
    protected final Semaphore semaphore;
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

    static EventQueueBuilder builder(MainEventBus mainEventBus) {
        return new EventQueueBuilder().mainEventBus(mainEventBus);
    }

    public static class EventQueueBuilder {
        private int queueSize = DEFAULT_QUEUE_SIZE;
        private EventEnqueueHook hook;
        private String taskId;
        private List<Runnable> onCloseCallbacks = new java.util.ArrayList<>();
        private TaskStateProvider taskStateProvider;
        private MainEventBus mainEventBus;

        public EventQueueBuilder queueSize(int queueSize) {
            this.queueSize = queueSize;
            return this;
        }

        public EventQueueBuilder hook(EventEnqueueHook hook) {
            this.hook = hook;
            return this;
        }

        public EventQueueBuilder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public EventQueueBuilder addOnCloseCallback(Runnable onCloseCallback) {
            if (onCloseCallback != null) {
                this.onCloseCallbacks.add(onCloseCallback);
            }
            return this;
        }

        public EventQueueBuilder taskStateProvider(TaskStateProvider taskStateProvider) {
            this.taskStateProvider = taskStateProvider;
            return this;
        }

        public EventQueueBuilder mainEventBus(MainEventBus mainEventBus) {
            this.mainEventBus = mainEventBus;
            return this;
        }

        public EventQueue build() {
            // MainEventBus is now REQUIRED - enforce single architectural path
            if (mainEventBus == null) {
                throw new IllegalStateException("MainEventBus is required for EventQueue creation");
            }
            return new MainQueue(queueSize, hook, taskId, onCloseCallbacks, taskStateProvider, mainEventBus);
        }
    }

    public int getQueueSize() {
        return queueSize;
    }

    public abstract void awaitQueuePollerStart() throws InterruptedException ;

    public abstract void signalQueuePollerStarted();

    public void enqueueEvent(Event event) {
        enqueueItem(new LocalEventQueueItem(event));
    }

    public abstract void enqueueItem(EventQueueItem item);

    public abstract EventQueue tap();

    /**
     * Dequeues an EventQueueItem from the queue.
     * <p>
     * This method returns the full EventQueueItem wrapper, allowing callers to check
     * metadata like whether the event is replicated via {@link EventQueueItem#isReplicated()}.
     * </p>
     * <p>
     * Note: MainQueue does not support dequeue operations - only ChildQueues can be consumed.
     * </p>
     *
     * @param waitMilliSeconds the maximum time to wait in milliseconds
     * @return the EventQueueItem, or null if timeout occurs
     * @throws EventQueueClosedException if the queue is closed and empty
     * @throws UnsupportedOperationException if called on MainQueue
     */
    public abstract EventQueueItem dequeueEventItem(int waitMilliSeconds) throws EventQueueClosedException;

    public void taskDone() {
        // TODO Not sure if needed yet. BlockingQueue.poll()/.take() remove the events.
    }

    /**
     * Returns the current size of the queue.
     * <p>
     * For MainQueue: returns the size of the MainEventBus queue (events pending persistence/distribution).
     * For ChildQueue: returns the size of the local consumption queue.
     * </p>
     *
     * @return the number of events currently in the queue
     */
    public abstract int size();

    public abstract void close();

    public abstract void close(boolean immediate);

    /**
     * Close this queue with control over parent notification (ChildQueue only).
     *
     * @param immediate If true, clear all pending events immediately
     * @param notifyParent If true, notify parent (standard behavior). If false, close this queue
     *                     without decrementing parent's reference count (used for non-blocking
     *                     non-final tasks to keep MainQueue alive for resubscription)
     * @throws UnsupportedOperationException if called on MainQueue
     */
    public abstract void close(boolean immediate, boolean notifyParent);

    public boolean isClosed() {
        return closed;
    }

    protected void doClose() {
        doClose(false);
    }

    protected void doClose(boolean immediate) {
        synchronized (this) {
            if (closed) {
                return;
            }
            LOGGER.debug("Closing {} (immediate={})", this, immediate);
            closed = true;
        }
        // Subclasses handle immediate close logic (e.g., ChildQueue clears its local queue)
    }

    static class MainQueue extends EventQueue {
        private final List<ChildQueue> children = new CopyOnWriteArrayList<>();
        private final CountDownLatch pollingStartedLatch = new CountDownLatch(1);
        private final AtomicBoolean pollingStarted = new AtomicBoolean(false);
        private final EventEnqueueHook enqueueHook;
        private final String taskId;
        private final List<Runnable> onCloseCallbacks;
        private final TaskStateProvider taskStateProvider;
        private final MainEventBus mainEventBus;

        MainQueue(int queueSize, EventEnqueueHook hook, String taskId, List<Runnable> onCloseCallbacks, TaskStateProvider taskStateProvider, MainEventBus mainEventBus) {
            super(queueSize);
            this.enqueueHook = hook;
            this.taskId = taskId;
            this.onCloseCallbacks = List.copyOf(onCloseCallbacks);  // Defensive copy
            this.taskStateProvider = taskStateProvider;
            this.mainEventBus = java.util.Objects.requireNonNull(mainEventBus, "MainEventBus is required");
            LOGGER.debug("Created MainQueue for task {} with {} onClose callbacks, TaskStateProvider: {}, MainEventBus configured",
                    taskId, onCloseCallbacks.size(), taskStateProvider != null);
        }

        public EventQueue tap() {
            ChildQueue child = new ChildQueue(this);
            children.add(child);
            return child;
        }

        /**
         * Returns the current number of child queues.
         * Useful for debugging and logging event distribution.
         */
        public int getChildCount() {
            return children.size();
        }

        @Override
        public EventQueueItem dequeueEventItem(int waitMilliSeconds) throws EventQueueClosedException {
            throw new UnsupportedOperationException("MainQueue cannot be consumed directly - use tap() to create a ChildQueue for consumption");
        }

        @Override
        public int size() {
            // Return size of MainEventBus queue (events pending persistence/distribution)
            return mainEventBus.size();
        }

        @Override
        public void enqueueItem(EventQueueItem item) {
            // MainQueue must accept events even when closed to support:
            // 1. Late-arriving replicated events for non-finalized tasks
            // 2. Events enqueued during onClose callbacks (before super.doClose())
            // 3. QueueClosedEvent termination for remote subscribers
            //
            // We bypass the parent's closed check and enqueue directly
            Event event = item.getEvent();

            // Acquire semaphore for backpressure
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Unable to acquire the semaphore to enqueue the event", e);
            }

            LOGGER.debug("Enqueued event {} {}", event instanceof Throwable ? event.toString() : event, this);

            // Submit to MainEventBus for centralized persistence + distribution
            // MainEventBus is guaranteed non-null by constructor requirement
            mainEventBus.submit(taskId, this, item);

            // Trigger replication hook if configured (for inter-process replication)
            if (enqueueHook != null) {
                enqueueHook.onEnqueue(item);
            }
        }

        @Override
        public void awaitQueuePollerStart() throws InterruptedException {
            LOGGER.debug("Waiting for queue poller to start on {}", this);
            pollingStartedLatch.await(10, TimeUnit.SECONDS);
            LOGGER.debug("Queue poller started on {}", this);
        }

        @Override
        public void signalQueuePollerStarted() {
            if (pollingStarted.get()) {
                return;
            }
            LOGGER.debug("Signalling that queue polling started {}", this);
            pollingStartedLatch.countDown();
            pollingStarted.set(true);
          }

        void childClosing(ChildQueue child, boolean immediate) {
            children.remove(child);  // Remove the closing child

            // Close immediately if requested
            if (immediate) {
                LOGGER.debug("MainQueue closing immediately (immediate=true)");
                this.doClose(immediate);
                return;
            }

            // If there are still children, keep queue open
            if (!children.isEmpty()) {
                LOGGER.debug("MainQueue staying open: {} children remaining", children.size());
                return;
            }

            // No children left - check if task is finalized before auto-closing
            if (taskStateProvider != null && taskId != null) {
                boolean isFinalized = taskStateProvider.isTaskFinalized(taskId);
                if (!isFinalized) {
                    LOGGER.debug("MainQueue for task {} has no children, but task is not finalized - keeping queue open for potential resubscriptions", taskId);
                    return;  // Don't close - keep queue open for fire-and-forget or late resubscribes
                }
                LOGGER.debug("MainQueue for task {} has no children and task is finalized - closing queue", taskId);
            } else {
                LOGGER.debug("MainQueue has no children and no TaskStateProvider - closing queue (legacy behavior)");
            }

            this.doClose(immediate);
        }

        /**
         * Distribute event to all ChildQueues.
         * Called by MainEventBusProcessor after TaskStore persistence.
         */
        void distributeToChildren(EventQueueItem item) {
            synchronized (children) {
                int childCount = children.size();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("MainQueue[{}]: Distributing event {} to {} children",
                                taskId, item.getEvent().getClass().getSimpleName(), childCount);
                }
                children.forEach(child -> {
                    LOGGER.debug("MainQueue[{}]: Enqueueing event {} to child queue",
                                taskId, item.getEvent().getClass().getSimpleName());
                    child.internalEnqueueItem(item);
                });
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("MainQueue[{}]: Completed distribution of {} to {} children",
                                taskId, item.getEvent().getClass().getSimpleName(), childCount);
                }
            }
        }

        /**
         * Get the count of active child queues.
         * Used for testing to verify reference counting mechanism.
         *
         * @return number of active child queues
         */
        public int getActiveChildCount() {
            return children.size();
        }

        @Override
        protected void doClose(boolean immediate) {
            // Invoke all callbacks BEFORE closing, so they can still enqueue events
            if (!onCloseCallbacks.isEmpty()) {
                LOGGER.debug("Invoking {} onClose callbacks for task {} BEFORE closing", onCloseCallbacks.size(), taskId);
                for (Runnable callback : onCloseCallbacks) {
                    try {
                        callback.run();
                    } catch (Exception e) {
                        LOGGER.error("Error in onClose callback for task {}", taskId, e);
                    }
                }
            }
            // Now close the queue
            super.doClose(immediate);
        }

        @Override
        public void close() {
            close(false);
        }

        @Override
        public void close(boolean immediate) {
            doClose(immediate);
            if (immediate) {
                // Force-close all remaining children
                children.forEach(child -> child.doClose(immediate));
            }
            children.clear();
        }

        @Override
        public void close(boolean immediate, boolean notifyParent) {
            throw new UnsupportedOperationException("MainQueue does not support notifyParent parameter - use close(boolean) instead");
        }
    }

    static class ChildQueue extends EventQueue {
        private final MainQueue parent;
        private final BlockingQueue<EventQueueItem> queue = new LinkedBlockingDeque<>();

        public ChildQueue(MainQueue parent) {
            this.parent = parent;
        }

        @Override
        public void enqueueEvent(Event event) {
            parent.enqueueEvent(event);
        }

        @Override
        public void enqueueItem(EventQueueItem item) {
            // ChildQueue delegates writes to parent MainQueue
            parent.enqueueItem(item);
        }

        private void internalEnqueueItem(EventQueueItem item) {
            // Internal method called by MainEventBusProcessor to add to local queue
            Event event = item.getEvent();
            if (isClosed()) {
                LOGGER.warn("ChildQueue is closed. Event will not be enqueued. {} {}", this, event);
                return;
            }
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Unable to acquire the semaphore to enqueue the event", e);
            }
            queue.add(item);
            LOGGER.debug("Enqueued event {} {}", event instanceof Throwable ? event.toString() : event, this);
        }

        @Override
        public EventQueueItem dequeueEventItem(int waitMilliSeconds) throws EventQueueClosedException {
            if (isClosed() && queue.isEmpty()) {
                LOGGER.debug("ChildQueue is closed, and empty. Sending termination message. {}", this);
                throw new EventQueueClosedException();
            }
            try {
                if (waitMilliSeconds <= 0) {
                    EventQueueItem item = queue.poll();
                    if (item != null) {
                        Event event = item.getEvent();
                        LOGGER.debug("Dequeued event item (no wait) {} {}", this, event instanceof Throwable ? event.toString() : event);
                        semaphore.release();
                    }
                    return item;
                }
                try {
                    LOGGER.trace("Polling ChildQueue {} (wait={}ms)", System.identityHashCode(this), waitMilliSeconds);
                    EventQueueItem item = queue.poll(waitMilliSeconds, TimeUnit.MILLISECONDS);
                    if (item != null) {
                        Event event = item.getEvent();
                        LOGGER.debug("Dequeued event item (waiting) {} {}", this, event instanceof Throwable ? event.toString() : event);
                        semaphore.release();
                    } else {
                        LOGGER.trace("Dequeue timeout (null) from ChildQueue {}", System.identityHashCode(this));
                    }
                    return item;
                } catch (InterruptedException e) {
                    LOGGER.debug("Interrupted dequeue (waiting) {}", this);
                    Thread.currentThread().interrupt();
                    return null;
                }
            } finally {
                signalQueuePollerStarted();
            }
        }

        @Override
        public EventQueue tap() {
            throw new IllegalStateException("Can only tap the main queue");
        }

        @Override
        public int size() {
            // Return size of local consumption queue
            return queue.size();
        }

        @Override
        public void awaitQueuePollerStart() throws InterruptedException {
            parent.awaitQueuePollerStart();
        }

        @Override
        public void signalQueuePollerStarted() {
            parent.signalQueuePollerStarted();
        }

        @Override
        protected void doClose(boolean immediate) {
            super.doClose(immediate);  // Sets closed flag
            if (immediate) {
                // Immediate close: clear pending events from local queue
                queue.clear();
                LOGGER.debug("Cleared ChildQueue for immediate close: {}", this);
            }
            // For graceful close, let the queue drain naturally through normal consumption
        }

        @Override
        public void close() {
            close(false);
        }

        @Override
        public void close(boolean immediate) {
            close(immediate, true);
        }

        @Override
        public void close(boolean immediate, boolean notifyParent) {
            this.doClose(immediate);           // Close self first
            if (notifyParent) {
                parent.childClosing(this, immediate);  // Notify parent
            } else {
                LOGGER.debug("Closing {} without notifying parent (keeping MainQueue alive)", this);
            }
        }
    }
}
