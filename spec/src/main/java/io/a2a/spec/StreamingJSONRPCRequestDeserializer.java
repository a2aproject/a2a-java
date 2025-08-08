package io.a2a.spec;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Custom Jackson deserializer for streaming JSON-RPC 2.0 requests in the A2A protocol.
 * This deserializer examines the "method" field of incoming JSON to determine which
 * specific StreamingJSONRPCRequest subclass should be instantiated.
 *
 * <p>The deserializer supports the following streaming request types:</p>
 * <ul>
 *   <li>TaskResubscriptionRequest - for resubscribing to task updates</li>
 *   <li>SendStreamingMessageRequest - for sending streaming messages</li>
 * </ul>
 *
 * <p>The deserialization process validates the JSON-RPC structure, extracts the method name,
 * and routes to the appropriate concrete class constructor. If an unsupported method is
 * encountered, a MethodNotFoundJsonMappingException is thrown.</p>
 *
 * @param <T> the generic type parameter (not used in this implementation)
 * @see StreamingJSONRPCRequest
 * @see JSONRPCRequestDeserializerBase
 * @see TaskResubscriptionRequest
 * @see SendStreamingMessageRequest
 */
public class StreamingJSONRPCRequestDeserializer<T> extends JSONRPCRequestDeserializerBase<StreamingJSONRPCRequest<?>> {

    /**
     * Default constructor for the streaming JSON-RPC request deserializer.
     */
    public StreamingJSONRPCRequestDeserializer() {
        this(null);
    }

    /**
     * Constructor with value class parameter.
     *
     * @param vc the value class (typically not used)
     */
    public StreamingJSONRPCRequestDeserializer(Class<?> vc) {
        super(vc);
    }

    /**
     * Deserializes JSON into the appropriate StreamingJSONRPCRequest subclass.
     * The method field is used to determine which specific request type to create.
     *
     * @param jsonParser the JSON parser containing the request data
     * @param context the deserialization context
     * @return the appropriate StreamingJSONRPCRequest subclass instance
     * @throws IOException if JSON parsing fails
     * @throws JsonProcessingException if JSON processing fails
     * @throws MethodNotFoundJsonMappingException if the method is not supported
     */
    @Override
    public StreamingJSONRPCRequest<?> deserialize(JsonParser jsonParser, DeserializationContext context)
            throws IOException, JsonProcessingException {
        JsonNode treeNode = jsonParser.getCodec().readTree(jsonParser);
        String jsonrpc = getAndValidateJsonrpc(treeNode, jsonParser);
        String method = getAndValidateMethod(treeNode, jsonParser);
        Object id = getAndValidateId(treeNode, jsonParser);
        JsonNode paramsNode = treeNode.get("params");

        switch (method) {
            case TaskResubscriptionRequest.METHOD:
                return new TaskResubscriptionRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, TaskIdParams.class));
            case SendStreamingMessageRequest.METHOD:
                return new SendStreamingMessageRequest(jsonrpc, id, method,
                        getAndValidateParams(paramsNode, jsonParser, treeNode, MessageSendParams.class));
            default:
                throw new MethodNotFoundJsonMappingException("Invalid method", getIdIfPossible(treeNode, jsonParser));
        }
    }
}
