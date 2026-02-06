package io.a2a.grpc.mapper;

import io.a2a.grpc.CreateTaskPushNotificationConfigRequest;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.TaskPushNotificationConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for CreateTaskPushNotificationConfigRequest → TaskPushNotificationConfig.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = {PushNotificationConfigMapper.class})
public interface CreateTaskPushNotificationConfigMapper {

    CreateTaskPushNotificationConfigMapper INSTANCE = A2AMappers.getMapper(CreateTaskPushNotificationConfigMapper.class);

    /**
     * Converts CreateTaskPushNotificationConfigRequest to domain TaskPushNotificationConfig.
     *
     * @param request the protobuf CreateTaskPushNotificationConfigRequest
     * @return domain TaskPushNotificationConfig
     */
    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "pushNotificationConfig", expression = "java(mapPushNotificationConfigWithId(request))")
    @Mapping(target = "tenant", source = "tenant")
    TaskPushNotificationConfig fromProto(CreateTaskPushNotificationConfigRequest request);

    /**
     * Converts domain TaskPushNotificationConfig to CreateTaskPushNotificationConfigRequest.
     *
     * @param config the domain TaskPushNotificationConfig
     * @return proto CreateTaskPushNotificationConfigRequest
     */
    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "configId", expression = "java(extractConfigId(config))")
    @Mapping(target = "config", source = "pushNotificationConfig")
    @Mapping(target = "tenant", source = "tenant")
    CreateTaskPushNotificationConfigRequest toProto(TaskPushNotificationConfig config);

    /**
     * Extracts the config ID from the configuration.
     *
     * @param config the TaskPushNotificationConfig
     * @return the extracted config ID
     */
    default String extractConfigId(TaskPushNotificationConfig config) {
        return config.pushNotificationConfig() != null ? config.pushNotificationConfig().id() : null;
    }

    /**
     * Maps the protobuf PushNotificationConfig to domain, injecting config_id from request.
     *
     * @param request the protobuf CreateTaskPushNotificationConfigRequest
     * @return domain PushNotificationConfig with config_id injected
     */
    default PushNotificationConfig mapPushNotificationConfigWithId(CreateTaskPushNotificationConfigRequest request) {
        if (!request.hasConfig()) {
            return null;
        }

        // Map the proto PushNotificationConfig
        PushNotificationConfig result = PushNotificationConfigMapper.INSTANCE.fromProto(request.getConfig());

        // Override ID with config_id from request
        String configId = request.getConfigId();
        if (configId != null && !configId.isEmpty() && !configId.equals(result.id())) {
            return new PushNotificationConfig(
                    result.url(),
                    result.token(),
                    result.authentication(),
                    configId
            );
        }

        return result;
    }
}
