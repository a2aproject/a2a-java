package io.a2a.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.a2a.spec.Artifact;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;

public class Utils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = Logger.getLogger(Utils.class.getName());
    static {
        // needed for date/time types
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    public static <T> T unmarshalFrom(String data, TypeReference<T> typeRef) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(data, typeRef);
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

    public static Task appendArtifactToTask(Task task, TaskArtifactUpdateEvent event, String taskId) {
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
            Artifact updated = new Artifact.Builder(existingArtifact)
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

        return new Task.Builder(task)
                .artifacts(artifacts)
                .build();

    }
}
