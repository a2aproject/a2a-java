package io.a2a.spec;



import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Parameters for retrieving push notification configuration for a specific task.
 * <p>
 * This record specifies which task's push notification configuration to retrieve, with
 * an optional filter by configuration ID if multiple configurations exist for the task.
 *
 * @param id the task identifier (required)
 * @param pushNotificationConfigId optional specific configuration ID to retrieve
 * @param tenant optional tenant, provided as a path parameter.
 * @see TaskPushNotificationConfig for the returned configuration structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record GetTaskPushNotificationConfigParams(String id, String pushNotificationConfigId, String tenant) {

    /**
     * Compact constructor that validates required fields.
     *
     * @param id the id parameter (see class-level JavaDoc)
     * @param pushNotificationConfigId the pushNotificationConfigId parameter (see class-level JavaDoc)
     * @param tenant the tenant parameter (see class-level JavaDoc)
     * @throws IllegalArgumentException if id or tenant is null
     */
    public GetTaskPushNotificationConfigParams {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("pushNotificationConfigId", pushNotificationConfigId);
        Assert.checkNotNullParam("tenant", tenant);
    }

    /**
     * Convenience constructor for creating parameters without tenant.
     *
     * @param id the task identifier (required)
     * @param pushNotificationConfigId optional configuration ID to retrieve
     */
    public GetTaskPushNotificationConfigParams(String id, String pushNotificationConfigId) {
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
     * Builder for constructing GetTaskPushNotificationConfigParams instances.
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
         * Sets the task ID.
         *
         * @param id the task ID
         * @return this builder for method chaining
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the push notification configuration ID.
         *
         * @param pushNotificationConfigId the configuration ID
         * @return this builder for method chaining
         */
        public Builder pushNotificationConfigId(String pushNotificationConfigId) {
            this.pushNotificationConfigId = pushNotificationConfigId;
            return this;
        }

        /**
         * Sets the tenant.
         *
         * @param tenant the tenant
         * @return this builder for method chaining
         */
        public Builder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        /**
         * Builds the parameters instance.
         *
         * @return a new GetTaskPushNotificationConfigParams
         */
        public GetTaskPushNotificationConfigParams build() {
            return new GetTaskPushNotificationConfigParams(
                    Assert.checkNotNullParam("id", id),
                    Assert.checkNotNullParam("pushNotificationConfigId", pushNotificationConfigId),
                    Assert.checkNotNullParam("tenant", tenant));
        }
    }
}
