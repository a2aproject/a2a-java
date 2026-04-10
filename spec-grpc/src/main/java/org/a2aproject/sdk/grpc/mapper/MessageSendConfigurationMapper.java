package org.a2aproject.sdk.grpc.mapper;

import org.a2aproject.sdk.spec.MessageSendConfiguration;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.spec.MessageSendConfiguration} and {@link org.a2aproject.sdk.grpc.SendMessageConfiguration}.
 * <p>
 * Handles bidirectional mapping with null/empty list conversions and task push notification config delegation.
 * Uses ADDER_PREFERRED strategy to avoid ProtocolStringList instantiation issues.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {TaskPushNotificationConfigMapper.class, A2ACommonFieldMapper.class})
public interface MessageSendConfigurationMapper {

    MessageSendConfigurationMapper INSTANCE = A2AMappers.getMapper(MessageSendConfigurationMapper.class);

    /**
     * Converts domain MessageSendConfiguration to proto SendMessageConfiguration.
     */
    @Mapping(target = "taskPushNotificationConfig", source = "taskPushNotificationConfig", conditionExpression = "java(domain.taskPushNotificationConfig() != null)")
    org.a2aproject.sdk.grpc.SendMessageConfiguration toProto(MessageSendConfiguration domain);

    /**
     * Converts proto SendMessageConfiguration to domain MessageSendConfiguration.
     * Uses Builder pattern for record construction.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "acceptedOutputModes", expression = "java(org.a2aproject.sdk.grpc.mapper.A2ACommonFieldMapper.INSTANCE.emptyListToNull(proto.getAcceptedOutputModesList()))")
    MessageSendConfiguration fromProto(org.a2aproject.sdk.grpc.SendMessageConfiguration proto);
}
