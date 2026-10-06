package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.spec.TaskStatus;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting TaskStatus between A2A Protocol v0.3 and v1.0.
 * <p>
 * Both versions are records with the same structure:
 * {@code TaskStatus(TaskState state, Message message, OffsetDateTime timestamp)}.
 * <p>
 * The conversion involves:
 * <ul>
 *   <li>Converting {@link TaskState_v0_3} to {@link org.a2aproject.sdk.spec.TaskState} (enum prefix mapping)</li>
 *   <li>Converting {@link Message_v0_3} to {@link org.a2aproject.sdk.spec.Message} (class ↔ record)</li>
 *   <li>Preserving the timestamp field (same type in both versions)</li>
 * </ul>
 */
@Mapper(config = A03ToV10MapperConfig.class, uses = {TaskStateMapper_v0_3.class, MessageMapper_v0_3.class})
public interface TaskStatusMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    TaskStatusMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(TaskStatusMapper_v0_3.class);

    /**
     * Converts v0.3 TaskStatus to v1.0 TaskStatus.
     * <p>
     * Converts the state enum and message object using their respective mappers.
     *
     * @param v03 the v0.3 task status
     * @return the equivalent v1.0 task status
     */
    default TaskStatus toV10(TaskStatus_v0_3 v03) {
        if (v03 == null) {
            return null;
        }

        return new TaskStatus(
            TaskStateMapper_v0_3.INSTANCE.toV10(v03.state()),
            MessageMapper_v0_3.INSTANCE.toV10(v03.message()),
            v03.timestamp()
        );
    }

    /**
     * Converts v1.0 TaskStatus to v0.3 TaskStatus.
     * <p>
     * Converts the state enum and message object using their respective mappers.
     *
     * @param v10 the v1.0 task status
     * @return the equivalent v0.3 task status
     */
    default TaskStatus_v0_3 fromV10(TaskStatus v10) {
        if (v10 == null) {
            return null;
        }

        return new TaskStatus_v0_3(
            TaskStateMapper_v0_3.INSTANCE.fromV10(v10.state()),
            MessageMapper_v0_3.INSTANCE.fromV10(v10.message()),
            v10.timestamp()
        );
    }
}
