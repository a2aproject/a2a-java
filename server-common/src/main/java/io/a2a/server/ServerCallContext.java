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
}
