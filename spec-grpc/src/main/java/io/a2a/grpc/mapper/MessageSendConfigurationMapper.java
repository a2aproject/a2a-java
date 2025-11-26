package io.a2a.grpc.mapper;

import io.a2a.spec.MessageSendConfiguration;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.MessageSendConfiguration} and {@link io.a2a.grpc.SendMessageConfiguration}.
 * <p>
 * Handles bidirectional mapping with null/empty list conversions and push notification config delegation.
 * Uses ADDER_PREFERRED strategy to avoid ProtocolStringList instantiation issues.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {PushNotificationConfigMapper.class, A2ACommonFieldMapper.class})
public interface MessageSendConfigurationMapper {

    MessageSendConfigurationMapper INSTANCE = A2AMappers.getMapper(MessageSendConfigurationMapper.class);

    /**
     * Converts domain MessageSendConfiguration to proto SendMessageConfiguration.
     */
    @Mapping(target = "pushNotificationConfig", source = "pushNotificationConfig", conditionExpression = "java(domain.pushNotificationConfig() != null)")
    io.a2a.grpc.SendMessageConfiguration toProto(MessageSendConfiguration domain);

    /**
     * Converts proto SendMessageConfiguration to domain MessageSendConfiguration.
     * Uses Builder pattern for record construction.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "acceptedOutputModes", expression = "java(io.a2a.grpc.mapper.A2ACommonFieldMapper.INSTANCE.emptyListToNull(proto.getAcceptedOutputModesList()))")
    MessageSendConfiguration fromProto(io.a2a.grpc.SendMessageConfiguration proto);
}
