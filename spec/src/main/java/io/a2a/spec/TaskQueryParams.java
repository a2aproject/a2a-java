package io.a2a.spec;

import io.a2a.util.Assert;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Defines parameters for querying a task, with an option to limit history length.
 *
 * @param id the ID for the task to be queried
 * @param historyLength the maximum number of items of history for the task to include in the response
 * @param metadata additional properties
 */
public record TaskQueryParams(String id, int historyLength, @Nullable Map<String, Object> metadata) {

    public TaskQueryParams {
        Assert.checkNotNullParam("id", id);
        if (historyLength < 0) {
            throw new IllegalArgumentException("Invalid history length");
        }
    }

    public TaskQueryParams(String id) {
        this(id, 0, null);
    }

    public TaskQueryParams(String id, int historyLength) {
        this(id, historyLength, null);
    }
}
