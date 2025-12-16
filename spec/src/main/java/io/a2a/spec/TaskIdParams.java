package io.a2a.spec;

import io.a2a.util.Assert;

/**
 * Parameters containing a task identifier for task-related operations.
 * <p>
 * This simple parameter record is used by operations that only need a task ID,
 * such as {@link CancelTaskRequest}, {@link SubscribeToTaskRequest}, and similar
 * task-specific requests. It optionally includes metadata for additional context.
 *
 * @param id the unique task identifier (required)
 * @param tenant optional tenant, provided as a path parameter.
 * @see CancelTaskRequest for task cancellation
 * @see SubscribeToTaskRequest for task resubscription
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
public record TaskIdParams(String id, String tenant) {

    /**
     * Compact constructor for validation.
     * Validates that required parameters are not null.
     */
    public TaskIdParams  {
        Assert.checkNotNullParam("id", id);
        Assert.checkNotNullParam("tenant", tenant);
    }

    /**
     * Convenience constructor with default tenant.
     *
     * @param id the task identifier (required)
     */
    public TaskIdParams(String id) {
        this(id, "");
    }
}
