package io.a2a.grpc.mapper;

import io.a2a.spec.Message;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.spec.Message.Role} and {@link io.a2a.grpc.Role}.
 * <p>
 * Handles enum conversion between domain and protobuf role representations:
 * <ul>
 *   <li>USER (domain) ↔ ROLE_USER (proto)</li>
 *   <li>AGENT (domain) ↔ ROLE_AGENT (proto)</li>
 * </ul>
 * <p>
 * <b>Manual Implementation Required:</b> Uses manual switch statements instead of @ValueMapping
 * to avoid mapstruct-spi-protobuf enum strategy initialization issues.
 */
@Mapper(config = ProtoMapperConfig.class)
public interface RoleMapper {

    RoleMapper INSTANCE = Mappers.getMapper(RoleMapper.class);

    /**
     * Converts domain Role to proto Role.
     * Maps USER → ROLE_USER, AGENT → ROLE_AGENT.
     */
    default io.a2a.grpc.Role toProto(Message.Role domain) {
        if (domain == null) {
            return io.a2a.grpc.Role.ROLE_UNSPECIFIED;
        }
        return switch (domain) {
            case USER -> io.a2a.grpc.Role.ROLE_USER;
            case AGENT -> io.a2a.grpc.Role.ROLE_AGENT;
        };
    }

    /**
     * Converts proto Role to domain Role.
     * Maps ROLE_USER → USER, ROLE_AGENT → AGENT.
     * ROLE_UNSPECIFIED returns null.
     */
    default Message.Role fromProto(io.a2a.grpc.Role proto) {
        if (proto == null || proto == io.a2a.grpc.Role.ROLE_UNSPECIFIED) {
            return null;
        }
        return switch (proto) {
            case ROLE_USER -> Message.Role.USER;
            case ROLE_AGENT -> Message.Role.AGENT;
            default -> null;
        };
    }
}
