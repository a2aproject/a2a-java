package org.a2aproject.sdk.compat03.spec;

import org.a2aproject.sdk.util.Assert;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Defines parameters for querying a task, with an option to limit history length.
 *
 * @param id the ID for the task to be queried
 * @param historyLength the maximum number of items of history for the task to include in the response
 * @param metadata additional properties
 */
public record TaskQueryParams_v0_3(String id, int historyLength, @Nullable Map<String, Object> metadata) {

    public TaskQueryParams_v0_3 {
        Assert.checkNotNullParam("id", id);
        if (historyLength < 0) {
            throw new IllegalArgumentException("Invalid history length");
        }
    }

    public TaskQueryParams_v0_3(String id) {
        this(id, 0, null);
    }

    public TaskQueryParams_v0_3(String id, int historyLength) {
        this(id, historyLength, null);
    }
}
