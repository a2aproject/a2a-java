package io.a2a.spec;

import io.a2a.util.Assert;

/**
 * Parameters for listing all push notification configurations for a task.
 * <p>
 * This record specifies which task's push notification configurations to list, returning
 * all configured notification endpoints for that task.
 *
 * @param id the task identifier (required)
 * @param tenant optional tenant, provided as a path parameter.
 * @see TaskPushNotificationConfig for the configuration structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record ListTaskPushNotificationConfigParams(String id, int pageSize, String pageToken, String tenant) {

    /**
     * Compact constructor for validation.
     * Validates that required parameters are not null.
     * @param id the task identifier
     * @param pageSize the maximum number of items to return per page
     * @param pageToken the pagination token for the next page
     * @param tenant the tenant identifier
     * @throws IllegalArgumentException if id or tenant is null
     */
    public ListTaskPushNotificationConfigParams {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("tenant", tenant);
    }

    /**
     * Convenience constructor with default tenant.
     *
     * @param id the task identifier (required)
     */
    public ListTaskPushNotificationConfigParams(String id) {
        this(id, 0, "", "");
    }

    /**
     * Validates and returns the effective page size.
     * If the requested pageSize is invalid (≤ 0 or > maxPageSize), returns maxPageSize.
     *
     * @param maxPageSize the maximum allowed page size
     * @return the effective page size (between 1 and maxPageSize)
     */
    public int getEffectivePageSize(int maxPageSize) {
      if (pageSize <= 0 || pageSize > maxPageSize) {
        return maxPageSize;
      }
      return pageSize;
    }
}
