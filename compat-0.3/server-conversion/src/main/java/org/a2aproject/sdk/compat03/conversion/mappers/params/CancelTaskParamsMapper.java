package org.a2aproject.sdk.compat03.conversion.mappers.params;

import java.util.Collections;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A03Mappers;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
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
 * @see org.a2aproject.sdk.compat03.spec.TaskIdParams
 * @see org.a2aproject.sdk.spec.CancelTaskParams
 */
@Mapper(config = A03ToV10MapperConfig.class)
public interface CancelTaskParamsMapper {

    /**
     * Singleton instance accessed via {@link A03Mappers} factory.
     */
    CancelTaskParamsMapper INSTANCE = A03Mappers.getMapper(CancelTaskParamsMapper.class);

    /**
     * Converts v0.3 {@code TaskIdParams} to v1.0 {@code CancelTaskParams}.
     * <p>
     * The v0.3 {@code metadata} field is preserved in the v1.0 type, and the v1.0
     * {@code tenant} field is set to the empty string default.
     *
     * @param v03 the v0.3 task ID params used for cancel operations
     * @return the equivalent v1.0 cancel task params
     */
    default org.a2aproject.sdk.spec.CancelTaskParams toV10(org.a2aproject.sdk.compat03.spec.TaskIdParams v03) {
        if (v03 == null) {
            return null;
        }
        return new org.a2aproject.sdk.spec.CancelTaskParams(
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
    default org.a2aproject.sdk.compat03.spec.TaskIdParams fromV10(org.a2aproject.sdk.spec.CancelTaskParams v10) {
        if (v10 == null) {
            return null;
        }
        return new org.a2aproject.sdk.compat03.spec.TaskIdParams(v10.id(), v10.metadata());
    }
}
