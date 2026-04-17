package io.a2a.spec;

import java.util.Map;

import io.a2a.util.Assert;

/**
 * Defines parameters containing a task ID, used for simple task operations.
 *
 * @param id the task ID
 * @param metadata optional additional metadata
 */
public record TaskIdParams(String id, Map<String, Object> metadata) {

    public TaskIdParams {
        Assert.checkNotNullParam("id", id);
    }

    public TaskIdParams(String id) {
        this(id, null);
    }
}
