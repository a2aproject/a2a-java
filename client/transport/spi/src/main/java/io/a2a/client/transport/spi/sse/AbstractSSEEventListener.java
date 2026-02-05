package io.a2a.client.transport.spi.sse;

import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import org.jspecify.annotations.Nullable;

/**
 * Abstract base class for SSE event listeners that provides common functionality
 * for handling Server-Sent Events across different transport implementations.
 * <p>
 * This class implements the Template Method pattern, where subclasses provide
 * the specific message parsing logic while the base class handles common concerns
 * like error handling and connection lifecycle management.
 */
public abstract class AbstractSSEEventListener {

    private static final Logger log = Logger.getLogger(AbstractSSEEventListener.class.getName());

    private final Consumer<StreamingEventKind> eventHandler;
    private final @Nullable Consumer<Throwable> errorHandler;

    /**
     * Creates a new SSE event listener with the specified handlers.
     *
     * @param eventHandler Handler for processing streaming events
     * @param errorHandler Optional handler for processing errors
     */
    protected AbstractSSEEventListener(Consumer<StreamingEventKind> eventHandler,
                                   @Nullable Consumer<Throwable> errorHandler) {
        this.eventHandler = eventHandler;
        this.errorHandler = errorHandler;
    }

    /**
     * Gets the event handler for processing streaming events.
     *
     * @return The event handler
     */
    protected Consumer<StreamingEventKind> getEventHandler() {
        return eventHandler;
    }

    /**
     * Gets the error handler for processing errors.
     *
     * @return The error handler, or null if not set
     */
    protected @Nullable Consumer<Throwable> getErrorHandler() {
        return errorHandler;
    }

    /**
     * Handles incoming SSE messages. Subclasses must implement the specific
     * parsing logic for their transport protocol.
     *
     * @param message The raw message string from the SSE stream
     * @param completableFuture Optional future for controlling the SSE connection
     */
    public abstract void onMessage(String message, @Nullable Future<Void> completableFuture);

    /**
     * Handles errors that occur during SSE streaming.
     * This method is identical across all implementations.
     *
     * @param throwable The error that occurred
     * @param future Optional future for closing the SSE connection
     */
    public void onError(Throwable throwable, @Nullable Future<Void> future) {
        if (errorHandler != null) {
            errorHandler.accept(throwable);
        }
        if (future != null) {
            future.cancel(true); // close SSE channel
        }
    }

    /**
     * Processes a parsed streaming event and handles auto-close logic for final events.
     * This method encapsulates the common logic for handling events and determining
     * when to close the SSE connection.
     *
     * @param event The parsed streaming event
     * @param future Optional future for closing the SSE connection
     */
    protected void handleEvent(StreamingEventKind event, @Nullable Future<Void> future) {
        eventHandler.accept(event);

        // Client-side auto-close on final events to prevent connection leaks
        // Handles both TaskStatusUpdateEvent and Task objects with final states
        // This covers late subscriptions to completed tasks and ensures no connection leaks
        if (shouldAutoClose(event) && future != null) {
            log.fine("Auto-closing SSE connection for final event: " + event.getClass().getSimpleName());
            future.cancel(true); // close SSE channel
        }
    }

    /**
     * Determines if the SSE connection should be automatically closed based on the event type.
     * The connection is closed when receiving final task states to prevent connection leaks.
     *
     * @param event The streaming event to check
     * @return true if the connection should be closed, false otherwise
     */
    protected boolean shouldAutoClose(StreamingEventKind event) {
        if (event instanceof TaskStatusUpdateEvent tue && tue.isFinal()) {
            return true;
        }
        if (event instanceof Task task) {
            TaskState state = task.status().state();
            return state.isFinal();
        }
        return false;
    }
}
