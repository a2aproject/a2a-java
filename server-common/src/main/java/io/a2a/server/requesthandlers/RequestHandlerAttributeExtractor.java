package io.a2a.server.requesthandlers;

import io.a2a.server.ServerCallContext;
import io.a2a.server.interceptors.InvocationContext;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RequestHandlerAttributeExtractor implements Supplier<Function<InvocationContext, Map<String, String>>> {

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
                    return Map.of("request", parameters[0].toString(), "extensions", context.getActivatedExtensions().stream().collect(Collectors.joining(",")), "a2a.method", (String) context.getState().get("method"));
                }
                default -> {
                    return Collections.emptyMap();
                }
            }
        };
    }
}
