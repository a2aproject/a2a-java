package io.a2a.spec;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.a2a.util.Assert;

/**
 * Parameters containing a task identifier for task-related operations.
 * <p>
 * This simple parameter record is used by operations that only need a task ID,
 * such as {@link CancelTaskRequest}, {@link TaskResubscriptionRequest}, and similar
 * task-specific requests. It optionally includes metadata for additional context.
 *
 * @param id the unique task identifier (required)
 * @param metadata optional arbitrary key-value metadata for the request
 * @see CancelTaskRequest for task cancellation
 * @see TaskResubscriptionRequest for task resubscription
 * @see <a href="https://a2a-protocol.org/latest/">A2A Protocol Specification</a>
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskIdParams(String id, Map<String, Object> metadata) {

    public TaskIdParams {
        Assert.checkNotNullParam("id", id);
    }

    public TaskIdParams(String id) {
        this(id, null);
    }
}
