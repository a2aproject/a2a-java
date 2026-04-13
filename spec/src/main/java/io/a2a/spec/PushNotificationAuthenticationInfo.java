package io.a2a.spec;

import java.util.List;
import io.a2a.util.Assert;

/**
 * Defines authentication details for a push notification endpoint.
 */
public record PushNotificationAuthenticationInfo(List<String> schemes, String credentials) {

    public PushNotificationAuthenticationInfo {
        Assert.checkNotNullParam("schemes", schemes);
    }
}
