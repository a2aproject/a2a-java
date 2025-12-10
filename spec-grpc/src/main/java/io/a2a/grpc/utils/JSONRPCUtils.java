package io.a2a.grpc.utils;


import io.a2a.json.JsonMappingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonWriter;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.a2a.grpc.StreamResponse;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.ContentTypeNotSupportedError;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigResponse;
import io.a2a.spec.GetAuthenticatedExtendedCardRequest;
import io.a2a.spec.GetAuthenticatedExtendedCardResponse;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.IdJsonMappingException;
import io.a2a.spec.InvalidAgentResponseError;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidParamsJsonMappingException;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCMessage;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.ListTaskPushNotificationConfigResponse;
import io.a2a.spec.ListTasksRequest;
import io.a2a.spec.ListTasksResponse;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.MethodNotFoundJsonMappingException;
import io.a2a.spec.PushNotificationNotSupportedError;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.SubscribeToTaskRequest;
import io.a2a.spec.TaskNotCancelableError;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.UnsupportedOperationError;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

import static io.a2a.spec.A2AErrorCodes.CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.INTERNAL_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.INVALID_AGENT_RESPONSE_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.INVALID_PARAMS_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.INVALID_REQUEST_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.JSON_PARSE_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.METHOD_NOT_FOUND_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.PUSH_NOTIFICATION_NOT_SUPPORTED_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.TASK_NOT_CANCELABLE_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.TASK_NOT_FOUND_ERROR_CODE;
import static io.a2a.spec.A2AErrorCodes.UNSUPPORTED_OPERATION_ERROR_CODE;


/**
 * Utilities for converting between JSON-RPC 2.0 messages and Protocol Buffer objects.
 * <p>
 * This class provides a unified strategy for handling JSON-RPC requests and responses in the A2A SDK
 * by bridging the JSON-RPC transport layer with Protocol Buffer-based internal representations.
 *
 * <h2>Conversion Strategy</h2>
 * The conversion process follows a two-step approach:
 * <ol>
 *   <li><b>JSON → Proto:</b> JSON-RPC messages are parsed using Gson, then converted to Protocol Buffer
 *       objects using Google's {@link JsonFormat} parser. This ensures consistent handling of field names,
 *       types, and nested structures according to the proto3 specification.</li>
 *   <li><b>Proto → Spec:</b> Protocol Buffer objects are converted to A2A spec objects using
 *       {@link ProtoUtils.FromProto} converters, which handle type mappings and create immutable
 *       spec-compliant Java objects.</li>
 * </ol>
 *
 * <h2>Request Processing Flow</h2>
 * <pre>
 * Incoming JSON-RPC Request
 *   ↓ parseRequestBody(String)
 * Validate version, id, method
 *   ↓ parseMethodRequest()
 * Parse params → Proto Builder
 *   ↓ ProtoUtils.FromProto.*
 * Create JSONRPCRequest&lt;?&gt; with spec objects
 * </pre>
 *
 * <h2>Response Processing Flow</h2>
 * <pre>
 * Incoming JSON-RPC Response
 *   ↓ parseResponseBody(String, String)
 * Validate version, id, check for errors
 *   ↓ Parse result/error
 * Proto Builder → spec objects
 *   ↓ ProtoUtils.FromProto.*
 * Create JSONRPCResponse&lt;?&gt; with result or error
 * </pre>
 *
 * <h2>Serialization Flow</h2>
 * <pre>
 * Proto MessageOrBuilder
 *   ↓ JsonFormat.printer()
 * Proto JSON string
 *   ↓ Gson JsonWriter
 * Complete JSON-RPC envelope
 * </pre>
 *
 * <h2>Error Handling</h2>
 * The class provides detailed error messages for common failure scenarios:
 * <ul>
 *   <li><b>Missing/invalid method:</b> Returns {@link MethodNotFoundError} with the invalid method name</li>
 *   <li><b>Invalid parameters:</b> Returns {@link InvalidParamsError} with proto parsing details</li>
 *   <li><b>Protocol version mismatch:</b> Returns {@link InvalidRequestError} with version info</li>
 *   <li><b>Missing/invalid id:</b> Returns {@link InvalidRequestError} with id validation details</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * This class is thread-safe. All methods are stateless and use immutable shared resources
 * ({@link Gson} instance is thread-safe, proto builders are created per-invocation).
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Parse incoming JSON-RPC request
 * String jsonRequest = """
 *     {"jsonrpc":"2.0","id":1,"method":"tasks.get","params":{"name":"tasks/task-123"}}
 *     """;
 * JSONRPCRequest<?> request = JSONRPCUtils.parseRequestBody(jsonRequest);
 *
 * // Create JSON-RPC request from proto
 * io.a2a.grpc.GetTaskRequest protoRequest = ...;
 * String json = JSONRPCUtils.toJsonRPCRequest("req-1", "tasks.get", protoRequest);
 *
 * // Create JSON-RPC response from proto
 * io.a2a.grpc.Task protoTask = ...;
 * String response = JSONRPCUtils.toJsonRPCResultResponse("req-1", protoTask);
 * }</pre>
 *
 * @see ProtoUtils
 * @see JSONRPCRequest
 * @see JSONRPCResponse
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 */
public class JSONRPCUtils {

