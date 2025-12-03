package io.a2a.grpc.mapper;

import io.a2a.grpc.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.TaskPushNotificationConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for SetTaskPushNotificationConfigRequest → TaskPushNotificationConfig.
 * <p>
 * Handles resource name parsing and ID override logic for creating push notification configs.
 * <p>
 * <b>Resource Name Handling:</b>
 * <ul>
 * <li>Extracts taskId from parent resource name (format: "tasks/{task_id}")</li>
 * <li>Fallback: Extracts from config.name if parent is blank</li>
 * <li>Overrides PushNotificationConfig.id with config_id from request</li>
 * </ul>
 * <p>
 * <b>Compile-Time Safety:</b> If the proto changes fields, MapStruct will fail to compile.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = {PushNotificationConfigMapper.class})
public interface SetTaskPushNotificationConfigMapper {

    SetTaskPushNotificationConfigMapper INSTANCE = A2AMappers.getMapper(SetTaskPushNotificationConfigMapper.class);

    /**
     * Converts SetTaskPushNotificationConfigRequest to domain TaskPushNotificationConfig.
     * <p>
     * Extracts taskId from parent resource name and maps PushNotificationConfig with
     * ID override from config_id field.
     *
     * @param request the protobuf SetTaskPushNotificationConfigRequest
     * @return domain TaskPushNotificationConfig
     */
    @Mapping(target = "taskId", expression = "java(extractTaskId(request))")
    @Mapping(target = "pushNotificationConfig", expression = "java(mapPushNotificationConfigWithId(request))")
    TaskPushNotificationConfig fromProto(SetTaskPushNotificationConfigRequest request);

    /**
     * Converts SetTaskPushNotificationConfigRequest to domain TaskPushNotificationConfig.
     * <p>
     * Extracts taskId from parent resource name and maps PushNotificationConfig with
     * ID override from config_id field.
     *
     * @param config the domainTaskPushNotificationConfig
     * @return proto SetTaskPushNotificationConfigRequest
     */
    @Mapping(target = "parent", expression = "java(ResourceNameParser.defineTaskName(config.taskId()))")
    @Mapping(target = "configId", expression = "java(extractConfigId(config))")
    @Mapping(target = "config", expression = "java(mapPushNotificationConfig(config))")
    SetTaskPushNotificationConfigRequest toProto(TaskPushNotificationConfig config);

    /**
     * Extracts the task ID from the parent resource name.
     * <p>
     * Format: "tasks/{task_id}"
     * Fallback: If parent is blank, extracts from config.name instead.
     *
     * @param request the protobuf SetTaskPushNotificationConfigRequest
     * @return the extracted task ID
     */
    default String extractTaskId(SetTaskPushNotificationConfigRequest request) {
        String parent = request.getParent();

        if (parent == null || parent.isBlank()) {
            // Fallback: extract from config.name
            return ResourceNameParser.extractTaskId(request.getConfig().getName());
        }

        // Extract from parent resource name
        return ResourceNameParser.extractParentId(parent);
    }

    /**
     * Extracts the config ID from the configuration. If it is not defined, the task ID is used.
     *
     * @param config the TaskPushNotificationConfig
     * @return the extracted config ID
     */
    default String extractConfigId(TaskPushNotificationConfig config) {
        if (config.pushNotificationConfig() != null && config.pushNotificationConfig().id() != null && !config.pushNotificationConfig().id().isBlank()) {
            return config.pushNotificationConfig().id();
        }
        return config.taskId();
    }

    /**
     * Maps the protobuf PushNotificationConfig to domain, injecting config_id from request.
     * <p>
     * The config_id from the request overrides the ID in the proto's PushNotificationConfig.
     *
     * @param request the protobuf SetTaskPushNotificationConfigRequest
     * @return domain PushNotificationConfig with config_id injected
     */
    default PushNotificationConfig mapPushNotificationConfigWithId(SetTaskPushNotificationConfigRequest request) {
        // Check if config and push_notification_config exist
        if (!request.hasConfig()
                || !request.getConfig().hasPushNotificationConfig()
                || request.getConfig().getPushNotificationConfig()
                        .equals(io.a2a.grpc.PushNotificationConfig.getDefaultInstance())) {
            return null;
        }

        // Map the proto PushNotificationConfig
        PushNotificationConfig result = PushNotificationConfigMapper.INSTANCE.fromProto(
                request.getConfig().getPushNotificationConfig()
        );

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

    /**
     * Maps the protobuf PushNotificationConfig to domain, injecting config_id from request.
     * <p>
     * The config_id from the request overrides the ID in the proto's PushNotificationConfig.
     *
     * @param request the protobuf SetTaskPushNotificationConfigRequest
     * @return domain PushNotificationConfig with config_id injected
     */
    default io.a2a.grpc.TaskPushNotificationConfig mapPushNotificationConfig(TaskPushNotificationConfig domain) {
        return TaskPushNotificationConfigMapper.INSTANCE.toProto(domain);
    }
}
