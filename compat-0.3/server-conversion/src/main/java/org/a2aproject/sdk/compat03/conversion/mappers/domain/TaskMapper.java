package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import java.util.List;
import java.util.stream.Collectors;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A03Mappers;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting Task between A2A Protocol v0.3 and v1.0.
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: Task is a class with getter methods (e.g., {@code getId()}, {@code getStatus()})</li>
 *   <li>v1.0: Task is a record with accessor methods (e.g., {@code id()}, {@code status()})</li>
 *   <li>v0.3 has a {@code kind} field with {@code getKind()} method</li>
 *   <li>v1.0 has a {@code kind()} method from the {@link org.a2aproject.sdk.spec.StreamingEventKind} interface</li>
 * </ul>
 * <p>
 * The conversion involves mapping nested types:
 * <ul>
 *   <li>{@link org.a2aproject.sdk.spec.TaskStatus} via {@link TaskStatusMapper}</li>
 *   <li>{@link Artifact} list via {@link ArtifactMapper}</li>
 *   <li>{@link Message} history list via {@link MessageMapper}</li>
 * </ul>
 */
@Mapper(config = A03ToV10MapperConfig.class, uses = {TaskStatusMapper.class, ArtifactMapper.class, MessageMapper.class})
public interface TaskMapper {

    /**
     * Singleton instance accessed via {@link A03Mappers} factory.
     */
    TaskMapper INSTANCE = A03Mappers.getMapper(TaskMapper.class);

    /**
     * Converts v0.3 Task to v1.0 Task.
     * <p>
     * Converts all nested objects including status, artifacts, and history using their respective mappers.
     *
     * @param v03 the v0.3 task
     * @return the equivalent v1.0 task
     */
    default Task toV10(org.a2aproject.sdk.compat03.spec.Task v03) {
        if (v03 == null) {
            return null;
        }

        List<Artifact> artifacts = v03.getArtifacts() != null
            ? v03.getArtifacts().stream()
                .map(ArtifactMapper.INSTANCE::toV10)
                .collect(Collectors.toList())
            : null;

        List<Message> history = v03.getHistory() != null
            ? v03.getHistory().stream()
                .map(MessageMapper.INSTANCE::toV10)
                .collect(Collectors.toList())
            : null;

        return new Task(
            v03.getId(),
            v03.getContextId(),
            TaskStatusMapper.INSTANCE.toV10(v03.getStatus()),
            artifacts,
            history,
            v03.getMetadata()
        );
    }

    /**
     * Converts v1.0 Task to v0.3 Task.
     * <p>
     * Converts all nested objects including status, artifacts, and history using their respective mappers.
     *
     * @param v10 the v1.0 task
     * @return the equivalent v0.3 task
     */
    default org.a2aproject.sdk.compat03.spec.Task fromV10(Task v10) {
        if (v10 == null) {
            return null;
        }

        List<org.a2aproject.sdk.compat03.spec.Artifact> artifacts = v10.artifacts() != null
            ? v10.artifacts().stream()
                .map(ArtifactMapper.INSTANCE::fromV10)
                .collect(Collectors.toList())
            : null;

        List<org.a2aproject.sdk.compat03.spec.Message> history = v10.history() != null
            ? v10.history().stream()
                .map(MessageMapper.INSTANCE::fromV10)
                .collect(Collectors.toList())
            : null;

        return new org.a2aproject.sdk.compat03.spec.Task(
            v10.id(),
            v10.contextId(),
            TaskStatusMapper.INSTANCE.fromV10(v10.status()),
            artifacts,
            history,
            v10.metadata()
        );
    }
}
