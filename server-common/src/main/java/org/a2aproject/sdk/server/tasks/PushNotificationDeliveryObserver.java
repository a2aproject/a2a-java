package org.a2aproject.sdk.server.tasks;

import org.jspecify.annotations.Nullable;

/** Optional observer of individual HTTP push notification delivery attempts. */
@FunctionalInterface
public interface PushNotificationDeliveryObserver {
    /** Called after a delivery attempt. Implementations must not change its outcome. */
    void onDelivery(@Nullable String taskId, @Nullable String configId, boolean success);
}
