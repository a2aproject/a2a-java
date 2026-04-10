package org.a2aproject.sdk.compat03.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.a2aproject.sdk.util.Assert;

/**
 * Parameters for getting list of pushNotificationConfigurations associated with a Task.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ListTaskPushNotificationConfigParams(String id, Map<String, Object> metadata) {

    public ListTaskPushNotificationConfigParams {
        Assert.checkNotNullParam("id", id);
    }

    public ListTaskPushNotificationConfigParams(String id) {
        this(id, null);
    }
}
