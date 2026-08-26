package org.a2aproject.sdk.server.tasks;

/**
 * Validates push notification destination URLs before outbound HTTP requests are made.
 * <p>
 * Prevents Server-Side Request Forgery (SSRF) by enforcing security policies on
 * the client-controlled push notification URLs. The default implementation
 * ({@link DefaultPushNotificationUrlValidator}) blocks requests to private networks,
 * cloud metadata endpoints, and non-HTTPS URLs.
 * <p>
 * Custom implementations can be provided via CDI to adjust the policy for specific
 * deployment environments.
 *
 * @see DefaultPushNotificationUrlValidator
 * @see BasePushNotificationSender
 */
public interface PushNotificationUrlValidator {

    /**
     * A validator that accepts every URL without checking.
     * Intended for tests and development environments where SSRF protection is not needed.
     */
    PushNotificationUrlValidator ALLOW_ALL = url -> {};

    /**
     * Validates that the given URL is safe to use as a push notification target.
     *
     * @param url the push notification URL to validate
     * @throws IllegalArgumentException if the URL violates the security policy
     */
    void validate(String url);
}
