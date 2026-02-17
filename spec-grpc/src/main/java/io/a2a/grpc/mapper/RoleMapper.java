package io.a2a.grpc.mapper;

import io.a2a.grpc.Role;
import io.a2a.spec.Message;
import org.mapstruct.Mapper;

/**
 * Mapper between {@link io.a2a.spec.Message.Role} and {@link io.a2a.grpc.Role}.
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
    default io.a2a.grpc.Role toProto(Message.Role domain) {
        if (domain == null) {
            return io.a2a.grpc.Role.ROLE_UNSPECIFIED;
        }
        return switch (domain) {
            case ROLE_USER -> io.a2a.grpc.Role.ROLE_USER;
            case ROLE_AGENT -> io.a2a.grpc.Role.ROLE_AGENT;
            case ROLE_UNSPECIFIED -> Role.ROLE_UNSPECIFIED;
        };
    }

    /**
     * Converts proto Role to domain Role.
     */
    default Message.Role fromProto(io.a2a.grpc.Role proto) {
        if (proto == null || proto == io.a2a.grpc.Role.ROLE_UNSPECIFIED) {
            return null;
        }
        return switch (proto) {
            case ROLE_USER -> Message.Role.ROLE_USER;
            case ROLE_AGENT -> Message.Role.ROLE_AGENT;
            default -> Message.Role.ROLE_UNSPECIFIED;
        };
    }
}
