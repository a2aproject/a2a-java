package org.a2aproject.sdk.grpc.mapper;

import org.mapstruct.Mapper;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.TaskState} and {@link org.a2aproject.sdk.grpc.TaskState}.
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
    default org.a2aproject.sdk.grpc.TaskState toProto(org.a2aproject.sdk.spec.TaskState domain) {
        if (domain == null) {
            return org.a2aproject.sdk.grpc.TaskState.TASK_STATE_UNSPECIFIED;
        }

        return switch (domain) {
            case TASK_STATE_SUBMITTED -> org.a2aproject.sdk.grpc.TaskState.TASK_STATE_SUBMITTED;
            case TASK_STATE_WORKING -> org.a2aproject.sdk.grpc.TaskState.TASK_STATE_WORKING;
            case TASK_STATE_INPUT_REQUIRED -> org.a2aproject.sdk.grpc.TaskState.TASK_STATE_INPUT_REQUIRED;
            case TASK_STATE_AUTH_REQUIRED -> org.a2aproject.sdk.grpc.TaskState.TASK_STATE_AUTH_REQUIRED;
            case TASK_STATE_COMPLETED -> org.a2aproject.sdk.grpc.TaskState.TASK_STATE_COMPLETED;
            case TASK_STATE_CANCELED -> org.a2aproject.sdk.grpc.TaskState.TASK_STATE_CANCELED;
            case TASK_STATE_FAILED -> org.a2aproject.sdk.grpc.TaskState.TASK_STATE_FAILED;
            case TASK_STATE_REJECTED -> org.a2aproject.sdk.grpc.TaskState.TASK_STATE_REJECTED;
            case UNRECOGNIZED -> org.a2aproject.sdk.grpc.TaskState.UNRECOGNIZED;
        };
    }

    /**
     * Converts proto TaskState to domain TaskState.
     *
     * @param proto the proto task state
     * @return the domain task state, or UNKNOWN if input is null or unrecognized
     */
    default org.a2aproject.sdk.spec.TaskState fromProto(org.a2aproject.sdk.grpc.TaskState proto) {
        if (proto == null) {
            return org.a2aproject.sdk.spec.TaskState.UNRECOGNIZED;
        }

        return switch (proto) {
            case TASK_STATE_SUBMITTED -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_SUBMITTED;
            case TASK_STATE_WORKING -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_WORKING;
            case TASK_STATE_INPUT_REQUIRED -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_INPUT_REQUIRED;
            case TASK_STATE_AUTH_REQUIRED -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_AUTH_REQUIRED;
            case TASK_STATE_COMPLETED -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_COMPLETED;
            case TASK_STATE_CANCELED -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_CANCELED;
            case TASK_STATE_FAILED -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_FAILED;
            case TASK_STATE_REJECTED -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_REJECTED;
            case TASK_STATE_UNSPECIFIED, UNRECOGNIZED -> org.a2aproject.sdk.spec.TaskState.UNRECOGNIZED;
        };
    }
}
