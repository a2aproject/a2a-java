package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.spec.Message;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting Message.Role enum between A2A Protocol v0.3 and v1.0.
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: {@code USER}, {@code AGENT}</li>
 *   <li>v1.0: {@code ROLE_USER}, {@code ROLE_AGENT}</li>
 * </ul>
 * <p>
 * The v1.0 enum adds a "ROLE_" prefix to align with protocol buffer conventions.
 */
@Mapper(config = A03ToV10MapperConfig.class)
public interface RoleMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    RoleMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(RoleMapper_v0_3.class);

    /**
     * Converts v0.3 Role to v1.0 Role.
     *
     * @param v03 the v0.3 role
     * @return the equivalent v1.0 role
     */
    default Message.Role toV10(Message_v0_3.Role v03) {
        if (v03 == null) {
            return null;
        }
        return switch (v03) {
            case USER -> Message.Role.ROLE_USER;
            case AGENT -> Message.Role.ROLE_AGENT;
        };
    }

    /**
     * Converts v1.0 Role to v0.3 Role.
     *
     * @param v10 the v1.0 role
     * @return the equivalent v0.3 role
     */
    default Message_v0_3.Role fromV10(Message.Role v10) {
        if (v10 == null) {
            return null;
        }
        return switch (v10) {
            case ROLE_USER -> Message_v0_3.Role.USER;
            case ROLE_AGENT -> Message_v0_3.Role.AGENT;
            default -> throw new IllegalArgumentException("Unrecognized Role: " + v10);
        };
    }
}
