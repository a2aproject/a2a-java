package org.a2aproject.sdk.compat03.util;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import org.a2aproject.sdk.compat03.json.JsonProcessingException_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;

import org.a2aproject.sdk.compat03.spec.Artifact_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.Part_v0_3;
import java.util.logging.Logger;



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
 * <li>Artifact streaming support via {@link #appendArtifactToTask(Task_v0_3, TaskArtifactUpdateEvent_v0_3, String)}</li>
 * <li>Type-safe exception rethrowing via {@link #rethrow(Throwable)}</li>
 * </ul>
 *
 * @see Gson for JSON processing
 * @see TaskArtifactUpdateEvent_v0_3 for streaming artifact updates
 */
public class Utils_v0_3 {


    private static final Logger log = Logger.getLogger(Utils_v0_3.class.getName());

    /**
     * Deserializes JSON string into a typed object using Gson.
     * <p>
     * This method uses the pre-configured {@link JsonUtil_v0_3#OBJECT_MAPPER} to parse JSON.
     *
     * @param <T> the target type
     * @param data JSON string to deserialize
     * @param typeRef class reference specifying the target type
     * @return deserialized object of type T
     * @throws JsonProcessingException_v0_3 if JSON parsing fails
     */
    public static <T> T unmarshalFrom(String data, Class<T> typeRef) throws JsonProcessingException_v0_3 {
        return JsonUtil_v0_3.fromJson(data, typeRef);
    }

    public static String toJsonString(Object data) {
        try {
            return JsonUtil_v0_3.toJson(data);
        } catch (JsonProcessingException_v0_3 e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    public static <T> T defaultIfNull(T value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public static <T extends Throwable> void rethrow(Throwable t) throws T {
        throw (T) t;
    }

    /**
     * Appends or updates an artifact in a task based on a {@link TaskArtifactUpdateEvent_v0_3}.
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
     * @see TaskArtifactUpdateEvent_v0_3 for streaming artifact updates
     * @see Artifact_v0_3 for artifact structure
     */
    public static Task_v0_3 appendArtifactToTask(Task_v0_3 task, TaskArtifactUpdateEvent_v0_3 event, String taskId) {
        // Append artifacts
        List<Artifact_v0_3> artifacts = task.getArtifacts() == null ? new ArrayList<>() : new ArrayList<>(task.getArtifacts());

        Artifact_v0_3 newArtifact = event.getArtifact();
        String artifactId = newArtifact.artifactId();
        boolean appendParts = event.isAppend() != null && event.isAppend();

        Artifact_v0_3 existingArtifact = null;
        int existingArtifactIndex = -1;

        for (int i = 0; i < artifacts.size(); i++) {
            Artifact_v0_3 curr = artifacts.get(i);
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
            List<Part_v0_3<?>> parts = new ArrayList<>(existingArtifact.parts());
            parts.addAll(newArtifact.parts());
            Artifact_v0_3 updated = new Artifact_v0_3.Builder(existingArtifact)
                    .parts(parts)
                    .build();
            artifacts.set(existingArtifactIndex, updated);
        } else {
            // We received a chunk to append, but we don't have an existing artifact.
            // We will ignore this chunk
            log.warning(
                    String.format("Received append=true for nonexistent artifact index for artifact %s in task %s. Ignoring chunk.",
                    artifactId, taskId));
        }

        return new Task_v0_3.Builder(task)
                .artifacts(artifacts)
                .build();

    }

    /**
     * Get the first defined URL in the supported interaces of the agent card.
     *
     * @param agentCard the agentcard where the interfaces are defined.
     * @return the first defined URL in the supported interaces of the agent card.
     * @throws A2AClientException
     */
//    public static String getFavoriteInterface(AgentCard agentCard) throws A2AClientException {
//        if (agentCard.supportedInterfaces() == null || agentCard.supportedInterfaces().isEmpty()) {
//            throw new A2AClientException("No server interface available in the AgentCard");
//        }
//        return agentCard.supportedInterfaces().get(0).url();
//    }

}
