package io.a2a.grpc.mapper;

import io.a2a.spec.GetTaskPushNotificationConfigParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.grpc.GetTaskPushNotificationConfigRequest} and {@link io.a2a.spec.GetTaskPushNotificationConfigParams}.
 * <p>
 * Extracts task ID and config ID from resource name using {@link ResourceNameParser}.
 * Handles both formats:
 * - "tasks/{taskId}" (uses taskId as configId)
 * - "tasks/{taskId}/pushNotificationConfigs/{configId}"
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface GetTaskPushNotificationConfigParamsMapper {

    GetTaskPushNotificationConfigParamsMapper INSTANCE = A2AMappers.getMapper(GetTaskPushNotificationConfigParamsMapper.class);

    /**
     * Converts proto GetTaskPushNotificationConfigRequest to domain GetTaskPushNotificationConfigParams.
     * Parses the name field to extract both task ID and config ID.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", expression = "java(ResourceNameParser.parseGetTaskPushNotificationConfigName(proto.getName())[0])")
    @Mapping(target = "pushNotificationConfigId", expression = "java(ResourceNameParser.parseGetTaskPushNotificationConfigName(proto.getName())[1])")
    @Mapping(target = "metadata", ignore = true)
    GetTaskPushNotificationConfigParams fromProto(io.a2a.grpc.GetTaskPushNotificationConfigRequest proto);

    /**
     * Converts domain Message to proto Message.Uses CommonFieldMapper for metadata conversion and ADDER_PREFERRED for lists.
     * @param domain
     * @return 
     */
    @Mapping(target = "name", expression = "java(ResourceNameParser.defineGetTaskPushNotificationConfigName(domain.id(), domain.pushNotificationConfigId()))")
    io.a2a.grpc.GetTaskPushNotificationConfigRequest toProto(GetTaskPushNotificationConfigParams domain);
}
