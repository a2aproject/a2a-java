package org.a2aproject.sdk.spec.util;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gson.Gson;
import org.a2aproject.sdk.spec.A2AClientException;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
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

    public static final String DEFAULT_AGENT_CARD_PATH = "/.well-known/agent-card.json";
    static final int MAX_TENANT_LENGTH = 256;

    private static final Logger log = Logger.getLogger(Utils.class.getName());

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Utils() {
        // Utility class - no instances
    }

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
     * Validates that {@code url} is a syntactically valid absolute URI.
     *
     * @param url the URL to validate
     * @throws URISyntaxException if the URL is syntactically invalid or not absolute
     */
    public static void validateAbsoluteUrl(String url) throws URISyntaxException {
        URI uri = new URI(url);
        if (!uri.isAbsolute()) {
            throw new URISyntaxException(url, "URI must be absolute");
        }
    }

    /**
     * Normalizes {@code baseUrl} and {@code cardPath} and concatenates them into a full card URL.
     *
     * <p>
     * Strips any trailing slash from {@code baseUrl} and ensures {@code cardPath} starts with
     * a leading slash before concatenating, so both {@code http://host/base/} and
     * {@code http://host/base} produce the same result.
     *
     * @param baseUrl the agent base URL, must not be null
     * @param cardPath the card endpoint path, must not be null
     * @return the normalized card URL
     */
    public static String buildCardUrl(String baseUrl, String cardPath) {
        String normalizedPath = cardPath.startsWith("/") ? cardPath : "/" + cardPath;
        return stripTrailingSlash(baseUrl) + normalizedPath;
    }

    /**
     * Strips any trailing slash and the standard well-known suffix from {@code baseUrl} so that
     * {@link #buildCardUrl} can append the desired path without doubling it.
     *
     * <p>
     * Only {@link #DEFAULT_AGENT_CARD_PATH} is stripped; custom paths are never inferred
     * from the URL structure.
     *
     * @param baseUrl the URL to strip
     * @return the URL with any trailing slash and well-known suffix removed
     */
    public static String stripWellKnownSuffix(String baseUrl) {
        String s = stripTrailingSlash(baseUrl);
        return s.endsWith(DEFAULT_AGENT_CARD_PATH)
                ? s.substring(0, s.length() - DEFAULT_AGENT_CARD_PATH.length())
                : s;
    }

    /**
     * Builds a base URL by combining a raw base URL string with an optional tenant identifier.
     *
     * <p>
     * Normalizes trailing slashes on the base URL and validates the tenant (must be a simple
     * identifier — no {@code /} or {@code ?}).
     *
     * @param baseUrl the base URL string, must not be null
     * @param tenant the tenant identifier, may be null for no tenant
     * @return the complete base URL with tenant path appended
     * @throws IllegalArgumentException if tenant validation fails
     */
    public static String buildBaseUrl(String baseUrl, @Nullable String tenant) {
        checkNotNullParam("baseUrl", baseUrl);
        String stripped = stripTrailingSlash(baseUrl);
        String tenantPath = extractTenant("", tenant);
        if (!tenantPath.isEmpty() && stripped.endsWith(tenantPath)) {
            return stripped;
        }
        return stripped + tenantPath;
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
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
     * Validates that a tenant identifier is safe and well-formed.
     * <p>
     * A tenant must be a simple identifier — it must not contain URL path elements
     * such as {@code /} or {@code ?}. Null and blank values are silently accepted
     * (they mean "no tenant"). This method rejects:
     * <ul>
     * <li>Excessive length (max 256 characters)</li>
     * <li>Invalid characters (only allows {@code a-zA-Z0-9_-.})</li>
     * </ul>
     *
     * @param tenant the tenant identifier to validate, may be {@code null}
     * @throws IllegalArgumentException if the tenant is invalid or unsafe
     */
    public static void validateTenant(@Nullable String tenant) {
        if (tenant == null || tenant.isBlank()) {
            return;
        }

        String stripped = normalizeTenant(tenant);
        if (stripped.isEmpty()) {
            return;
        }

        if (stripped.length() > MAX_TENANT_LENGTH) {
            throw new IllegalArgumentException("Tenant exceeds maximum length of " + MAX_TENANT_LENGTH + " characters");
        }

        if (!stripped.matches("^[a-zA-Z0-9_.\\-]+$")) {
            throw new IllegalArgumentException(
                    "Tenant contains invalid characters. Only a-zA-Z0-9_-. are allowed");
        }
    }

    private static String normalizeTenant(String tenant) {
        String stripped = tenant;
        if (stripped.startsWith("/")) {
            stripped = stripped.substring(1);
        }
        if (stripped.endsWith("/")) {
            stripped = stripped.substring(0, stripped.length() - 1);
        }
        return stripped;
    }

    /**
     * Extracts and normalizes a tenant identifier into a URL path segment, using the agent's
     * default tenant if no override is provided.
     * <p>
     * Leading and trailing slashes are stripped before validation — the tenant must be a simple
     * identifier (e.g. {@code "acme"}), not a path (e.g. {@code "org/team"}).
     * <p>
     * If the provided {@code tenant} parameter is null or blank, the {@code agentTenant} is
     * returned instead.
     *
     * @param agentTenant the default tenant from the agent card, may be null or blank
     * @param tenant the tenant override from the request, may be null or blank
     * @return the tenant as a URL path segment (e.g. {@code "/acme"}), or empty string if no tenant
     * @throws IllegalArgumentException if the tenant is invalid or unsafe
     */
    private static String extractTenant(@Nullable String agentTenant, @Nullable String tenant) {
        String raw = tenant;
        if (raw == null || raw.isBlank()) {
            raw = agentTenant;
        }
        if (raw == null || raw.isBlank()) {
            return "";
        }

        validateTenant(raw);

        String stripped = normalizeTenant(raw);
        if (stripped.isEmpty()) {
            return "";
        }

        return "/" + stripped;
    }

    /**
     * Builds a base URL for A2A operations by combining the agent's URL with a tenant identifier.
     * <p>
     * This method:
     * <ul>
     * <li>Uses the tenant from the {@link AgentInterface} as the default</li>
     * <li>Allows overriding with a custom tenant if provided</li>
     * <li>Normalizes trailing slashes on the base URL</li>
     * <li>Validates the tenant (must be a simple identifier — no {@code /} or {@code ?})</li>
     * <li>Avoids doubling the tenant when the URL already ends with the tenant path</li>
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
     *
     * // URL already contains the tenant — no doubling
     * AgentInterface iface3 = new AgentInterface("http+json", "http://example.com/acme");
     * String url3 = Utils.buildBaseUrl(iface3, "acme");
     * // Returns: "http://example.com/acme"
     * }</pre>
     *
     * @param agentInterface the agent interface containing the base URL and default tenant, must not be null
     * @param tenant the tenant identifier override, may be null to use the interface default
     * @return the complete base URL with tenant path appended
     * @throws IllegalArgumentException if agentInterface is null or tenant validation fails
     */
    public static String buildBaseUrl(AgentInterface agentInterface, @Nullable String tenant) {
        checkNotNullParam("agentInterface", agentInterface);

        String baseUrl = stripTrailingSlash(agentInterface.url());
        String tenantPath = extractTenant(agentInterface.tenant(), tenant);
        if (!tenantPath.isEmpty() && baseUrl.endsWith(tenantPath)) {
            return baseUrl;
        }
        return baseUrl + tenantPath;
    }
}
