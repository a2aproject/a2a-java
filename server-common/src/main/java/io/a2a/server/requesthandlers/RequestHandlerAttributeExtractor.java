package io.a2a.server.requesthandlers;

import io.a2a.server.ServerCallContext;
import io.a2a.server.interceptors.InvocationContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RequestHandlerAttributeExtractor implements Supplier<Function<InvocationContext, Map<String, String>>> {

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
                    if (parameters[0] == null || parameters[1] == null) {
                        return Collections.emptyMap();
                    }

                    ServerCallContext context = (ServerCallContext) parameters[1];
                    Map<String, String> attributes = new HashMap<>();
                    attributes.put("request", parameters[0].toString());
                    attributes.put("extensions", context.getActivatedExtensions().stream().collect(Collectors.joining(",")));

                    String a2aMethod = (String) context.getState().get("method");
                    if (a2aMethod != null) {
                        attributes.put("a2a.method", a2aMethod);
                    }
                    return attributes;
                }
                default -> {
                    LOGGER.warning("Unexpected method %s called.".formatted(method));
                    return Collections.emptyMap();
                }
            }
        };
    }
}
