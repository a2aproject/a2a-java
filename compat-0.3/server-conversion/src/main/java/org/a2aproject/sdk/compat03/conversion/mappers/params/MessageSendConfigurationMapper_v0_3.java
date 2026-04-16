package org.a2aproject.sdk.compat03.conversion.mappers.params;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskPushNotificationConfigMapper_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendConfiguration_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.spec.MessageSendConfiguration;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting MessageSendConfiguration between A2A Protocol v0.3 and v1.0.
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: {@code PushNotificationConfig pushNotificationConfig, Boolean blocking}</li>
 *   <li>v1.0: {@code TaskPushNotificationConfig taskPushNotificationConfig, Boolean returnImmediately}</li>
 * </ul>
 * <p>
 * Conversion strategy:
 * <ul>
 *   <li>{@code blocking} (v0.3) ↔ {@code returnImmediately} (v1.0): Inverse semantics - {@code returnImmediately = !blocking}</li>
 *   <li>{@code PushNotificationConfig} wraps to {@code TaskPushNotificationConfig} with empty taskId</li>
 * </ul>
 */
@Mapper(config = A03ToV10MapperConfig.class, uses = {TaskPushNotificationConfigMapper_v0_3.class})
public interface MessageSendConfigurationMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    MessageSendConfigurationMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(MessageSendConfigurationMapper_v0_3.class);

    /**
     * Converts v0.3 MessageSendConfiguration to v1.0 MessageSendConfiguration.
     * <p>
     * Converts {@code blocking} to {@code returnImmediately} with inverse semantics,
     * and wraps {@code PushNotificationConfig} into {@code TaskPushNotificationConfig}.
     *
     * @param v03 the v0.3 message send configuration
     * @return the equivalent v1.0 message send configuration
     */
    default MessageSendConfiguration toV10(
            MessageSendConfiguration_v0_3 v03) {
        if (v03 == null) {
            return null;
        }

        // Convert PushNotificationConfig to TaskPushNotificationConfig if present
        TaskPushNotificationConfig taskPushConfig = null;
        if (v03.pushNotificationConfig() != null) {
            // Wrap the push notification config with an empty taskId (will be set by the server)
            TaskPushNotificationConfig_v0_3 v03TaskConfig =
                new TaskPushNotificationConfig_v0_3(
                    "",  // Empty taskId - will be populated by server
                    v03.pushNotificationConfig()
                );
            taskPushConfig = TaskPushNotificationConfigMapper_v0_3.INSTANCE.toV10(v03TaskConfig);
        }

        // Convert blocking to returnImmediately (inverse semantics)
        Boolean returnImmediately = v03.blocking() != null ? !v03.blocking() : null;

        return new MessageSendConfiguration(
            v03.acceptedOutputModes(),
            v03.historyLength(),
            taskPushConfig,
            returnImmediately
        );
    }

    /**
     * Converts v1.0 MessageSendConfiguration to v0.3 MessageSendConfiguration.
     * <p>
     * Converts {@code returnImmediately} to {@code blocking} with inverse semantics,
     * and extracts {@code PushNotificationConfig} from {@code TaskPushNotificationConfig}.
     *
     * @param v10 the v1.0 message send configuration
     * @return the equivalent v0.3 message send configuration
     */
    default MessageSendConfiguration_v0_3 fromV10(
            MessageSendConfiguration v10) {
        if (v10 == null) {
            return null;
        }

        // Extract PushNotificationConfig from TaskPushNotificationConfig if present
        PushNotificationConfig_v0_3 pushConfig = null;
        if (v10.taskPushNotificationConfig() != null) {
            TaskPushNotificationConfig_v0_3 v03TaskConfig =
                TaskPushNotificationConfigMapper_v0_3.INSTANCE.fromV10(v10.taskPushNotificationConfig());
            pushConfig = v03TaskConfig.pushNotificationConfig();
        }

        // Convert returnImmediately to blocking (inverse semantics)
        Boolean blocking = v10.returnImmediately() != null ? !v10.returnImmediately() : null;

        return new MessageSendConfiguration_v0_3(
            v10.acceptedOutputModes(),
            v10.historyLength(),
            pushConfig,
            blocking
        );
    }
}
