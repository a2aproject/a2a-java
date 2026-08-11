package org.a2aproject.sdk.server.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.a2aproject.sdk.server.config.A2AConfigProvider;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * In-memory implementation of the PushNotificationConfigStore interface.
 *
 * Stores push notification configurations in memory
 */
@ApplicationScoped
public class InMemoryPushNotificationConfigStore implements PushNotificationConfigStore {

    /**
     * Default maximum number of push notification configs allowed per task.
     * Prevents a single task from accumulating an unbounded list of configs
     * (each config consumes memory and can trigger outbound HTTP requests).
     * Overridable via {@code a2a.push-notification-config.max-per-task}.
     */
    public static final int MAX_PUSH_CONFIGS_PER_TASK = PushNotificationConfigStore.DEFAULT_MAX_PUSH_CONFIGS_PER_TASK;

    private final Map<String, List<TaskPushNotificationConfig>> pushNotificationInfos = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, String> protocolVersions = Collections.synchronizedMap(new HashMap<>());

    @Inject
    @Nullable A2AConfigProvider configProvider;

    @Inject
    public InMemoryPushNotificationConfigStore() {
    }

    @Override
    public TaskPushNotificationConfig setInfo(TaskPushNotificationConfig notificationConfig) {
        String taskId = Assert.checkNotNullParam("taskId", notificationConfig.taskId());
        List<TaskPushNotificationConfig> notificationConfigList = pushNotificationInfos.getOrDefault(taskId, new ArrayList<>());
        TaskPushNotificationConfig.Builder builder = TaskPushNotificationConfig.builder(notificationConfig);
        if (notificationConfig.id().isEmpty()) {
            builder.id(taskId);
        }
        notificationConfig = builder.build();

        // Enforce the per-task limit and remove any existing config with the same
        // ID in a single pass. Re-registering/updating an already-registered config
        // ID is allowed; only genuinely new configs count against the limit.
        String configId = notificationConfig.id();
        boolean isExistingConfig = notificationConfigList.removeIf(
                existing -> existing.id() != null && existing.id().equals(configId));
        int maxPerTask = PushNotificationConfigStore.maxPushConfigsPerTask(configProvider);
        if (!isExistingConfig && notificationConfigList.size() >= maxPerTask) {
            throw new InvalidParamsError("Too many push notification configs for task " + taskId
                    + " (max " + maxPerTask + ")");
        }

        notificationConfigList.add(notificationConfig);
        pushNotificationInfos.put(taskId, notificationConfigList);
        return notificationConfig;
    }

    @Override
    public TaskPushNotificationConfig setInfo(TaskPushNotificationConfig config, @Nullable String protocolVersion) {
        TaskPushNotificationConfig result = setInfo(config);
        protocolVersions.put(result.taskId() + ":" + result.id(), PushNotificationConfigStore.resolveProtocolVersion(protocolVersion));
        return result;
    }

    @Override
    public ListTaskPushNotificationConfigsResult getInfo(ListTaskPushNotificationConfigsParams params) {
        List<TaskPushNotificationConfig> configs = pushNotificationInfos.get(params.id());
        if (configs == null) {
            return new ListTaskPushNotificationConfigsResult(Collections.emptyList());
        }
        if (params.pageSize() <= 0) {
            return new ListTaskPushNotificationConfigsResult(new ArrayList<>(configs), null);
        }
        if (params.pageToken() != null && !params.pageToken().isBlank()) {
            //find first index
            int index = findFirstIndex(configs, params.pageToken());
            if (index < configs.size()) {
                configs = configs.subList(index, configs.size());
            }
        }
        if (configs.size() <= params.pageSize()) {
            return new ListTaskPushNotificationConfigsResult(new ArrayList<>(configs), null);
        }
        String newToken = configs.get(params.pageSize()).id();
        return new ListTaskPushNotificationConfigsResult(new ArrayList<>(configs.subList(0, params.pageSize())), newToken);
    }

    private int findFirstIndex(List<TaskPushNotificationConfig> configs, String id) {
        //find first index
        Iterator<TaskPushNotificationConfig> iter = configs.iterator();
        int index = 0;
        while (iter.hasNext()) {
            if (id.equals(iter.next().id())) {
                return index;
            }
            index++;
        }
        return index;
    }

    @Override
    public void deleteInfo(String taskId, String configId) {
        if (configId == null) {
            configId = taskId;
        }
        List<TaskPushNotificationConfig> notificationConfigList = pushNotificationInfos.get(taskId);
        if (notificationConfigList == null || notificationConfigList.isEmpty()) {
            return;
        }

        Iterator<TaskPushNotificationConfig> notificationConfigIterator = notificationConfigList.iterator();
        while (notificationConfigIterator.hasNext()) {
            TaskPushNotificationConfig config = notificationConfigIterator.next();
            if (configId.equals(config.id())) {
                notificationConfigIterator.remove();
                break;
            }
        }
        protocolVersions.remove(taskId + ":" + configId);
        if (notificationConfigList.isEmpty()) {
            pushNotificationInfos.remove(taskId);
        }
    }

    @Override
    public String getProtocolVersion(String taskId, String configId) {
        String version = protocolVersions.get(taskId + ":" + configId);
        return PushNotificationConfigStore.resolveProtocolVersion(version);
    }

    @Override
    public Map<String, String> getProtocolVersions(String taskId) {
        String prefix = taskId + ":";
        Map<String, String> result = new HashMap<>();
        protocolVersions.forEach((key, version) -> {
            if (key.startsWith(prefix)) {
                result.put(key.substring(prefix.length()), version);
            }
        });
        return result;
    }
}
