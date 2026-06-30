package org.a2aproject.sdk.integrations.springboot.server.rest;

/**
 * Observer for streaming subscription startup events.
 *
 * <p>The bean is optional. Production applications do not need to define it, but tests and demos
 * can use it to observe when a streaming response has been initialized.
 */
@FunctionalInterface
public interface StreamingSubscriptionObserver {

    void onStreamingSubscription();
}
