package io.a2a.extras.pushnotificationconfigstore.database.jpa;

import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.List;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.spec.ListTaskPushNotificationConfigParams;
import io.a2a.spec.ListTaskPushNotificationConfigResult;
import io.a2a.util.Assert;
import io.a2a.util.PageToken;
import io.a2a.spec.TaskPushNotificationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Alternative
@Priority(50)
public class JpaDatabasePushNotificationConfigStore implements PushNotificationConfigStore {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaDatabasePushNotificationConfigStore.class);

    private static final Instant NULL_TIMESTAMP_SENTINEL = Instant.EPOCH;

    @PersistenceContext(unitName = "a2a-java")
    EntityManager em;

    @Transactional
    @Override
    public TaskPushNotificationConfig setInfo(TaskPushNotificationConfig notificationConfig) {
        String taskId = Assert.checkNotNullParam("taskId", notificationConfig.taskId());
        // Ensure config has an ID - default to taskId if not provided (mirroring InMemoryPushNotificationConfigStore behavior)
        if (notificationConfig.id().isEmpty()) {
            // This means the taskId and configId are same. This will not allow having multiple configs for a single Task.
            // The configId is a required field in the spec and should not be empty
            notificationConfig = TaskPushNotificationConfig.builder(notificationConfig).id(taskId).build();
        }

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
        LOGGER.debug("Retrieving PushNotificationConfigs for Task '{}' with params: pageSize={}, pageToken={}",
            taskId, params.pageSize(), params.pageToken());

        // Parse pageToken once at the beginning
        PageToken pageToken = PageToken.fromString(params.pageToken());

        try {
            StringBuilder queryBuilder = new StringBuilder("SELECT c FROM JpaPushNotificationConfig c WHERE c.id.taskId = :taskId");

            if (pageToken != null) {
                // Keyset pagination: get tasks where timestamp < tokenTimestamp OR (timestamp = tokenTimestamp AND id > tokenId)
                // All tasks have timestamps (TaskStatus canonical constructor ensures this)
                queryBuilder.append(" AND (COALESCE(c.createdAt, :nullSentinel) < :tokenTimestamp OR (COALESCE(c.createdAt, :nullSentinel) = :tokenTimestamp AND c.id.configId > :tokenId))");
            }

            queryBuilder.append(" ORDER BY  COALESCE(c.createdAt, :nullSentinel) DESC, c.id.configId ASC");

            TypedQuery<JpaPushNotificationConfig> query = em.createQuery(queryBuilder.toString(), JpaPushNotificationConfig.class);
            query.setParameter("taskId", taskId);
            query.setParameter("nullSentinel", NULL_TIMESTAMP_SENTINEL);

            if (pageToken != null) {
                query.setParameter("tokenTimestamp", pageToken.timestamp());
                query.setParameter("tokenId", pageToken.id());
            }

            int pageSize = params.getEffectivePageSize();
            query.setMaxResults(pageSize + 1);
            List<JpaPushNotificationConfig> jpaConfigsPage = query.getResultList();

            String nextPageToken = null;
            if (jpaConfigsPage.size() > pageSize) {
              // There are more results than the page size, and in this case, a nextToken should be created with the last item.
              // Format: "timestamp_millis:taskId" for keyset pagination
              jpaConfigsPage = jpaConfigsPage.subList(0, pageSize);
              JpaPushNotificationConfig lastConfig =  jpaConfigsPage.get(jpaConfigsPage.size() - 1);
              Instant timestamp = lastConfig.getCreatedAt() != null ? lastConfig.getCreatedAt() : NULL_TIMESTAMP_SENTINEL;
              nextPageToken = new PageToken(timestamp, lastConfig.getId().getConfigId()).toString();
            }

            List<TaskPushNotificationConfig> taskPushNotificationConfigs = jpaConfigsPage.stream()
                    .map(jpaConfig -> {
                        try {
                            TaskPushNotificationConfig config = jpaConfig.getConfig();
                            // Set taskId and tenant from the params
                            return TaskPushNotificationConfig.builder(config)
                                    .taskId(params.id())
                                    .tenant(params.tenant())
                                    .build();
                        } catch (JsonProcessingException e) {
                            LOGGER.error("Failed to deserialize PushNotificationConfig for Task '{}' with ID: {}",
                                    taskId, jpaConfig.getId().getConfigId(), e);
                            throw new RuntimeException("Failed to deserialize PushNotificationConfig for Task '" +
                                    taskId + "' with ID: " + jpaConfig.getId().getConfigId(), e);
                        }
                    })
                    .toList();

            LOGGER.debug("Successfully retrieved {} PushNotificationConfigs for Task '{}'", taskPushNotificationConfigs.size(), taskId);

            return new ListTaskPushNotificationConfigResult(taskPushNotificationConfigs, nextPageToken);
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
