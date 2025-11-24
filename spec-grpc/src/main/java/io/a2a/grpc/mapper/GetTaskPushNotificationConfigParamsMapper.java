package io.a2a.grpc.mapper;

import io.a2a.spec.GetTaskPushNotificationConfigParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.grpc.GetTaskPushNotificationConfigRequest} and {@link io.a2a.spec.GetTaskPushNotificationConfigParams}.
 * <p>
 * Extracts task ID and config ID from resource name using {@link ResourceNameParser}.
 * Handles both formats:
 * - "tasks/{taskId}" (uses taskId as configId)
 * - "tasks/{taskId}/pushNotificationConfigs/{configId}"
 */
@Mapper(config = ProtoMapperConfig.class)
public interface GetTaskPushNotificationConfigParamsMapper {

    GetTaskPushNotificationConfigParamsMapper INSTANCE = Mappers.getMapper(GetTaskPushNotificationConfigParamsMapper.class);

    /**
     * Converts proto GetTaskPushNotificationConfigRequest to domain GetTaskPushNotificationConfigParams.
     * Parses the name field to extract both task ID and config ID.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", expression = "java(ResourceNameParser.parseGetTaskPushNotificationConfigName(proto.getName())[0])")
    @Mapping(target = "pushNotificationConfigId", expression = "java(ResourceNameParser.parseGetTaskPushNotificationConfigName(proto.getName())[1])")
    @Mapping(target = "metadata", ignore = true)
    GetTaskPushNotificationConfigParams fromProto(io.a2a.grpc.GetTaskPushNotificationConfigRequest proto);
}
