package io.a2a.spec;

import io.a2a.util.Assert;
import io.a2a.util.Utils;
import org.jspecify.annotations.Nullable;

/**
 * Parameters for listing all push notification configurations for a task.
 * <p>
 * This record specifies which task's push notification configurations to list, returning
 * all configured notification endpoints for that task.
 *
 * @param id the task identifier (required)
 * @param tenant optional tenant, provided as a path parameter.
 * @param pageSize the maximum number of items to return per page
 * @param pageToken the pagination token for the next page
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
     * Validates and returns the effective page size (between 1 and 100, defaults to 100).
     *
     * @return the effective page size
     */
    public int getEffectivePageSize() {
      if (pageSize <= 0 || pageSize > 100) {
        return 100;
      }
      return pageSize;
    }

    /**
     * Create a new Builder
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing instances.
     */
    public static class Builder {
        private @Nullable String id;
        private @Nullable Integer pageSize;
        private @Nullable String pageToken;
        private @Nullable String tenant;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the id.
         *
         * @param id the task identifier (required)
         * @return this builder for method chaining
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the pageSize.
         *
         * @param pageSize the maximum number of items to return per page
         * @return this builder for method chaining
         */
        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Sets the pageToken.
         *
         * @param pageToken the pagination token for the next page
         * @return this builder for method chaining
         */
        public Builder pageToken(String pageToken) {
            this.pageToken = pageToken;
            return this;
        }

        /**
         * Sets the tenant.
         *
         * @param tenant the tenant identifier
         * @return this builder for method chaining
         */
        public Builder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        /**
         * Builds the ListTaskPushNotificationConfigParams.
         *
         * @return a new ListTaskPushNotificationConfigParams instance
         * @throws IllegalArgumentException if id is null
         */
        public ListTaskPushNotificationConfigParams build() {
            return new ListTaskPushNotificationConfigParams(
                Assert.checkNotNullParam("id", id),
                pageSize != null ? pageSize : 0,
                pageToken != null ? pageToken : "",
                Utils.defaultIfNull(tenant,"")
            );
        }
    }
}