    private static final Logger log = Logger.getLogger(JSONRPCUtils.class.getName());
    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .create();

    public static JSONRPCRequest<?> parseRequestBody(String body) throws JsonMappingException {
        JsonElement jelement = JsonParser.parseString(body);
        JsonObject jsonRpc = jelement.getAsJsonObject();
        if (!jsonRpc.has("method")) {
            throw new IdJsonMappingException(
                    "JSON-RPC request missing required 'method' field. Request must include: jsonrpc, id, method, and params.",
                    getIdIfPossible(jsonRpc));
        }
        String version = getAndValidateJsonrpc(jsonRpc);
        Object id = getAndValidateId(jsonRpc);
        String method = jsonRpc.get("method").getAsString();
        JsonElement paramsNode = jsonRpc.get("params");

        try {
            return parseMethodRequest(version, id, method, paramsNode);
        } catch (InvalidParamsError e) {
            throw new InvalidParamsJsonMappingException(e.getMessage(), id);
        }
    }

    private static JSONRPCRequest<?> parseMethodRequest(String version, Object id, String method, JsonElement paramsNode) throws InvalidParamsError, MethodNotFoundJsonMappingException {
        switch (method) {
            case GetTaskRequest.METHOD -> {
                io.a2a.grpc.GetTaskRequest.Builder builder = io.a2a.grpc.GetTaskRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new GetTaskRequest(version, id, ProtoUtils.FromProto.taskQueryParams(builder));
            }
            case CancelTaskRequest.METHOD -> {
                io.a2a.grpc.CancelTaskRequest.Builder builder = io.a2a.grpc.CancelTaskRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new CancelTaskRequest(version, id, ProtoUtils.FromProto.taskIdParams(builder));
            }
            case ListTasksRequest.METHOD -> {
                io.a2a.grpc.ListTasksRequest.Builder builder = io.a2a.grpc.ListTasksRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new ListTasksRequest(version, id, ProtoUtils.FromProto.listTasksParams(builder));
            }
            case SetTaskPushNotificationConfigRequest.METHOD -> {
                io.a2a.grpc.SetTaskPushNotificationConfigRequest.Builder builder = io.a2a.grpc.SetTaskPushNotificationConfigRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new SetTaskPushNotificationConfigRequest(version, id, ProtoUtils.FromProto.setTaskPushNotificationConfig(builder));
            }
            case GetTaskPushNotificationConfigRequest.METHOD -> {
                io.a2a.grpc.GetTaskPushNotificationConfigRequest.Builder builder = io.a2a.grpc.GetTaskPushNotificationConfigRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new GetTaskPushNotificationConfigRequest(version, id, ProtoUtils.FromProto.getTaskPushNotificationConfigParams(builder));
            }
            case SendMessageRequest.METHOD -> {
                io.a2a.grpc.SendMessageRequest.Builder builder = io.a2a.grpc.SendMessageRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new SendMessageRequest(version, id, ProtoUtils.FromProto.messageSendParams(builder));
            }
            case ListTaskPushNotificationConfigRequest.METHOD -> {
                io.a2a.grpc.ListTaskPushNotificationConfigRequest.Builder builder = io.a2a.grpc.ListTaskPushNotificationConfigRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new ListTaskPushNotificationConfigRequest(version, id, ProtoUtils.FromProto.listTaskPushNotificationConfigParams(builder));
            }
            case DeleteTaskPushNotificationConfigRequest.METHOD -> {
                io.a2a.grpc.DeleteTaskPushNotificationConfigRequest.Builder builder = io.a2a.grpc.DeleteTaskPushNotificationConfigRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new DeleteTaskPushNotificationConfigRequest(version, id, ProtoUtils.FromProto.deleteTaskPushNotificationConfigParams(builder));
            }
            case GetAuthenticatedExtendedCardRequest.METHOD -> {
                return new GetAuthenticatedExtendedCardRequest(version, id);
            }
            case SendStreamingMessageRequest.METHOD -> {
                io.a2a.grpc.SendMessageRequest.Builder builder = io.a2a.grpc.SendMessageRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new SendStreamingMessageRequest(version, id, ProtoUtils.FromProto.messageSendParams(builder));
            }
            case SubscribeToTaskRequest.METHOD -> {
                io.a2a.grpc.SubscribeToTaskRequest.Builder builder = io.a2a.grpc.SubscribeToTaskRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new SubscribeToTaskRequest(version, id, ProtoUtils.FromProto.taskIdParams(builder));
            }
            default ->
                throw new MethodNotFoundJsonMappingException("Unsupported JSON-RPC method: '" + method + "'", id);
        }
    }

