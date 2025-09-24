package io.a2a.extras.pushnotificationconfigstore.database.jpa;

import java.util.List;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.spec.PushNotificationConfig;
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
        PushNotificationConfig.Builder builder = new PushNotificationConfig.Builder(notificationConfig);
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
                existingJpaConfig.setConfig(taskId, notificationConfig);
                LOGGER.debug("Updated existing PushNotificationConfig for Task '{}' with ID: {}",
                        taskId, notificationConfig.id());
            } else {
                // Create new entity
                JpaPushNotificationConfig jpaConfig = JpaPushNotificationConfig.createFromConfig(taskId, notificationConfig);
                em.merge(jpaConfig);
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
    public List<PushNotificationConfig> getInfo(String taskId) {
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
            return configs;
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve PushNotificationConfigs for Task '{}'", taskId, e);
            throw e;
        }
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
