package io.a2a.spec;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Result of a list tasks request containing matching tasks and pagination information.
 *
 * @param tasks Array of tasks matching the specified criteria
 * @param totalSize Total number of tasks available (before pagination)
 * @param pageSize Number of tasks returned in this response
 * @param nextPageToken Token for retrieving the next page of results (null if no more results)
 */
public record ListTasksResult(
        List<Task> tasks,
        int totalSize,
        int pageSize,
        @Nullable String nextPageToken
) {
    /**
     * Compact constructor for validation.
     * Validates parameters and creates a defensive copy of the tasks list.
     *
     * @param tasks the list of tasks
     * @param totalSize total number of tasks available
     * @param pageSize number of tasks in this page
     * @param nextPageToken token for next page
     * @throws IllegalArgumentException if validation fails
     */
    public ListTasksResult {
        Assert.checkNotNullParam("tasks", tasks);
        if (totalSize < 0) {
            throw new IllegalArgumentException("totalSize must be non-negative");
        }
        if (pageSize < 0) {
            throw new IllegalArgumentException("pageSize must be non-negative");
        }
        if (pageSize != tasks.size()) {
            throw new IllegalArgumentException("pageSize must be equal to the number of tasks in the list. Got " + pageSize + ", but list has " + tasks.size() + " tasks.");
        }
        // Make defensive copy
        tasks = List.copyOf(tasks);
    }

    /**
     * Constructor for results without pagination.
     *
     * @param tasks the list of tasks
     */
    public ListTasksResult(List<Task> tasks) {
        this(tasks, tasks.size(), tasks.size(), null);
    }

    /**
     * Returns whether there are more results available.
     *
     * @return true if there are more pages of results
     */
    public boolean hasMoreResults() {
        return nextPageToken != null && !nextPageToken.isEmpty();
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
        private List<Task> tasks;
        private int totalSize;
        private int pageSize;
        private String nextPageToken;

        /**
         * Creates a new Builder with all fields unset.
         */
        private Builder() {
        }

        /**
         * Sets the tasks list.
         *
         * @param tasks the list of tasks
         * @return this builder for method chaining
         */
        public Builder tasks(List<Task> tasks) {
            this.tasks = tasks;
            return this;
        }

        /**
         * Sets the totalSize.
         *
         * @param totalSize the totalSize
         * @return this builder for method chaining
         */
        public Builder totalSize(int totalSize) {
            this.totalSize = totalSize;
            return this;
        }

        /**
         * Sets the pageSize.
         *
         * @param pageSize the pageSize
         * @return this builder for method chaining
         */
        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Sets the nextPageToken.
         *
         * @param nextPageToken the nextPageToken
         * @return this builder for method chaining
         */
        public Builder nextPageToken(String nextPageToken) {
            this.nextPageToken = nextPageToken;
            return this;
        }

        /**
         * Builds the ListTasksResult.
         *
         * @return a new ListTasksResult instance
         * @throws IllegalArgumentException if validation fails
         */
        public ListTasksResult build() {
            return new ListTasksResult(tasks, totalSize, pageSize, nextPageToken);
        }
    }
}