    public static StreamResponse parseResponseEvent(String body) throws JsonMappingException {
        JsonElement jelement = JsonParser.parseString(body);
        JsonObject jsonRpc = jelement.getAsJsonObject();
        String version = getAndValidateJsonrpc(jsonRpc);
        Object id = getAndValidateId(jsonRpc);
        JsonElement paramsNode = jsonRpc.get("result");
        if (jsonRpc.has("error")) {
            throw processError(jsonRpc.getAsJsonObject("error"));
        }
        StreamResponse.Builder builder = StreamResponse.newBuilder();
        parseRequestBody(paramsNode, builder);
        return builder.build();
    }

    public static JSONRPCResponse<?> parseResponseBody(String body, String method) throws JsonMappingException {
        JsonElement jelement = JsonParser.parseString(body);
        JsonObject jsonRpc = jelement.getAsJsonObject();
        String version = getAndValidateJsonrpc(jsonRpc);
        Object id = getAndValidateId(jsonRpc);
        JsonElement paramsNode = jsonRpc.get("result");
        if (jsonRpc.has("error")) {
            return parseError(jsonRpc.getAsJsonObject("error"), id, method);
        }
        switch (method) {
            case GetTaskRequest.METHOD -> {
                io.a2a.grpc.Task.Builder builder = io.a2a.grpc.Task.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new GetTaskResponse(id, ProtoUtils.FromProto.task(builder));
            }
            case CancelTaskRequest.METHOD -> {
                io.a2a.grpc.Task.Builder builder = io.a2a.grpc.Task.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new CancelTaskResponse(id, ProtoUtils.FromProto.task(builder));
            }
            case ListTasksRequest.METHOD -> {
                io.a2a.grpc.ListTasksResponse.Builder builder = io.a2a.grpc.ListTasksResponse.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new ListTasksResponse(id, ProtoUtils.FromProto.listTasksResult(builder));
            }
            case SetTaskPushNotificationConfigRequest.METHOD -> {
                io.a2a.grpc.TaskPushNotificationConfig.Builder builder = io.a2a.grpc.TaskPushNotificationConfig.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new SetTaskPushNotificationConfigResponse(id, ProtoUtils.FromProto.taskPushNotificationConfig(builder));
            }
            case GetTaskPushNotificationConfigRequest.METHOD -> {
                io.a2a.grpc.TaskPushNotificationConfig.Builder builder = io.a2a.grpc.TaskPushNotificationConfig.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new GetTaskPushNotificationConfigResponse(id, ProtoUtils.FromProto.taskPushNotificationConfig(builder));
            }
            case SendMessageRequest.METHOD -> {
                io.a2a.grpc.SendMessageResponse.Builder builder = io.a2a.grpc.SendMessageResponse.newBuilder();
                parseRequestBody(paramsNode, builder);
                if (builder.hasMsg()) {
                    return new SendMessageResponse(id, ProtoUtils.FromProto.message(builder.getMsg()));
                }
                return new SendMessageResponse(id, ProtoUtils.FromProto.task(builder.getTask()));
            }
            case ListTaskPushNotificationConfigRequest.METHOD -> {
                io.a2a.grpc.ListTaskPushNotificationConfigResponse.Builder builder = io.a2a.grpc.ListTaskPushNotificationConfigResponse.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new ListTaskPushNotificationConfigResponse(id, ProtoUtils.FromProto.listTaskPushNotificationConfigParams(builder));
            }
            case DeleteTaskPushNotificationConfigRequest.METHOD -> {
                return new DeleteTaskPushNotificationConfigResponse(id);
            }
            case GetAuthenticatedExtendedCardRequest.METHOD -> {
                io.a2a.grpc.AgentCard.Builder builder = io.a2a.grpc.AgentCard.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new GetAuthenticatedExtendedCardResponse(id, ProtoUtils.FromProto.agentCard(builder));
            }
            default ->
                throw new MethodNotFoundJsonMappingException("Unsupported JSON-RPC method: '" + method + "' in response parsing.", getIdIfPossible(jsonRpc));
        }
    }

