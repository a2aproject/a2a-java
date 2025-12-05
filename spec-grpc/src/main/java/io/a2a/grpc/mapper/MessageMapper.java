package io.a2a.grpc.mapper;

import io.a2a.spec.Message;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.Message} and {@link io.a2a.grpc.Message}.
 * <p>
 * Uses ADDER_PREFERRED strategy for List fields (parts, extensions, referenceTaskIds)
 * to avoid ProtocolStringList instantiation issues.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {RoleMapper.class, PartMapper.class, A2ACommonFieldMapper.class})
public interface MessageMapper {

    MessageMapper INSTANCE = A2AMappers.getMapper(MessageMapper.class);

    /**
     * Converts domain Message to proto Message.
     * Uses CommonFieldMapper for metadata conversion and ADDER_PREFERRED for lists.
     */
    @Mapping(target = "messageId", source = "messageId", conditionExpression = "java(domain.getMessageId() != null)")
    @Mapping(target = "contextId", source = "contextId", conditionExpression = "java(domain.getContextId() != null)")
    @Mapping(target = "taskId", source = "taskId", conditionExpression = "java(domain.getTaskId() != null)")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataToProto")
    io.a2a.grpc.Message toProto(Message domain);

    /**
     * Converts proto Message to domain Message.
     * Handles empty string → null and Struct conversions via CommonFieldMapper.
     * Uses Builder pattern explicitly configured via @BeanMapping.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "messageId", source = "messageId", qualifiedByName = "emptyToNull")
    @Mapping(target = "contextId", source = "contextId", qualifiedByName = "emptyToNull")
    @Mapping(target = "taskId", source = "taskId", qualifiedByName = "emptyToNull")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataFromProto")
    @Mapping(target = "extensions", expression = "java(io.a2a.grpc.mapper.A2ACommonFieldMapper.INSTANCE.emptyListToNull(proto.getExtensionsList()))")
    @Mapping(target = "referenceTaskIds", expression = "java(io.a2a.grpc.mapper.A2ACommonFieldMapper.INSTANCE.emptyListToNull(proto.getReferenceTaskIdsList()))")
    Message fromProto(io.a2a.grpc.Message proto);
}
