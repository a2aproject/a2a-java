package org.a2aproject.sdk.examples.springboot.rest.client;

import org.jspecify.annotations.Nullable;

public record SpringBootRestClientDemoRequest(
        @Nullable String helloMessage,
        @Nullable String streamMessage,
        @Nullable Integer streamingTimeoutSeconds) {
}
