package io.a2a.spec;



import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Parameters for deleting a push notification configuration from a task.
 * <p>
 * This record specifies which task and which specific push notification configuration
 * to remove, allowing cleanup of notification endpoints that are no longer needed.
 *
 * @param id the task identifier (required)
 * @param pushNotificationConfigId the specific configuration ID to delete (required)
 * @param tenant optional tenant, provided as a path parameter.
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record DeleteTaskPushNotificationConfigParams(String id, String pushNotificationConfigId, String tenant) {

    /**
     * Compact constructor that validates required fields.
     *
     * @param id the id parameter (see class-level JavaDoc)
     * @param pushNotificationConfigId the pushNotificationConfigId parameter (see class-level JavaDoc)
     * @param tenant the tenant parameter (see class-level JavaDoc)
     * @throws IllegalArgumentException if id or pushNotificationConfigId is null
     */
    public DeleteTaskPushNotificationConfigParams {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("pushNotificationConfigId", pushNotificationConfigId);
        Assert.checkNotNullParam("tenant", tenant);
    }

    /**
     * Creates parameters without optional metadata.
     *
     * @param id the task identifier (required)
     * @param pushNotificationConfigId the configuration ID to delete (required)
     * @throws IllegalArgumentException if id or pushNotificationConfigId is null
     */
    public DeleteTaskPushNotificationConfigParams(String id, String pushNotificationConfigId) {
        this(id, pushNotificationConfigId, "");
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
     * Builder for constructing {@link DeleteTaskPushNotificationConfigParams} instances.
     * <p>
     * Provides a fluent API for setting parameters with optional metadata.
     */
    public static class Builder {
        @Nullable String id;
        @Nullable String pushNotificationConfigId;
        @Nullable String tenant;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the task identifier.
         *
         * @param id the task ID (required)
         * @return this builder for method chaining
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the push notification configuration ID to delete.
         *
         * @param pushNotificationConfigId the configuration ID (required)
         * @return this builder for method chaining
         */
        public Builder pushNotificationConfigId(String pushNotificationConfigId) {
            this.pushNotificationConfigId = pushNotificationConfigId;
            return this;
        }

        /**
         * Sets optional tenant for the request.
         *
         * @param tenant arbitrary tenant (optional)
         * @return this builder for method chaining
         */
        public Builder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        /**
         * Builds a new {@link DeleteTaskPushNotificationConfigParams} from the current builder state.
         *
         * @return a new DeleteTaskPushNotificationConfigParams instance
         * @throws IllegalArgumentException if id or pushNotificationConfigId is null
         */
        public DeleteTaskPushNotificationConfigParams build() {
            return new DeleteTaskPushNotificationConfigParams(
                    Assert.checkNotNullParam("id", id),
                    Assert.checkNotNullParam("pushNotificationConfigId", pushNotificationConfigId),
                    Assert.checkNotNullParam("tenant", tenant));
        }
    }
}
