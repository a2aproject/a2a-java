package io.a2a.spec;

import io.a2a.util.Assert;
import io.a2a.util.Utils;
import org.jspecify.annotations.Nullable;

/**
 * Defines parameters for querying a task, with an option to limit history length.
 *
 * @param id the ID for the task to be queried
 * @param historyLength the maximum number of items of history for the task to include in the response
 * @param tenant optional tenant, provided as a path parameter.
 */
public record TaskQueryParams(String id, @Nullable Integer historyLength, String tenant) {

    /**
     * Compact constructor for validation.
     * Validates that required parameters are not null and historyLength is non-negative if provided.
     *
     * @param id the task identifier
     * @param historyLength maximum number of history items
     * @param tenant the tenant identifier
     * @throws IllegalArgumentException if historyLength is negative
     */
    public TaskQueryParams {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("tenant", tenant);
        if (historyLength != null && historyLength < 0) {
            throw new IllegalArgumentException("Invalid history length");
        }
    }

    /**
     * Convenience constructor with default tenant.
     *
     * @param id the task identifier (required)
     * @param historyLength maximum number of history items to include (optional)
     */
    public TaskQueryParams(String id, @Nullable Integer historyLength) {
        this(id, historyLength, "");
    }

    /**
     * Convenience constructor with defaults for tenant and historyLength.
     *
     * @param id the task identifier (required)
     */
    public TaskQueryParams(String id) {
        this(id, null, "");
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
        private @Nullable Integer historyLength;
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
         * Sets the historyLength.
         *
         * @param historyLength the maximum number of history items to include
         * @return this builder for method chaining
         */
        public Builder historyLength(Integer historyLength) {
            this.historyLength = historyLength;
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
         * Builds the TaskQueryParams.
         *
         * @return a new TaskQueryParams instance
         * @throws IllegalArgumentException if id is null or historyLength is negative
         */
        public TaskQueryParams build() {
            return new TaskQueryParams(
                Assert.checkNotNullParam("id", id),
                historyLength,
                Utils.defaultIfNull(tenant,"")
            );
        }
    }
}
