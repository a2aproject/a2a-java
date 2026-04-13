package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A03Mappers;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting TaskArtifactUpdateEvent between A2A Protocol v0.3 and v1.0.
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: TaskArtifactUpdateEvent is a class with getter methods (e.g., {@code getTaskId()}, {@code isAppend()})</li>
 *   <li>v1.0: TaskArtifactUpdateEvent is a record with accessor methods (e.g., {@code taskId()}, {@code append()})</li>
 * </ul>
 * <p>
 * Both versions have the same structure:
 * {@code TaskArtifactUpdateEvent(taskId, artifact, contextId, append, lastChunk, metadata)}.
 */
@Mapper(config = A03ToV10MapperConfig.class, uses = {ArtifactMapper.class})
public interface TaskArtifactUpdateEventMapper {

    /**
     * Singleton instance accessed via {@link A03Mappers} factory.
     */
    TaskArtifactUpdateEventMapper INSTANCE = A03Mappers.getMapper(TaskArtifactUpdateEventMapper.class);

    /**
     * Converts v0.3 TaskArtifactUpdateEvent to v1.0 TaskArtifactUpdateEvent.
     * <p>
     * Converts the nested Artifact using ArtifactMapper.
     *
     * @param v03 the v0.3 task artifact update event
     * @return the equivalent v1.0 task artifact update event
     */
    default TaskArtifactUpdateEvent toV10(org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent v03) {
        if (v03 == null) {
            return null;
        }

        return new TaskArtifactUpdateEvent(
            v03.getTaskId(),
            ArtifactMapper.INSTANCE.toV10(v03.getArtifact()),
            v03.getContextId(),
            v03.isAppend(),
            v03.isLastChunk(),
            v03.getMetadata()
        );
    }

    /**
     * Converts v1.0 TaskArtifactUpdateEvent to v0.3 TaskArtifactUpdateEvent.
     * <p>
     * Converts the nested Artifact using ArtifactMapper.
     *
     * @param v10 the v1.0 task artifact update event
     * @return the equivalent v0.3 task artifact update event
     */
    default org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent fromV10(TaskArtifactUpdateEvent v10) {
        if (v10 == null) {
            return null;
        }

        return new org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent(
            v10.taskId(),
            ArtifactMapper.INSTANCE.fromV10(v10.artifact()),
            v10.contextId(),
            v10.append(),
            v10.lastChunk(),
            v10.metadata()
        );
    }
}
