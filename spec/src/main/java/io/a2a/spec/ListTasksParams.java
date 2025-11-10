package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

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
 * @param metadata Additional filter properties
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ListTasksParams(
        @Nullable String contextId,
        @Nullable TaskState status,
        @Nullable Integer pageSize,
        @Nullable String pageToken,
        @Nullable Integer historyLength,
        @Nullable Instant lastUpdatedAfter,
        @Nullable Boolean includeArtifacts,
        @Nullable Map<String, Object> metadata
) {
    /**
     * Default constructor for listing all tasks.
     */
    public ListTasksParams() {
        this(null, null, null, null, null, null, null, null);
    }

    /**
     * Constructor with pagination.
     *
     * @param pageSize Maximum number of tasks to return
     * @param pageToken Token for pagination
     */
    public ListTasksParams(Integer pageSize, String pageToken) {
        this(null, null, pageSize, pageToken, null, null, null, null);
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

    public static class Builder {
        private String contextId;
        private TaskState status;
        private Integer pageSize;
        private String pageToken;
        private Integer historyLength;
        private Instant lastUpdatedAfter;
        private Boolean includeArtifacts;
        private Map<String, Object> metadata;

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

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ListTasksParams build() {
            return new ListTasksParams(contextId, status, pageSize, pageToken, historyLength,
                    lastUpdatedAfter, includeArtifacts, metadata);
        }
    }
}
