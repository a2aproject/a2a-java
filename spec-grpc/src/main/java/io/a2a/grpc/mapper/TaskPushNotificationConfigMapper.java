package io.a2a.grpc.mapper;

import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.TaskPushNotificationConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for TaskPushNotificationConfig.
 * <p>
 * Maps between domain TaskPushNotificationConfig and proto TaskPushNotificationConfig.
 * The proto now has direct task_id and id fields instead of a resource name.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = {PushNotificationConfigMapper.class})
public interface TaskPushNotificationConfigMapper {

    TaskPushNotificationConfigMapper INSTANCE = A2AMappers.getMapper(TaskPushNotificationConfigMapper.class);

    /**
     * Converts domain TaskPushNotificationConfig to protobuf TaskPushNotificationConfig.
     *
     * @param config the domain TaskPushNotificationConfig
     * @return protobuf TaskPushNotificationConfig
     */
    @Mapping(target = "id", expression = "java(extractId(config))")
    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "pushNotificationConfig", source = "pushNotificationConfig")
    @Mapping(target = "tenant", source = "tenant", conditionExpression = "java(config.tenant() != null)")
    io.a2a.grpc.TaskPushNotificationConfig toProto(TaskPushNotificationConfig config);

    /**
     * Converts protobuf TaskPushNotificationConfig to domain TaskPushNotificationConfig.
     *
     * @param proto the protobuf TaskPushNotificationConfig
     * @return domain TaskPushNotificationConfig
     */
    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "pushNotificationConfig", expression = "java(mapPushNotificationConfigWithId(proto))")
    @Mapping(target = "tenant", expression = "java(proto.getTenant().isEmpty() ? null : proto.getTenant())")
    TaskPushNotificationConfig fromProto(io.a2a.grpc.TaskPushNotificationConfig proto);

    /**
     * Extracts the config ID from the PushNotificationConfig.
     *
     * @param config the domain TaskPushNotificationConfig
     * @return the config ID
     */
    default String extractId(TaskPushNotificationConfig config) {
        return config.pushNotificationConfig() != null ? config.pushNotificationConfig().id() : null;
    }

    /**
     * Maps the protobuf PushNotificationConfig to domain, ensuring the ID matches the proto's id field.
     *
     * @param proto the protobuf TaskPushNotificationConfig
     * @return domain PushNotificationConfig with correct ID
     */
    default PushNotificationConfig mapPushNotificationConfigWithId(io.a2a.grpc.TaskPushNotificationConfig proto) {
        if (!proto.hasPushNotificationConfig() ||
            proto.getPushNotificationConfig().equals(io.a2a.grpc.PushNotificationConfig.getDefaultInstance())) {
            return null;
        }

        // Map the proto PushNotificationConfig
        PushNotificationConfig result = PushNotificationConfigMapper.INSTANCE.fromProto(proto.getPushNotificationConfig());

        // Override ID with the id from TaskPushNotificationConfig if they differ
        String configId = proto.getId();
        if (configId != null && !configId.isEmpty() && !configId.equals(result.id())) {
            return new PushNotificationConfig(result.url(), result.token(), result.authentication(), configId);
        }

        return result;
    }
}
