package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.spec.TaskState;
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
 * @see TaskState_v0_3
 * @see TaskState
 */
@Mapper(config = A03ToV10MapperConfig.class)
public interface TaskStateMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    TaskStateMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(TaskStateMapper_v0_3.class);

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
    default TaskState toV10(TaskState_v0_3 v03) {
        if (v03 == null) {
            return TaskState.UNRECOGNIZED;
        }
        return switch (v03) {
            case SUBMITTED -> TaskState.TASK_STATE_SUBMITTED;
            case WORKING -> TaskState.TASK_STATE_WORKING;
            case INPUT_REQUIRED -> TaskState.TASK_STATE_INPUT_REQUIRED;
            case AUTH_REQUIRED -> TaskState.TASK_STATE_AUTH_REQUIRED;
            case COMPLETED -> TaskState.TASK_STATE_COMPLETED;
            case CANCELED -> TaskState.TASK_STATE_CANCELED;
            case FAILED -> TaskState.TASK_STATE_FAILED;
            case REJECTED -> TaskState.TASK_STATE_REJECTED;
            case UNKNOWN -> TaskState.UNRECOGNIZED;
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
    default TaskState_v0_3 fromV10(TaskState v10) {
        if (v10 == null) {
            return TaskState_v0_3.UNKNOWN;
        }
        return switch (v10) {
            case TASK_STATE_SUBMITTED -> TaskState_v0_3.SUBMITTED;
            case TASK_STATE_WORKING -> TaskState_v0_3.WORKING;
            case TASK_STATE_INPUT_REQUIRED -> TaskState_v0_3.INPUT_REQUIRED;
            case TASK_STATE_AUTH_REQUIRED -> TaskState_v0_3.AUTH_REQUIRED;
            case TASK_STATE_COMPLETED -> TaskState_v0_3.COMPLETED;
            case TASK_STATE_CANCELED -> TaskState_v0_3.CANCELED;
            case TASK_STATE_FAILED -> TaskState_v0_3.FAILED;
            case TASK_STATE_REJECTED -> TaskState_v0_3.REJECTED;
            case UNRECOGNIZED -> TaskState_v0_3.UNKNOWN;
        };
    }
}
