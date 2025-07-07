package io.a2a.spec;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class NonStreamingJSONRPCRequestDeserializer extends JSONRPCRequestDeserializerBase<NonStreamingJSONRPCRequest<?>> {

    public NonStreamingJSONRPCRequestDeserializer() {
        this(null);
    }

    public NonStreamingJSONRPCRequestDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public NonStreamingJSONRPCRequest<?> deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException, JsonProcessingException {
        JsonNode treeNode = jsonParser.getCodec().readTree(jsonParser);
        String jsonrpc = getAndValidateJsonrpc(treeNode, jsonParser);
        String method = getAndValidateMethod(treeNode, jsonParser);
        Object id = getAndValidateId(treeNode, jsonParser);
        JsonNode paramsNode = treeNode.get("params");

        switch (method) {
            case GetTaskRequest.METHOD:
                return new GetTaskRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, TaskQueryParams.class));
            case CancelTaskRequest.METHOD:
                return new CancelTaskRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, TaskIdParams.class));
            case SetTaskPushNotificationConfigRequest.METHOD:
                return new SetTaskPushNotificationConfigRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, TaskPushNotificationConfig.class));
            case GetTaskPushNotificationConfigRequest.METHOD:
                return new GetTaskPushNotificationConfigRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, TaskIdParams.class));
            case SendMessageRequest.METHOD:
                return new SendMessageRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, MessageSendParams.class));
            default:
                throw new MethodNotFoundJsonMappingException("Invalid method", getIdIfPossible(treeNode, jsonParser));
        }
    }
}
