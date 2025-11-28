package io.a2a.transport.grpc.handler;

import io.a2a.server.interceptors.InvocationContext;
import io.grpc.Context;
import io.a2a.transport.grpc.context.GrpcContextKeys;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class GrpcAttributeExtractor implements Supplier<Function<InvocationContext, Map<String, String>>> {

    @Override
    public Function<InvocationContext, Map<String, String>> get() {
        return ctx -> {
            String method = ctx.method().getName();
            Object[] parameters = ctx.parameters();

            switch (method) {
                case "sendMessage",
                     "getTask",
                     "listTasks",
                     "cancelTask",
                     "createTaskPushNotificationConfig",
                     "getTaskPushNotificationConfig",
                     "listTaskPushNotificationConfig",
                     "sendStreamingMessage",
                     "taskSubscription",
                     "deleteTaskPushNotificationConfig" -> {
                    Context currentContext = Context.current();
                    return Map.of("gen_ai.agent.a2a.request", parameters[0].toString(), "extensions", GrpcContextKeys.EXTENSIONS_HEADER_KEY.get(), "gen_ai.agent.operation.name", GrpcContextKeys.GRPC_METHOD_NAME_KEY.get(currentContext));
                }
                default -> {
                    return Collections.emptyMap();
                }
            }
        };
    }
}
