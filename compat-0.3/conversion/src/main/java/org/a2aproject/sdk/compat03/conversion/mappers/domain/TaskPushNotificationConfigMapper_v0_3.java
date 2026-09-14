package org.a2aproject.sdk.compat03.conversion.mappers.domain;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.spec.PushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
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
@Mapper(config = A03ToV10MapperConfig.class, uses = {AuthenticationInfoMapper_v0_3.class})
public interface TaskPushNotificationConfigMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    TaskPushNotificationConfigMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(TaskPushNotificationConfigMapper_v0_3.class);

    /**
     * Converts v0.3 TaskPushNotificationConfig to v1.0 TaskPushNotificationConfig.
     * <p>
     * Flattens the nested {@code PushNotificationConfig} structure and adds the tenant field (default "").
     *
     * @param v03 the v0.3 task push notification config
     * @return the equivalent v1.0 task push notification config
     */
    default TaskPushNotificationConfig toV10(
            TaskPushNotificationConfig_v0_3 v03) {
        if (v03 == null) {
            return null;
        }

        PushNotificationConfig_v0_3 pushConfig = v03.pushNotificationConfig();

        // v0.3 id can be null; v1.0 requires non-null id but stores use empty string to auto-assign
        String id = pushConfig.id() != null ? pushConfig.id() : "";

        return new TaskPushNotificationConfig(
            id,
            v03.taskId(),
            pushConfig.url(),
            pushConfig.token(),
            AuthenticationInfoMapper_v0_3.INSTANCE.toV10FromPushNotification(pushConfig.authentication()),
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
    default TaskPushNotificationConfig_v0_3 fromV10(
            TaskPushNotificationConfig v10) {
        if (v10 == null) {
            return null;
        }

        PushNotificationConfig_v0_3 pushConfig =
            new PushNotificationConfig_v0_3(
                v10.url(),
                v10.token(),
                AuthenticationInfoMapper_v0_3.INSTANCE.fromV10ToPushNotification(v10.authentication()),
                v10.id()
            );

        return new TaskPushNotificationConfig_v0_3(
            v10.taskId(),
            pushConfig
        );
    }
}
