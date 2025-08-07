package io.a2a.server.tasks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.a2a.spec.PushNotificationConfig;

/**
 * In-memory implementation of the PushNotificationConfigStore interface.
 *
 *     Stores push notification configurations in memory
 */
@ApplicationScoped
public class InMemoryPushNotificationConfigStore implements PushNotificationConfigStore {

    private final Map<String, List<PushNotificationConfig>> pushNotificationInfos = Collections.synchronizedMap(new HashMap<>());

    @Inject
    public InMemoryPushNotificationConfigStore() {
    }

    @Override
    public void setInfo(String taskId, PushNotificationConfig notificationConfig) {
        List<PushNotificationConfig> notificationConfigList = pushNotificationInfos.getOrDefault(taskId, new ArrayList<>());
        PushNotificationConfig.Builder builder = new PushNotificationConfig.Builder(notificationConfig);
        if (notificationConfig.id() == null) {
            builder.id(taskId);
        }
        notificationConfig = builder.build();

        Iterator<PushNotificationConfig> notificationConfigIterator = notificationConfigList.iterator();
        while (notificationConfigIterator.hasNext()) {
            PushNotificationConfig config = notificationConfigIterator.next();
            if (config.id().equals(notificationConfig.id())) {
                notificationConfigIterator.remove();
                break;
            }
        }
        notificationConfigList.add(notificationConfig);
        pushNotificationInfos.put(taskId, notificationConfigList);
    }

    @Override
    public List<PushNotificationConfig> getInfo(String taskId) {
        return pushNotificationInfos.get(taskId);
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
