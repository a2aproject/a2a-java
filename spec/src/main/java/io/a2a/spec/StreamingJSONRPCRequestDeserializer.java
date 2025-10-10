package io.a2a.spec;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

public class StreamingJSONRPCRequestDeserializer<T> extends JSONRPCRequestDeserializerBase<StreamingJSONRPCRequest<?>> {

    public StreamingJSONRPCRequestDeserializer() {
        this(null);
    }

    public StreamingJSONRPCRequestDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public StreamingJSONRPCRequest<?> deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException, JsonProcessingException {
        JsonNode treeNode = jsonParser.getCodec().readTree(jsonParser);
        String jsonrpc = getAndValidateJsonrpc(treeNode, jsonParser);
        String method = getAndValidateMethod(treeNode, jsonParser);
        Object id = getAndValidateId(treeNode, jsonParser);
        JsonNode paramsNode = treeNode.get("params");

        switch (method) {
            case TaskResubscriptionRequest.METHOD -> {
                return new TaskResubscriptionRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, TaskIdParams.class));
            }
            case SendStreamingMessageRequest.METHOD -> {
                return new SendStreamingMessageRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, MessageSendParams.class));
            }
            default -> throw new MethodNotFoundJsonMappingException("Invalid method", getIdIfPossible(treeNode, jsonParser));
        }
    }
}
