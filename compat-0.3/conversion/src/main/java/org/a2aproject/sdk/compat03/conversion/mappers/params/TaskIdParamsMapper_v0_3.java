package org.a2aproject.sdk.compat03.conversion.mappers.params;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting {@code TaskIdParams} between A2A Protocol v0.3 and v1.0.
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: {@code TaskIdParams(String id, Map<String, Object> metadata)}</li>
 *   <li>v1.0: {@code TaskIdParams(String id, String tenant)}</li>
 * </ul>
 * <p>
 * Conversion strategy:
 * <ul>
 *   <li>0.3 → 1.0: Drop {@code metadata} field, add {@code tenant} field (default to "")</li>
 *   <li>1.0 → 0.3: Drop {@code tenant} field, add {@code metadata} field (set to null)</li>
 * </ul>
 *
 * @see TaskIdParams_v0_3
 * @see TaskIdParams
 */
@Mapper(config = A03ToV10MapperConfig.class)
public interface TaskIdParamsMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    TaskIdParamsMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(TaskIdParamsMapper_v0_3.class);

    /**
     * Converts v0.3 {@code TaskIdParams} to v1.0 {@code TaskIdParams}.
     * <p>
     * The {@code metadata} field from v0.3 is dropped, and the v1.0 {@code tenant} field
     * is set to the empty string default.
     *
     * @param v03 the v0.3 task ID params
     * @return the equivalent v1.0 task ID params
     */
    default TaskIdParams toV10(TaskIdParams_v0_3 v03) {
        if (v03 == null) {
            return null;
        }
        return new TaskIdParams(v03.id(), "");
    }

    /**
     * Converts v1.0 {@code TaskIdParams} to v0.3 {@code TaskIdParams}.
     * <p>
     * The {@code tenant} field from v1.0 is dropped, and the v0.3 {@code metadata} field
     * is set to null.
     *
     * @param v10 the v1.0 task ID params
     * @return the equivalent v0.3 task ID params
     */
    default TaskIdParams_v0_3 fromV10(TaskIdParams v10) {
        if (v10 == null) {
            return null;
        }
        return new TaskIdParams_v0_3(v10.id(), null);
    }
}
