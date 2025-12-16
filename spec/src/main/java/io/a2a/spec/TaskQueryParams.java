package io.a2a.spec;

import io.a2a.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Defines parameters for querying a task, with an option to limit history length.
 *
 * @param id the ID for the task to be queried
 * @param historyLength the maximum number of items of history for the task to include in the response
 * @param tenant optional tenant, provided as a path parameter.
 */
public record TaskQueryParams(String id, @Nullable Integer historyLength, String tenant) {

    public TaskQueryParams {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("tenant", tenant);
        if (historyLength != null && historyLength < 0) {
            throw new IllegalArgumentException("Invalid history length");
        }
    }
    public TaskQueryParams(String id, Integer historyLength) {
        this(id, historyLength, "");
    }

    public TaskQueryParams(String id) {
        this(id, 0, "");
    }
}
