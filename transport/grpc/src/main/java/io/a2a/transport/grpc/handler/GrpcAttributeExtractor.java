package io.a2a.transport.grpc.handler;


import io.a2a.server.interceptors.InvocationContext;
import io.a2a.transport.grpc.context.GrpcContextKeys;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Extracts OpenTelemetry trace attributes from gRPC invocation context.
 * <p>
 * This extractor pulls relevant information from gRPC method calls to populate
 * span attributes for distributed tracing.
 */
public class GrpcAttributeExtractor implements Supplier<Function<InvocationContext, Map<String, String>>> {
    private static final Logger LOGGER = Logger.getLogger(GrpcAttributeExtractor.class.getName());

    @Override
    public @NonNull Function<InvocationContext, Map<String, String>> get() {
        return ctx -> {
            if (ctx == null || ctx.method() == null) {
                return Collections.emptyMap();
            }

            String method = ctx.method().getName();
            if (method == null) {
                return Collections.emptyMap();
            }

            Object[] parameters = ctx.parameters();
            if (parameters == null || parameters.length == 0) {
                return Collections.emptyMap();
            }

            switch (method) {
                case "sendMessage",
                     "getTask",
                     "listTasks",
                     "cancelTask",
                     "createTaskPushNotificationConfig",
                     "getTaskPushNotificationConfig",
                     "listTaskPushNotificationConfig",
                     "sendStreamingMessage",
                     "subscribeToTask",
                     "deleteTaskPushNotificationConfig" -> {
                    return extractAttributes(parameters);
                }
                default -> {
                    LOGGER.warning("Unexpected method %s called.".formatted(method));
                    return Collections.emptyMap();
                }
            }
        };
    }

    /**
     * Extracts trace attributes from method parameters and gRPC context.
     *
     * @param parameters the method invocation parameters
     * @return map of attribute key-value pairs for tracing
     */
    private @NonNull Map<String, String> extractAttributes(Object @NonNull [] parameters) {
        Map<String, String> attributes = new HashMap<>();

        // Add request parameter if available
        if (parameters.length > 0 && parameters[0] != null) {
            attributes.put("gen_ai.agent.a2a.request", parameters[0].toString());
        }

        // Add gRPC extensions header if available
        String extensions = GrpcContextKeys.EXTENSIONS_HEADER_KEY.get();
        if (extensions != null) {
            attributes.put("extensions", extensions);
        }

        // Add gRPC operation name if available
        String operationName = GrpcContextKeys.GRPC_METHOD_NAME_KEY.get();
        if (operationName != null) {
            attributes.put("gen_ai.agent.operation.name", operationName);
        }
        return attributes;
    }
}
