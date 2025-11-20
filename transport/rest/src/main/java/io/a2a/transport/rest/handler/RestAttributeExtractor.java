package io.a2a.transport.rest.handler;

import static io.a2a.transport.rest.context.RestContextKeys.METHOD_NAME_KEY;

import io.a2a.server.ServerCallContext;
import io.a2a.server.interceptors.InvocationContext;
import java.util.Collections;
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
                    return Map.of("request", (String) parameters[0], "extensions", context.getActivatedExtensions().stream().collect(Collectors.joining(",")), "a2a.method", (String) context.getState().get(METHOD_NAME_KEY));
                }
                case "setTaskPushNotificationConfiguration" -> {
                    ServerCallContext context = (ServerCallContext) parameters[2];
                    return Map.of("taskId", (String) parameters[0], "request", (String) parameters[1], "extensions", context.getActivatedExtensions().stream().collect(Collectors.joining(",")), "a2a.method", (String) context.getState().get(METHOD_NAME_KEY));
                }
                case "cancelTask",
                     "resubscribeTask",
                     "listTaskPushNotificationConfigurations" -> {
                    ServerCallContext context = (ServerCallContext) parameters[1];
                    return Map.of("taskId", (String) parameters[0], "extensions", context.getActivatedExtensions().stream().collect(Collectors.joining(",")), "a2a.method", (String) context.getState().get(METHOD_NAME_KEY));
                }
                case "getTask" -> {
                    ServerCallContext context = (ServerCallContext) parameters[2];
                    return Map.of("taskId", (String) parameters[0], "historyLength", "" + (int) parameters[1], "extensions", context.getActivatedExtensions().stream().collect(Collectors.joining(",")), "a2a.method", (String) context.getState().get(METHOD_NAME_KEY));
                }
                case "getTaskPushNotificationConfiguration",
                     "deleteTaskPushNotificationConfiguration" -> {
                    ServerCallContext context = (ServerCallContext) parameters[2];
                    return Map.of("taskId", (String) parameters[0], "configId", (String) parameters[1], "extensions", context.getActivatedExtensions().stream().collect(Collectors.joining(",")), "a2a.method", (String) context.getState().get(METHOD_NAME_KEY));
                }
                case "listTasks" -> {
                    ServerCallContext context = (ServerCallContext) parameters[6];
                    return Map.of("contextId", (String) parameters[0], "status", (String) parameters[1], "extensions", context.getActivatedExtensions().stream().collect(Collectors.joining(",")), "a2a.method", (String) context.getState().get(METHOD_NAME_KEY));
                }
                default -> {
                    return Collections.emptyMap();
                }
            }
        };
    }
}
