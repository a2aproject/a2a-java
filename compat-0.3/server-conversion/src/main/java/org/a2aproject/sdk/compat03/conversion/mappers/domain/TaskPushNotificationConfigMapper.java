package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A03Mappers;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting TaskPushNotificationConfig between A2A Protocol v0.3 and v1.0.
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: Nested structure with {@code TaskPushNotificationConfig(taskId, PushNotificationConfig)}</li>
 *   <li>v1.0: Flattened structure with {@code TaskPushNotificationConfig(id, taskId, url, token, authentication, tenant)}</li>
 * </ul>
 * <p>
 * Conversion strategy:
 * <ul>
 *   <li>v0.3 → v1.0: Extract fields from nested {@code PushNotificationConfig}, add tenant field (default "")</li>
 *   <li>v1.0 → v0.3: Nest url/token/authentication/id into {@code PushNotificationConfig}, drop tenant field</li>
 * </ul>
 */
@Mapper(config = A03ToV10MapperConfig.class, uses = {AuthenticationInfoMapper.class})
public interface TaskPushNotificationConfigMapper {

    /**
     * Singleton instance accessed via {@link A03Mappers} factory.
     */
    TaskPushNotificationConfigMapper INSTANCE = A03Mappers.getMapper(TaskPushNotificationConfigMapper.class);

    /**
     * Converts v0.3 TaskPushNotificationConfig to v1.0 TaskPushNotificationConfig.
     * <p>
     * Flattens the nested {@code PushNotificationConfig} structure and adds the tenant field (default "").
     *
     * @param v03 the v0.3 task push notification config
     * @return the equivalent v1.0 task push notification config
     */
    default TaskPushNotificationConfig toV10(
            org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig v03) {
        if (v03 == null) {
            return null;
        }

        org.a2aproject.sdk.compat03.spec.PushNotificationConfig pushConfig = v03.pushNotificationConfig();

        return new TaskPushNotificationConfig(
            pushConfig.id(),
            v03.taskId(),
            pushConfig.url(),
            pushConfig.token(),
            AuthenticationInfoMapper.INSTANCE.toV10FromPushNotification(pushConfig.authentication()),
            ""  // Default tenant
        );
    }

    /**
     * Converts v1.0 TaskPushNotificationConfig to v0.3 TaskPushNotificationConfig.
     * <p>
     * Nests the url/token/authentication/id fields into a {@code PushNotificationConfig} and drops the tenant field.
     *
     * @param v10 the v1.0 task push notification config
     * @return the equivalent v0.3 task push notification config
     */
    default org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig fromV10(
            TaskPushNotificationConfig v10) {
        if (v10 == null) {
            return null;
        }

        org.a2aproject.sdk.compat03.spec.PushNotificationConfig pushConfig =
            new org.a2aproject.sdk.compat03.spec.PushNotificationConfig(
                v10.url(),
                v10.token(),
                AuthenticationInfoMapper.INSTANCE.fromV10ToPushNotification(v10.authentication()),
                v10.id()
            );

        return new org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig(
            v10.taskId(),
            pushConfig
        );
    }
}
