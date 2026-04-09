package org.a2aproject.sdk.grpc.utils;

import org.a2aproject.sdk.spec.A2AErrorCodes;
import static org.a2aproject.sdk.spec.A2AMethods.CANCEL_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_STREAMING_MESSAGE_METHOD;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonWriter;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.a2aproject.sdk.grpc.StreamResponse;
import org.a2aproject.sdk.jsonrpc.common.json.IdJsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.json.InvalidParamsJsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.MethodNotFoundJsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2AMessage;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2ARequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2AResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CancelTaskRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CancelTaskResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetExtendedAgentCardRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetExtendedAgentCardResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTaskPushNotificationConfigsRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTaskPushNotificationConfigsResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendMessageRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendMessageResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendStreamingMessageRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SubscribeToTaskRequest;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.ContentTypeNotSupportedError;
import org.a2aproject.sdk.spec.ExtendedAgentCardNotConfiguredError;
import org.a2aproject.sdk.spec.ExtensionSupportRequiredError;
import org.a2aproject.sdk.spec.InvalidAgentResponseError;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.JSONParseError;
import org.a2aproject.sdk.spec.MethodNotFoundError;
import org.a2aproject.sdk.spec.PushNotificationNotSupportedError;
import org.a2aproject.sdk.spec.TaskNotCancelableError;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.a2aproject.sdk.spec.VersionNotSupportedError;
import org.a2aproject.sdk.util.Utils;
import org.jspecify.annotations.Nullable;

import static org.a2aproject.sdk.spec.A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.LIST_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_MESSAGE_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SUBSCRIBE_TO_TASK_METHOD;

/**
 * Utilities for converting between JSON-RPC 2.0 messages and Protocol Buffer objects.
 * <p>
 * This class provides a unified strategy for handling JSON-RPC requests and responses in the A2A SDK
 * by bridging the JSON-RPC transport layer with Protocol Buffer-based internal representations.
 *
 * <h2>Conversion Strategy</h2>
 * The conversion process follows a two-step approach:
 * <ol>
 * <li><b>JSON → Proto:</b> JSON-RPC messages are parsed using Gson, then converted to Protocol Buffer
 * objects using Google's {@link JsonFormat} parser. This ensures consistent handling of field names,
 * types, and nested structures according to the proto3 specification.</li>
 * <li><b>Proto → Spec:</b> Protocol Buffer objects are converted to A2A spec objects using
 * {@link ProtoUtils.FromProto} converters, which handle type mappings and create immutable
 * spec-compliant Java objects.</li>
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
 * <li><b>Missing/invalid method:</b> Returns {@link MethodNotFoundError} with the invalid method name</li>
 * <li><b>Invalid parameters:</b> Returns {@link InvalidParamsError} with proto parsing details</li>
 * <li><b>Protocol version mismatch:</b> Returns {@link InvalidRequestError} with version info</li>
 * <li><b>Missing/invalid id:</b> Returns {@link InvalidRequestError} with id validation details</li>
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
 * org.a2aproject.sdk.grpc.GetTaskRequest protoRequest = ...;
 * String json = JSONRPCUtils.toJsonRPCRequest("req-1", "tasks.get", protoRequest);
 *
 * // Create JSON-RPC response from proto
 * org.a2aproject.sdk.grpc.Task protoTask = ...;
 * String response = JSONRPCUtils.toJsonRPCResultResponse("req-1", protoTask);
 * }</pre>
 *
 * @see ProtoUtils
 * @see A2ARequest
 * @see A2AResponse
 * @see <a href="https://www.jsonrpc.org/specification">JSON-RPC 2.0 Specification</a>
 */
public class JSONRPCUtils {

