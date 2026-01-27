package io.a2a.server.requesthandlers;

import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_CONTEXT_ID;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_EXTENSIONS;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_MESSAGE_ID;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_OPERATION_NAME;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_PARTS_NUMBER;

import io.a2a.server.ServerCallContext;
import io.a2a.server.interceptors.AttributeExtractor;
import io.a2a.server.interceptors.InvocationContext;
import io.a2a.spec.Message;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_REQUEST;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_ROLE;

import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_TASK_ID;

import io.a2a.spec.MessageSendParams;

public class RequestHandlerAttributeExtractor implements AttributeExtractor {

    private static final Logger LOGGER = Logger.getLogger(RequestHandlerAttributeExtractor.class.getName());

    @Override
    @SuppressWarnings("NullAway") // Null checks performed inline
    public Function<InvocationContext, Map<String, String>> get() {
        return ctx -> {
            if (ctx == null || ctx.method() == null) {
                return Collections.emptyMap();
            }

            String method = ctx.method().getName();
            if (method == null) {
                return Collections.emptyMap();
            }

            Object[] parameters = ctx.parameters();
            if (parameters == null || parameters.length < 2) {
                return Collections.emptyMap();
            }

            switch (method) {
                case "onMessageSend", "onMessageSendStream" -> {
                    Map<String, String> attributes = processRequest(parameters);
                    if (parameters[0] != null && parameters[0] instanceof MessageSendParams messageSendParams) {
                        processMessage(attributes, messageSendParams.message());
                    }
                    return attributes;
                }
                case "onCancelTask",
                     "onResubscribeToTask",
                     "getPushNotificationConfig",
                     "setPushNotificationConfig",
                     "onGetTask",
                     "listPushNotificationConfig",
                     "deletePushNotificationConfig",
                     "onListTasks" -> {
                    Map<String, String> attributes = processRequest(parameters);
                        return attributes;
                    }
                default -> {
                    LOGGER.warning("Unexpected method %s called.".formatted(method));
                    return Collections.emptyMap();
                }
            }
        };
    }

    private Map<String, String> processRequest(Object[] parameters) {
        if (parameters[0] == null || parameters[1] == null) {
            return Collections.emptyMap();
        }
        ServerCallContext context = (ServerCallContext) parameters[1];
        Map<String, String> attributes = new HashMap<>();
        if (extractRequest()) {
            attributes.put(GENAI_REQUEST, parameters[0].toString());
        }
        attributes.put(GENAI_EXTENSIONS, context.getActivatedExtensions().stream().collect(Collectors.joining(",")));

        String a2aMethod = (String) context.getState().get("method");
        if (a2aMethod != null) {
            attributes.put(GENAI_OPERATION_NAME, a2aMethod);
        }
        return attributes;
    }

    private void processMessage(Map<String, String> attributes, Message message) {
        attributes.put(GENAI_MESSAGE_ID, message.messageId());
        attributes.put(GENAI_TASK_ID, message.taskId());
        attributes.put(GENAI_CONTEXT_ID, message.contextId());
        attributes.put(GENAI_PARTS_NUMBER, "" + message.parts().size());
        attributes.put(GENAI_ROLE, message.role().asString());
    }
}
