package io.a2a.spec;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Result of listing push notification configurations for a task with pagination support.
 *
 * @param configs List of push notification configurations for the task
 * @param nextPageToken Token for retrieving the next page of results (null if no more results)
 */
public record ListTaskPushNotificationConfigResult(
        List<TaskPushNotificationConfig> configs,
        @Nullable String nextPageToken
) {
    /**
     * Compact constructor for validation.
     * Validates parameters and creates a defensive copy of the configs list.
     *
     * @param configs the list of push notification configurations
     * @param nextPageToken token for next page
     * @throws IllegalArgumentException if validation fails
     */
    public ListTaskPushNotificationConfigResult {
        Assert.checkNotNullParam("configs", configs);
        // Make defensive copy
        configs = List.copyOf(configs);
    }

    /**
     * Constructor for results without pagination.
     *
     * @param configs the list of push notification configurations
     */
    public ListTaskPushNotificationConfigResult(List<TaskPushNotificationConfig> configs) {
        this(configs, null);
    }

    /**
     * Returns whether there are more results available.
     *
     * @return true if there are more pages of results
     */
    public boolean hasMoreResults() {
        return nextPageToken != null && !nextPageToken.isEmpty();
    }

    public int size() {
        return configs.size();
    }
}
