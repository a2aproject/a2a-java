package io.a2a.transport.jsonrpc.handler;

import static io.a2a.transport.jsonrpc.context.JSONRPCContextKeys.METHOD_NAME_KEY;

import io.a2a.server.ServerCallContext;
import io.a2a.server.interceptors.InvocationContext;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JsonRPCAttributeExtractor implements Supplier<Function<InvocationContext, Map<String, String>>> {

    @Override
    public Function<InvocationContext, Map<String, String>> get() {
        return ctx -> {
            String method = ctx.method().getName();
            Object[] parameters = ctx.parameters();

            switch (method) {
                case "onMessageSend",
                     "onMessageSendStream",
                     "onCancelTask",
                     "onResubscribeToTask",
                     "getPushNotificationConfig",
                     "setPushNotificationConfig",
                     "onGetTask",
                     "listPushNotificationConfig",
                     "deletePushNotificationConfig",
                     "onListTasks" -> {
                    ServerCallContext context = (ServerCallContext) parameters[1];
                    return Map.of("gen_ai.agent.a2a.request", parameters[0].toString(), "gen_ai.agent.a2a.extensions", context.getActivatedExtensions().stream().collect(Collectors.joining(",")), "gen_ai.agent.operation.name", (String) context.getState().get(METHOD_NAME_KEY));
                }
                default -> {
                    return Collections.emptyMap();
                }
            }
        };
    }
}
