package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A03Mappers;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
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
public interface RoleMapper {

    /**
     * Singleton instance accessed via {@link A03Mappers} factory.
     */
    RoleMapper INSTANCE = A03Mappers.getMapper(RoleMapper.class);

    /**
     * Converts v0.3 Role to v1.0 Role.
     *
     * @param v03 the v0.3 role
     * @return the equivalent v1.0 role
     */
    default Message.Role toV10(org.a2aproject.sdk.compat03.spec.Message.Role v03) {
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
    default org.a2aproject.sdk.compat03.spec.Message.Role fromV10(Message.Role v10) {
        if (v10 == null) {
            return null;
        }
        return switch (v10) {
            case ROLE_USER -> org.a2aproject.sdk.compat03.spec.Message.Role.USER;
            case ROLE_AGENT -> org.a2aproject.sdk.compat03.spec.Message.Role.AGENT;
            default -> throw new IllegalArgumentException("Unrecognized Role: " + v10);
        };
    }
}
