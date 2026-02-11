package io.a2a.spec;

import io.a2a.util.Assert;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * Parameters for listing tasks with optional filtering and pagination.
 *
 * @param contextId Filter tasks by context ID to get tasks from a specific conversation or session
 * @param status Filter tasks by their current status state
 * @param pageSize Maximum number of tasks to return (1-100, defaults to 50)
 * @param pageToken Token for pagination from a previous ListTasksResult
 * @param historyLength Number of recent messages to include in each task's history (defaults to 0)
 * @param statusTimestampAfter Filter tasks updated after this timestamp
 * @param includeArtifacts Whether to include artifacts in the returned tasks (defaults to false)
 * @param tenant optional tenant, provided as a path parameter.
 */
public record ListTasksParams(
        @Nullable String contextId,
        @Nullable TaskState status,
        @Nullable Integer pageSize,
        @Nullable String pageToken,
        @Nullable Integer historyLength,
        @Nullable Instant statusTimestampAfter,
        @Nullable Boolean includeArtifacts,
        String tenant
) {
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 50;
    /**
     * Compact constructor for validation.
     * Validates that the tenant parameter is not null and parameters are within valid ranges.
     *
     * @param contextId filter by context ID
     * @param status filter by task status
     * @param pageSize maximum number of results per page
     * @param pageToken pagination token
     * @param historyLength number of history items to include
     * @param statusTimestampAfter filter by status timestamp
     * @param includeArtifacts whether to include artifacts
     * @param tenant the tenant identifier
     * @throws InvalidParamsError if tenant is null or if pageSize or historyLength are out of valid range
     */
    public ListTasksParams {
        Assert.checkNotNullParam("tenant", tenant);
        // Validate pageSize (1-100)
        if (pageSize != null && (pageSize < MIN_PAGE_SIZE || pageSize > MAX_PAGE_SIZE)) {
            throw new InvalidParamsError(null,
                "pageSize must be between " + MIN_PAGE_SIZE + " and " + MAX_PAGE_SIZE + ", got: " + pageSize, null);
        }

        // Validate historyLength (>= 0)
        if (historyLength != null && historyLength < 0) {
            throw new InvalidParamsError(null,
                "historyLength must be non-negative, got: " + historyLength, null);
        }
    }
    /**
     * Default constructor for listing all tasks.
     */
    public ListTasksParams() {
        this(null, null, null, null, null, null, null, "");
    }

    /**
     * Constructor with pagination.
     *
     * @param pageSize Maximum number of tasks to return
     * @param pageToken Token for pagination
     */
    public ListTasksParams(Integer pageSize, String pageToken) {
        this(null, null, pageSize, pageToken, null, null, null, "");
    }

    /**
     * Returns the effective page size (defaults to 50 if not specified).
     * Values are validated in the constructor to be within the range [1, 100].
     *
     * @return the effective page size
     */
    public int getEffectivePageSize() {
        return pageSize != null ? pageSize : DEFAULT_PAGE_SIZE;
    }

    /**
     * Returns the effective history length (defaults to 0 if not specified).
     * Values are validated in the constructor to be non-negative.
     *
     * @return the effective history length
     */
    public int getEffectiveHistoryLength() {
        return historyLength != null ? historyLength : 0;
    }

    /**
     * Returns whether to include artifacts (defaults to false).
     *
     * @return true if artifacts should be included
     */
    public boolean shouldIncludeArtifacts() {
        return includeArtifacts != null && includeArtifacts;
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
        private @Nullable String contextId;
        private @Nullable TaskState status;
        private @Nullable Integer pageSize;
        private @Nullable String pageToken;
        private @Nullable Integer historyLength;
        private @Nullable Instant statusTimestampAfter;
        private @Nullable Boolean includeArtifacts;
        private @Nullable String tenant;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the contextId.
         *
         * @param contextId the contextId
         * @return this builder for method chaining
         */
        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        /**
         * Sets the status.
         *
         * @param status the status
         * @return this builder for method chaining
         */
        public Builder status(TaskState status) {
            this.status = status;
            return this;
        }

        /**
         * Sets the pageSize.
         *
         * @param pageSize the pageSize
         * @return this builder for method chaining
         */
        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Sets the pageToken.
         *
         * @param pageToken the pageToken
         * @return this builder for method chaining
         */
        public Builder pageToken(String pageToken) {
            this.pageToken = pageToken;
            return this;
        }

        /**
         * Sets the historyLength.
         *
         * @param historyLength the historyLength
         * @return this builder for method chaining
         */
        public Builder historyLength(Integer historyLength) {
            this.historyLength = historyLength;
            return this;
        }

        /**
         * Sets the statusTimestampAfter.
         *
         * @param statusTimestampAfter the statusTimestampAfter
         * @return this builder for method chaining
         */
        public Builder statusTimestampAfter(Instant statusTimestampAfter) {
            this.statusTimestampAfter = statusTimestampAfter;
            return this;
        }

        /**
         * Sets the includeArtifacts.
         *
         * @param includeArtifacts the includeArtifacts
         * @return this builder for method chaining
         */
        public Builder includeArtifacts(Boolean includeArtifacts) {
            this.includeArtifacts = includeArtifacts;
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
         * Builds the ListTasksParams.
         *
         * @return a new ListTasksParams instance
         */
        public ListTasksParams build() {
            return new ListTasksParams(contextId, status, pageSize, pageToken, historyLength,
                    statusTimestampAfter, includeArtifacts, Assert.checkNotNullParam("tenant", tenant));
        }
    }
}
