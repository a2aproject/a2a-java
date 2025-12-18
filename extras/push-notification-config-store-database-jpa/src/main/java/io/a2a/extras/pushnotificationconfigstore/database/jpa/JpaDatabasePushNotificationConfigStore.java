package io.a2a.extras.pushnotificationconfigstore.database.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import io.a2a.json.JsonProcessingException;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.TaskPushNotificationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Alternative
@Priority(50)
public class JpaDatabasePushNotificationConfigStore implements PushNotificationConfigStore {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaDatabasePushNotificationConfigStore.class);
    
    @PersistenceContext(unitName = "a2a-java")
    EntityManager em;

    @Transactional
    @Override
    public PushNotificationConfig setInfo(String taskId, PushNotificationConfig notificationConfig) {
        // Ensure config has an ID - default to taskId if not provided (mirroring InMemoryPushNotificationConfigStore behavior)
        PushNotificationConfig.Builder builder = PushNotificationConfig.builder(notificationConfig);
        if (notificationConfig.id() == null || notificationConfig.id().isEmpty()) {
            builder.id(taskId);
        }
        notificationConfig = builder.build();

        LOGGER.debug("Saving PushNotificationConfig for Task '{}' with ID: {}", taskId, notificationConfig.id());
        try {
            TaskConfigId configId = new TaskConfigId(taskId, notificationConfig.id());

            // Check if entity already exists
            JpaPushNotificationConfig existingJpaConfig = em.find(JpaPushNotificationConfig.class, configId);

            if (existingJpaConfig != null) {
                // Update existing entity
                existingJpaConfig.setConfig(notificationConfig);
                LOGGER.debug("Updated existing PushNotificationConfig for Task '{}' with ID: {}",
                        taskId, notificationConfig.id());
            } else {
                // Create new entity
                JpaPushNotificationConfig jpaConfig = JpaPushNotificationConfig.createFromConfig(taskId, notificationConfig);
                em.persist(jpaConfig);
                LOGGER.debug("Persisted new PushNotificationConfig for Task '{}' with ID: {}",
                        taskId, notificationConfig.id());
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize PushNotificationConfig for Task '{}' with ID: {}",
                    taskId, notificationConfig.id(), e);
            throw new RuntimeException("Failed to serialize PushNotificationConfig for Task '" +
                    taskId + "' with ID: " + notificationConfig.id(), e);
        }
        return notificationConfig;
    }

    @Transactional
    @Override
    public ListTaskPushNotificationConfigResult getInfo(ListTaskPushNotificationConfigParams params) {
        String taskId = params.id();
        LOGGER.debug("Retrieving PushNotificationConfigs for Task '{}'", taskId);
        try {
            List<JpaPushNotificationConfig> jpaConfigs = em.createQuery(
                    "SELECT c FROM JpaPushNotificationConfig c WHERE c.id.taskId = :taskId",
                    JpaPushNotificationConfig.class)
                    .setParameter("taskId", taskId)
                    .getResultList();

            List<PushNotificationConfig> configs = jpaConfigs.stream()
                    .map(jpaConfig -> {
                        try {
                            return jpaConfig.getConfig();
                        } catch (JsonProcessingException e) {
                            LOGGER.error("Failed to deserialize PushNotificationConfig for Task '{}' with ID: {}",
                                    taskId, jpaConfig.getId().getConfigId(), e);
                            throw new RuntimeException("Failed to deserialize PushNotificationConfig for Task '" +
                                    taskId + "' with ID: " + jpaConfig.getId().getConfigId(), e);
                        }
                    })
                    .toList();

            LOGGER.debug("Successfully retrieved {} PushNotificationConfigs for Task '{}'", configs.size(), taskId);

            // Handle pagination
            if (configs.isEmpty()) {
                return new ListTaskPushNotificationConfigResult(Collections.emptyList());
            }

            if (params.pageSize() <= 0) {
                return new ListTaskPushNotificationConfigResult(convertPushNotificationConfig(configs, params), null);
            }

            // Apply pageToken filtering if provided
            List<PushNotificationConfig> paginatedConfigs = configs;
            if (params.pageToken() != null && !params.pageToken().isBlank()) {
                int index = findFirstIndex(configs, params.pageToken());
                if (index < configs.size()) {
                    paginatedConfigs = configs.subList(index, configs.size());
                }
            }

            // Apply page size limit
            if (paginatedConfigs.size() <= params.pageSize()) {
                return new ListTaskPushNotificationConfigResult(convertPushNotificationConfig(paginatedConfigs, params), null);
            }

            String nextToken = paginatedConfigs.get(params.pageSize()).token();
            return new ListTaskPushNotificationConfigResult(
                    convertPushNotificationConfig(paginatedConfigs.subList(0, params.pageSize()), params),
                    nextToken);
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve PushNotificationConfigs for Task '{}'", taskId, e);
            throw e;
        }
    }

    private int findFirstIndex(List<PushNotificationConfig> configs, String token) {
        for (int i = 0; i < configs.size(); i++) {
            if (token.equals(configs.get(i).token())) {
                return i;
            }
        }
        return configs.size();
    }

    private List<TaskPushNotificationConfig> convertPushNotificationConfig(List<PushNotificationConfig> pushNotificationConfigList, ListTaskPushNotificationConfigParams params) {
        List<TaskPushNotificationConfig> taskPushNotificationConfigList = new ArrayList<>(pushNotificationConfigList.size());
        for (PushNotificationConfig pushNotificationConfig : pushNotificationConfigList) {
            TaskPushNotificationConfig taskPushNotificationConfig = new TaskPushNotificationConfig(params.id(), pushNotificationConfig, params.tenant());
            taskPushNotificationConfigList.add(taskPushNotificationConfig);
        }
        return taskPushNotificationConfigList;
    }

    @Transactional
    @Override
    public void deleteInfo(String taskId, String configId) {
        if (configId == null) {
            configId = taskId;
        }

        LOGGER.debug("Deleting PushNotificationConfig for Task '{}' with Config ID: {}", taskId, configId);
        JpaPushNotificationConfig jpaConfig = em.find(JpaPushNotificationConfig.class,
                new TaskConfigId(taskId, configId));

        if (jpaConfig != null) {
            em.remove(jpaConfig);
            LOGGER.debug("Successfully deleted PushNotificationConfig for Task '{}' with Config ID: {}",
                    taskId, configId);
        } else {
            LOGGER.debug("PushNotificationConfig not found for deletion with Task '{}' and Config ID: {}",
                    taskId, configId);
        }
    }
}
