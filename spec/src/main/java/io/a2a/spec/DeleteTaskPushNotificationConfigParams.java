package io.a2a.spec;



import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Parameters for deleting a push notification configuration from a task.
 * <p>
 * This record specifies which task and which specific push notification configuration
 * to remove, allowing cleanup of notification endpoints that are no longer needed.
 *
 * @param taskId the task identifier (required)
 * @param id the specific configuration ID to delete (required)
 * @param tenant optional tenant, provided as a path parameter.
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record DeleteTaskPushNotificationConfigParams(String taskId, String id, String tenant) {

    /**
     * Compact constructor that validates required fields.
     *
     * @param taskId the taskId parameter (see class-level JavaDoc)
     * @param id the id parameter (see class-level JavaDoc)
     * @param tenant the tenant parameter (see class-level JavaDoc)
     * @throws IllegalArgumentException if taskId or id is null
     */
    public DeleteTaskPushNotificationConfigParams {
        Assert.checkNotNullParam("taskId", taskId);
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("tenant", tenant);
    }

    /**
     * Creates parameters without optional metadata.
     *
     * @param taskId the task identifier (required)
     * @param id the configuration ID to delete (required)
     * @throws IllegalArgumentException if taskId or id is null
     */
    public DeleteTaskPushNotificationConfigParams(String taskId, String id) {
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
     * Builder for constructing {@link DeleteTaskPushNotificationConfigParams} instances.
     * <p>
     * Provides a fluent API for setting parameters with optional metadata.
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
         * Sets the task identifier.
         *
         * @param taskId the task ID (required)
         * @return this builder for method chaining
         */
        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        /**
         * Sets the push notification configuration ID to delete.
         *
         * @param id the configuration ID (required)
         * @return this builder for method chaining
         */
        public Builder id(String id) {
            this.id = id;
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
         * @throws IllegalArgumentException if taskId or id is null
         */
        public DeleteTaskPushNotificationConfigParams build() {
            return new DeleteTaskPushNotificationConfigParams(
                    Assert.checkNotNullParam("taskId", taskId),
                    Assert.checkNotNullParam("id", id),
                    Assert.checkNotNullParam("tenant", tenant));
        }
    }
}
