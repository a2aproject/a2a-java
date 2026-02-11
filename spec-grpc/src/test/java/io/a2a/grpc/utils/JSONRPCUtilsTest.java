package io.a2a.grpc.utils;

import static io.a2a.grpc.utils.JSONRPCUtils.ERROR_MESSAGE;
import static io.a2a.spec.A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static io.a2a.spec.A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonSyntaxException;
import io.a2a.jsonrpc.common.json.InvalidParamsJsonMappingException;
import io.a2a.jsonrpc.common.json.JsonMappingException;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.wrappers.A2ARequest;
import io.a2a.jsonrpc.common.wrappers.GetTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.GetTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigResponse;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.TaskPushNotificationConfig;
import org.junit.jupiter.api.Test;

public class JSONRPCUtilsTest {

    @Test
    public void testParseCreateTaskPushNotificationConfigRequest_ValidProtoFormat() throws JsonProcessingException {
        String validRequest = """
            {
              "jsonrpc": "2.0",
              "method": "CreateTaskPushNotificationConfig",
              "id": "1",
              "params": {
                "taskId": "task-123",
                "configId": "config-456",
                "tenant": "",
                "config": {
                  "url": "https://example.com/callback",
                  "authentication": {
                    "scheme": "jwt"
                  }
                }
              }
            }
            """;

        A2ARequest<?> request = JSONRPCUtils.parseRequestBody(validRequest, null);

        assertNotNull(request);
        assertInstanceOf(CreateTaskPushNotificationConfigRequest.class, request);
        CreateTaskPushNotificationConfigRequest setRequest = (CreateTaskPushNotificationConfigRequest) request;
        assertEquals("2.0", setRequest.getJsonrpc());
        assertEquals(1, setRequest.getId());
        assertEquals("CreateTaskPushNotificationConfig", setRequest.getMethod());

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
                "taskId": "task-123",
                "id": "config-456"
              }
            }
            """;

        A2ARequest<?> request = JSONRPCUtils.parseRequestBody(validRequest, null);

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
              "method": "CreateTaskPushNotificationConfig",
              "params": {
                "parent": "tasks/task-123"
            """; // Missing closing braces

        JsonSyntaxException exception = assertThrows(JsonSyntaxException.class, () -> {
            JSONRPCUtils.parseRequestBody(malformedRequest, null);
        });
        assertEquals("java.io.EOFException: End of input at line 6 column 1 path $.params.parent", exception.getMessage());
    }

    @Test
    public void testParseInvalidParams_ThrowsInvalidParamsJsonMappingException() {
        String invalidParamsRequest = """
            {
              "jsonrpc": "2.0",
              "method": "CreateTaskPushNotificationConfig",
              "id": "3",
              "params": "not_a_dict"
            }
            """;

        InvalidParamsJsonMappingException exception = assertThrows(
            InvalidParamsJsonMappingException.class,
            () -> JSONRPCUtils.parseRequestBody(invalidParamsRequest, null)
        );
        assertEquals(3, exception.getId());
    }

    @Test
    public void testParseInvalidProtoStructure_ThrowsInvalidParamsJsonMappingException() {
        String invalidStructure = """
            {
              "jsonrpc": "2.0",
              "method": "CreateTaskPushNotificationConfig",
              "id": "4",
              "params": {
                "invalid_field": "value"
              }
            }
            """;

        InvalidParamsJsonMappingException exception = assertThrows(
            InvalidParamsJsonMappingException.class,
            () -> JSONRPCUtils.parseRequestBody(invalidStructure, null)
        );
        assertEquals(4, exception.getId());
        assertEquals(ERROR_MESSAGE.formatted("invalid_field in message a2a.v1.CreateTaskPushNotificationConfigRequest"), exception.getMessage());
    }

    @Test
    public void testParseNumericalTimestampThrowsInvalidParamsJsonMappingException() {
        String valideRequest = """
            {
              "jsonrpc": "2.0",
              "method": "ListTasks",
              "id": "1",
              "params": {
                "statusTimestampAfter": "2023-10-27T10:00:00Z"
              }
            }
            """;
        String invalidRequest = """
            {
              "jsonrpc": "2.0",
              "method": "ListTasks",
              "id": "2",
              "params": {
                "statusTimestampAfter": "1"
              }
            }
            """;

        try {
            A2ARequest<?> request = JSONRPCUtils.parseRequestBody(valideRequest, null);
            assertEquals(1, request.getId());
        } catch (JsonProcessingException e) {
            fail(e);
        }
        InvalidParamsJsonMappingException exception = assertThrows(
                InvalidParamsJsonMappingException.class,
                () -> JSONRPCUtils.parseRequestBody(invalidRequest, null)
        );
        assertEquals(2, exception.getId());
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
            () -> JSONRPCUtils.parseRequestBody(missingRoleMessage, null)
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
            () -> JSONRPCUtils.parseRequestBody(unkownFieldMessage, null)
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
    public void testGenerateCreateTaskPushNotificationConfigResponse_Success() throws Exception {
        TaskPushNotificationConfig config = new TaskPushNotificationConfig(
            "task-123",
            PushNotificationConfig.builder()
                .url("https://example.com/callback")
                .id("config-456")
                .build(),
                "tenant"
        );

        String responseJson = """
            {
              "jsonrpc": "2.0",
              "id": "1",
              "result": {
                "taskId": "task-123",
                "id": "config-456",
                "pushNotificationConfig": {
                  "url": "https://example.com/callback",
                  "id": "config-456"
                }
              }
            }
            """;

        CreateTaskPushNotificationConfigResponse response =
            (CreateTaskPushNotificationConfigResponse) JSONRPCUtils.parseResponseBody(responseJson, SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

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
                "taskId": "task-123",
                "id": "config-456",
                "pushNotificationConfig": {
                  "url": "https://example.com/callback",
                  "id": "config-456"
                }
              }
            }
            """;

        GetTaskPushNotificationConfigResponse response =
            (GetTaskPushNotificationConfigResponse) JSONRPCUtils.parseResponseBody(responseJson, GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

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

        CreateTaskPushNotificationConfigResponse response =
            (CreateTaskPushNotificationConfigResponse) JSONRPCUtils.parseResponseBody(errorResponse, SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

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

        CreateTaskPushNotificationConfigResponse response =
            (CreateTaskPushNotificationConfigResponse) JSONRPCUtils.parseResponseBody(errorResponse, SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

        assertNotNull(response);
        assertEquals(6, response.getId());
        assertNotNull(response.getError());
        assertInstanceOf(JSONParseError.class, response.getError());
        assertEquals(-32700, response.getError().getCode());
        assertEquals("Parse error", response.getError().getMessage());
    }
}
