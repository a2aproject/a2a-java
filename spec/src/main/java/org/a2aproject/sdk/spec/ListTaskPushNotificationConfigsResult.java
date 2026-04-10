package org.a2aproject.sdk.spec;

import java.util.List;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Result of listing push notification configurations for a task with pagination support.
 *
 * @param configs List of push notification configurations for the task
 * @param nextPageToken Token for retrieving the next page of results (null if no more results)
 */
public record ListTaskPushNotificationConfigsResult(List<TaskPushNotificationConfig> configs,
        @Nullable String nextPageToken) {
    /**
     * Compact constructor for validation.
     * Validates parameters and creates a defensive copy of the configs list.
     *
     * @param configs the list of push notification configurations
     * @param nextPageToken token for next page
     * @throws IllegalArgumentException if validation fails
     */
    public ListTaskPushNotificationConfigsResult {
        Assert.checkNotNullParam("configs", configs);
        // Make defensive copy
        configs = List.copyOf(configs);
    }

    /**
     * Constructor for results without pagination.
     *
     * @param configs the list of push notification configurations
     */
    public ListTaskPushNotificationConfigsResult(List<TaskPushNotificationConfig> configs) {
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

    /**
     * Return the size of the configs.
     * @return the size of the configs.
     */
    public int size() {
        return configs.size();
    }

    /**
     * Return if the configs is empty or not.
     * @return true if the configs is empty - false otherwise.
     */
    public boolean isEmpty() {
        return configs.isEmpty();
    }
}