    public static JSONRPCResponse<?> parseError(JsonObject error, Object id, String method) throws JsonMappingException {
        JSONRPCError rpcError = processError(error);
        switch (method) {
            case GetTaskRequest.METHOD -> {
                return new GetTaskResponse(id, rpcError);
            }
            case CancelTaskRequest.METHOD -> {
                return new CancelTaskResponse(id, rpcError);
            }
            case ListTasksRequest.METHOD -> {
                return new ListTasksResponse(id, rpcError);
            }
            case SetTaskPushNotificationConfigRequest.METHOD -> {
                return new SetTaskPushNotificationConfigResponse(id, rpcError);
            }
            case GetTaskPushNotificationConfigRequest.METHOD -> {
                return new GetTaskPushNotificationConfigResponse(id, rpcError);
            }
            case SendMessageRequest.METHOD -> {
                return new SendMessageResponse(id, rpcError);
            }
            case ListTaskPushNotificationConfigRequest.METHOD -> {
                return new ListTaskPushNotificationConfigResponse(id, rpcError);
            }
            case DeleteTaskPushNotificationConfigRequest.METHOD -> {
                return new DeleteTaskPushNotificationConfigResponse(id, rpcError);
            }
            default ->
                throw new MethodNotFoundJsonMappingException("Unsupported JSON-RPC method: '" + method + "'", id);
        }
    }

