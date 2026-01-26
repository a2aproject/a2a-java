package io.a2a.server.util.sse;

import io.a2a.grpc.utils.JSONRPCUtils;
import io.a2a.jsonrpc.common.wrappers.A2AErrorResponse;
import io.a2a.jsonrpc.common.wrappers.A2AResponse;
import io.a2a.jsonrpc.common.wrappers.CancelTaskResponse;
import io.a2a.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.GetExtendedAgentCardResponse;
import io.a2a.jsonrpc.common.wrappers.GetTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.GetTaskResponse;
import io.a2a.jsonrpc.common.wrappers.ListTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.ListTasksResponse;
import io.a2a.jsonrpc.common.wrappers.SendMessageResponse;
import io.a2a.jsonrpc.common.wrappers.SendStreamingMessageResponse;

/**
 * Framework-agnostic utility for formatting A2A responses as Server-Sent Events (SSE).
 * <p>
 * Provides static methods to serialize A2A responses to JSON and format them as SSE events.
 * This allows HTTP server frameworks (Vert.x, Jakarta/WildFly, etc.) to use their own
 * reactive libraries for publisher mapping while sharing the serialization logic.
 * <p>
 * <b>Example usage (Quarkus/Vert.x with Mutiny):</b>
 * <pre>{@code
 * Flow.Publisher<A2AResponse<?>> responses = handler.onMessageSendStream(request, context);
 * AtomicLong eventId = new AtomicLong(0);
 *
 * Multi<String> sseEvents = Multi.createFrom().publisher(responses)
 *     .map(response -> SseFormatter.formatResponseAsSSE(response, eventId.getAndIncrement()));
 *
 * sseEvents.subscribe().with(sseEvent -> httpResponse.write(Buffer.buffer(sseEvent)));
 * }</pre>
 * <p>
 * <b>Example usage (Jakarta/WildFly with custom reactive library):</b>
 * <pre>{@code
 * Flow.Publisher<String> jsonStrings = restHandler.getJsonPublisher();
 * AtomicLong eventId = new AtomicLong(0);
 *
 * Flow.Publisher<String> sseEvents = mapPublisher(jsonStrings,
 *     json -> SseFormatter.formatJsonAsSSE(json, eventId.getAndIncrement()));
 * }</pre>
 */
public class SseFormatter {

    private SseFormatter() {
        // Utility class - prevent instantiation
    }

    /**
     * Format an A2A response as an SSE event.
     * <p>
     * Serializes the response to JSON and formats as:
     * <pre>
     * data: {"jsonrpc":"2.0","result":{...},"id":123}
     * id: 0
     *
     * </pre>
     *
     * @param response the A2A response to format
     * @param eventId  the SSE event ID
     * @return SSE-formatted string (ready to write to HTTP response)
     */
    public static String formatResponseAsSSE(A2AResponse<?> response, long eventId) {
        String jsonData = serializeResponse(response);
        return "data: " + jsonData + "\nid: " + eventId + "\n\n";
    }

    /**
     * Format a pre-serialized JSON string as an SSE event.
     * <p>
     * Wraps the JSON in SSE format as:
     * <pre>
     * data: {"jsonrpc":"2.0","result":{...},"id":123}
     * id: 0
     *
     * </pre>
     * <p>
     * Use this when you already have JSON strings (e.g., from REST transport)
     * and just need to add SSE formatting.
     *
     * @param jsonString the JSON string to wrap
     * @param eventId    the SSE event ID
     * @return SSE-formatted string (ready to write to HTTP response)
     */
    public static String formatJsonAsSSE(String jsonString, long eventId) {
        return "data: " + jsonString + "\nid: " + eventId + "\n\n";
    }

    /**
     * Serialize an A2AResponse to JSON string.
     */
    private static String serializeResponse(A2AResponse<?> response) {
        // For error responses, use standard JSON-RPC error format
        if (response instanceof A2AErrorResponse error) {
            return JSONRPCUtils.toJsonRPCErrorResponse(error.getId(), error.getError());
        }
        if (response.getError() != null) {
            return JSONRPCUtils.toJsonRPCErrorResponse(response.getId(), response.getError());
        }

        // Convert domain response to protobuf message and serialize
        com.google.protobuf.MessageOrBuilder protoMessage = convertToProto(response);
        return JSONRPCUtils.toJsonRPCResultResponse(response.getId(), protoMessage);
    }

    /**
     * Convert A2AResponse to protobuf message for serialization.
     */
    private static com.google.protobuf.MessageOrBuilder convertToProto(A2AResponse<?> response) {
        if (response instanceof GetTaskResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.task(r.getResult());
        } else if (response instanceof CancelTaskResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.task(r.getResult());
        } else if (response instanceof SendMessageResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.taskOrMessage(r.getResult());
        } else if (response instanceof ListTasksResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.listTasksResult(r.getResult());
        } else if (response instanceof CreateTaskPushNotificationConfigResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.createTaskPushNotificationConfigResponse(r.getResult());
        } else if (response instanceof GetTaskPushNotificationConfigResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.getTaskPushNotificationConfigResponse(r.getResult());
        } else if (response instanceof ListTaskPushNotificationConfigResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.listTaskPushNotificationConfigResponse(r.getResult());
        } else if (response instanceof DeleteTaskPushNotificationConfigResponse) {
            // DeleteTaskPushNotificationConfig has no result body, just return empty message
            return com.google.protobuf.Empty.getDefaultInstance();
        } else if (response instanceof GetExtendedAgentCardResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.getExtendedCardResponse(r.getResult());
        } else if (response instanceof SendStreamingMessageResponse r) {
            return io.a2a.grpc.utils.ProtoUtils.ToProto.taskOrMessageStream(r.getResult());
        } else {
            throw new IllegalArgumentException("Unknown response type: " + response.getClass().getName());
        }
    }
}
