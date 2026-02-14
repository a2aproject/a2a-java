package io.a2a.spec;



import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Parameters for retrieving push notification configuration for a specific task.
 * <p>
 * This record specifies which task's push notification configuration to retrieve, with
 * an optional filter by configuration ID if multiple configurations exist for the task.
 *
 * @param taskId the task identifier (required)
 * @param id optional specific configuration ID to retrieve
 * @param tenant optional tenant, provided as a path parameter.
 * @see TaskPushNotificationConfig for the returned configuration structure
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record GetTaskPushNotificationConfigParams(String taskId, String id, String tenant) {

    /**
     * Compact constructor that validates required fields.
     *
     * @param taskId the taskId parameter (see class-level JavaDoc)
     * @param id the id parameter (see class-level JavaDoc)
     * @param tenant the tenant parameter (see class-level JavaDoc)
     * @throws IllegalArgumentException if taskId or tenant is null
     */
    public GetTaskPushNotificationConfigParams {
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("tenant", tenant);
    }

    /**
     * Convenience constructor for creating parameters without tenant.
     *
     * @param taskId the task identifier (required)
     * @param id optional configuration ID to retrieve
     */
    public GetTaskPushNotificationConfigParams(String taskId, String id) {
        this(taskId, id, "");
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
        @Nullable String taskId;
        @Nullable String id;
        @Nullable String tenant;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the task ID.
         *
         * @param taskId the task ID
         * @return this builder for method chaining
         */
        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        /**
         * Sets the push notification configuration ID.
         *
         * @param id the configuration ID
         * @return this builder for method chaining
         */
        public Builder id(String id) {
            this.id = id;
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
                    Assert.checkNotNullParam("taskId", taskId),
                    Assert.checkNotNullParam("id", id),
                    Assert.checkNotNullParam("tenant", tenant));
        }
    }
}
