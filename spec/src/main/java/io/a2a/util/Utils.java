package io.a2a.util;

import static io.a2a.util.Assert.checkNotNullParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.Artifact;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import org.jspecify.annotations.Nullable;

/**
 * Utility class providing common helper methods for A2A Protocol operations.
 * <p>
 * This class contains static utility methods for JSON serialization/deserialization,
 * null-safe operations, artifact management, and other common tasks used throughout
 * the A2A Java SDK.
 * <p>
 * Key capabilities:
 * <ul>
 * <li>JSON processing with pre-configured {@link Gson}</li>
 * <li>Null-safe value defaults via {@link #defaultIfNull(Object, Object)}</li>
 * <li>Artifact streaming support via {@link #appendArtifactToTask(Task, TaskArtifactUpdateEvent, String)}</li>
 * <li>Type-safe exception rethrowing via {@link #rethrow(Throwable)}</li>
 * </ul>
 *
 * @see Gson for JSON processing
 * @see TaskArtifactUpdateEvent for streaming artifact updates
 */
public class Utils {

    private static final Logger log = Logger.getLogger(Utils.class.getName());

    /**
     * Returns the provided value if non-null, otherwise returns the default value.
     * <p>
     * This is a null-safe utility for providing default values when a parameter
     * might be null.
     *
     * @param <T> the value type
     * @param value the value to check
     * @param defaultValue the default value to return if value is null
     * @return value if non-null, otherwise defaultValue
     */
    public static <T> T defaultIfNull(@Nullable T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    /**
     * Rethrows a checked exception as an unchecked exception.
     * <p>
     * This method uses type erasure to bypass checked exception handling,
     * allowing checked exceptions to be thrown without explicit declaration.
     * Use with caution as it bypasses Java's compile-time exception checking.
     *
     * @param <T> the throwable type
     * @param t the throwable to rethrow
     * @throws T the rethrown exception
     */
    public static <T extends Throwable> void rethrow(Throwable t) throws T {
        throw (T) t;
    }

    /**
     * Appends or updates an artifact in a task based on a {@link TaskArtifactUpdateEvent}.
     * <p>
     * This method handles streaming artifact updates, supporting both:
     * <ul>
     * <li>Adding new artifacts to the task</li>
     * <li>Replacing existing artifacts (when {@code append=false})</li>
     * <li>Appending parts to existing artifacts (when {@code append=true})</li>
     * </ul>
     * <p>
     * The {@code append} flag in the event determines the behavior:
     * <ul>
     * <li>{@code false} or {@code null}: Replace/add the entire artifact</li>
     * <li>{@code true}: Append the new artifact's parts to an existing artifact with matching {@code artifactId}</li>
     * </ul>
     *
     * @param task the current task to update
     * @param event the artifact update event containing the new/updated artifact
     * @param taskId the task ID (for logging purposes)
     * @return a new Task instance with the updated artifacts list
     * @see TaskArtifactUpdateEvent for streaming artifact updates
     * @see Artifact for artifact structure
     */
    public static Task appendArtifactToTask(Task task, TaskArtifactUpdateEvent event, String taskId) {
        // Append artifacts
        List<Artifact> artifacts = task.artifacts() == null ? new ArrayList<>() : new ArrayList<>(task.artifacts());

        Artifact newArtifact = event.artifact();
        String artifactId = newArtifact.artifactId();
        boolean appendParts = event.append() != null && event.append();

        Artifact existingArtifact = null;
        int existingArtifactIndex = -1;

        for (int i = 0; i < artifacts.size(); i++) {
            Artifact curr = artifacts.get(i);
            if (curr.artifactId() != null && curr.artifactId().equals(artifactId)) {
                existingArtifact = curr;
                existingArtifactIndex = i;
                break;
            }
        }

        if (!appendParts) {
            // This represents the first chunk for this artifact index
            if (existingArtifactIndex >= 0) {
                // Replace the existing artifact entirely with the new artifact
                log.fine(String.format("Replacing artifact at id %s for task %s", artifactId, taskId));
                artifacts.set(existingArtifactIndex, newArtifact);
            } else {
                // Append the new artifact since no artifact with this id/index exists yet
                log.fine(String.format("Adding artifact at id %s for task %s", artifactId, taskId));
                artifacts.add(newArtifact);
            }

        } else if (existingArtifact != null) {
            // Append new parts to the existing artifact's parts list
            // Do this to a copy
            log.fine(String.format("Appending parts to artifact id %s for task %s", artifactId, taskId));
            List<Part<?>> parts = new ArrayList<>(existingArtifact.parts());
            parts.addAll(newArtifact.parts());

            Map<String, Object> mergedMetadata = null;
            if (existingArtifact.metadata() != null || newArtifact.metadata() != null) {
                mergedMetadata = new HashMap<>();
                if (existingArtifact.metadata() != null) {
                    mergedMetadata.putAll(existingArtifact.metadata());
                }
                if (newArtifact.metadata() != null) {
                    mergedMetadata.putAll(newArtifact.metadata());
                }
            }

            Artifact updated = Artifact.builder(existingArtifact)
                    .parts(parts)
                    .metadata(mergedMetadata)
                    .build();
            artifacts.set(existingArtifactIndex, updated);
        } else {
            // We received a chunk to append, but we don't have an existing artifact.
            // We will ignore this chunk
            log.warning(
                    String.format("Received append=true for nonexistent artifact index for artifact %s in task %s. Ignoring chunk.",
                            artifactId, taskId));
        }

        return Task.builder(task)
                .artifacts(artifacts)
                .build();

    }

    /**
     * Get the first defined URL in the supported interaces of the agent card.
     *
     * @param agentCard the agentcard where the interfaces are defined.
     * @return the first defined URL in the supported interaces of the agent card.
     * @throws A2AClientException if no server interface is available in the AgentCard
     */
    public static AgentInterface getFavoriteInterface(AgentCard agentCard) throws A2AClientException {
        if (agentCard.supportedInterfaces() == null || agentCard.supportedInterfaces().isEmpty()) {
            throw new A2AClientException("No server interface available in the AgentCard");
        }
        return agentCard.supportedInterfaces().get(0);
    }

    /**
     * Validates that a tenant path is safe and well-formed.
     * <p>
     * This method performs security validation to prevent:
     * <ul>
     * <li>Path traversal attacks (e.g., {@code ../../admin})</li>
     * <li>Excessive length (max 256 characters)</li>
     * <li>Invalid characters (only allows {@code /a-zA-Z0-9_-.})</li>
     * </ul>
     *
     * @param tenant the tenant path to validate
     * @throws IllegalArgumentException if the tenant is invalid or unsafe
     */
    private static void validateTenant(String tenant) {
        if (tenant.isEmpty()) {
            return; // Empty string is valid (no tenant)
        }

        if (tenant.length() > 256) {
            throw new IllegalArgumentException("Tenant path exceeds maximum length of 256 characters");
        }

        if (tenant.contains("..")) {
            throw new IllegalArgumentException("Tenant path contains invalid '..' sequence (path traversal attempt)");
        }

        if (tenant.contains("//")) {
            throw new IllegalArgumentException("Tenant path contains invalid '//' sequence");
        }

        if (!tenant.matches("^[/a-zA-Z0-9_.\\-]+$")) {
            throw new IllegalArgumentException("Tenant path contains invalid characters. Only /a-zA-Z0-9_-. are allowed");
        }
    }

    /**
     * Extracts and normalizes a tenant path, using the agent's default tenant if no override is provided.
     * <p>
     * This method normalizes tenant paths by ensuring they:
     * <ul>
     * <li>Start with a forward slash ({@code /})</li>
     * <li>Do not end with a forward slash (unless it's just {@code /})</li>
     * <li>Are validated for security (no path traversal, length limits, valid characters)</li>
     * </ul>
     * <p>
     * If the provided {@code tenant} parameter is null or blank, the {@code agentTenant} is returned instead.
     *
     * @param agentTenant the default tenant from the agent card, must not be null
     * @param tenant the tenant override from the request, may be null or blank
     * @return the normalized tenant path
     * @throws IllegalArgumentException if the tenant is invalid or unsafe
     */
    private static String extractTenant(String agentTenant, @Nullable String tenant) {
        checkNotNullParam("agentTenant", agentTenant);

        String tenantPath = tenant;
        if (tenantPath == null || tenantPath.isBlank()) {
            return agentTenant;
        }

        // Normalize slashes
        if (!tenantPath.startsWith("/")) {
            tenantPath = '/' + tenantPath;
        }
        if (tenantPath.endsWith("/") && tenantPath.length() > 1) {
            tenantPath = tenantPath.substring(0, tenantPath.length() - 1);
        }

        // Validate for security
        validateTenant(tenantPath);

        return tenantPath;
    }

    /**
     * Builds a base URL for A2A operations by combining the agent's URL with a tenant path.
     * <p>
     * This method:
     * <ul>
     * <li>Uses the tenant from the {@link AgentInterface} as the default</li>
     * <li>Allows overriding with a custom tenant path if provided</li>
     * <li>Normalizes trailing slashes on the base URL</li>
     * <li>Validates and normalizes the tenant path</li>
     * </ul>
     * <p>
     * Example:
     * <pre>{@code
     * AgentInterface iface = new AgentInterface("jsonrpc", "http://example.com", "default-tenant");
     * String url = Utils.buildBaseUrl(iface, null);
     * // Returns: "http://example.com/default-tenant"
     *
     * String url2 = Utils.buildBaseUrl(iface, "custom-tenant");
     * // Returns: "http://example.com/custom-tenant"
     * }</pre>
     *
     * @param agentInterface the agent interface containing the base URL and default tenant, must not be null
     * @param tenant the tenant override from the request, may be null to use the interface default
     * @return the complete base URL with tenant path appended
     * @throws IllegalArgumentException if agentInterface is null or tenant validation fails
     */
    public static String buildBaseUrl(AgentInterface agentInterface, @Nullable String tenant) {
        checkNotNullParam("agentInterface", agentInterface);

        String agentUrl = agentInterface.url();
        agentUrl = agentUrl.endsWith("/") ? agentUrl.substring(0, agentUrl.length() - 1) : agentUrl;
        StringBuilder urlBuilder = new StringBuilder(agentUrl);
        urlBuilder.append(extractTenant(agentInterface.tenant(), tenant));
        return urlBuilder.toString();
    }
}
