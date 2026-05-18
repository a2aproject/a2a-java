package org.a2aproject.sdk.server.tasks;

import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.jspecify.annotations.Nullable;

public interface PushNotificationPayloadFormatter {

    String targetVersion();

    @Nullable String formatPayload(StreamingEventKind event, @Nullable Task taskSnapshot);
}
