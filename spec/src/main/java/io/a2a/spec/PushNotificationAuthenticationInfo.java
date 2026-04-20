package io.a2a.spec;

import java.util.List;
import io.a2a.util.Assert;

/**
 * Defines authentication details for a push notification endpoint.
 *
 * @param schemes the list of authentication scheme identifiers
 * @param credentials optional credentials string for the authentication scheme
 */
public record PushNotificationAuthenticationInfo(List<String> schemes, String credentials) {

    public PushNotificationAuthenticationInfo {
        Assert.checkNotNullParam("schemes", schemes);
    }
}
