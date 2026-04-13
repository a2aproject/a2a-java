package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A03Mappers;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting {@code TaskState} enum between A2A Protocol v0.3 and v1.0.
 * <p>
 * This is a critical mapper because v1.0 adds the {@code TASK_STATE_} prefix to all enum constants:
 * <ul>
 *   <li>v0.3: {@code SUBMITTED, WORKING, ...}</li>
 *   <li>v1.0: {@code TASK_STATE_SUBMITTED, TASK_STATE_WORKING, ...}</li>
 * </ul>
 * <p>
 * Additionally, the {@code UNKNOWN} state in v0.3 maps to {@code UNRECOGNIZED} in v1.0.
 * <p>
 * This mapper uses manual switch statements instead of {@code @ValueMapping} to:
 * <ul>
 *   <li>Avoid mapstruct-spi-protobuf enum strategy initialization issues</li>
 *   <li>Handle explicit null mapping (null → UNRECOGNIZED/UNKNOWN)</li>
 *   <li>Provide clear, compile-time-safe enum conversions</li>
 * </ul>
 *
 * @see org.a2aproject.sdk.compat03.spec.TaskState
 * @see org.a2aproject.sdk.spec.TaskState
 */
@Mapper(config = A03ToV10MapperConfig.class)
public interface TaskStateMapper {

    /**
     * Singleton instance accessed via {@link A03Mappers} factory.
     */
    TaskStateMapper INSTANCE = A03Mappers.getMapper(TaskStateMapper.class);

    /**
     * Converts a v0.3 {@code TaskState} to v1.0 {@code TaskState}.
     * <p>
     * Mapping:
     * <pre>
     * 0.3                    → 1.0
     * ─────────────────────────────────────────────
     * SUBMITTED              → TASK_STATE_SUBMITTED
     * WORKING                → TASK_STATE_WORKING
     * INPUT_REQUIRED         → TASK_STATE_INPUT_REQUIRED
     * AUTH_REQUIRED          → TASK_STATE_AUTH_REQUIRED
     * COMPLETED              → TASK_STATE_COMPLETED
     * CANCELED               → TASK_STATE_CANCELED
     * FAILED                 → TASK_STATE_FAILED
     * REJECTED               → TASK_STATE_REJECTED
     * UNKNOWN                → UNRECOGNIZED
     * null                   → UNRECOGNIZED
     * </pre>
     *
     * @param v03 the v0.3 task state (may be null)
     * @return the equivalent v1.0 task state (never null)
     */
    default org.a2aproject.sdk.spec.TaskState toV10(org.a2aproject.sdk.compat03.spec.TaskState v03) {
        if (v03 == null) {
            return org.a2aproject.sdk.spec.TaskState.UNRECOGNIZED;
        }
        return switch (v03) {
            case SUBMITTED -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_SUBMITTED;
            case WORKING -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_WORKING;
            case INPUT_REQUIRED -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_INPUT_REQUIRED;
            case AUTH_REQUIRED -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_AUTH_REQUIRED;
            case COMPLETED -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_COMPLETED;
            case CANCELED -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_CANCELED;
            case FAILED -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_FAILED;
            case REJECTED -> org.a2aproject.sdk.spec.TaskState.TASK_STATE_REJECTED;
            case UNKNOWN -> org.a2aproject.sdk.spec.TaskState.UNRECOGNIZED;
        };
    }

    /**
     * Converts a v1.0 {@code TaskState} to v0.3 {@code TaskState}.
     * <p>
     * Reverse mapping:
     * <pre>
     * 1.0                          → 0.3
     * ───────────────────────────────────────────────────
     * TASK_STATE_SUBMITTED         → SUBMITTED
     * TASK_STATE_WORKING           → WORKING
     * TASK_STATE_INPUT_REQUIRED    → INPUT_REQUIRED
     * TASK_STATE_AUTH_REQUIRED     → AUTH_REQUIRED
     * TASK_STATE_COMPLETED         → COMPLETED
     * TASK_STATE_CANCELED          → CANCELED
     * TASK_STATE_FAILED            → FAILED
     * TASK_STATE_REJECTED          → REJECTED
     * UNRECOGNIZED                 → UNKNOWN
     * null                         → UNKNOWN
     * </pre>
     *
     * @param v10 the v1.0 task state (may be null)
     * @return the equivalent v0.3 task state (never null)
     */
    default org.a2aproject.sdk.compat03.spec.TaskState fromV10(org.a2aproject.sdk.spec.TaskState v10) {
        if (v10 == null) {
            return org.a2aproject.sdk.compat03.spec.TaskState.UNKNOWN;
        }
        return switch (v10) {
            case TASK_STATE_SUBMITTED -> org.a2aproject.sdk.compat03.spec.TaskState.SUBMITTED;
            case TASK_STATE_WORKING -> org.a2aproject.sdk.compat03.spec.TaskState.WORKING;
            case TASK_STATE_INPUT_REQUIRED -> org.a2aproject.sdk.compat03.spec.TaskState.INPUT_REQUIRED;
            case TASK_STATE_AUTH_REQUIRED -> org.a2aproject.sdk.compat03.spec.TaskState.AUTH_REQUIRED;
            case TASK_STATE_COMPLETED -> org.a2aproject.sdk.compat03.spec.TaskState.COMPLETED;
            case TASK_STATE_CANCELED -> org.a2aproject.sdk.compat03.spec.TaskState.CANCELED;
            case TASK_STATE_FAILED -> org.a2aproject.sdk.compat03.spec.TaskState.FAILED;
            case TASK_STATE_REJECTED -> org.a2aproject.sdk.compat03.spec.TaskState.REJECTED;
            case UNRECOGNIZED -> org.a2aproject.sdk.compat03.spec.TaskState.UNKNOWN;
        };
    }
}
