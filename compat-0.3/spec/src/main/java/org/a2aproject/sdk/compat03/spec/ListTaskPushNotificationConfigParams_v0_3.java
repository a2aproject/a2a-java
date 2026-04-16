package org.a2aproject.sdk.compat03.spec;

import java.util.Map;

import org.a2aproject.sdk.util.Assert;

/**
 * Parameters for getting list of pushNotificationConfigurations associated with a Task.
 */
public record ListTaskPushNotificationConfigParams_v0_3(String id, Map<String, Object> metadata) {

    public ListTaskPushNotificationConfigParams_v0_3 {
        Assert.checkNotNullParam("id", id);
    }

    public ListTaskPushNotificationConfigParams_v0_3(String id) {
        this(id, null);
    }
}
