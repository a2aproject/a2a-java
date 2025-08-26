package io.a2a.spec;

import static io.a2a.util.Utils.OBJECT_MAPPER;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public abstract class JSONRPCRequestDeserializerBase<T> extends StdDeserializer<JSONRPCRequest<?>> {

    public JSONRPCRequestDeserializerBase() {
        this(null);
    }

    public JSONRPCRequestDeserializerBase(Class<?> vc) {
        super(vc);
    }

    protected <T> T getAndValidateParams(JsonNode paramsNode, JsonParser jsonParser, JsonNode node, Class<T> paramsType) throws JsonMappingException {
        if (paramsNode == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.treeToValue(paramsNode, paramsType);
        } catch (JsonProcessingException e) {
            throw new InvalidParamsJsonMappingException("Invalid params", e, getIdIfPossible(node, jsonParser));
        }
    }

    protected String getAndValidateJsonrpc(JsonNode treeNode, JsonParser jsonParser) throws JsonMappingException {
        JsonNode jsonrpcNode = treeNode.get("jsonrpc");
        if (jsonrpcNode == null || ! jsonrpcNode.asText().equals(JSONRPCMessage.JSONRPC_VERSION)) {
            throw new IdJsonMappingException("Invalid JSON-RPC protocol version", getIdIfPossible(treeNode, jsonParser));
        }
        return jsonrpcNode.asText();
    }

    protected String getAndValidateMethod(JsonNode treeNode, JsonParser jsonParser) throws JsonMappingException {
        JsonNode methodNode = treeNode.get("method");
        if (methodNode == null) {
            throw new IdJsonMappingException("Missing method", getIdIfPossible(treeNode, jsonParser));
        }
        String method = methodNode.asText();
        if (! isValidMethodName(method)) {
            throw new MethodNotFoundJsonMappingException("Invalid method", getIdIfPossible(treeNode, jsonParser));
        }
        return method;
    }

    protected Object getAndValidateId(JsonNode treeNode, JsonParser jsonParser) throws JsonProcessingException {
        JsonNode idNode = treeNode.get("id");
        Object id = null;
        if (idNode != null) {
            if (idNode.isTextual()) {
                id = OBJECT_MAPPER.treeToValue(idNode, String.class);
            } else if (idNode.isNumber()) {
                id = OBJECT_MAPPER.treeToValue(idNode, Integer.class);
            } else {
                throw new JsonMappingException(jsonParser, "Invalid id");
            }
        }
        return id;
    }

    protected Object getIdIfPossible(JsonNode treeNode, JsonParser jsonParser) {
        try {
            return getAndValidateId(treeNode, jsonParser);
        } catch (JsonProcessingException e) {
            // id can't be determined
            return null;
        }
    }

    protected static boolean isValidMethodName(String methodName) {
        return methodName != null && (methodName.equals(CancelTaskRequest.METHOD)
                || methodName.equals(GetTaskRequest.METHOD)
                || methodName.equals(GetTaskPushNotificationConfigRequest.METHOD)
                || methodName.equals(SetTaskPushNotificationConfigRequest.METHOD)
                || methodName.equals(TaskResubscriptionRequest.METHOD)
                || methodName.equals(SendMessageRequest.METHOD)
                || methodName.equals(SendStreamingMessageRequest.METHOD)
                || methodName.equals(ListTaskPushNotificationConfigRequest.METHOD)
                || methodName.equals(DeleteTaskPushNotificationConfigRequest.METHOD)
                || methodName.equals(GetAuthenticatedExtendedCardRequest.METHOD));

    }
}
