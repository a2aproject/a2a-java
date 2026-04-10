package org.a2aproject.sdk.grpc.mapper;

import org.a2aproject.sdk.grpc.Role;
import org.a2aproject.sdk.spec.Message;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.Message.Role} and {@link org.a2aproject.sdk.grpc.Role}.
 * <p>
 * Handles enum conversion between domain and protobuf role representations:
 * <ul>
 *   <li>ROLE_USER (domain) ↔ ROLE_USER (proto)</li>
 *   <li>ROLE_AGENT (domain) ↔ ROLE_AGENT (proto)</li>
 *   <li>ROLE_UNSPECIFIED (domain) ↔ ROLE_UNSPECIFIED (proto)</li>
 * </ul>
 * <p>
 * <b>Manual Implementation Required:</b> Uses manual switch statements instead of @ValueMapping
 * to avoid mapstruct-spi-protobuf enum strategy initialization issues.
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface RoleMapper {

    RoleMapper INSTANCE = A2AMappers.getMapper(RoleMapper.class);

    /**
     * Converts domain Role to proto Role.
     */
    default org.a2aproject.sdk.grpc.Role toProto(Message.Role domain) {
        if (domain == null) {
            return org.a2aproject.sdk.grpc.Role.ROLE_UNSPECIFIED;
        }
        return switch (domain) {
            case ROLE_USER -> org.a2aproject.sdk.grpc.Role.ROLE_USER;
            case ROLE_AGENT -> org.a2aproject.sdk.grpc.Role.ROLE_AGENT;
            case ROLE_UNSPECIFIED -> Role.ROLE_UNSPECIFIED;
        };
    }

    /**
     * Converts proto Role to domain Role.
     */
    default Message.Role fromProto(org.a2aproject.sdk.grpc.Role proto) {
        if (proto == null || proto == org.a2aproject.sdk.grpc.Role.ROLE_UNSPECIFIED) {
            return null;
        }
        return switch (proto) {
            case ROLE_USER -> Message.Role.ROLE_USER;
            case ROLE_AGENT -> Message.Role.ROLE_AGENT;
            default -> Message.Role.ROLE_UNSPECIFIED;
        };
    }
}
