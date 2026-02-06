package io.a2a.grpc.mapper;

import io.a2a.spec.GetTaskPushNotificationConfigParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.grpc.GetTaskPushNotificationConfigRequest} and {@link io.a2a.spec.GetTaskPushNotificationConfigParams}.
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface GetTaskPushNotificationConfigParamsMapper {

    GetTaskPushNotificationConfigParamsMapper INSTANCE = A2AMappers.getMapper(GetTaskPushNotificationConfigParamsMapper.class);

    /**
     * Converts proto GetTaskPushNotificationConfigRequest to domain GetTaskPushNotificationConfigParams.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "taskId")
    @Mapping(target = "pushNotificationConfigId", source = "id")
    @Mapping(target = "tenant", source = "tenant")
    GetTaskPushNotificationConfigParams fromProto(io.a2a.grpc.GetTaskPushNotificationConfigRequest proto);

    /**
     * Converts domain GetTaskPushNotificationConfigParams to proto GetTaskPushNotificationConfigRequest.
     */
    @Mapping(target = "taskId", source = "id")
    @Mapping(target = "id", source = "pushNotificationConfigId", conditionExpression = "java(domain.pushNotificationConfigId() != null)")
    @Mapping(target = "tenant", source = "tenant")
    io.a2a.grpc.GetTaskPushNotificationConfigRequest toProto(GetTaskPushNotificationConfigParams domain);
}
