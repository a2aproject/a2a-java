package org.a2aproject.sdk.grpc.mapper;

import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for TaskPushNotificationConfig.
 * <p>
 * Maps between domain TaskPushNotificationConfig and proto TaskPushNotificationConfig.
 * The proto has direct id, task_id, url, token, authentication, and tenant fields.
 */
@Mapper(config = A2AProtoMapperConfig.class,
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        uses = {AuthenticationInfoMapper.class, A2ACommonFieldMapper.class})
public interface TaskPushNotificationConfigMapper {

    TaskPushNotificationConfigMapper INSTANCE = A2AMappers.getMapper(TaskPushNotificationConfigMapper.class);

    /**
     * Converts domain TaskPushNotificationConfig to protobuf TaskPushNotificationConfig.
     *
     * @param domain the domain TaskPushNotificationConfig
     * @return protobuf TaskPushNotificationConfig
     */
    @Mapping(target = "id", source = "id", conditionExpression = "java(domain.id() != null)")
    @Mapping(target = "taskId", source = "taskId", conditionExpression = "java(domain.taskId() != null)")
    @Mapping(target = "url", source = "url", conditionExpression = "java(domain.url() != null)")
    @Mapping(target = "token", source = "token", conditionExpression = "java(domain.token() != null)")
    @Mapping(target = "tenant", source = "tenant", conditionExpression = "java(domain.tenant() != null)")
    @Mapping(target = "authentication", source = "authentication", conditionExpression = "java(domain.authentication() != null)")
    org.a2aproject.sdk.grpc.TaskPushNotificationConfig toProto(TaskPushNotificationConfig domain);

    /**
     * Converts protobuf TaskPushNotificationConfig to domain TaskPushNotificationConfig.
     *
     * @param proto the protobuf TaskPushNotificationConfig
     * @return domain TaskPushNotificationConfig
     */
    @Mapping(target = "token", source = "token", qualifiedByName = "emptyToNull")
    @Mapping(target = "tenant", source = "tenant", qualifiedByName = "emptyToNull")
    @Mapping(target = "taskId", source = "taskId", qualifiedByName = "emptyToNull")
    @Mapping(target = "authentication", source = "authentication", conditionExpression = "java(proto.hasAuthentication())")
    TaskPushNotificationConfig fromProto(org.a2aproject.sdk.grpc.TaskPushNotificationConfig proto);
}
