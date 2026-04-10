package org.a2aproject.sdk.compat03.conversion.mappers.params;

import java.util.Collections;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.spec.TaskIdParams_v0_3;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting cancel task parameters between A2A Protocol v0.3 and v1.0.
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: Uses {@code TaskIdParams(String id, Map<String, Object> metadata)}</li>
 *   <li>v1.0: Uses {@code CancelTaskParams(String id, String tenant, Map<String, Object> metadata)}</li>
 * </ul>
 * <p>
 * Conversion strategy:
 * <ul>
 *   <li>0.3 → 1.0: Convert {@code TaskIdParams} to {@code CancelTaskParams} (add {@code tenant} field = "", preserve {@code metadata})</li>
 *   <li>1.0 → 0.3: Convert {@code CancelTaskParams} to {@code TaskIdParams} (drop {@code tenant} field, preserve {@code metadata})</li>
 * </ul>
 *
 * @see TaskIdParams_v0_3
 * @see CancelTaskParams
 */
@Mapper(config = A03ToV10MapperConfig.class)
public interface CancelTaskParamsMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    CancelTaskParamsMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(CancelTaskParamsMapper_v0_3.class);

    /**
     * Converts v0.3 {@code TaskIdParams} to v1.0 {@code CancelTaskParams}.
     * <p>
     * The v0.3 {@code metadata} field is preserved in the v1.0 type, and the v1.0
     * {@code tenant} field is set to the empty string default.
     *
     * @param v03 the v0.3 task ID params used for cancel operations
     * @return the equivalent v1.0 cancel task params
     */
    default CancelTaskParams toV10(TaskIdParams_v0_3 v03) {
        if (v03 == null) {
            return null;
        }
        return new CancelTaskParams(
            v03.id(),
            "",  // Default tenant
            v03.metadata() != null ? v03.metadata() : Collections.emptyMap()
        );
    }

    /**
     * Converts v1.0 {@code CancelTaskParams} to v0.3 {@code TaskIdParams}.
     * <p>
     * The v1.0 {@code tenant} field is dropped, and the v1.0 {@code metadata} field
     * is preserved in the v0.3 type.
     *
     * @param v10 the v1.0 cancel task params
     * @return the equivalent v0.3 task ID params
     */
    default TaskIdParams_v0_3 fromV10(CancelTaskParams v10) {
        if (v10 == null) {
            return null;
        }
        return new TaskIdParams_v0_3(v10.id(), v10.metadata());
    }
}
