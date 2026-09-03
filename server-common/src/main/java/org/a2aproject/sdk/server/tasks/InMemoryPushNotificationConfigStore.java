package org.a2aproject.sdk.server.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private final ConcurrentHashMap<String, List<TaskPushNotificationConfig>> pushNotificationInfos = new ConcurrentHashMap<>();
    private final Map<String, String> protocolVersions = new ConcurrentHashMap<>();

    @Inject
    @Nullable A2AConfigProvider configProvider;

    @Inject
    public InMemoryPushNotificationConfigStore() {
    }

    @Override
    public TaskPushNotificationConfig setInfo(TaskPushNotificationConfig notificationConfig) {
        return setInfo(notificationConfig, false);
    }

    private TaskPushNotificationConfig setInfo(
            TaskPushNotificationConfig notificationConfig,
            boolean allowDefaultConfigUpdate) {
        String taskId = Assert.checkNotNullParam("taskId", notificationConfig.taskId());
        TaskPushNotificationConfig.Builder builder = TaskPushNotificationConfig.builder(notificationConfig);
        String requestedConfigId = notificationConfig.id();
        boolean configIdIsMissing = requestedConfigId == null || requestedConfigId.isEmpty();
        String configId = configIdIsMissing ? taskId : requestedConfigId;
        if (configIdIsMissing) {
            builder.id(taskId);
        }
        TaskPushNotificationConfig config = builder.build();
        int maxPerTask = PushNotificationConfigStore.maxPushConfigsPerTask(configProvider);

        pushNotificationInfos.compute(taskId, (key, list) -> {
            List<TaskPushNotificationConfig> mutable = list == null ? new ArrayList<>() : new ArrayList<>(list);
            boolean defaultConfigAlreadyExists = configIdIsMissing
                    && mutable.stream().anyMatch(existing -> existing.id() != null && existing.id().equals(configId));
            if (defaultConfigAlreadyExists && !allowDefaultConfigUpdate) {
                throw new InvalidParamsError("A push notification config with the default ID already exists for task "
                        + taskId + "; specify the config ID explicitly to update it");
            }
            boolean isExistingConfig = mutable.removeIf(
                    existing -> existing.id() != null && existing.id().equals(configId));
            if (!isExistingConfig && mutable.size() >= maxPerTask) {
                throw new InvalidParamsError("Too many push notification configs for task " + taskId
                        + " (max " + maxPerTask + ")");
            }
            mutable.add(config);
            return List.copyOf(mutable);
        });
        return config;
    }

    @Override
    public TaskPushNotificationConfig setInfo(TaskPushNotificationConfig config, @Nullable String protocolVersion) {
        TaskPushNotificationConfig result = setInfo(config, "0.3".equals(protocolVersion));
        protocolVersions.put(
                result.taskId() + ":" + result.id(),
                PushNotificationConfigStore.resolveProtocolVersion(protocolVersion));
        return result;
    }

    @Override
    public ListTaskPushNotificationConfigsResult getInfo(ListTaskPushNotificationConfigsParams params) {
        List<TaskPushNotificationConfig> configs = pushNotificationInfos.get(params.id());
        if (configs == null) {
            return new ListTaskPushNotificationConfigsResult(Collections.emptyList());
        }
        if (params.pageSize() <= 0) {
            return new ListTaskPushNotificationConfigsResult(configs, null);
        }
        if (params.pageToken() != null && !params.pageToken().isBlank()) {
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
        for (int i = 0; i < configs.size(); i++) {
            if (id.equals(configs.get(i).id())) {
                return i;
            }
        }
        return configs.size();
    }

    @Override
    public void deleteInfo(String taskId, String configId) {
        if (configId == null) {
            configId = taskId;
        }
        String deleteId = configId;
        pushNotificationInfos.computeIfPresent(taskId, (key, list) -> {
            List<TaskPushNotificationConfig> mutable = new ArrayList<>(list);
            mutable.removeIf(config -> deleteId.equals(config.id()));
            return mutable.isEmpty() ? null : List.copyOf(mutable);
        });
        protocolVersions.remove(taskId + ":" + deleteId);
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
