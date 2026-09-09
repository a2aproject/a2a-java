package org.a2aproject.sdk.extras.opentelemetry;

public interface A2AObservabilityNames {
    
    String EXTRACT_REQUEST_SYS_PROPERTY = "org.a2aproject.sdk.server.extract.request";
    String EXTRACT_RESPONSE_SYS_PROPERTY = "org.a2aproject.sdk.server.extract.response";

    String ERROR_TYPE = "error.type";

    String GENAI_PREFIX = "gen_ai.agent.a2a";
    String GENAI_CONFIG_ID = GENAI_PREFIX + ".config_id";
    String GENAI_CONTEXT_ID = GENAI_PREFIX + ".context_id"; //gen_ai.conversation.id
    String GENAI_EXTENSIONS = GENAI_PREFIX + ".extensions";
    String GENAI_MESSAGE_ID = GENAI_PREFIX + ".message_id";
    String GENAI_OPERATION_NAME = GENAI_PREFIX + ".operation.name"; //gen_ai.agent.operation.name ?
    String GENAI_PARTS_NUMBER = GENAI_PREFIX + ".parts.number";
    String GENAI_PROTOCOL = GENAI_PREFIX + ".protocol";
    String GENAI_STATUS = GENAI_PREFIX + ".status";
    String GENAI_REQUEST = GENAI_PREFIX + ".request"; //gen_ai.input.messages ?
    String GENAI_RESPONSE = GENAI_PREFIX + ".response"; // gen_ai.output.messages ?
    String GENAI_ROLE = GENAI_PREFIX + ".role";
    String GENAI_TASK_ID = GENAI_PREFIX + ".task_id";

    // Push notification
    String PUSH_NOTIFICATION_EVENT_KIND = GENAI_PREFIX + ".push_notification.event_kind";

    // Metrics
    String GENAI_CLIENT_OPERATION_DURATION = "gen_ai.client.operation.duration";
    String GENAI_STREAMING_DURATION = GENAI_PREFIX + ".streaming.duration";
    String GENAI_SYSTEM = "gen_ai.system";
    String GENAI_SYSTEM_VALUE = "a2a";
}
