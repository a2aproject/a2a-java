package io.a2a.grpc.mapper;

import org.mapstruct.Mapper;

/**
 * Mapper between {@link io.a2a.spec.TaskState} and {@link io.a2a.grpc.TaskState}.
 * <p>
 * Handles the conversion between domain TaskState enum (with string-based wire format)
 * and protobuf TaskState enum (with integer-based wire format and TASK_STATE_ prefix).
 * <p>
 * Note: Proto uses CANCELLED spelling while domain uses CANCELED.
 * <p>
 * <b>Manual Implementation Required:</b> Uses manual switch statements instead of @ValueMapping
 * to avoid mapstruct-spi-protobuf enum strategy initialization issues.
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface TaskStateMapper {

    TaskStateMapper INSTANCE = A2AMappers.getMapper(TaskStateMapper.class);

    /**
     * Converts domain TaskState to proto TaskState.
     *
     * @param domain the domain task state
     * @return the proto task state, or TASK_STATE_UNSPECIFIED if input is null
     */
    default io.a2a.grpc.TaskState toProto(io.a2a.spec.TaskState domain) {
        if (domain == null) {
            return io.a2a.grpc.TaskState.TASK_STATE_UNSPECIFIED;
        }

        return switch (domain) {
            case SUBMITTED -> io.a2a.grpc.TaskState.TASK_STATE_SUBMITTED;
            case WORKING -> io.a2a.grpc.TaskState.TASK_STATE_WORKING;
            case INPUT_REQUIRED -> io.a2a.grpc.TaskState.TASK_STATE_INPUT_REQUIRED;
            case AUTH_REQUIRED -> io.a2a.grpc.TaskState.TASK_STATE_AUTH_REQUIRED;
            case COMPLETED -> io.a2a.grpc.TaskState.TASK_STATE_COMPLETED;
            case CANCELED -> io.a2a.grpc.TaskState.TASK_STATE_CANCELED;
            case FAILED -> io.a2a.grpc.TaskState.TASK_STATE_FAILED;
            case REJECTED -> io.a2a.grpc.TaskState.TASK_STATE_REJECTED;
            case UNKNOWN -> io.a2a.grpc.TaskState.UNRECOGNIZED;
        };
    }

    /**
     * Converts proto TaskState to domain TaskState.
     *
     * @param proto the proto task state
     * @return the domain task state, or UNKNOWN if input is null or unrecognized
     */
    default io.a2a.spec.TaskState fromProto(io.a2a.grpc.TaskState proto) {
        if (proto == null) {
            return io.a2a.spec.TaskState.UNKNOWN;
        }

        return switch (proto) {
            case TASK_STATE_SUBMITTED -> io.a2a.spec.TaskState.SUBMITTED;
            case TASK_STATE_WORKING -> io.a2a.spec.TaskState.WORKING;
            case TASK_STATE_INPUT_REQUIRED -> io.a2a.spec.TaskState.INPUT_REQUIRED;
            case TASK_STATE_AUTH_REQUIRED -> io.a2a.spec.TaskState.AUTH_REQUIRED;
            case TASK_STATE_COMPLETED -> io.a2a.spec.TaskState.COMPLETED;
            case TASK_STATE_CANCELED -> io.a2a.spec.TaskState.CANCELED;
            case TASK_STATE_FAILED -> io.a2a.spec.TaskState.FAILED;
            case TASK_STATE_REJECTED -> io.a2a.spec.TaskState.REJECTED;
            case TASK_STATE_UNSPECIFIED, UNRECOGNIZED -> io.a2a.spec.TaskState.UNKNOWN;
        };
    }
}
