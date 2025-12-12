package io.a2a.grpc.utils;

import static io.a2a.grpc.utils.JSONRPCUtils.ERROR_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.a2a.json.JsonProcessingException;
import com.google.gson.JsonSyntaxException;
import io.a2a.json.JsonMappingException;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidParamsJsonMappingException;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCRequest;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.TaskPushNotificationConfig;
import org.junit.jupiter.api.Test;

public class JSONRPCUtilsTest {

    @Test
    public void testParseSetTaskPushNotificationConfigRequest_ValidProtoFormat() throws JsonProcessingException {
        String validRequest = """
            {
              "jsonrpc": "2.0",
              "method": "SetTaskPushNotificationConfig",
              "id": "1",
              "params": {
                "parent": "tasks/task-123",
                "configId": "config-456",
                "config": {
                  "name": "tasks/task-123/pushNotificationConfigs/config-456",
                  "pushNotificationConfig": {
                    "url": "https://example.com/callback",
                    "authentication": {
                      "schemes": ["jwt"]
                    }
                  }
                }
              }
            }
            """;

        JSONRPCRequest<?> request = JSONRPCUtils.parseRequestBody(validRequest);

        assertNotNull(request);
        assertInstanceOf(SetTaskPushNotificationConfigRequest.class, request);
        SetTaskPushNotificationConfigRequest setRequest = (SetTaskPushNotificationConfigRequest) request;
        assertEquals("2.0", setRequest.getJsonrpc());
        assertEquals(1, setRequest.getId());
        assertEquals("SetTaskPushNotificationConfig", setRequest.getMethod());

        TaskPushNotificationConfig config = setRequest.getParams();
        assertNotNull(config);
        assertEquals("task-123", config.taskId());
        assertNotNull(config.pushNotificationConfig());
        assertEquals("https://example.com/callback", config.pushNotificationConfig().url());
    }

    @Test
    public void testParseGetTaskPushNotificationConfigRequest_ValidProtoFormat() throws JsonProcessingException {
        String validRequest = """
            {
              "jsonrpc": "2.0",
              "method": "GetTaskPushNotificationConfig",
              "id": "2",
              "params": {
                "name": "tasks/task-123/pushNotificationConfigs/config-456"
              }
            }
            """;

        JSONRPCRequest<?> request = JSONRPCUtils.parseRequestBody(validRequest);

        assertNotNull(request);
        assertInstanceOf(GetTaskPushNotificationConfigRequest.class, request);
        GetTaskPushNotificationConfigRequest getRequest = (GetTaskPushNotificationConfigRequest) request;
        assertEquals("2.0", getRequest.getJsonrpc());
        assertEquals(2, getRequest.getId());
        assertEquals("GetTaskPushNotificationConfig", getRequest.getMethod());
        assertNotNull(getRequest.getParams());
        assertEquals("task-123", getRequest.getParams().id());
    }

    @Test
    public void testParseMalformedJSON_ThrowsJsonSyntaxException() {
        String malformedRequest = """
            {
              "jsonrpc": "2.0",
              "method": "SetTaskPushNotificationConfig",
              "params": {
                "parent": "tasks/task-123"
            """; // Missing closing braces

        JsonSyntaxException exception = assertThrows(JsonSyntaxException.class, () -> {
            JSONRPCUtils.parseRequestBody(malformedRequest);
        });
        assertEquals("java.io.EOFException: End of input at line 6 column 1 path $.params.parent", exception.getMessage());
    }

    @Test
    public void testParseInvalidParams_ThrowsInvalidParamsJsonMappingException() {
        String invalidParamsRequest = """
            {
              "jsonrpc": "2.0",
              "method": "SetTaskPushNotificationConfig",
              "id": "3",
              "params": "not_a_dict"
            }
            """;

        InvalidParamsJsonMappingException exception = assertThrows(
            InvalidParamsJsonMappingException.class,
            () -> JSONRPCUtils.parseRequestBody(invalidParamsRequest)
        );
        assertEquals(3, exception.getId());
    }