    private static JSONRPCError processError(JsonObject error) {
        String message = error.has("message") ? error.get("message").getAsString() : null;
        Integer code = error.has("code") ? error.get("code").getAsInt() : null;
        String data = error.has("data") ? error.get("data").toString() : null;
        if (code != null) {
            switch (code) {
                case JSON_PARSE_ERROR_CODE:
                    return new JSONParseError(code, message, data);
                case INVALID_REQUEST_ERROR_CODE:
                    return new InvalidRequestError(code, message, data);
                case METHOD_NOT_FOUND_ERROR_CODE:
                    return new MethodNotFoundError(code, message, data);
                case INVALID_PARAMS_ERROR_CODE:
                    return new InvalidParamsError(code, message, data);
                case INTERNAL_ERROR_CODE:
                    return new io.a2a.spec.InternalError(code, message, data);
                case PUSH_NOTIFICATION_NOT_SUPPORTED_ERROR_CODE:
                    return new PushNotificationNotSupportedError(code, message, data);
                case UNSUPPORTED_OPERATION_ERROR_CODE:
                    return new UnsupportedOperationError(code, message, data);
                case CONTENT_TYPE_NOT_SUPPORTED_ERROR_CODE:
                    return new ContentTypeNotSupportedError(code, message, data);
                case INVALID_AGENT_RESPONSE_ERROR_CODE:
                    return new InvalidAgentResponseError(code, message, data);
                case TASK_NOT_CANCELABLE_ERROR_CODE:
                    return new TaskNotCancelableError(code, message, data);
                case TASK_NOT_FOUND_ERROR_CODE:
                    return new TaskNotFoundError(code, message, data);
                default:
                    return new JSONRPCError(code, message, data);
            }
        }
        return new JSONRPCError(code, message, data);
    }

    protected static void parseRequestBody(JsonElement jsonRpc, com.google.protobuf.Message.Builder builder) throws JSONRPCError {
        try (Writer writer = new StringWriter()) {
            GSON.toJson(jsonRpc, writer);
            parseJsonString(writer.toString(), builder);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to serialize JSON element to string during proto conversion. JSON: {0}", jsonRpc);
            log.log(Level.SEVERE, "Serialization error details", e);
            throw new InvalidParamsError(
                    "Failed to parse request content. " +
                    "This may indicate invalid JSON structure or unsupported field types. Error: " + e.getMessage());
        }
    }

    public static void parseJsonString(String body, com.google.protobuf.Message.Builder builder) throws JSONRPCError {
        try {
            JsonFormat.parser().merge(body, builder);
        } catch (InvalidProtocolBufferException e) {
            log.log(Level.SEVERE, "Protocol buffer parsing failed for JSON: {0}", body);
            log.log(Level.SEVERE, "Proto parsing error details", e);
            throw new InvalidParamsError(
                    "Invalid request content: " + extractProtoErrorMessage(e) +
                    ". Please verify the request matches the expected schema for this method.");
        }
    }

    /**
     * Extracts a user-friendly error message from Protocol Buffer parsing exceptions.
     *
     * @param e the InvalidProtocolBufferException
     * @return a cleaned error message with field information
     */
    private static String extractProtoErrorMessage(InvalidProtocolBufferException e) {
        String message = e.getMessage();
        if (message == null) {
            return "unknown parsing error";
        }
        // Extract field name if present in error message
        if (message.contains("Cannot find field:")) {
            return message.substring(message.indexOf("Cannot find field:"));
        }
        if (message.contains("Invalid value for")) {
            return message.substring(message.indexOf("Invalid value for"));
        }
        return message;
    }

    protected static String getAndValidateJsonrpc(JsonObject jsonRpc) throws JsonMappingException {
        if (!jsonRpc.has("jsonrpc")) {
            throw new IdJsonMappingException(
                    "Missing required 'jsonrpc' field. All requests must include 'jsonrpc': '2.0'",
                    getIdIfPossible(jsonRpc));
        }
        String version = jsonRpc.get("jsonrpc").getAsString();
        if (!JSONRPCMessage.JSONRPC_VERSION.equals(version)) {
            throw new IdJsonMappingException(
                    "Unsupported JSON-RPC version: '" + version + "'. Expected version '2.0'",
                    getIdIfPossible(jsonRpc));
        }
        return version;
    }

