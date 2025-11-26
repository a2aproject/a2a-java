package io.a2a.grpc.mapper;

import io.a2a.spec.MessageSendParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.MessageSendParams} and {@link io.a2a.grpc.SendMessageRequest}.
 * <p>
 * Handles bidirectional mapping with message/request field name difference and Struct conversions.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = {MessageMapper.class, MessageSendConfigurationMapper.class, A2ACommonFieldMapper.class})
public interface MessageSendParamsMapper {

    MessageSendParamsMapper INSTANCE = A2AMappers.getMapper(MessageSendParamsMapper.class);

    /**
     * Converts domain MessageSendParams to proto SendMessageRequest.
     * Maps domain "message" field to proto "request" field.
     */
    @Mapping(target = "request", source = "message")
    @Mapping(target = "configuration", source = "configuration", conditionExpression = "java(domain.configuration() != null)")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataToProto")
    io.a2a.grpc.SendMessageRequest toProto(MessageSendParams domain);

    /**
     * Converts proto SendMessageRequest to domain MessageSendParams.
     * Maps proto "request" field to domain "message" field.
     * Uses Builder pattern for record construction.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "message", source = "request")
    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "metadataFromProto")
    MessageSendParams fromProto(io.a2a.grpc.SendMessageRequest proto);
}