    @Test
    public void testParseInvalidProtoStructure_ThrowsInvalidParamsJsonMappingException() {
        String invalidStructure = """
            {
              "jsonrpc": "2.0",
              "method": "SetTaskPushNotificationConfig",
              "id": "4",
              "params": {
                "invalid_field": "value"
              }
            }
            """;

        InvalidParamsJsonMappingException exception = assertThrows(
            InvalidParamsJsonMappingException.class,
            () -> JSONRPCUtils.parseRequestBody(invalidStructure)
        );
        assertEquals(4, exception.getId());
        assertEquals(ERROR_MESSAGE.formatted("invalid_field in message a2a.v1.SetTaskPushNotificationConfigRequest"), exception.getMessage());
    }

    @Test
    public void testParseMissingField_ThrowsInvalidParamsError() throws JsonMappingException {
        String missingRoleMessage=  """
           {
              "jsonrpc":"2.0",
              "method":"SendMessage",
              "id": "18",
              "params":{
                "message":{
                  "messageId":"message-1234",
                  "contextId":"context-1234",
                  "parts":[
                    {
                      "text":"tell me a joke"
                    }
                  ],
                  "metadata":{
                    
                  }
                }
              }
            }""";
        InvalidParamsJsonMappingException exception = assertThrows(
            InvalidParamsJsonMappingException.class,
            () -> JSONRPCUtils.parseRequestBody(missingRoleMessage)
        );
        assertEquals(18, exception.getId());
    }
    @Test
    public void testParseUnknownField_ThrowsJsonMappingException() throws JsonMappingException {
        String unkownFieldMessage=  """
           {
              "jsonrpc":"2.0",
              "method":"SendMessage",
              "id": "18",
              "params":{
                "message":{
                  "role": "ROLE_AGENT",
                  "unknown":"field",
                  "messageId":"message-1234",
                  "contextId":"context-1234",
                  "parts":[
                    {
                      "text":"tell me a joke"
                    }
                  ],
                  "metadata":{
                    
                  }
                }
              }
            }""";
        JsonMappingException exception = assertThrows(
            JsonMappingException.class,
            () -> JSONRPCUtils.parseRequestBody(unkownFieldMessage)
        );
        assertEquals(ERROR_MESSAGE.formatted("unknown in message a2a.v1.Message"), exception.getMessage());
    }

    @Test
    public void testParseInvalidTypeWithNullId_UsesEmptyStringSentinel() throws Exception {
        // Test the low-level convertProtoBufExceptionToJsonProcessingException with null ID
        // This tests the sentinel value logic directly
        com.google.protobuf.InvalidProtocolBufferException protoException =
            new com.google.protobuf.InvalidProtocolBufferException("Expected ENUM but found \"INVALID_VALUE\"");

        // Use reflection to call the private method for testing
        java.lang.reflect.Method method = JSONRPCUtils.class.getDeclaredMethod(
            "convertProtoBufExceptionToJsonProcessingException",
            com.google.protobuf.InvalidProtocolBufferException.class,
            Object.class
        );
        method.setAccessible(true);

        JsonProcessingException exception = (JsonProcessingException) method.invoke(
            null,
            protoException,
            null  // null ID
        );

        // Should be InvalidParamsJsonMappingException with empty string sentinel
        assertInstanceOf(InvalidParamsJsonMappingException.class, exception,
            "Expected InvalidParamsJsonMappingException for proto error");
        InvalidParamsJsonMappingException invalidParamsException = (InvalidParamsJsonMappingException) exception;
        assertEquals("", invalidParamsException.getId(),
            "Expected empty string sentinel when ID is null");
    }

