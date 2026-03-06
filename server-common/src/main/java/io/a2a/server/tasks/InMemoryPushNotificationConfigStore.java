package io.a2a.server.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.a2a.util.Assert;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.TaskPushNotificationConfig;

/**
 * In-memory implementation of the PushNotificationConfigStore interface.
 *
 * Stores push notification configurations in memory
 */
@ApplicationScoped
public class InMemoryPushNotificationConfigStore implements PushNotificationConfigStore {

    private final Map<String, List<TaskPushNotificationConfig>> pushNotificationInfos = Collections.synchronizedMap(new HashMap<>());

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

        Iterator<TaskPushNotificationConfig> notificationConfigIterator = notificationConfigList.iterator();
        while (notificationConfigIterator.hasNext()) {
            TaskPushNotificationConfig config = notificationConfigIterator.next();
            if (config.id() != null  && config.id().equals(notificationConfig.id())) {
                notificationConfigIterator.remove();
                break;
            }
        }
        notificationConfigList.add(notificationConfig);
        pushNotificationInfos.put(taskId, notificationConfigList);
        return notificationConfig;
    }

    @Override
    public ListTaskPushNotificationConfigResult getInfo(ListTaskPushNotificationConfigParams params) {
        List<TaskPushNotificationConfig> configs = pushNotificationInfos.get(params.id());
        if (configs == null) {
            return new ListTaskPushNotificationConfigResult(Collections.emptyList());
        }
        if (params.pageSize() <= 0) {
            return new ListTaskPushNotificationConfigResult(new ArrayList<>(configs), null);
        }
        if (params.pageToken() != null && !params.pageToken().isBlank()) {
            //find first index
            int index = findFirstIndex(configs, params.pageToken());
            if (index < configs.size()) {
                configs = configs.subList(index, configs.size());
            }
        }
        if (configs.size() <= params.pageSize()) {
            return new ListTaskPushNotificationConfigResult(new ArrayList<>(configs), null);
        }
        String newToken = configs.get(params.pageSize()).id();
        return new ListTaskPushNotificationConfigResult(new ArrayList<>(configs.subList(0, params.pageSize())), newToken);
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
        if (notificationConfigList.isEmpty()) {
            pushNotificationInfos.remove(taskId);
        }
    }
}
