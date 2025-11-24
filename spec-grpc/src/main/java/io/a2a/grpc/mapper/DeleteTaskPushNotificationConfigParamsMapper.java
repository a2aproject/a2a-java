package io.a2a.grpc.mapper;

import io.a2a.spec.DeleteTaskPushNotificationConfigParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper between {@link io.a2a.grpc.DeleteTaskPushNotificationConfigRequest} and {@link io.a2a.spec.DeleteTaskPushNotificationConfigParams}.
 * <p>
 * Extracts task ID and config ID from resource name format "tasks/{taskId}/pushNotificationConfigs/{configId}" using {@link ResourceNameParser}.
 */
@Mapper(config = ProtoMapperConfig.class)
public interface DeleteTaskPushNotificationConfigParamsMapper {

    DeleteTaskPushNotificationConfigParamsMapper INSTANCE = Mappers.getMapper(DeleteTaskPushNotificationConfigParamsMapper.class);

    /**
     * Converts proto DeleteTaskPushNotificationConfigRequest to domain DeleteTaskPushNotificationConfigParams.
     * Parses the name field to extract both task ID and config ID.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", expression = "java(ResourceNameParser.parseTaskPushNotificationConfigName(proto.getName())[0])")
    @Mapping(target = "pushNotificationConfigId", expression = "java(ResourceNameParser.parseTaskPushNotificationConfigName(proto.getName())[1])")
    @Mapping(target = "metadata", ignore = true)
    DeleteTaskPushNotificationConfigParams fromProto(io.a2a.grpc.DeleteTaskPushNotificationConfigRequest proto);
}
