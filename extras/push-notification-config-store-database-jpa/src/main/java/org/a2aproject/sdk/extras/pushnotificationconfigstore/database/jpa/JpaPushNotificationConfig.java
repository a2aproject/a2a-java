package org.a2aproject.sdk.extras.pushnotificationconfigstore.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import java.time.Instant;

@Entity
@Table(name = "a2a_push_notification_configs")
public class JpaPushNotificationConfig {
    @EmbeddedId
    private TaskConfigId id;

    @Column(name = "task_data", columnDefinition = "TEXT", nullable = false)
    private String configJson;

    @Column(name = "created_at")
    private Instant createdAt;

    @Transient
    private TaskPushNotificationConfig config;

    // Default constructor required by JPA
    public JpaPushNotificationConfig() {
    }

    public JpaPushNotificationConfig(TaskConfigId id, String configJson) {
        this.id = id;
        this.configJson = configJson;
    }

    @PrePersist
    protected void onCreate() {
      if (createdAt == null) {
        createdAt = Instant.now();
      }
    }

    public TaskConfigId getId() {
        return id;
    }

    public void setId(TaskConfigId id) {
        this.id = id;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public TaskPushNotificationConfig getConfig() throws JsonProcessingException {
        if (config == null) {
            this.config = JsonUtil.fromJson(configJson, TaskPushNotificationConfig.class);
        }
        return config;
    }

    public void setConfig(TaskPushNotificationConfig config) throws JsonProcessingException {
        if (config.id() == null || !config.id().equals(id.getConfigId())) {
            throw new IllegalArgumentException("Mismatched config id. " +
                    "Expected '" + id.getConfigId() + "'. Got: '" + config.id() + "'");
        }
        configJson = JsonUtil.toJson(config);
        this.config = config;
    }

    public Instant getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
      this.createdAt = createdAt;
    }

    static JpaPushNotificationConfig createFromConfig(String taskId, TaskPushNotificationConfig config) throws JsonProcessingException {
        String json = JsonUtil.toJson(config);
        JpaPushNotificationConfig jpaPushNotificationConfig =
                new JpaPushNotificationConfig(new TaskConfigId(taskId, config.id()), json);
        jpaPushNotificationConfig.config = config;
        return jpaPushNotificationConfig;
    }
}
