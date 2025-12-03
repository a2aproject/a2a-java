package io.a2a.grpc.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonSyntaxException;
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

        assertThrows(JsonSyntaxException.class, () -> {
            JSONRPCUtils.parseRequestBody(malformedRequest);
        });
    }

    @Test
    public void testParseInvalidParams_ThrowsInvalidParamsError() {
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
    public void testParseInvalidProtoStructure_ThrowsInvalidParamsError() {
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
