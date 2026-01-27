package io.a2a.transport.jsonrpc.handler;

import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_EXTENSIONS;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_OPERATION_NAME;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_PROTOCOL;
import static io.a2a.server.interceptors.A2AObservabilityNames.GENAI_REQUEST;
import static io.a2a.transport.jsonrpc.context.JSONRPCContextKeys.METHOD_NAME_KEY;

import io.a2a.server.ServerCallContext;
import io.a2a.server.interceptors.AttributeExtractor;
import io.a2a.server.interceptors.InvocationContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class JSONRPCAttributeExtractor implements AttributeExtractor {
    private static final Logger LOGGER = Logger.getLogger(JSONRPCAttributeExtractor.class.getName());

    @Override
    public Function<InvocationContext, Map<String, String>> get() {
        return ctx -> {
            String method = ctx.method().getName();
            Object[] parameters = ctx.parameters() == null ?  new Object[]{} : ctx.parameters();
            if( ctx.parameters() == null || ctx.parameters().length < 2) {
                throw new IllegalArgumentException("wrong parameters passed");
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
                     "onListTasks",
                     "onGetExtendedCardRequest" -> {
                    ServerCallContext context = (ServerCallContext) parameters[1];
                    Map<String, String> attributes = new HashMap<>();
                    if(extractRequest() && parameters[0] != null) {
                        attributes.put(GENAI_REQUEST, parameters[0].toString());
                    }
                    attributes.put(GENAI_PROTOCOL, "JSONRPC");
                    attributes.put(GENAI_EXTENSIONS, context.getActivatedExtensions().stream().collect(Collectors.joining(",")));

                    String operationName = (String) context.getState().get(METHOD_NAME_KEY);
                    if (operationName != null) {
                        attributes.put(GENAI_OPERATION_NAME, operationName);
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
