package io.a2a.extras.pushnotificationconfigstore.database.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.util.Utils;

@Entity
@Table(name = "a2a_push_notification_configs")
public class JpaPushNotificationConfig {
    @EmbeddedId
    private TaskConfigId id;

    @Column(name = "task_data", columnDefinition = "TEXT", nullable = false)
    private String configJson;

    @Transient
    private PushNotificationConfig config;

    // Default constructor required by JPA
    public JpaPushNotificationConfig() {
    }

    public JpaPushNotificationConfig(TaskConfigId id, String configJson) {
        this.id = id;
        this.configJson = configJson;
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

    public PushNotificationConfig getConfig() throws JsonProcessingException {
        if (config == null) {
            this.config = Utils.unmarshalFrom(configJson, PushNotificationConfig.TYPE_REFERENCE);
        }
        return config;
    }

    public void setConfig(PushNotificationConfig config) throws JsonProcessingException {
        if (config.id() == null || !config.id().equals(id.getConfigId())) {
            throw new IllegalArgumentException("Mismatched config id. " +
                    "Expected '" + id.getConfigId() + "'. Got: '" + config.id() + "'");
        }
        configJson = Utils.OBJECT_MAPPER.writeValueAsString(config);
        this.config = config;
    }

    static JpaPushNotificationConfig createFromConfig(String taskId, PushNotificationConfig config) throws JsonProcessingException {
        String json = Utils.OBJECT_MAPPER.writeValueAsString(config);
        JpaPushNotificationConfig jpaPushNotificationConfig =
                new JpaPushNotificationConfig(new TaskConfigId(taskId, config.id()), json);
        jpaPushNotificationConfig.config = config;
        return jpaPushNotificationConfig;
    }
}
