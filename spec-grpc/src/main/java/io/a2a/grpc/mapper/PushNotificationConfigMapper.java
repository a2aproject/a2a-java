package io.a2a.grpc.mapper;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.spec.PushNotificationConfig} and {@link io.a2a.grpc.PushNotificationConfig}.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {AuthenticationInfoMapper.class, A2ACommonFieldMapper.class})
public interface PushNotificationConfigMapper {

    PushNotificationConfigMapper INSTANCE = A2AMappers.getMapper(PushNotificationConfigMapper.class);

    @Mapping(target = "url", source = "url", conditionExpression = "java(domain.url() != null)")
    @Mapping(target = "token", source = "token", conditionExpression = "java(domain.token() != null)")
    @Mapping(target = "authentication", source = "authentication", conditionExpression = "java(domain.authentication() != null)")
    @Mapping(target = "id", source = "id", conditionExpression = "java(domain.id() != null)")
    io.a2a.grpc.PushNotificationConfig toProto(io.a2a.spec.PushNotificationConfig domain);

    /**
     * Converts proto PushNotificationConfig to domain.
     * Uses declarative mappings with empty string → null conversion via CommonFieldMapper.
     */
    @Mapping(target = "token", source = "token", qualifiedByName = "emptyToNull")
    @Mapping(target = "id", source = "id", qualifiedByName = "emptyToNull")
    @Mapping(target = "authentication", source = "authentication", conditionExpression = "java(proto.hasAuthentication())")
    io.a2a.spec.PushNotificationConfig fromProto(io.a2a.grpc.PushNotificationConfig proto);
}
