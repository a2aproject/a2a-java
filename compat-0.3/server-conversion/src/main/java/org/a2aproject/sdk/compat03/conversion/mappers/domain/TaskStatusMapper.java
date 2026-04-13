package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A03Mappers;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
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
 *   <li>Converting {@link org.a2aproject.sdk.compat03.spec.TaskState} to {@link org.a2aproject.sdk.spec.TaskState} (enum prefix mapping)</li>
 *   <li>Converting {@link org.a2aproject.sdk.compat03.spec.Message} to {@link org.a2aproject.sdk.spec.Message} (class ↔ record)</li>
 *   <li>Preserving the timestamp field (same type in both versions)</li>
 * </ul>
 */
@Mapper(config = A03ToV10MapperConfig.class, uses = {TaskStateMapper.class, MessageMapper.class})
public interface TaskStatusMapper {

    /**
     * Singleton instance accessed via {@link A03Mappers} factory.
     */
    TaskStatusMapper INSTANCE = A03Mappers.getMapper(TaskStatusMapper.class);

    /**
     * Converts v0.3 TaskStatus to v1.0 TaskStatus.
     * <p>
     * Converts the state enum and message object using their respective mappers.
     *
     * @param v03 the v0.3 task status
     * @return the equivalent v1.0 task status
     */
    default TaskStatus toV10(org.a2aproject.sdk.compat03.spec.TaskStatus v03) {
        if (v03 == null) {
            return null;
        }

        return new TaskStatus(
            TaskStateMapper.INSTANCE.toV10(v03.state()),
            MessageMapper.INSTANCE.toV10(v03.message()),
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
    default org.a2aproject.sdk.compat03.spec.TaskStatus fromV10(TaskStatus v10) {
        if (v10 == null) {
            return null;
        }

        return new org.a2aproject.sdk.compat03.spec.TaskStatus(
            TaskStateMapper.INSTANCE.fromV10(v10.state()),
            MessageMapper.INSTANCE.fromV10(v10.message()),
            v10.timestamp()
        );
    }
}
