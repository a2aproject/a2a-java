package org.a2aproject.sdk.grpc.mapper;

import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest} and {@link org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams}.
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface DeleteTaskPushNotificationConfigParamsMapper {

    DeleteTaskPushNotificationConfigParamsMapper INSTANCE = A2AMappers.getMapper(DeleteTaskPushNotificationConfigParamsMapper.class);

    /**
     * Converts proto DeleteTaskPushNotificationConfigRequest to domain DeleteTaskPushNotificationConfigParams.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenant", source = "tenant")
    DeleteTaskPushNotificationConfigParams fromProto(org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest proto);

    /**
     * Converts domain DeleteTaskPushNotificationConfigParams to proto DeleteTaskPushNotificationConfigRequest.
     */
    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "id", source = "id")
    @Mapping(target = "tenant", source = "tenant")
    org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest toProto(DeleteTaskPushNotificationConfigParams domain);
}
