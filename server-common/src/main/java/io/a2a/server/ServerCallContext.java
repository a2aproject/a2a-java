package io.a2a.server;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.a2a.server.auth.User;
import org.jspecify.annotations.Nullable;

public class ServerCallContext {
    // TODO Not totally sure yet about these field types
    private final Map<Object, Object> modelConfig = new ConcurrentHashMap<>();
    private final Map<String, Object> state;
    private final User user;
    private final Set<String> requestedExtensions;
    private final Set<String> activatedExtensions;
    private final @Nullable String requestedProtocolVersion;
    private volatile @Nullable Runnable eventConsumerCancelCallback;

    public ServerCallContext(User user, Map<String, Object> state, Set<String> requestedExtensions) {
        this(user, state, requestedExtensions, null);
    }

    public ServerCallContext(User user, Map<String, Object> state, Set<String> requestedExtensions, @Nullable String requestedProtocolVersion) {
        this.user = user;
        this.state = state;
        this.requestedExtensions = new HashSet<>(requestedExtensions);
        this.activatedExtensions = new HashSet<>(); // Always starts empty, populated later by application code
        this.requestedProtocolVersion = requestedProtocolVersion;
    }

    public Map<String, Object> getState() {
        return state;
    }

    public User getUser() {
        return user;
    }

    public Set<String> getRequestedExtensions() {
        return new HashSet<>(requestedExtensions);
    }

    public Set<String> getActivatedExtensions() {
        return new HashSet<>(activatedExtensions);
    }

    public void activateExtension(String extensionUri) {
        activatedExtensions.add(extensionUri);
    }

    public void deactivateExtension(String extensionUri) {
        activatedExtensions.remove(extensionUri);
    }

    public boolean isExtensionActivated(String extensionUri) {
        return activatedExtensions.contains(extensionUri);
    }

    public boolean isExtensionRequested(String extensionUri) {
        return requestedExtensions.contains(extensionUri);
    }

    public @Nullable String getRequestedProtocolVersion() {
        return requestedProtocolVersion;
    }

    /**
     * Sets the callback to be invoked when the client disconnects or the call is cancelled.
     * <p>
     * This callback is typically used to stop the EventConsumer polling loop when a client
     * disconnects from a streaming endpoint. The callback is invoked by transport layers
     * (JSON-RPC over HTTP/SSE, REST over HTTP/SSE, gRPC streaming) when they detect that
     * the client has closed the connection.
     * </p>
     * <p>
     * <strong>Thread Safety:</strong> The callback may be invoked from any thread, depending
     * on the transport implementation. Implementations should be thread-safe.
     * </p>
     * <strong>Example Usage:</strong>
     * <pre>{@code
     * EventConsumer consumer = new EventConsumer(queue);
     * context.setEventConsumerCancelCallback(consumer::cancel);
     * }</pre>
     *
     * @param callback the callback to invoke on client disconnect, or null to clear any existing callback
     * @see #invokeEventConsumerCancelCallback()
     */
    public void setEventConsumerCancelCallback(@Nullable Runnable callback) {
        this.eventConsumerCancelCallback = callback;
    }

    /**
     * Invokes the EventConsumer cancel callback if one has been set.
     * <p>
     * This method is called by transport layers when a client disconnects or cancels a
     * streaming request. It triggers the callback registered via
     * {@link #setEventConsumerCancelCallback(Runnable)}, which typically stops the
     * EventConsumer polling loop.
     * </p>
     * <p>
     * <strong>Transport-Specific Behavior:</strong>
     * </p>
     * <ul>
     *   <li><strong>JSON-RPC/REST over HTTP/SSE:</strong> Called from Vert.x
     *       {@code HttpServerResponse.closeHandler()} when the SSE connection is closed</li>
     *   <li><strong>gRPC streaming:</strong> Called from gRPC
     *       {@code Context.CancellationListener.cancelled()} when the call is cancelled</li>
     * </ul>
     * <p>
     * <strong>Thread Safety:</strong> This method is thread-safe. The callback is stored
     * in a volatile field and null-checked before invocation to prevent race conditions.
     * </p>
     * <p>
     * If no callback has been set, this method does nothing (no-op).
     * </p>
     *
     * @see #setEventConsumerCancelCallback(Runnable)
     * @see io.a2a.server.events.EventConsumer#cancel()
     */
    public void invokeEventConsumerCancelCallback() {
        Runnable callback = this.eventConsumerCancelCallback;
        if (callback != null) {
            callback.run();
        }
    }
}