    private static final Logger log = Logger.getLogger(JSONRPCUtils.class.getName());
    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .create();
    private static final Pattern EXTRACT_WRONG_VALUE = Pattern.compile("Expect (.*) but got: \".*\"");
    private static final Pattern EXTRACT_WRONG_TYPE = Pattern.compile("Expected (.*) but found \".*\"");
    static final String ERROR_MESSAGE = "Invalid request content: %s. Please verify the request matches the expected schema for this method.";

    public static A2ARequest<?> parseRequestBody(String body, @Nullable String tenant) throws JsonMappingException, JsonProcessingException {
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
            return parseMethodRequest(version, id, method, paramsNode, tenant);
        } catch (InvalidParamsError e) {
            throw new InvalidParamsJsonMappingException(Utils.defaultIfNull(e.getMessage(), "Invalid parameters"), id);
        }
    }

    private static A2ARequest<?> parseMethodRequest(String version, Object id, String method, JsonElement paramsNode, @Nullable String tenant) throws InvalidParamsError, MethodNotFoundJsonMappingException, JsonProcessingException {
        switch (method) {
            case GET_TASK_METHOD -> {
                org.a2aproject.sdk.grpc.GetTaskRequest.Builder builder = org.a2aproject.sdk.grpc.GetTaskRequest.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                if (tenant != null && !tenant.isBlank() && (builder.getTenant() == null || builder.getTenant().isBlank())) {
                    builder.setTenant(tenant);
                }
                return new GetTaskRequest(version, id, ProtoUtils.FromProto.taskQueryParams(builder));
            }
            case CANCEL_TASK_METHOD -> {
                org.a2aproject.sdk.grpc.CancelTaskRequest.Builder builder = org.a2aproject.sdk.grpc.CancelTaskRequest.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                if (tenant != null && !tenant.isBlank() && (builder.getTenant() == null || builder.getTenant().isBlank())) {
                    builder.setTenant(tenant);
                }
                return new CancelTaskRequest(version, id, ProtoUtils.FromProto.cancelTaskParams(builder));
            }
            case LIST_TASK_METHOD -> {
                org.a2aproject.sdk.grpc.ListTasksRequest.Builder builder = org.a2aproject.sdk.grpc.ListTasksRequest.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                if (tenant != null && !tenant.isBlank() && (builder.getTenant() == null || builder.getTenant().isBlank())) {
                    builder.setTenant(tenant);
                }
                return new ListTasksRequest(version, id, ProtoUtils.FromProto.listTasksParams(builder));
            }
            case SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD -> {
                org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder builder = org.a2aproject.sdk.grpc.TaskPushNotificationConfig.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                if (tenant != null && !tenant.isBlank() && (builder.getTenant() == null || builder.getTenant().isBlank())) {
                    builder.setTenant(tenant);
                }
                return new CreateTaskPushNotificationConfigRequest(version, id, ProtoUtils.FromProto.createTaskPushNotificationConfig(builder));
            }
            case GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD -> {
                org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest.Builder builder = org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                if (tenant != null && !tenant.isBlank() && (builder.getTenant() == null || builder.getTenant().isBlank())) {
                    builder.setTenant(tenant);
                }
                return new GetTaskPushNotificationConfigRequest(version, id, ProtoUtils.FromProto.getTaskPushNotificationConfigParams(builder));
            }
            case SEND_MESSAGE_METHOD -> {
                org.a2aproject.sdk.grpc.SendMessageRequest.Builder builder = org.a2aproject.sdk.grpc.SendMessageRequest.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                if (tenant != null && !tenant.isBlank() && (builder.getTenant() == null || builder.getTenant().isBlank())) {
                    builder.setTenant(tenant);
                }
                return new SendMessageRequest(version, id, ProtoUtils.FromProto.messageSendParams(builder));
            }
            case LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD -> {
                org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest.Builder builder = org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                if (tenant != null && !tenant.isBlank() && (builder.getTenant() == null || builder.getTenant().isBlank())) {
                    builder.setTenant(tenant);
                }
                return new ListTaskPushNotificationConfigsRequest(version, id, ProtoUtils.FromProto.listTaskPushNotificationConfigsParams(builder));
            }
            case DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD -> {
                org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest.Builder builder = org.a2aproject.sdk.grpc.DeleteTaskPushNotificationConfigRequest.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                if (tenant != null && !tenant.isBlank() && (builder.getTenant() == null || builder.getTenant().isBlank())) {
                    builder.setTenant(tenant);
                }
                return new DeleteTaskPushNotificationConfigRequest(version, id, ProtoUtils.FromProto.deleteTaskPushNotificationConfigParams(builder));
            }
            case GET_EXTENDED_AGENT_CARD_METHOD -> {
                return new GetExtendedAgentCardRequest(version, id);
            }
            case SEND_STREAMING_MESSAGE_METHOD -> {
                org.a2aproject.sdk.grpc.SendMessageRequest.Builder builder = org.a2aproject.sdk.grpc.SendMessageRequest.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                if (tenant != null && !tenant.isBlank() && (builder.getTenant() == null || builder.getTenant().isBlank())) {
                    builder.setTenant(tenant);
                }
                return new SendStreamingMessageRequest(version, id, ProtoUtils.FromProto.messageSendParams(builder));
            }
            case SUBSCRIBE_TO_TASK_METHOD -> {
                org.a2aproject.sdk.grpc.SubscribeToTaskRequest.Builder builder = org.a2aproject.sdk.grpc.SubscribeToTaskRequest.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                if (tenant != null && !tenant.isBlank() && (builder.getTenant() == null || builder.getTenant().isBlank())) {
                    builder.setTenant(tenant);
                }
                return new SubscribeToTaskRequest(version, id, ProtoUtils.FromProto.taskIdParams(builder));
            }
            default ->
                throw new MethodNotFoundJsonMappingException("Unsupported JSON-RPC method: '" + method + "'", id);
        }
    }

    public static StreamResponse parseResponseEvent(String body) throws JsonMappingException, JsonProcessingException {
        JsonElement jelement = JsonParser.parseString(body);
        JsonObject jsonRpc = jelement.getAsJsonObject();
        String version = getAndValidateJsonrpc(jsonRpc);
        Object id = getAndValidateId(jsonRpc);
        JsonElement paramsNode = jsonRpc.get("result");
        if (jsonRpc.has("error")) {
            throw processError(jsonRpc.getAsJsonObject("error"));
        }
        StreamResponse.Builder builder = StreamResponse.newBuilder();
        parseRequestBody(paramsNode, builder, id);
        return builder.build();
    }

    public static A2AResponse<?> parseResponseBody(String body, String method) throws JsonMappingException, JsonProcessingException {
        JsonElement jelement = JsonParser.parseString(body);
        JsonObject jsonRpc = jelement.getAsJsonObject();
        String version = getAndValidateJsonrpc(jsonRpc);
        Object id = getAndValidateId(jsonRpc);
        JsonElement paramsNode = jsonRpc.get("result");
        if (jsonRpc.has("error")) {
            return parseError(jsonRpc.getAsJsonObject("error"), id, method);
        }
        switch (method) {
            case GET_TASK_METHOD -> {
                org.a2aproject.sdk.grpc.Task.Builder builder = org.a2aproject.sdk.grpc.Task.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                return new GetTaskResponse(id, ProtoUtils.FromProto.task(builder));
            }
            case CANCEL_TASK_METHOD -> {
                org.a2aproject.sdk.grpc.Task.Builder builder = org.a2aproject.sdk.grpc.Task.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                return new CancelTaskResponse(id, ProtoUtils.FromProto.task(builder));
            }
            case LIST_TASK_METHOD -> {
                org.a2aproject.sdk.grpc.ListTasksResponse.Builder builder = org.a2aproject.sdk.grpc.ListTasksResponse.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                return new ListTasksResponse(id, ProtoUtils.FromProto.listTasksResult(builder));
            }
            case SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD -> {
                org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder builder = org.a2aproject.sdk.grpc.TaskPushNotificationConfig.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                return new CreateTaskPushNotificationConfigResponse(id, ProtoUtils.FromProto.taskPushNotificationConfig(builder));
            }
            case GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD -> {
                org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder builder = org.a2aproject.sdk.grpc.TaskPushNotificationConfig.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                return new GetTaskPushNotificationConfigResponse(id, ProtoUtils.FromProto.taskPushNotificationConfig(builder));
            }
            case SEND_MESSAGE_METHOD -> {
                org.a2aproject.sdk.grpc.SendMessageResponse.Builder builder = org.a2aproject.sdk.grpc.SendMessageResponse.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                if (builder.hasMessage()) {
                    return new SendMessageResponse(id, ProtoUtils.FromProto.message(builder.getMessage()));
                }
                return new SendMessageResponse(id, ProtoUtils.FromProto.task(builder.getTask()));
            }
            case LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD -> {
                org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsResponse.Builder builder = org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsResponse.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                return new ListTaskPushNotificationConfigsResponse(id, ProtoUtils.FromProto.listTaskPushNotificationConfigsResult(builder));
            }
            case DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD -> {
                return new DeleteTaskPushNotificationConfigResponse(id);
            }
            case GET_EXTENDED_AGENT_CARD_METHOD -> {
                org.a2aproject.sdk.grpc.AgentCard.Builder builder = org.a2aproject.sdk.grpc.AgentCard.newBuilder();
                parseRequestBody(paramsNode, builder, id);
                return new GetExtendedAgentCardResponse(id, ProtoUtils.FromProto.agentCard(builder));
            }
            default ->
                throw new MethodNotFoundJsonMappingException("Unsupported JSON-RPC method: '" + method + "' in response parsing.", getIdIfPossible(jsonRpc));
        }
    }

    public static A2AResponse<?> parseError(JsonObject error, Object id, String method) throws JsonMappingException {
        A2AError rpcError = processError(error);
        switch (method) {
            case GET_TASK_METHOD -> {
                return new GetTaskResponse(id, rpcError);
            }
            case CANCEL_TASK_METHOD -> {
                return new CancelTaskResponse(id, rpcError);
            }
            case LIST_TASK_METHOD -> {
                return new ListTasksResponse(id, rpcError);
            }
            case SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD -> {
                return new CreateTaskPushNotificationConfigResponse(id, rpcError);
            }
            case GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD -> {
                return new GetTaskPushNotificationConfigResponse(id, rpcError);
            }
            case SEND_MESSAGE_METHOD -> {
                return new SendMessageResponse(id, rpcError);
            }
            case LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD -> {
                return new ListTaskPushNotificationConfigsResponse(id, rpcError);
            }
            case DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD -> {
                return new DeleteTaskPushNotificationConfigResponse(id, rpcError);
            }
            default ->
                throw new MethodNotFoundJsonMappingException("Unsupported JSON-RPC method: '" + method + "'", id);
        }
    }

    @SuppressWarnings("unchecked")
    private static A2AError processError(JsonObject error) {
        String message = error.has("message") ? error.get("message").getAsString() : null;
        Integer code = error.has("code") ? error.get("code").getAsInt() : null;
        Map<String, Object> details = null;
        if (error.has("details") && error.get("details").isJsonObject()) {
            details =GSON.fromJson(error.get("details"), Map.class);
        }
        if (code != null) {
            A2AErrorCodes errorCode = A2AErrorCodes.fromCode(code);
            if (errorCode != null) {
                return switch (errorCode) {
                    case JSON_PARSE -> new JSONParseError(code, message, details);
                    case INVALID_REQUEST -> new InvalidRequestError(code, message, details);
                    case METHOD_NOT_FOUND -> new MethodNotFoundError(code, message, details);
                    case INVALID_PARAMS -> new InvalidParamsError(code, message, details);
                    case INTERNAL -> new org.a2aproject.sdk.spec.InternalError(code, message, details);
                    case PUSH_NOTIFICATION_NOT_SUPPORTED -> new PushNotificationNotSupportedError(code, message, details);
                    case UNSUPPORTED_OPERATION -> new UnsupportedOperationError(code, message, details);
                    case CONTENT_TYPE_NOT_SUPPORTED -> new ContentTypeNotSupportedError(code, message, details);
                    case INVALID_AGENT_RESPONSE -> new InvalidAgentResponseError(code, message, details);
                    case EXTENDED_AGENT_CARD_NOT_CONFIGURED -> new ExtendedAgentCardNotConfiguredError(code, message, details);
                    case EXTENSION_SUPPORT_REQUIRED -> new ExtensionSupportRequiredError(code, message, details);
                    case VERSION_NOT_SUPPORTED -> new VersionNotSupportedError(code, message, details);
                    case TASK_NOT_CANCELABLE -> new TaskNotCancelableError(code, message, details);
                    case TASK_NOT_FOUND -> new TaskNotFoundError(message, details);
                };
            }
            return new A2AError(code, message == null ? "" : message, details);
        }
        return new A2AError(A2AErrorCodes.INTERNAL.code(), message == null ? "" : message, details);
    }

    protected static void parseRequestBody(JsonElement jsonRpc, com.google.protobuf.Message.Builder builder, Object id) throws JsonProcessingException {
        parseJsonString(jsonRpc.toString(), builder, id);
    }

    public static void parseJsonString(String body, com.google.protobuf.Message.Builder builder, Object id) throws JsonProcessingException {
        try {
            JsonFormat.parser().merge(body, builder);
        } catch (InvalidProtocolBufferException e) {
            log.log(Level.FINE, "Protocol buffer parsing failed for JSON: {0}", body);
            log.log(Level.FINE, "Proto parsing error details", e);
            throw convertProtoBufExceptionToJsonProcessingException(e, id);
        }
    }

    /**
     * Extracts a user-friendly error message from Protocol Buffer parsing exceptions.
     * <p>
     * This method converts low-level protobuf exceptions into appropriate JsonProcessingException:
     * <ul>
     *   <li>{@link InvalidParamsJsonMappingException} - When the request ID is known and the error
     *       is related to invalid parameter structure (wrong type, missing field, invalid value).
     *       This maps to JSON-RPC error code -32602 (Invalid params).</li>
     *   <li>{@link JsonProcessingException} - When the request ID is unknown and the error is
     *       related to malformed JSON structure. This maps to JSON-RPC error code -32700 (Parse error).</li>
     *   <li>{@link JsonMappingException} - When the error doesn't fit specific patterns and we
     *       cannot determine the exact error type. This is a generic JSON-RPC error.</li>
     * </ul>
     * <p>
     * <b>ID Handling:</b> When the request ID is null, we use an empty string ("") as a sentinel value
     * for InvalidParamsJsonMappingException. This is required because the JSON-RPC spec mandates that
     * error responses must include the request ID if it could be determined. An empty string indicates
     * "we tried to get the ID but it was invalid/missing", while a null JsonMappingException indicates
     * "we couldn't even parse enough to attempt ID extraction".
     *
     * @param e the InvalidProtocolBufferException from proto parsing
     * @param id the request ID if it could be extracted, null otherwise
     * @return an appropriate JsonProcessingException subtype based on the error and ID availability
     */
    private static JsonProcessingException convertProtoBufExceptionToJsonProcessingException(InvalidProtocolBufferException e, Object id) {
        // Log the original exception for debugging purposes
        log.log(Level.FINE, "Converting protobuf parsing exception to JSON-RPC error. Request ID: {0}", id);
        log.log(Level.FINE, "Original proto exception details", e);

        String message = e.getMessage();
        if (message == null) {
            return new JsonProcessingException(ERROR_MESSAGE.formatted("unknown parsing error"));
        }

        // Extract field name if present in error message - check common prefixes
        String[] prefixes = {"Cannot find field: ", "Invalid value for", "Invalid enum value:", "Failed to parse"};
        for (String prefix : prefixes) {
            if (message.contains(prefix)) {
                return new InvalidParamsJsonMappingException(ERROR_MESSAGE.formatted(message.substring(message.indexOf(prefix) + prefix.length())), id);
            }
        }

        // Try to extract specific error details using regex patterns
        Matcher matcher = EXTRACT_WRONG_TYPE.matcher(message);
        if (matcher.matches() && matcher.group(1) != null) {
            // ID is null -> use empty string sentinel value (see javadoc above)
            return new InvalidParamsJsonMappingException(ERROR_MESSAGE.formatted(matcher.group(1)), Utils.defaultIfNull(id, ""));
        }
        matcher = EXTRACT_WRONG_VALUE.matcher(message);
        if (matcher.matches() && matcher.group(1) != null) {
            // ID is null -> use empty string sentinel value (see javadoc above)
            return new InvalidParamsJsonMappingException(ERROR_MESSAGE.formatted(matcher.group(1)), Utils.defaultIfNull(id, ""));
        }

        // Generic error - couldn't match specific patterns
        return new JsonMappingException(ERROR_MESSAGE.formatted(message));
    }

    protected static String getAndValidateJsonrpc(JsonObject jsonRpc) throws JsonMappingException {
        if (!jsonRpc.has("jsonrpc")) {
            throw new IdJsonMappingException(
                    "Missing required 'jsonrpc' field. All requests must include 'jsonrpc': '2.0'",
                    getIdIfPossible(jsonRpc));
        }
        String version = jsonRpc.get("jsonrpc").getAsString();
        if (!A2AMessage.JSONRPC_VERSION.equals(version)) {
            throw new IdJsonMappingException(
                    "Unsupported JSON-RPC version: '" + version + "'. Expected version '2.0'",
                    getIdIfPossible(jsonRpc));
        }
        return version;
    }

    /**
     * Try to get the request id if possible , returns "UNDETERMINED ID" otherwise.
     * This should be only used for errors.
     *
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
                throw new JsonMappingException(null, "Invalid 'id' type: " + jsonRpc.get("id").getClass().getSimpleName()
                        + ". ID must be a JSON string or number, not an object or array.");
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
                String resultValue = JsonFormat.printer().alwaysPrintFieldsWithNoPresence().omittingInsignificantWhitespace().print(payload);
                output.name("params").jsonValue(resultValue);
            }
            output.endObject();
            return result.toString();
        } catch (IOException ex) {
            throw new RuntimeException(
                    "Failed to serialize JSON-RPC request for method '" + method + "'. "
                    + "This indicates an internal error in JSON generation. Request ID: " + requestId, ex);
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
            String resultValue = JsonFormat.printer().alwaysPrintFieldsWithNoPresence().omittingInsignificantWhitespace().print(builder);
            output.name("result").jsonValue(resultValue);
            output.endObject();
            return result.toString();
        } catch (IOException ex) {
            throw new RuntimeException(
                    "Failed to serialize JSON-RPC success response. "
                    + "Proto type: " + builder.getClass().getSimpleName() + ", Request ID: " + requestId, ex);
        }
    }

    public static String toJsonRPCErrorResponse(Object requestId, A2AError error) {
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
            if (!error.getDetails().isEmpty()) {
                output.name("details");
                GSON.toJson(error.getDetails(), Map.class, output);
            }
            output.endObject();
            output.endObject();
            return result.toString();
        } catch (IOException ex) {
            throw new RuntimeException(
                    "Failed to serialize JSON-RPC error response. "
                    + "Error code: " + error.getCode() + ", Request ID: " + requestId, ex);
        }
    }
}
