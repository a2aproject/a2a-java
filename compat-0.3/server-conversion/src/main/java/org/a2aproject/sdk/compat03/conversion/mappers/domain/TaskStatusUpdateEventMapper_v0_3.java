package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting TaskStatusUpdateEvent between A2A Protocol v0.3 and v1.0.
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: TaskStatusUpdateEvent is a class with getter methods</li>
 *   <li>v1.0: TaskStatusUpdateEvent is a record with accessor methods</li>
 * </ul>
 * <p>
 * Both versions have the same structure:
 * {@code TaskStatusUpdateEvent(taskId, status, contextId, isFinal, metadata)}.
 */
@Mapper(config = A03ToV10MapperConfig.class, uses = {TaskStatusMapper_v0_3.class})
public interface TaskStatusUpdateEventMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    TaskStatusUpdateEventMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(TaskStatusUpdateEventMapper_v0_3.class);

    /**
     * Converts v0.3 TaskStatusUpdateEvent to v1.0 TaskStatusUpdateEvent.
     * <p>
     * Converts the nested TaskStatus using TaskStatusMapper.
     *
     * @param v03 the v0.3 task status update event
     * @return the equivalent v1.0 task status update event
     */
    default TaskStatusUpdateEvent toV10(TaskStatusUpdateEvent_v0_3 v03) {
        if (v03 == null) {
            return null;
        }

        return new TaskStatusUpdateEvent(
            v03.getTaskId(),
            TaskStatusMapper_v0_3.INSTANCE.toV10(v03.getStatus()),
            v03.getContextId(),
            v03.isFinal(),
            v03.getMetadata()
        );
    }

    /**
     * Converts v1.0 TaskStatusUpdateEvent to v0.3 TaskStatusUpdateEvent.
     * <p>
     * Converts the nested TaskStatus using TaskStatusMapper.
     *
     * @param v10 the v1.0 task status update event
     * @return the equivalent v0.3 task status update event
     */
    default TaskStatusUpdateEvent_v0_3 fromV10(TaskStatusUpdateEvent v10) {
        if (v10 == null) {
            return null;
        }

        return new TaskStatusUpdateEvent_v0_3(
            v10.taskId(),
            TaskStatusMapper_v0_3.INSTANCE.fromV10(v10.status()),
            v10.contextId(),
            v10.isFinal(),
            v10.metadata()
        );
    }
}
