package org.a2aproject.sdk.compat03.spec;

import java.util.Map;

import org.a2aproject.sdk.util.Assert;

/**
 * Defines parameters containing a task ID, used for simple task operations.
 */
public record TaskIdParams_v0_3(String id, Map<String, Object> metadata) {

    public TaskIdParams_v0_3 {
        Assert.checkNotNullParam("id", id);
    }

    public TaskIdParams_v0_3(String id) {
        this(id, null);
    }
}
