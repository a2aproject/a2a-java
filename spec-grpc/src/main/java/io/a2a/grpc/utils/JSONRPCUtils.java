package io.a2a.grpc.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonWriter;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigResponse;
import io.a2a.spec.GetAuthenticatedExtendedCardRequest;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.IdJsonMappingException;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCMessage;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.JSONRPCResponse;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.ListTaskPushNotificationConfigResponse;
import io.a2a.spec.ListTasksRequest;
import io.a2a.spec.ListTasksResponse;
import io.a2a.spec.MethodNotFoundJsonMappingException;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.SubscribeToTaskRequest;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JSONRPCUtils {

    private static final Logger log = Logger.getLogger(JSONRPCUtils.class.getName());
    private static final Gson GSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();

    public static JSONRPCRequest<?> parseRequestBody(String body) throws JsonProcessingException {
        JsonElement jelement = JsonParser.parseString(body);
        JsonObject jsonRpc = jelement.getAsJsonObject();
        if (!jsonRpc.has("method")) {
            throw new IdJsonMappingException("Missing method", getIdIfPossible(jsonRpc));
        }
        String version = getAndValidateJsonrpc(jsonRpc);
        Object id = getAndValidateId(jsonRpc);
        String method = jsonRpc.get("method").getAsString();
        JsonElement paramsNode = jsonRpc.get("params");

        switch (method) {
            case GetTaskRequest.METHOD -> {
                io.a2a.grpc.GetTaskRequest.Builder builder = io.a2a.grpc.GetTaskRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new GetTaskRequest(version, id, method, ProtoUtils.FromProto.taskQueryParams(builder));
            }
            case CancelTaskRequest.METHOD -> {
                io.a2a.grpc.CancelTaskRequest.Builder builder = io.a2a.grpc.CancelTaskRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new CancelTaskRequest(version, id, method, ProtoUtils.FromProto.taskIdParams(builder));
            }
            case ListTasksRequest.METHOD -> {
                io.a2a.grpc.ListTasksRequest.Builder builder = io.a2a.grpc.ListTasksRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new ListTasksRequest(version, id, method, ProtoUtils.FromProto.listTasksParams(builder));
            }
            case SetTaskPushNotificationConfigRequest.METHOD -> {
                io.a2a.grpc.SetTaskPushNotificationConfigRequest.Builder builder = io.a2a.grpc.SetTaskPushNotificationConfigRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new SetTaskPushNotificationConfigRequest(version, id, method, ProtoUtils.FromProto.setTaskPushNotificationConfig(builder));
            }
            case GetTaskPushNotificationConfigRequest.METHOD -> {
                io.a2a.grpc.GetTaskPushNotificationConfigRequest.Builder builder = io.a2a.grpc.GetTaskPushNotificationConfigRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new GetTaskPushNotificationConfigRequest(version, id, method, ProtoUtils.FromProto.getTaskPushNotificationConfigParams(builder));
            }
            case SendMessageRequest.METHOD -> {
                io.a2a.grpc.SendMessageRequest.Builder builder = io.a2a.grpc.SendMessageRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new SendMessageRequest(version, id, method, ProtoUtils.FromProto.messageSendParams(builder));
            }
            case ListTaskPushNotificationConfigRequest.METHOD -> {
                io.a2a.grpc.ListTaskPushNotificationConfigRequest.Builder builder = io.a2a.grpc.ListTaskPushNotificationConfigRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new ListTaskPushNotificationConfigRequest(version, id, method, ProtoUtils.FromProto.listTaskPushNotificationConfigParams(builder));
            }
            case DeleteTaskPushNotificationConfigRequest.METHOD -> {
                io.a2a.grpc.DeleteTaskPushNotificationConfigRequest.Builder builder = io.a2a.grpc.DeleteTaskPushNotificationConfigRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new DeleteTaskPushNotificationConfigRequest(version, id, method, ProtoUtils.FromProto.deleteTaskPushNotificationConfigParams(builder));
            }
            case GetAuthenticatedExtendedCardRequest.METHOD -> {
                return new GetAuthenticatedExtendedCardRequest(version, id, method, null);
            }
            case SendStreamingMessageRequest.METHOD -> {
                io.a2a.grpc.SendMessageRequest.Builder builder = io.a2a.grpc.SendMessageRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new SendStreamingMessageRequest(version, id, method, ProtoUtils.FromProto.messageSendParams(builder));
            }
            case SubscribeToTaskRequest.METHOD -> {
                io.a2a.grpc.SubscribeToTaskRequest.Builder builder = io.a2a.grpc.SubscribeToTaskRequest.newBuilder();
                parseRequestBody(paramsNode, builder);
                return new SubscribeToTaskRequest(version, id, method, ProtoUtils.FromProto.taskIdParams(builder));
            }
            default ->
                throw new MethodNotFoundJsonMappingException("Invalid method", getIdIfPossible(jsonRpc));
        }
    }
    
    public static JSONRPCResponse<?> parseResponseBody(String body, String method) throws JsonProcessingException {
        JsonElement jelement = JsonParser.parseString(body);
        JsonObject jsonRpc = jelement.getAsJsonObject();
        String version = getAndValidateJsonrpc(jsonRpc);
        Object id = getAndValidateId(jsonRpc);
        JsonElement paramsNode = jsonRpc.get("result");
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
                if(builder.hasMsg()) {
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
            default ->
                throw new MethodNotFoundJsonMappingException("Invalid method", getIdIfPossible(jsonRpc));
        }
    }

    protected static void parseRequestBody(JsonElement jsonRpc, com.google.protobuf.Message.Builder builder) throws JSONRPCError {
        try (Writer writer = new StringWriter()) {
            GSON.toJson(jsonRpc, writer);
            JSONRPCUtils.parseRequestBody(writer.toString(), builder);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error parsing JSON request body: {0}", jsonRpc);
            log.log(Level.SEVERE, "Parse error details", e);
            throw new InvalidParamsError("Failed to parse request body: " + e.getMessage());
        }
    }

    protected static void parseRequestBody(String body, com.google.protobuf.Message.Builder builder) throws JSONRPCError {
        try {
            JsonFormat.parser().merge(body, builder);
        } catch (InvalidProtocolBufferException e) {
            log.log(Level.SEVERE, "Error parsing JSON request body: {0}", body);
            log.log(Level.SEVERE, "Parse error details", e);
            throw new InvalidParamsError("Failed to parse request body: " + e.getMessage());
        }
    }

    protected static String getAndValidateJsonrpc(JsonObject jsonRpc) throws JsonMappingException {
        if (!jsonRpc.has("jsonrpc") || !JSONRPCMessage.JSONRPC_VERSION.equals(jsonRpc.get("jsonrpc").getAsString())) {
            throw new IdJsonMappingException("Invalid JSON-RPC protocol version", getIdIfPossible(jsonRpc));
        }
        return jsonRpc.get("jsonrpc").getAsString();
    }

    protected static Object getIdIfPossible(JsonObject jsonRpc) {
        try {
            return getAndValidateId(jsonRpc);
        } catch (JsonProcessingException e) {
            // id can't be determined
            return null;
        }
    }

    protected static Object getAndValidateId(JsonObject jsonRpc) throws JsonProcessingException {
        Object id = null;
        if (jsonRpc.has("id")) {
            if (jsonRpc.get("id").isJsonPrimitive()) {
                try {
                    id = jsonRpc.get("id").getAsInt();
                } catch (UnsupportedOperationException | NumberFormatException | IllegalStateException e) {
                    id = jsonRpc.get("id").getAsString();
                }
            } else {
                throw new JsonMappingException(null, "Invalid id");
            }
        }
        return id;
    }

    public static String toJsonRPCString(String requestId, String method, com.google.protobuf.MessageOrBuilder builder) {
        try (StringWriter result = new StringWriter(); JsonWriter output = GSON.newJsonWriter(result).beginObject()) {
            output.name("jsonrpc").value("2.0");
            String id = requestId;
            if (requestId == null) {
                id = UUID.randomUUID().toString();
            }
            output.name("id").value(id);
            if (method != null) {
                output.name("method").value(method);
            }
            output.name("result").beginObject().jsonValue(JsonFormat.printer().print(builder));
            output.endObject().endObject();
            return result.toString();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     *
     * public static String toJsonRPCString(String id, com.google.protobuf.Message.Builder builder) {
     * try (StringWriter result = new StringWriter()) {
     * JsonObjectBuilder json = Json.createObjectBuilder();
     * json.add("jsonrpc", "2.0");
     * json.add("id", id);
     * json.add("result", Json.createObjectBuilder(Json.createParser(new
     * StringReader(JsonFormat.printer().print(builder))).getObject()));
     * Json.createWriter(result).writeObject(json.build());
     * return result.toString();
     * } catch (IOException ex) {
     * throw new RuntimeException(ex);
     * }
     * }
     *
     * public static String toJsonRPCString(String id, JSONRPCError error) {
     * try (StringWriter result = new StringWriter()) {
     * JsonObjectBuilder errorBuilder = Json.createObjectBuilder();
     * errorBuilder.add("code", error.getCode());
     * errorBuilder.add("message", error.getMessage());
     * if (error.getData() != null) {
     * errorBuilder.add("data", error.getData().toString());
     * }
     * JsonObjectBuilder json = Json.createObjectBuilder();
     * json.add("jsonrpc", "2.0");
     * json.add("id", id);
     * json.add("error", errorBuilder);
     * Json.createWriter(result).writeObject(json.build());
     * return result.toString();
     * } catch (IOException ex) {
     * throw new RuntimeException(ex);
     * }
     * }
     */
}
