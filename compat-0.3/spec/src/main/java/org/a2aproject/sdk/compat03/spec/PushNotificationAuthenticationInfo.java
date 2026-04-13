package org.a2aproject.sdk.compat03.spec;

import java.util.List;
import org.a2aproject.sdk.util.Assert;

/**
 * Defines authentication details for a push notification endpoint.
 */
public record PushNotificationAuthenticationInfo(List<String> schemes, String credentials) {

    public PushNotificationAuthenticationInfo {
        Assert.checkNotNullParam("schemes", schemes);
    }
}
