package org.a2aproject.sdk.compat03.conversion.mappers.result;

import java.util.List;
import java.util.stream.Collectors;

import org.a2aproject.sdk.compat03.conversion.mappers.config.A2AMappers_v0_3;
import org.a2aproject.sdk.compat03.conversion.mappers.config.A03ToV10MapperConfig;
import org.a2aproject.sdk.compat03.conversion.mappers.domain.TaskPushNotificationConfigMapper_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.mapstruct.Mapper;

/**
 * Bidirectional mapper for converting between v0.3 {@code List<TaskPushNotificationConfig>}
 * and v1.0 {@link ListTaskPushNotificationConfigsResult}.
 * <p>
 * Key differences:
 * <ul>
 *   <li>v0.3: Returns a plain {@code List<TaskPushNotificationConfig>} (no pagination support)</li>
 *   <li>v1.0: Returns {@link ListTaskPushNotificationConfigsResult} with pagination support (nextPageToken)</li>
 * </ul>
 * <p>
 * Conversion strategy:
 * <ul>
 *   <li>v0.3 → v1.0: Wrap the list in {@code ListTaskPushNotificationConfigsResult} with no nextPageToken</li>
 *   <li>v1.0 → v0.3: Extract the configs list (discard nextPageToken as 0.3 doesn't support pagination)</li>
 * </ul>
 */
@Mapper(config = A03ToV10MapperConfig.class, uses = {TaskPushNotificationConfigMapper_v0_3.class})
public interface ListTaskPushNotificationConfigsResultMapper_v0_3 {

    /**
     * Singleton instance accessed via {@link A2AMappers_v0_3} factory.
     */
    ListTaskPushNotificationConfigsResultMapper_v0_3 INSTANCE = A2AMappers_v0_3.getMapper(ListTaskPushNotificationConfigsResultMapper_v0_3.class);

    /**
     * Converts v0.3 {@code List<TaskPushNotificationConfig>} to v1.0 {@link ListTaskPushNotificationConfigsResult}.
     * <p>
     * Wraps the list in a result object with no nextPageToken (pagination not supported in 0.3).
     * Converts each TaskPushNotificationConfig using TaskPushNotificationConfigMapper.
     *
     * @param v03List the v0.3 list of task push notification configs
     * @return the equivalent v1.0 result object
     */
    default ListTaskPushNotificationConfigsResult toV10(
            List<TaskPushNotificationConfig_v0_3> v03List) {
        if (v03List == null) {
            return null;
        }

        List<TaskPushNotificationConfig> v10Configs = v03List.stream()
            .map(TaskPushNotificationConfigMapper_v0_3.INSTANCE::toV10)
            .collect(Collectors.toList());

        return new ListTaskPushNotificationConfigsResult(v10Configs, null);
    }

    /**
     * Converts v1.0 {@link ListTaskPushNotificationConfigsResult} to v0.3 {@code List<TaskPushNotificationConfig>}.
     * <p>
     * Extracts the configs list and discards the nextPageToken (pagination not supported in 0.3).
     * Converts each TaskPushNotificationConfig using TaskPushNotificationConfigMapper.
     *
     * @param v10Result the v1.0 result object
     * @return the equivalent v0.3 list
     */
    default List<TaskPushNotificationConfig_v0_3> fromV10(
            ListTaskPushNotificationConfigsResult v10Result) {
        if (v10Result == null) {
            return null;
        }

        return v10Result.configs().stream()
            .map(TaskPushNotificationConfigMapper_v0_3.INSTANCE::fromV10)
            .collect(Collectors.toList());
    }
}
