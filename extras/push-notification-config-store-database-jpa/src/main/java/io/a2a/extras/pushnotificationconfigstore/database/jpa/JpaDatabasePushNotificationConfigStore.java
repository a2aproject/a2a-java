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
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.TaskPushNotificationConfig;
import java.util.stream.Collectors;
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
    public PushNotificationConfig setInfo(String taskId, PushNotificationConfig notificationConfig) {
        // Ensure config has an ID - default to taskId if not provided (mirroring InMemoryPushNotificationConfigStore behavior)
        PushNotificationConfig.Builder builder = PushNotificationConfig.builder(notificationConfig);
        if (notificationConfig.id() == null || notificationConfig.id().isEmpty()) {
            // This means the taskId and configId are same. This will not allow having multiple configs for a single Task.
            // The configId is a required field in the spec and should not be empty
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
        LOGGER.debug("Retrieving PushNotificationConfigs for Task '{}' with params: pageSize={}, pageToken={}",
            taskId, params.pageSize(), params.pageToken());
        try {
            StringBuilder queryBuilder = new StringBuilder("SELECT c FROM JpaPushNotificationConfig c WHERE c.id.taskId = :taskId");

            if (params.pageToken() != null && !params.pageToken().isEmpty()) {
              String[] tokenParts = params.pageToken().split(":", 2);
              if (tokenParts.length == 2) {
                // Keyset pagination: get tasks where timestamp < tokenTimestamp OR (timestamp = tokenTimestamp AND id > tokenId)
                // All tasks have timestamps (TaskStatus canonical constructor ensures this)
                queryBuilder.append(" AND (COALESCE(c.createdAt, :nullSentinel) < :tokenTimestamp OR (COALESCE(c.createdAt, :nullSentinel) = :tokenTimestamp AND c.id.configId > :tokenId))");
              } else {
                // Based on the comments in the test case, if the pageToken is invalid start from the beginning.
              }
            }

            queryBuilder.append(" ORDER BY  COALESCE(c.createdAt, :nullSentinel) DESC, c.id.configId ASC");

            TypedQuery<JpaPushNotificationConfig> query = em.createQuery(queryBuilder.toString(), JpaPushNotificationConfig.class);
            query.setParameter("taskId", taskId);
            query.setParameter("nullSentinel", NULL_TIMESTAMP_SENTINEL);

            if (params.pageToken() != null && !params.pageToken().isEmpty()) {
              String[] tokenParts = params.pageToken().split(":", 2);
              if (tokenParts.length == 2) {
                try {
                  long timestampMillis = Long.parseLong(tokenParts[0]);
                  String tokenId = tokenParts[1];

                  Instant tokenTimestamp = Instant.ofEpochMilli(timestampMillis);
                  query.setParameter("tokenTimestamp", tokenTimestamp);
                  query.setParameter("tokenId", tokenId);
                } catch (NumberFormatException e) {
                  // Malformed timestamp in pageToken
                  throw new io.a2a.spec.InvalidParamsError(null,
                      "Invalid pageToken format: timestamp must be numeric milliseconds", null);
                }
              }
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
              nextPageToken = timestamp.toEpochMilli() + ":" + lastConfig.getId().getConfigId();
            }

            List<PushNotificationConfig> configs = jpaConfigsPage.stream()
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

            List<TaskPushNotificationConfig> taskPushNotificationConfigs = configs.stream()
                .map(config -> new TaskPushNotificationConfig(params.id(), config, params.tenant()))
                .collect(Collectors.toList());

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
