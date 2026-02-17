package io.a2a.spec;

import io.a2a.util.Assert;
import io.a2a.util.Utils;
import org.jspecify.annotations.Nullable;

/**
 * Parameters containing a task identifier for task-related operations.
 * <p>
 * This simple parameter record is used by operations that only need a task ID,
 * and similar task-specific requests. It optionally includes metadata for additional context.
 *
 * @param id the unique task identifier (required)
 * @param tenant optional tenant, provided as a path parameter.
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record TaskIdParams(String id, String tenant) {

    /**
     * Compact constructor for validation.
     * Validates that required parameters are not null.
     *
     * @param id the task identifier
     * @param tenant the tenant identifier
     */
    public TaskIdParams  {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("tenant", tenant);
    }

    /**
     * Convenience constructor with default tenant.
     *
     * @param id the task identifier (required)
     */
    public TaskIdParams(String id) {
        this(id, "");
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
         * Builds the TaskIdParams.
         *
         * @return a new TaskIdParams instance
         * @throws IllegalArgumentException if id is null
         */
        public TaskIdParams build() {
            return new TaskIdParams(
                Assert.checkNotNullParam("id", id),
                Utils.defaultIfNull(tenant,"")
            );
        }
    }
}
