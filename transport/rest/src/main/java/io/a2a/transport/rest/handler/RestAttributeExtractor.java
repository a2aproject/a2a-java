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

public class RestAttributeExtractor implements Supplier<Function<InvocationContext, Map<String, String>>> {

    @Override
    public Function<InvocationContext, Map<String, String>> get() {
        return ctx -> {
            String method = ctx.method().getName();
            Object[] parameters = ctx.parameters();

            switch (method) {
                case "sendMessage",
                     "sendStreamingMessage"-> {
                    ServerCallContext context = (ServerCallContext) parameters[1];
                    Map<String, String > result = new HashMap<>(Map.of("gen_ai.agent.a2a.request", (String) parameters[0]));
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "setTaskPushNotificationConfiguration" -> {
                    ServerCallContext context = (ServerCallContext) parameters[2];
                    Map<String, String > result = new HashMap<>(Map.of("gen_ai.agent.a2a.taskId", (String) parameters[0], "gen_ai.agent.a2a.request", (String) parameters[1]));
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "cancelTask",
                     "resubscribeTask",
                     "listTaskPushNotificationConfigurations" -> {
                    ServerCallContext context = (ServerCallContext) parameters[1];
                    Map<String, String > result = new HashMap<>(Map.of("gen_ai.agent.a2a.taskId", (String) parameters[0]));
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "getTask" -> {
                    ServerCallContext context = (ServerCallContext) parameters[2];
                    Map<String, String > result = new HashMap<>(Map.of("gen_ai.agent.a2a.taskId", (String) parameters[0], "gen_ai.agent.a2a.historyLength", "" + (int) parameters[1]));
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "getTaskPushNotificationConfiguration",
                     "deleteTaskPushNotificationConfiguration" -> {
                    ServerCallContext context = (ServerCallContext) parameters[2];
                    Map<String, String > result = new HashMap<>(Map.of("gen_ai.agent.a2a.taskId", (String) parameters[0], "gen_ai.agent.a2a.configId", (String) parameters[1]));
                    result.putAll(processServerCallContext(context));
                    return result;
                }
                case "listTasks" -> {
                    ServerCallContext context = (ServerCallContext) parameters[6];
                    Map<String, String > result = new HashMap<>(Map.of("gen_ai.agent.a2a.contextId", (String) parameters[0], "gen_ai.agent.a2a.status", (String) parameters[1]));
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
         return Map.of( "gen_ai.agent.a2a.extensions", context.getActivatedExtensions().stream().collect(Collectors.joining(",")), "gen_ai.agent.a2a.operation.name", (String) context.getState().get(METHOD_NAME_KEY));
    }
}
