package org.a2aproject.sdk.compat03.spec;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.a2aproject.sdk.util.Assert;

/**
 * Defines authentication details for a push notification endpoint.
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PushNotificationAuthenticationInfo(List<String> schemes, String credentials) {

    public PushNotificationAuthenticationInfo {
        Assert.checkNotNullParam("schemes", schemes);
    }
}
