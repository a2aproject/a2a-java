package io.a2a.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;

import io.a2a.spec.Artifact;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;

/**
 * Utility class providing common helper methods for A2A Protocol operations.
 * <p>
 * This class contains static utility methods for JSON serialization/deserialization,
 * null-safe operations, artifact management, and other common tasks used throughout
 * the A2A Java SDK.
 * <p>
 * Key capabilities:
 * <ul>
 *   <li>JSON processing with pre-configured {@link ObjectMapper}</li>
 *   <li>Null-safe value defaults via {@link #defaultIfNull(Object, Object)}</li>
 *   <li>Artifact streaming support via {@link #appendArtifactToTask(Task, TaskArtifactUpdateEvent)}</li>
 *   <li>Type-safe exception rethrowing via {@link #rethrow(Throwable)}</li>
 * </ul>
 *
 * @see ObjectMapper for JSON processing
 * @see TaskArtifactUpdateEvent for streaming artifact updates
 */
public class Utils {

    /**
     * Pre-configured Jackson {@link ObjectMapper} for JSON operations.
     * <p>
     * This mapper is configured with:
     * <ul>
     *   <li>{@link JavaTimeModule} for Java 8 date/time type support</li>
     * </ul>
     * <p>
     * Used throughout the SDK for consistent JSON serialization and deserialization.
     */
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger log = Logger.getLogger(Utils.class.getName());

    static {
        // needed for date/time types
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    /**
     * Deserializes JSON string into a typed object using Jackson.
     * <p>
     * This method uses the pre-configured {@link #OBJECT_MAPPER} to parse JSON
     * with support for generic types via {@link TypeReference}.
     *
     * @param <T> the target type
     * @param data JSON string to deserialize
     * @param typeRef type reference specifying the target type (for generic types)
     * @return deserialized object of type T
     * @throws JsonProcessingException if JSON parsing fails
     */
    public static <T> T unmarshalFrom(String data, TypeReference<T> typeRef) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(data, typeRef);
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
    public static <T> T defaultIfNull(T value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
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
     *   <li>Adding new artifacts to the task</li>
     *   <li>Replacing existing artifacts (when {@code append=false})</li>
     *   <li>Appending parts to existing artifacts (when {@code append=true})</li>
     * </ul>
     * <p>
     * The {@code append} flag in the event determines the behavior:
     * <ul>
     *   <li>{@code false} or {@code null}: Replace/add the entire artifact</li>
     *   <li>{@code true}: Append the new artifact's parts to an existing artifact with matching {@code artifactId}</li>
     * </ul>
     *
     * @param task  the current task to update
     * @param event the artifact update event containing the new/updated artifact
     * @return a new Task instance with the updated artifacts list
     * @see TaskArtifactUpdateEvent for streaming artifact updates
     * @see Artifact for artifact structure
     */
    // FIXME manipulation & update of Task could be provide by methods on the Task class
    public static Task appendArtifactToTask(Task task, TaskArtifactUpdateEvent event) {
        // Append artifacts
        List<Artifact> artifacts = task.getArtifacts() == null ? new ArrayList<>() : new ArrayList<>(task.getArtifacts());

        Artifact newArtifact = event.getArtifact();
        String artifactId = newArtifact.artifactId();
        boolean appendParts = event.isAppend() != null && event.isAppend();

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
                log.fine(String.format("Replacing artifact at id %s for task %s", artifactId, task.getId()));
                artifacts.set(existingArtifactIndex, newArtifact);
            } else {
                // Append the new artifact since no artifact with this id/index exists yet
                log.fine(String.format("Adding artifact at id %s for task %s", artifactId, task.getId()));
                artifacts.add(newArtifact);
            }

        } else if (existingArtifact != null) {
            // Append new parts to the existing artifact's parts list
            // Do this to a copy
            log.fine(String.format("Appending parts to artifact id %s for task %s", artifactId, task.getId()));
            List<Part<?>> parts = new ArrayList<>(existingArtifact.parts());
            parts.addAll(newArtifact.parts());
            Artifact updated = new Artifact.Builder(existingArtifact)
                    .parts(parts)
                    .build();
            artifacts.set(existingArtifactIndex, updated);
        } else {
            // We received a chunk to append, but we don't have an existing artifact.
            // We will ignore this chunk
            log.warning(
                    String.format("Received append=true for nonexistent artifact index for artifact %s in task %s. Ignoring chunk.",
                    artifactId, task.getId()));
        }

        return new Task.Builder(task)
                .artifacts(artifacts)
                .build();

    }

    /**
     * Serializes an object to a JSON string using Jackson.
     * <p>
     * This method uses the pre-configured {@link #OBJECT_MAPPER} to produce
     * JSON representation of the provided object.
     *
     * @param o the object to serialize
     * @return JSON string representation of the object
     * @throws RuntimeException if JSON serialization fails (wraps {@link JsonProcessingException})
     */
    public static String toJsonString(Object o) {
        try {
            return OBJECT_MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get the first defined URL in the supported interaces of the agent card.
     * @param agentCard the agentcard where the interfaces are defined.
     * @return the first defined URL in the supported interaces of the agent card.
     * @throws A2AClientException
     */
    public static String getFavoriteInterface(AgentCard agentCard) throws A2AClientException {
        if(agentCard.supportedInterfaces() == null || agentCard.supportedInterfaces().isEmpty()) {
            throw new A2AClientException("No server interface available in the AgentCard");
        }
        return agentCard.supportedInterfaces().get(0).url();
    }

}
