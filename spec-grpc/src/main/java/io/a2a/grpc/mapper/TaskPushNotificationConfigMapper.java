package io.a2a.grpc.mapper;

import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.TaskPushNotificationConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for TaskPushNotificationConfig with resource name handling.
 * <p>
 * This mapper provides compile-time safety while handling the Google Cloud resource name
 * convention where the protobuf has a single "name" field in the format
 * "tasks/{task_id}/pushNotificationConfigs/{config_id}", but the domain model has separate
 * taskId and configId fields.
 * <p>
 * <b>Resource Name Format:</b> tasks/{task_id}/pushNotificationConfigs/{config_id}
 * <p>
 * <b>Compile-Time Safety:</b> If the proto adds/removes/renames fields, MapStruct will
 * fail to compile, ensuring we update the domain model accordingly.
 */
@Mapper(config = A2AProtoMapperConfig.class, uses = {PushNotificationConfigMapper.class})
public interface TaskPushNotificationConfigMapper {

    TaskPushNotificationConfigMapper INSTANCE = A2AMappers.getMapper(TaskPushNotificationConfigMapper.class);

    /**
     * Converts domain TaskPushNotificationConfig to protobuf TaskPushNotificationConfig.
     * <p>
     * Synthesizes the resource name from taskId and configId following the format:
     * "tasks/{task_id}/pushNotificationConfigs/{config_id}"
     *
     * @param config the domain TaskPushNotificationConfig
     * @return protobuf TaskPushNotificationConfig with synthesized resource name
     */
    @Mapping(target = "name", expression = "java(buildResourceName(config))")
    @Mapping(target = "pushNotificationConfig", source = "pushNotificationConfig")
    io.a2a.grpc.TaskPushNotificationConfig toProto(TaskPushNotificationConfig config);

    /**
     * Converts protobuf TaskPushNotificationConfig to domain TaskPushNotificationConfig.
     * <p>
     * Parses the resource name to extract taskId and configId, then creates the domain object.
     * The configId is injected into the PushNotificationConfig if it differs from what's in the proto.
     *
     * @param proto the protobuf TaskPushNotificationConfig
     * @return domain TaskPushNotificationConfig with extracted taskId and configId
     * @throws IllegalArgumentException if the resource name format is invalid
     */
    @Mapping(target = "taskId", expression = "java(extractTaskId(proto))")
    @Mapping(target = "pushNotificationConfig", expression = "java(mapPushNotificationConfigWithId(proto))")
    TaskPushNotificationConfig fromProto(io.a2a.grpc.TaskPushNotificationConfig proto);

    /**
     * Builds the resource name from domain model fields.
     * <p>
     * Format: "tasks/{task_id}/pushNotificationConfigs/{config_id}"
     * If configId is null, format: "tasks/{task_id}/pushNotificationConfigs"
     *
     * @param config the domain TaskPushNotificationConfig
     * @return the synthesized resource name
     */
    default String buildResourceName(TaskPushNotificationConfig config) {
        String taskId = config.taskId();
        String configId = config.pushNotificationConfig().id();

        if (configId == null || configId.isEmpty()) {
            return "tasks/" + taskId + "/pushNotificationConfigs";
        }
        return "tasks/" + taskId + "/pushNotificationConfigs/" + configId;
    }

    /**
     * Extracts the task ID from the protobuf resource name.
     *
     * @param proto the protobuf TaskPushNotificationConfig
     * @return the extracted task ID
     * @throws IllegalArgumentException if the resource name format is invalid
     */
    default String extractTaskId(io.a2a.grpc.TaskPushNotificationConfig proto) {
        String[] parts = ResourceNameParser.parseTaskPushNotificationConfigName(proto.getName());
        return parts[0]; // taskId
    }

    /**
     * Maps the protobuf PushNotificationConfig to domain, injecting the configId from the resource name.
     * <p>
     * The configId is parsed from the resource name and may override the ID in the proto's
     * PushNotificationConfig if they differ.
     *
     * @param proto the protobuf TaskPushNotificationConfig
     * @return domain PushNotificationConfig with correct ID
     * @throws IllegalArgumentException if the resource name format is invalid
     */
    default PushNotificationConfig mapPushNotificationConfigWithId(io.a2a.grpc.TaskPushNotificationConfig proto) {
        // Parse configId from resource name
        String[] parts = ResourceNameParser.parseTaskPushNotificationConfigName(proto.getName());
        String configId = parts[1]; // configId

        // Check if proto has PushNotificationConfig
        if (!proto.hasPushNotificationConfig() ||
            proto.getPushNotificationConfig().equals(io.a2a.grpc.PushNotificationConfig.getDefaultInstance())) {
            return null;
        }

        // Map the proto PushNotificationConfig
        PushNotificationConfig result = PushNotificationConfigMapper.INSTANCE.fromProto(proto.getPushNotificationConfig());

        // Override ID if configId from resource name differs from the one in PushNotificationConfig
        if (configId != null && !configId.isEmpty() && !configId.equals(result.id())) {
            return new PushNotificationConfig(result.url(), result.token(), result.authentication(), configId);
        }

        return result;
    }
}
