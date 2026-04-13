package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;

/**
 * Parameters for getting list of pushNotificationConfigurations associated with a Task.
 */
public record ListTaskPushNotificationConfigParams(String id, Map<String, Object> metadata) {

    public ListTaskPushNotificationConfigParams {
        Assert.checkNotNullParam("id", id);
    }

    public ListTaskPushNotificationConfigParams(String id) {
        this(id, null);
    }
}
