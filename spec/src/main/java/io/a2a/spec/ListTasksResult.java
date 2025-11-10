package io.a2a.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ListTasksResult(
        List<Task> tasks,
        int totalSize,
        int pageSize,
        @Nullable String nextPageToken
) {
    public ListTasksResult {
        Assert.checkNotNullParam("tasks", tasks);
        if (totalSize < 0) {
            throw new IllegalArgumentException("totalSize must be non-negative");
        }
        if (pageSize < 0) {
            throw new IllegalArgumentException("pageSize must be non-negative");
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

    public static class Builder {
        private List<Task> tasks;
        private int totalSize;
        private int pageSize;
        private String nextPageToken;

        public Builder tasks(List<Task> tasks) {
            this.tasks = tasks;
            return this;
        }

        public Builder totalSize(int totalSize) {
            this.totalSize = totalSize;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder nextPageToken(String nextPageToken) {
            this.nextPageToken = nextPageToken;
            return this;
        }

        public ListTasksResult build() {
            return new ListTasksResult(tasks, totalSize, pageSize, nextPageToken);
        }
    }
}
