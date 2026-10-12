package org.a2aproject.sdk.compat03.transport.jsonrpc.handler;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.CancelTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetAuthenticatedExtendedCardRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.InternalError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONParseError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCMessage_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.MethodNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.NonStreamingJSONRPCRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.StreamingJSONRPCRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskResubscriptionRequest_v0_3;
import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/** Decodes compat JSON-RPC envelopes and typed requests without a server runtime. */
public final class JSONRPCRequestDecoder_v0_3 {

    private JSONRPCRequestDecoder_v0_3() {
    }

    public sealed interface DecodeResult permits DecodedRequest, DecodeFailure {
    }

    /** The recovered ID is kept separately from the deserialized wrapper's ID. */
    public record DecodedRequest(@Nullable Object requestId, String methodName,
            JSONRPCRequest_v0_3<?> request) implements DecodeResult {
        public DecodedRequest {
            Assert.checkNotNullParam("methodName", methodName);
            Assert.checkNotNullParam("request", request);
        }
    }

    /** The method is available only after envelope validation has recovered it. */
    public record DecodeFailure(@Nullable Object requestId, @Nullable String methodName,
            JSONRPCError_v0_3 error) implements DecodeResult {
        public DecodeFailure {
            Assert.checkNotNullParam("error", error);
        }
    }

    public static DecodeResult decode(@Nullable String body) {
        Object requestId = null;
        String methodName = null;
        try {
            com.google.gson.JsonObject node;
            try {
                node = parseBody(body);
            } catch (Exception e) {
                throw new JSONParseError_v0_3(e.getMessage());
            }
            // Parsing a null body above must map to JSONParseError before this nullability refinement.
            String requestBody = Assert.checkNotNullParam("body", body);

            // Extract id field early so error responses can include it
            com.google.gson.JsonElement idElement = node.get("id");
            if (idElement != null && !idElement.isJsonNull() && !idElement.isJsonPrimitive()) {
                throw new InvalidRequestError_v0_3("Invalid JSON-RPC request: 'id' must be a string, number, or null");
            }
            if (idElement != null && !idElement.isJsonNull() && idElement.isJsonPrimitive()) {
                com.google.gson.JsonPrimitive idPrimitive = idElement.getAsJsonPrimitive();
                requestId = idPrimitive.isNumber() ? idPrimitive.getAsLong() : idPrimitive.getAsString();
            }

            // Validate jsonrpc field
            com.google.gson.JsonElement jsonrpcElement = node.get("jsonrpc");
            if (jsonrpcElement == null || !jsonrpcElement.isJsonPrimitive()
                    || !JSONRPCMessage_v0_3.JSONRPC_VERSION.equals(jsonrpcElement.getAsString())) {
                throw new InvalidRequestError_v0_3("Invalid JSON-RPC request: missing or invalid 'jsonrpc' field");
            }

            // Validate method field
            com.google.gson.JsonElement methodElement = node.get("method");
            if (methodElement == null || !methodElement.isJsonPrimitive()) {
                throw new InvalidRequestError_v0_3("Invalid JSON-RPC request: missing or invalid 'method' field");
            }

            methodName = methodElement.getAsString();

            boolean streaming = SendStreamingMessageRequest_v0_3.METHOD.equals(methodName)
                    || TaskResubscriptionRequest_v0_3.METHOD.equals(methodName);
            JSONRPCRequest_v0_3<?> request = streaming
                    ? deserializeStreamingRequest(requestBody, methodName)
                    : deserializeNonStreamingRequest(requestBody, methodName);
            return new DecodedRequest(requestId, methodName, request);
        } catch (JSONRPCError_v0_3 e) {
            return new DecodeFailure(requestId, methodName, e);
        } catch (JsonSyntaxException e) {
            return new DecodeFailure(requestId, methodName, new JSONParseError_v0_3(e.getMessage()));
        } catch (Throwable t) {
            return new DecodeFailure(requestId, methodName, new InternalError_v0_3(t.getMessage()));
        }
    }

    @SuppressWarnings("NullAway") // Let Gson preserve the JSON parse error and message for a null body.
    private static com.google.gson.JsonObject parseBody(@Nullable String body) {
        return JsonParser.parseString(body).getAsJsonObject();
    }

    private static NonStreamingJSONRPCRequest_v0_3<?> deserializeNonStreamingRequest(String body, String methodName) {
        try {
            return switch (methodName) {
                case GetTaskRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, GetTaskRequest_v0_3.class);
                case CancelTaskRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, CancelTaskRequest_v0_3.class);
                case SendMessageRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, SendMessageRequest_v0_3.class);
                case SetTaskPushNotificationConfigRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, SetTaskPushNotificationConfigRequest_v0_3.class);
                case GetTaskPushNotificationConfigRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, GetTaskPushNotificationConfigRequest_v0_3.class);
                case ListTaskPushNotificationConfigRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, ListTaskPushNotificationConfigRequest_v0_3.class);
                case DeleteTaskPushNotificationConfigRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, DeleteTaskPushNotificationConfigRequest_v0_3.class);
                case GetAuthenticatedExtendedCardRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, GetAuthenticatedExtendedCardRequest_v0_3.class);
                default -> throw new MethodNotFoundError_v0_3();
            };
        } catch (JSONRPCError_v0_3 e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidParamsError_v0_3(e.getMessage());
        }
    }

    private static StreamingJSONRPCRequest_v0_3<?> deserializeStreamingRequest(String body, String methodName) {
        try {
            return switch (methodName) {
                case SendStreamingMessageRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, SendStreamingMessageRequest_v0_3.class);
                case TaskResubscriptionRequest_v0_3.METHOD -> JsonUtil_v0_3.fromJson(body, TaskResubscriptionRequest_v0_3.class);
                default -> throw new MethodNotFoundError_v0_3();
            };
        } catch (JSONRPCError_v0_3 e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidParamsError_v0_3(e.getMessage());
        }
    }

}
