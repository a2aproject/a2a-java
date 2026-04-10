package org.a2aproject.sdk.grpc.mapper;

import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest} and {@link org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams}.
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface GetTaskPushNotificationConfigParamsMapper {

    GetTaskPushNotificationConfigParamsMapper INSTANCE = A2AMappers.getMapper(GetTaskPushNotificationConfigParamsMapper.class);

    /**
     * Converts proto GetTaskPushNotificationConfigRequest to domain GetTaskPushNotificationConfigParams.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenant", source = "tenant")
    GetTaskPushNotificationConfigParams fromProto(org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest proto);

    /**
     * Converts domain GetTaskPushNotificationConfigParams to proto GetTaskPushNotificationConfigRequest.
     */
    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "id", source = "id", conditionExpression = "java(domain.id() != null)")
    @Mapping(target = "tenant", source = "tenant")
    org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest toProto(GetTaskPushNotificationConfigParams domain);
}
