package org.a2aproject.sdk.compat03.conversion.mappers.params;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting {@code TaskQueryParams} between A2A Protocol v0.3 and v1.0.
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: {@code TaskQueryParams(String id, int historyLength, Map<String, Object> metadata)}</li>
 *   <li>v1.0: {@code TaskQueryParams(String id, Integer historyLength, String tenant)}</li>
 * </ul>
 * <p>
 * Conversion strategy:
 * <ul>
 *   <li>0.3 → 1.0:
 *     <ul>
 *       <li>{@code historyLength}: primitive int → nullable Integer (0 → null)</li>
 *       <li>{@code metadata}: dropped</li>
 *       <li>{@code tenant}: added as ""</li>
 *     </ul>
 *   </li>
 *   <li>1.0 → 0.3:
 *     <ul>
 *       <li>{@code historyLength}: nullable Integer → primitive int (null → 0)</li>
 *       <li>{@code metadata}: added as null</li>
 *       <li>{@code tenant}: dropped</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @see TaskQueryParams_v0_3
 * @see TaskQueryParams
 */
@Mapper(config = A03ToV10MapperConfig.class)
public interface TaskQueryParamsMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    TaskQueryParamsMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(TaskQueryParamsMapper_v0_3.class);

    /**
     * Converts v0.3 {@code TaskQueryParams} to v1.0 {@code TaskQueryParams}.
     * <p>
     * The {@code metadata} field from v0.3 is dropped, the {@code historyLength} is converted
     * from primitive int to nullable Integer (0 becomes null), and the v1.0 {@code tenant}
     * field is set to the empty string default.
     *
     * @param v03 the v0.3 task query params
     * @return the equivalent v1.0 task query params
     */
    default TaskQueryParams toV10(TaskQueryParams_v0_3 v03) {
        if (v03 == null) {
            return null;
        }
        // Convert historyLength: 0 (default) → null, non-zero → Integer
        Integer historyLength = v03.historyLength() == 0 ? null : v03.historyLength();
        return new TaskQueryParams(v03.id(), historyLength, "");
    }

    /**
     * Converts v1.0 {@code TaskQueryParams} to v0.3 {@code TaskQueryParams}.
     * <p>
     * The {@code tenant} field from v1.0 is dropped, the {@code historyLength} is converted
     * from nullable Integer to primitive int (null becomes 0), and the v0.3 {@code metadata}
     * field is set to null.
     *
     * @param v10 the v1.0 task query params
     * @return the equivalent v0.3 task query params
     */
    default TaskQueryParams_v0_3 fromV10(TaskQueryParams v10) {
        if (v10 == null) {
            return null;
        }
        // Convert historyLength: null → 0 (default), Integer → int
        int historyLength = v10.historyLength() == null ? 0 : v10.historyLength();
        return new TaskQueryParams_v0_3(v10.id(), historyLength, null);
    }
}
