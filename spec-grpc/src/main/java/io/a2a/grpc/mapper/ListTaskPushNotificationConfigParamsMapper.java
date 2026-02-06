package io.a2a.grpc.mapper;

import io.a2a.spec.ListTaskPushNotificationConfigParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.grpc.ListTaskPushNotificationConfigRequest} and {@link io.a2a.spec.ListTaskPushNotificationConfigParams}.
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface ListTaskPushNotificationConfigParamsMapper {

    ListTaskPushNotificationConfigParamsMapper INSTANCE = A2AMappers.getMapper(ListTaskPushNotificationConfigParamsMapper.class);

    /**
     * Converts proto ListTaskPushNotificationConfigRequest to domain ListTaskPushNotificationConfigParams.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "taskId")
    @Mapping(target = "tenant", source = "tenant")
    ListTaskPushNotificationConfigParams fromProto(io.a2a.grpc.ListTaskPushNotificationConfigRequest proto);

    /**
     * Converts domain ListTaskPushNotificationConfigParams to proto ListTaskPushNotificationConfigRequest.
     */
    @Mapping(target = "taskId", source = "id")
    @Mapping(target = "tenant", source = "tenant")
    io.a2a.grpc.ListTaskPushNotificationConfigRequest toProto(ListTaskPushNotificationConfigParams domain);
}
