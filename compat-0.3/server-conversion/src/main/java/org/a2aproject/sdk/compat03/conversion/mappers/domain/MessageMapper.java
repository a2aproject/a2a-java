package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A03Mappers;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Part;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting Message between A2A Protocol v0.3 and v1.0.
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: Message is a class with getter methods (e.g., {@code getRole()}, {@code getParts()})</li>
 *   <li>v1.0: Message is a record with accessor methods (e.g., {@code role()}, {@code parts()})</li>
 *   <li>Role enum values have "ROLE_" prefix in v1.0</li>
 *   <li>Part types (TextPart, FilePart, DataPart) changed from classes to records in v1.0</li>
 * </ul>
 * <p>
 * Uses {@link RoleMapper} and {@link PartMapper} for nested conversions.
 */
@Mapper(config = A03ToV10MapperConfig.class, uses = {RoleMapper.class, PartMapper.class})
public interface MessageMapper {

    /**
     * Singleton instance accessed via {@link A03Mappers} factory.
     */
    MessageMapper INSTANCE = A03Mappers.getMapper(MessageMapper.class);

    /**
     * Converts v0.3 Message to v1.0 Message.
     * <p>
     * Converts all fields including role, parts, messageId, contextId, taskId,
     * referenceTaskIds, metadata, and extensions.
     *
     * @param v03 the v0.3 message
     * @return the equivalent v1.0 message
     */
    default Message toV10(org.a2aproject.sdk.compat03.spec.Message v03) {
        if (v03 == null) {
            return null;
        }

        Message.Role role = RoleMapper.INSTANCE.toV10(v03.getRole());
        List<Part<?>> parts = v03.getParts().stream()
            .map(PartMapper.INSTANCE::toV10)
            .collect(Collectors.toList());

        return new Message(
            role,
            parts,
            v03.getMessageId(),
            v03.getContextId(),
            v03.getTaskId(),
            v03.getReferenceTaskIds(),
            v03.getMetadata(),
            v03.getExtensions()
        );
    }

    /**
     * Converts v1.0 Message to v0.3 Message.
     * <p>
     * Converts all fields including role, parts, messageId, contextId, taskId,
     * referenceTaskIds, metadata, and extensions.
     *
     * @param v10 the v1.0 message
     * @return the equivalent v0.3 message
     */
    default org.a2aproject.sdk.compat03.spec.Message fromV10(Message v10) {
        if (v10 == null) {
            return null;
        }

        org.a2aproject.sdk.compat03.spec.Message.Role role = RoleMapper.INSTANCE.fromV10(v10.role());
        List<org.a2aproject.sdk.compat03.spec.Part<?>> parts = v10.parts().stream()
            .map(PartMapper.INSTANCE::fromV10)
            .collect(Collectors.toList());

        return new org.a2aproject.sdk.compat03.spec.Message(
            role,
            parts,
            v10.messageId(),
            v10.contextId(),
            v10.taskId(),
            v10.referenceTaskIds(),
            v10.metadata(),
            v10.extensions()
        );
    }
}
