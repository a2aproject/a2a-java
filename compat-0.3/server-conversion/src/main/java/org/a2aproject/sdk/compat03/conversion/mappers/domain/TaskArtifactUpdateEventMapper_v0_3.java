package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent_v0_3;
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
@Mapper(config = A03ToV10MapperConfig.class, uses = {ArtifactMapper_v0_3.class})
public interface TaskArtifactUpdateEventMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    TaskArtifactUpdateEventMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(TaskArtifactUpdateEventMapper_v0_3.class);

    /**
     * Converts v0.3 TaskArtifactUpdateEvent to v1.0 TaskArtifactUpdateEvent.
     * <p>
     * Converts the nested Artifact using ArtifactMapper.
     *
     * @param v03 the v0.3 task artifact update event
     * @return the equivalent v1.0 task artifact update event
     */
    default TaskArtifactUpdateEvent toV10(TaskArtifactUpdateEvent_v0_3 v03) {
        if (v03 == null) {
            return null;
        }

        return new TaskArtifactUpdateEvent(
            v03.getTaskId(),
            ArtifactMapper_v0_3.INSTANCE.toV10(v03.getArtifact()),
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
    default TaskArtifactUpdateEvent_v0_3 fromV10(TaskArtifactUpdateEvent v10) {
        if (v10 == null) {
            return null;
        }

        return new TaskArtifactUpdateEvent_v0_3(
            v10.taskId(),
            ArtifactMapper_v0_3.INSTANCE.fromV10(v10.artifact()),
            v10.contextId(),
            v10.append(),
            v10.lastChunk(),
            v10.metadata()
        );
    }
}
