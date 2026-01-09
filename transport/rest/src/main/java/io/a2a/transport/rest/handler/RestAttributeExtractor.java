package io.a2a.transport.rest.handler;

import static io.a2a.transport.rest.context.RestContextKeys.METHOD_NAME_KEY;

import io.a2a.server.ServerCallContext;
import io.a2a.server.interceptors.InvocationContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public class RestAttributeExtractor implements Supplier<Function<InvocationContext, Map<String, String>>> {

    @Override
    public Function<InvocationContext, Map<String, String>> get() {
        return ctx -> {
            String method = ctx.method().getName();
            Object[] parameters = ctx.parameters() == null ?  new Object[]{} : ctx.parameters();
            if( ctx.parameters() == null || ctx.parameters().length < 2) {
                throw new IllegalArgumentException("wrong parameters passed");
            }
            switch (method) {
                case "sendMessage",
                     "sendStreamingMessage"-> {
                    ServerCallContext context = (ServerCallContext) parameters[2];
                    Map<String, String> result = new HashMap<>();
                    putIfNotNull(result, "gen_ai.agent.a2a.request", (String) parameters[0]);
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "setTaskPushNotificationConfiguration" -> {
                    ServerCallContext context = (ServerCallContext) parameters[3];
                    Map<String, String> result = new HashMap<>();
                    putIfNotNull(result, "gen_ai.agent.a2a.taskId", (String) parameters[0]);
                    putIfNotNull(result, "gen_ai.agent.a2a.request", (String) parameters[1]);
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "cancelTask",
                     "subscribeToTask",
                     "listTaskPushNotificationConfigurations" -> {
                    ServerCallContext context = (ServerCallContext) parameters[2];
                    Map<String, String> result = new HashMap<>();
                    putIfNotNull(result, "gen_ai.agent.a2a.taskId", (String) parameters[0]);
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "getTask" -> {
                    ServerCallContext context = (ServerCallContext) parameters[3];
                    Map<String, String> result = new HashMap<>();
                    putIfNotNull(result, "gen_ai.agent.a2a.taskId", (String) parameters[0]);
                    putIfNotNull(result, "gen_ai.agent.a2a.historyLength", "" + (int) parameters[1]);
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "getTaskPushNotificationConfiguration",
                     "deleteTaskPushNotificationConfiguration" -> {
                    ServerCallContext context = (ServerCallContext) parameters[3];
                    Map<String, String> result = new HashMap<>();
                    putIfNotNull(result, "gen_ai.agent.a2a.taskId", (String) parameters[0]);
                    putIfNotNull(result, "gen_ai.agent.a2a.configId", (String) parameters[1]);
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "listTasks" -> {
                    ServerCallContext context = (ServerCallContext) parameters[7];
                    Map<String, String> result = new HashMap<>();
                    putIfNotNull(result, "gen_ai.agent.a2a.contextId", (String) parameters[0]);
                    putIfNotNull(result, "gen_ai.agent.a2a.status", (String) parameters[1]);
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                default -> {
                    return Collections.emptyMap();
                }
            }
        };
    }

    private Map<String, String> processServerCallContext(ServerCallContext context) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("gen_ai.agent.a2a.extensions", context.getActivatedExtensions().stream().collect(Collectors.joining(",")));

        String operationName = (String)context.getState().get(METHOD_NAME_KEY);
        if (operationName != null) {
            attributes.put("gen_ai.agent.a2a.operation.name", operationName);
        }

        return attributes;
    }

    private void putIfNotNull(Map<String, String> map, String key, @Nullable String value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
