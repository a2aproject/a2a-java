package io.a2a.transport.rest.handler;

import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_CONFIG_ID;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_CONTEXT_ID;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_EXTENSIONS;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_OPERATION_NAME;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_PROTOCOL;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_REQUEST;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_STATUS;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_TASK_ID;
import static io.a2a.transport.rest.context.RestContextKeys.METHOD_NAME_KEY;

import io.a2a.server.ServerCallContext;
import io.a2a.server.interceptors.AttributeExtractor;
import io.a2a.server.interceptors.InvocationContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public class RestAttributeExtractor implements AttributeExtractor {
    private static final Logger LOGGER = Logger.getLogger(RestAttributeExtractor.class.getName());

    @Override
    public Function<InvocationContext, Map<String, String>> get() {
        return ctx -> {
            String method = ctx.method().getName();
            Object[] parameters = ctx.parameters() == null ?  new Object[]{} : ctx.parameters();
            if( ctx.parameters() == null || ctx.parameters().length < 2) {
                throw new IllegalArgumentException("wrong parameters passed");
            }
            switch (method) {
                case "getExtendedAgentCard" -> {
                    ServerCallContext context = (ServerCallContext) parameters[1];
                    Map<String, String> result = new HashMap<>();
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "sendMessage",
                     "sendStreamingMessage"-> {
                    ServerCallContext context = (ServerCallContext) parameters[2];
                    Map<String, String> result = new HashMap<>();
                    if(extractRequest()) {
                        putIfNotNull(result, GENAI_REQUEST, (String) parameters[0]);
                    }
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "setTaskPushNotificationConfiguration" -> {
                    ServerCallContext context = (ServerCallContext) parameters[3];
                    Map<String, String> result = new HashMap<>();
                    putIfNotNull(result, GENAI_TASK_ID, (String) parameters[0]);
                    if(extractRequest()) {
                        putIfNotNull(result, GENAI_REQUEST, (String) parameters[1]);
                    }
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "cancelTask",
                     "subscribeToTask",
                     "listTaskPushNotificationConfigurations" -> {
                    ServerCallContext context = (ServerCallContext) parameters[2];
                    Map<String, String> result = new HashMap<>();
                    putIfNotNull(result, GENAI_TASK_ID, (String) parameters[0]);
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "getTask" -> {
                    ServerCallContext context = (ServerCallContext) parameters[3];
                    Map<String, String> result = new HashMap<>();
                    putIfNotNull(result, GENAI_TASK_ID, (String) parameters[0]);
                    putIfNotNull(result, "gen_ai.agent.a2a.historyLength", "" + (int) parameters[1]);
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "getTaskPushNotificationConfiguration",
                     "deleteTaskPushNotificationConfiguration" -> {
                    ServerCallContext context = (ServerCallContext) parameters[3];
                    Map<String, String> result = new HashMap<>();
                    putIfNotNull(result, GENAI_TASK_ID, (String) parameters[0]);
                    putIfNotNull(result, GENAI_CONFIG_ID, (String) parameters[1]);
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "listTasks" -> {
                    ServerCallContext context = (ServerCallContext) parameters[7];
                    Map<String, String> result = new HashMap<>();
                    putIfNotNull(result,GENAI_CONTEXT_ID, (String) parameters[0]);
                    putIfNotNull(result, GENAI_STATUS, (String) parameters[1]);
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                default -> {
                    LOGGER.warning("Unexpected method %s called.".formatted(method));
                    return Collections.emptyMap();
                }
            }
        };
    }

    private Map<String, String> processServerCallContext(ServerCallContext context) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(GENAI_EXTENSIONS, context.getActivatedExtensions().stream().collect(Collectors.joining(",")));
        attributes.put(GENAI_PROTOCOL, "HTTP+JSON");
        String operationName = (String)context.getState().get(METHOD_NAME_KEY);
        if (operationName != null) {
            attributes.put(GENAI_OPERATION_NAME, operationName);
        }

        return attributes;
    }

    private void putIfNotNull(Map<String, String> map, String key, @Nullable String value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
