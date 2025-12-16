package io.a2a.spec;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Parameters for listing tasks with optional filtering and pagination.
 *
 * @param contextId Filter tasks by context ID to get tasks from a specific conversation or session
 * @param status Filter tasks by their current status state
 * @param pageSize Maximum number of tasks to return (1-100, defaults to 50)
 * @param pageToken Token for pagination from a previous ListTasksResult
 * @param historyLength Number of recent messages to include in each task's history (defaults to 0)
 * @param lastUpdatedAfter Filter tasks updated after this timestamp
 * @param includeArtifacts Whether to include artifacts in the returned tasks (defaults to false)
 * @param tenant optional tenant, provided as a path parameter.
 */
public record ListTasksParams(
        @Nullable String contextId,
        @Nullable TaskState status,
        @Nullable Integer pageSize,
        @Nullable String pageToken,
        @Nullable Integer historyLength,
        @Nullable Instant lastUpdatedAfter,
        @Nullable Boolean includeArtifacts,
        String tenant
) {
    public ListTasksParams {
        Assert.checkNotNullParam("tenant", tenant);
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
     * Validates and returns the effective page size (between 1 and 100, defaults to 50).
     *
     * @return the effective page size
     */
    public int getEffectivePageSize() {
        if (pageSize == null) {
            return 50;
        }
        if (pageSize < 1) {
            return 1;
        }
        if (pageSize > 100) {
            return 100;
        }
        return pageSize;
    }

    /**
     * Returns the effective history length (non-negative, defaults to 0).
     *
     * @return the effective history length
     */
    public int getEffectiveHistoryLength() {
        if (historyLength == null || historyLength < 0) {
            return 0;
        }
        return historyLength;
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

    public static class Builder {
        private String contextId;
        private TaskState status;
        private Integer pageSize;
        private String pageToken;
        private Integer historyLength;
        private Instant lastUpdatedAfter;
        private Boolean includeArtifacts;
        private String tenant;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        public Builder status(TaskState status) {
            this.status = status;
            return this;
        }

        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder pageToken(String pageToken) {
            this.pageToken = pageToken;
            return this;
        }

        public Builder historyLength(Integer historyLength) {
            this.historyLength = historyLength;
            return this;
        }

        public Builder lastUpdatedAfter(Instant lastUpdatedAfter) {
            this.lastUpdatedAfter = lastUpdatedAfter;
            return this;
        }

        public Builder includeArtifacts(Boolean includeArtifacts) {
            this.includeArtifacts = includeArtifacts;
            return this;
        }

        public Builder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public ListTasksParams build() {
            return new ListTasksParams(contextId, status, pageSize, pageToken, historyLength,
                    lastUpdatedAfter, includeArtifacts, tenant);
        }
    }
}
