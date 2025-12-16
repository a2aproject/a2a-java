package io.a2a.grpc.mapper;

import io.a2a.spec.ListTaskPushNotificationConfigParams;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper between {@link io.a2a.grpc.ListTaskPushNotificationConfigRequest} and {@link io.a2a.spec.ListTaskPushNotificationConfigParams}.
 * <p>
 * Extracts task ID from parent resource name format "tasks/{id}" using {@link ResourceNameParser}.
 */
@Mapper(config = A2AProtoMapperConfig.class)
public interface ListTaskPushNotificationConfigParamsMapper {

    ListTaskPushNotificationConfigParamsMapper INSTANCE = A2AMappers.getMapper(ListTaskPushNotificationConfigParamsMapper.class);

    /**
     * Converts proto ListTaskPushNotificationConfigRequest to domain ListTaskPushNotificationConfigParams.
     * Extracts task ID from the parent field and maps pagination parameters.
     */
    @BeanMapping(builder = @Builder(buildMethod = "build"))
    @Mapping(target = "id", expression = "java(ResourceNameParser.extractParentId(proto.getParent()))")
    @Mapping(target = "tenant", source = "tenant")
    ListTaskPushNotificationConfigParams fromProto(io.a2a.grpc.ListTaskPushNotificationConfigRequest proto);

    /**
     * Converts domain ListTaskPushNotificationConfigParams to proto ListTaskPushNotificationConfigRequest.
     * Constructs the parent field from task ID.
     */
    @Mapping(target = "parent", expression = "java(ResourceNameParser.defineTaskName(domain.id()))")
    @Mapping(target = "pageSize", ignore = true)
    @Mapping(target = "pageToken", ignore = true)
    io.a2a.grpc.ListTaskPushNotificationConfigRequest toProto(ListTaskPushNotificationConfigParams domain);
}
