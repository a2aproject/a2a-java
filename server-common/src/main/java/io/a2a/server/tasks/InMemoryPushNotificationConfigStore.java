package io.a2a.server.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.TaskPushNotificationConfig;

/**
 * In-memory implementation of the PushNotificationConfigStore interface.
 *
 * Stores push notification configurations in memory
 */
@ApplicationScoped
public class InMemoryPushNotificationConfigStore implements PushNotificationConfigStore {

    private final Map<String, List<PushNotificationConfig>> pushNotificationInfos = Collections.synchronizedMap(new HashMap<>());

    @Inject
    public InMemoryPushNotificationConfigStore() {
    }

    @Override
    public PushNotificationConfig setInfo(String taskId, PushNotificationConfig notificationConfig) {
        List<PushNotificationConfig> notificationConfigList = pushNotificationInfos.getOrDefault(taskId, new ArrayList<>());
        PushNotificationConfig.Builder builder = PushNotificationConfig.builder(notificationConfig);
        if (notificationConfig.id() == null || notificationConfig.id().isEmpty()) {
            builder.id(taskId);
        }
        notificationConfig = builder.build();

        Iterator<PushNotificationConfig> notificationConfigIterator = notificationConfigList.iterator();
        while (notificationConfigIterator.hasNext()) {
            PushNotificationConfig config = notificationConfigIterator.next();
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
        List<PushNotificationConfig> configs = pushNotificationInfos.get(params.id());
        if (configs == null) {
            return new ListTaskPushNotificationConfigResult(Collections.emptyList());
        }
        if (params.pageSize() <= 0) {
            return new ListTaskPushNotificationConfigResult(convertPushNotificationConfig(configs, params), null);
        }
        if (params.pageToken() != null && !params.pageToken().isBlank()) {
            //find first index
            int index = findFirstIndex(configs, params.pageToken());
            if (index < configs.size()) {
                configs = configs.subList(index, configs.size());
            }
        }
        if (configs.size() <= params.pageSize()) {
            return new ListTaskPushNotificationConfigResult(convertPushNotificationConfig(configs, params), null);
        }
        String newToken = configs.get(params.pageSize()).id();
        return new ListTaskPushNotificationConfigResult(convertPushNotificationConfig(configs.subList(0, params.pageSize()), params), newToken);
    }

    private int findFirstIndex(List<PushNotificationConfig> configs, String id) {
        //find first index
        Iterator<PushNotificationConfig> iter = configs.iterator();
        int index = 0;
        while (iter.hasNext()) {
            if (id.equals(iter.next().id())) {
                return index;
            }
            index++;
        }
        return index;
    }

    private List<TaskPushNotificationConfig> convertPushNotificationConfig(List<PushNotificationConfig> pushNotificationConfigList, ListTaskPushNotificationConfigParams params) {
        List<TaskPushNotificationConfig> taskPushNotificationConfigList = new ArrayList<>(pushNotificationConfigList.size());
        for (PushNotificationConfig pushNotificationConfig : pushNotificationConfigList) {
            TaskPushNotificationConfig taskPushNotificationConfig = new TaskPushNotificationConfig(params.id(), pushNotificationConfig, params.tenant());
            taskPushNotificationConfigList.add(taskPushNotificationConfig);
        }
        return taskPushNotificationConfigList;
    }

    @Override
    public void deleteInfo(String taskId, String configId) {
        if (configId == null) {
            configId = taskId;
        }
        List<PushNotificationConfig> notificationConfigList = pushNotificationInfos.get(taskId);
        if (notificationConfigList == null || notificationConfigList.isEmpty()) {
            return;
        }

        Iterator<PushNotificationConfig> notificationConfigIterator = notificationConfigList.iterator();
        while (notificationConfigIterator.hasNext()) {
            PushNotificationConfig config = notificationConfigIterator.next();
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
