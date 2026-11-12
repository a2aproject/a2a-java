package org.a2aproject.sdk.extras.opentelemetry;

public interface A2AObservabilityNames {

    // Protocol log events
    String A2A_EVENT_AUTHORIZATION = "a2a.authorization";
    String A2A_EVENT_NEGOTIATION = "a2a.negotiation";
    String A2A_EVENT_NOTIFICATION_DELIVERY = "a2a.notification.delivery";
    String A2A_EVENT_NOTIFICATION_DELIVERY_ATTEMPT = "a2a.notification.delivery.attempt";
    String A2A_EVENT_NOTIFICATION_SUBSCRIPTION = "a2a.notification.subscription";
    String A2A_EVENT_TASK_ARTIFACT = "a2a.task.artifact";
    String A2A_EVENT_TASK_EVENT = "a2a.task.event";
    String A2A_EVENT_TASK_REQUEST = "a2a.task.request";
    String A2A_EVENT_TASK_RESPONSE = "a2a.task.response";
    String A2A_EVENT_TASK_STATE_CHANGE = "a2a.task.state_change";
    String A2A_EVENT_TASK_STREAM_END = "a2a.task.stream.end";
    String A2A_EVENT_TASK_SUBSCRIPTION = "a2a.task.subscription";

    // Protocol log directions
    String A2A_LOG_DIRECTION_INBOUND = "inbound";
    String A2A_LOG_DIRECTION_OUTBOUND = "outbound";

    // Protocol log attributes
    String A2A_ARTIFACT_ID = "a2a.artifact.id";
    String A2A_CONTEXT_ID = "a2a.context.id";
    String A2A_EVENT_KIND = "a2a.event.kind";
    String A2A_EVENT_NAME = "a2a.event.name";
    String A2A_LOG_DIRECTION = "a2a.log.direction";
    String A2A_MESSAGE_ID = "a2a.message.id";
    String A2A_NOTIFICATION_ID = "a2a.notification.id";
    String A2A_OPERATION_NAME = "a2a.operation.name";
    String A2A_PAYLOAD_OMITTED = "a2a.payload.omitted";
    String A2A_PAYLOAD_PROJECTED_SIZE = "a2a.payload.projected_size";
    String A2A_PAYLOAD_TRUNCATED = "a2a.payload.truncated";
    String A2A_TASK_ID = "a2a.task.id";
    String A2A_TASK_STATE = "a2a.task.state";

    // Protocol log kinds
    String A2A_KIND_ALLOW = "allow";
    String A2A_KIND_CANCEL = "cancel";
    String A2A_KIND_COMPLETE = "complete";
    String A2A_KIND_CREATED = "created";
    String A2A_KIND_DELETED = "deleted";
    String A2A_KIND_DENY = "deny";
    String A2A_KIND_ERROR = "error";
    String A2A_KIND_FAILURE = "failure";
    String A2A_KIND_SUCCESS = "success";

    String EXTRACT_REQUEST_SYS_PROPERTY = "org.a2aproject.sdk.server.extract.request";
    String EXTRACT_RESPONSE_SYS_PROPERTY = "org.a2aproject.sdk.server.extract.response";

    String ERROR_TYPE = "error.type";

    /**
     * Fixed error marker recorded in histogram attributes on failure.
     *
     * <p>Intentionally low-cardinality: metrics use this constant to avoid unbounded label sets,
     * while span attributes carry the actual exception class name for debugging.
     */
    String ERROR_TYPE_VALUE = "error";

    String GENAI_PREFIX = "gen_ai.agent.a2a";
    String GENAI_CONFIG_ID = GENAI_PREFIX + ".config_id";
    String GENAI_CONTEXT_ID = GENAI_PREFIX + ".context_id"; // gen_ai.conversation.id
    String GENAI_EXTENSIONS = GENAI_PREFIX + ".extensions";
    String GENAI_MESSAGE_ID = GENAI_PREFIX + ".message_id";
    String GENAI_OPERATION_NAME = GENAI_PREFIX + ".operation.name"; // gen_ai.agent.operation.name ?
    String GENAI_PARTS_NUMBER = GENAI_PREFIX + ".parts.number";
    String GENAI_TRANSPORT = GENAI_PREFIX + ".transport";
    String GENAI_STATUS = GENAI_PREFIX + ".status";
    String GENAI_PROTOCOL_VERSION = GENAI_PREFIX + ".protocol.version";
    String GENAI_REQUEST = GENAI_PREFIX + ".request"; // gen_ai.input.messages ?
    String GENAI_RESPONSE = GENAI_PREFIX + ".response"; // gen_ai.output.messages ?
    String GENAI_ROLE = GENAI_PREFIX + ".role";
    String GENAI_TASK_ID = GENAI_PREFIX + ".task_id";

    // Streaming event attributes
    String GENAI_STREAMING_EVENT = GENAI_PREFIX + ".streaming-event";
    String GENAI_TASK_STATUS_STATE = GENAI_PREFIX + ".task.status.state";

    // Streaming span name suffixes
    String STREAMING_EVENT_SUFFIX = "-event";
    String STREAMING_END_SUFFIX = "-end";

    // Metrics
    String GENAI_CLIENT_OPERATION_DURATION = "gen_ai.client.operation.duration";
    String GENAI_STREAMING_DURATION = GENAI_PREFIX + ".streaming.duration";
    String GENAI_SYSTEM = "gen_ai.system";
    String GENAI_SYSTEM_VALUE = "a2a";

    // Push notification
    String PUSH_NOTIFICATION_EVENT_KIND = GENAI_PREFIX + ".push_notification.event_kind";
    String PUSH_NOTIFICATION_DELIVERY_FAILED = "delivery_failed";
}
