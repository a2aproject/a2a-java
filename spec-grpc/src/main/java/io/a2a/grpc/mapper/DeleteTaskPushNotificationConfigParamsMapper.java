package io.a2a.grpc.mapper;

import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.grpc.DeleteTaskPushNotificationConfigRequest} and {@link io.a2a.spec.DeleteTaskPushNotificationConfigParams}.
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface DeleteTaskPushNotificationConfigParamsMapper {

    DeleteTaskPushNotificationConfigParamsMapper INSTANCE = A2AMappers.getMapper(DeleteTaskPushNotificationConfigParamsMapper.class);

    /**
     * Converts proto DeleteTaskPushNotificationConfigRequest to domain DeleteTaskPushNotificationConfigParams.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "taskId")
    @Mapping(target = "pushNotificationConfigId", source = "id")
    @Mapping(target = "tenant", source = "tenant")
    DeleteTaskPushNotificationConfigParams fromProto(io.a2a.grpc.DeleteTaskPushNotificationConfigRequest proto);

    /**
     * Converts domain DeleteTaskPushNotificationConfigParams to proto DeleteTaskPushNotificationConfigRequest.
     */
    @Mapping(target = "taskId", source = "id")
    @Mapping(target = "id", source = "pushNotificationConfigId")
    @Mapping(target = "tenant", source = "tenant")
    io.a2a.grpc.DeleteTaskPushNotificationConfigRequest toProto(DeleteTaskPushNotificationConfigParams domain);
}