    @Test
    public void testParseInvalidTypeWithValidId_PreservesId() throws Exception {
        // Test the low-level convertProtoBufExceptionToJsonProcessingException with valid ID
        com.google.protobuf.InvalidProtocolBufferException protoException =
            new com.google.protobuf.InvalidProtocolBufferException("Expected ENUM but found \"INVALID_VALUE\"");

        // Use reflection to call the private method for testing
        java.lang.reflect.Method method = JSONRPCUtils.class.getDeclaredMethod(
            "convertProtoBufExceptionToJsonProcessingException",
            com.google.protobuf.InvalidProtocolBufferException.class,
            Object.class
        );
        method.setAccessible(true);

        JsonProcessingException exception = (JsonProcessingException) method.invoke(
            null,
            protoException,
            42  // Valid ID
        );

        // Should be InvalidParamsJsonMappingException with preserved ID
        assertInstanceOf(InvalidParamsJsonMappingException.class, exception,
            "Expected InvalidParamsJsonMappingException for proto error");
        InvalidParamsJsonMappingException invalidParamsException = (InvalidParamsJsonMappingException) exception;
        assertEquals(42, invalidParamsException.getId(),
            "Expected actual ID to be preserved when present");
    }

    @Test
    public void testGenerateSetTaskPushNotificationConfigResponse_Success() throws Exception {
        TaskPushNotificationConfig config = new TaskPushNotificationConfig(
            "task-123",
            new io.a2a.spec.PushNotificationConfig.Builder()
                .url("https://example.com/callback")
                .id("config-456")
                .build()
        );

        String responseJson = """
            {
              "jsonrpc": "2.0",
              "id": "1",
              "result": {
                "name": "tasks/task-123/pushNotificationConfigs/config-456",
                "pushNotificationConfig": {
                  "url": "https://example.com/callback",
                  "id": "config-456"
                }
              }
            }
            """;

        SetTaskPushNotificationConfigResponse response =
            (SetTaskPushNotificationConfigResponse) JSONRPCUtils.parseResponseBody(responseJson, SetTaskPushNotificationConfigRequest.METHOD);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertNotNull(response.getResult());
        assertEquals("task-123", response.getResult().taskId());
        assertEquals("https://example.com/callback", response.getResult().pushNotificationConfig().url());
    }

    @Test
    public void testGenerateGetTaskPushNotificationConfigResponse_Success() throws Exception {
        String responseJson = """
            {
              "jsonrpc": "2.0",
              "id": "2",
              "result": {
                "name": "tasks/task-123/pushNotificationConfigs/config-456",
                "pushNotificationConfig": {
                  "url": "https://example.com/callback",
                  "id": "config-456"
                }
              }
            }
            """;

        GetTaskPushNotificationConfigResponse response =
            (GetTaskPushNotificationConfigResponse) JSONRPCUtils.parseResponseBody(responseJson, GetTaskPushNotificationConfigRequest.METHOD);

        assertNotNull(response);
        assertEquals(2, response.getId());
        assertNotNull(response.getResult());
        assertEquals("task-123", response.getResult().taskId());
        assertEquals("https://example.com/callback", response.getResult().pushNotificationConfig().url());
    }

    @Test
    public void testParseErrorResponse_InvalidParams() throws Exception {
        String errorResponse = """
            {
              "jsonrpc": "2.0",
              "id": "5",
              "error": {
                "code": -32602,
                "message": "Invalid params"
              }
            }
            """;

        SetTaskPushNotificationConfigResponse response =
            (SetTaskPushNotificationConfigResponse) JSONRPCUtils.parseResponseBody(errorResponse, SetTaskPushNotificationConfigRequest.METHOD);

        assertNotNull(response);
        assertEquals(5, response.getId());
        assertNotNull(response.getError());
        assertInstanceOf(InvalidParamsError.class, response.getError());
        assertEquals(-32602, response.getError().getCode());
        assertEquals("Invalid params", response.getError().getMessage());
    }

    @Test
    public void testParseErrorResponse_ParseError() throws Exception {
        String errorResponse = """
            {
              "jsonrpc": "2.0",
              "id": 6,
              "error": {
                "code": -32700,
                "message": "Parse error"
              }
            }
            """;

        SetTaskPushNotificationConfigResponse response =
            (SetTaskPushNotificationConfigResponse) JSONRPCUtils.parseResponseBody(errorResponse, SetTaskPushNotificationConfigRequest.METHOD);

        assertNotNull(response);
        assertEquals(6, response.getId());
        assertNotNull(response.getError());
        assertInstanceOf(JSONParseError.class, response.getError());
        assertEquals(-32700, response.getError().getCode());
        assertEquals("Parse error", response.getError().getMessage());
    }
}
