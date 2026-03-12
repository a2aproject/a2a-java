package io.a2a.grpc.mapper;

import io.a2a.spec.ListTaskPushNotificationConfigsParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.grpc.ListTaskPushNotificationConfigsRequest} and {@link io.a2a.spec.ListTaskPushNotificationConfigsParams}.
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface ListTaskPushNotificationConfigsParamsMapper {

    ListTaskPushNotificationConfigsParamsMapper INSTANCE = A2AMappers.getMapper(ListTaskPushNotificationConfigsParamsMapper.class);

    /**
     * Converts proto ListTaskPushNotificationConfigsRequest to domain ListTaskPushNotificationConfigsParams.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", source = "taskId")
    @Mapping(target = "tenant", source = "tenant")
    ListTaskPushNotificationConfigsParams fromProto(io.a2a.grpc.ListTaskPushNotificationConfigsRequest proto);

    /**
     * Converts domain ListTaskPushNotificationConfigsParams to proto ListTaskPushNotificationConfigsRequest.
     */
    @Mapping(target = "taskId", source = "id")
    @Mapping(target = "tenant", source = "tenant")
    io.a2a.grpc.ListTaskPushNotificationConfigsRequest toProto(ListTaskPushNotificationConfigsParams domain);
}