    /**
     * Try to get the request id if possible , returns "UNDETERMINED ID" otherwise.
     * This should be only used for errors.
     * @param jsonRpc the json rpc JSON.
     * @return the request id if possible , "UNDETERMINED ID" otherwise.
     */
    protected static Object getIdIfPossible(JsonObject jsonRpc) {
        try {
            return getAndValidateId(jsonRpc);
        } catch (JsonMappingException e) {
            // id can't be determined
            return "UNDETERMINED ID";
        }
    }

    protected static Object getAndValidateId(JsonObject jsonRpc) throws JsonMappingException {
        Object id = null;
        if (jsonRpc.has("id")) {
            if (jsonRpc.get("id").isJsonPrimitive()) {
                try {
                    id = jsonRpc.get("id").getAsInt();
                } catch (UnsupportedOperationException | NumberFormatException | IllegalStateException e) {
                    id = jsonRpc.get("id").getAsString();
                }
            } else {
                throw new JsonMappingException(null,  "Invalid 'id' type: " + jsonRpc.get("id").getClass().getSimpleName() +
                    ". ID must be a JSON string or number, not an object or array.");
            }
        }
        if (id == null) {
            throw new JsonMappingException(null, "Request 'id' cannot be null. Use a string or number identifier.");
        }
        return id;
    }

    public static String toJsonRPCRequest(@Nullable String requestId, String method, com.google.protobuf.@Nullable MessageOrBuilder payload) {
        try (StringWriter result = new StringWriter(); JsonWriter output = GSON.newJsonWriter(result)) {
            output.beginObject();
            output.name("jsonrpc").value("2.0");
            String id = requestId;
            if (requestId == null) {
                id = UUID.randomUUID().toString();
            }
            output.name("id").value(id);
            if (method != null) {
                output.name("method").value(method);
            }
            if (payload != null) {
                String resultValue = JsonFormat.printer().omittingInsignificantWhitespace().print(payload);
                output.name("params").jsonValue(resultValue);
            }
            output.endObject();
            return result.toString();
        } catch (IOException ex) {
            throw new RuntimeException(
                    "Failed to serialize JSON-RPC request for method '" + method + "'. " +
                    "This indicates an internal error in JSON generation. Request ID: " + requestId, ex);
        }
    }

    public static String toJsonRPCResultResponse(Object requestId, com.google.protobuf.MessageOrBuilder builder) {
        try (StringWriter result = new StringWriter(); JsonWriter output = GSON.newJsonWriter(result)) {
            output.beginObject();
            output.name("jsonrpc").value("2.0");
            if (requestId != null) {
                if (requestId instanceof String string) {
                    output.name("id").value(string);
                } else if (requestId instanceof Number number) {
                    output.name("id").value(number.longValue());
                }
            }
            String resultValue = JsonFormat.printer().omittingInsignificantWhitespace().print(builder);
            output.name("result").jsonValue(resultValue);
            output.endObject();
            return result.toString();
        } catch (IOException ex) {
            throw new RuntimeException(
                    "Failed to serialize JSON-RPC success response. " +
                    "Proto type: " + builder.getClass().getSimpleName() + ", Request ID: " + requestId, ex);
        }
    }

    public static String toJsonRPCErrorResponse(Object requestId, JSONRPCError error) {
        try (StringWriter result = new StringWriter(); JsonWriter output = GSON.newJsonWriter(result)) {
            output.beginObject();
            output.name("jsonrpc").value("2.0");
            if (requestId != null) {
                if (requestId instanceof String string) {
                    output.name("id").value(string);
                } else if (requestId instanceof Number number) {
                    output.name("id").value(number.longValue());
                }
            }
            output.name("error");
            output.beginObject();
            output.name("code").value(error.getCode());
            output.name("message").value(error.getMessage());
            if (error.getData() != null) {
                output.name("data").value(error.getData().toString());
            }
            output.endObject();
            output.endObject();
            return result.toString();
        } catch (IOException ex) {
            throw new RuntimeException(
                    "Failed to serialize JSON-RPC error response. " +
                    "Error code: " + error.getCode() + ", Request ID: " + requestId, ex);
        }
    }
}
