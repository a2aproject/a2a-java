package org.a2aproject.sdk.grpc.utils;

import static org.a2aproject.sdk.grpc.utils.JSONRPCUtils.ERROR_MESSAGE;
import static org.a2aproject.sdk.spec.A2AMethods.CANCEL_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.LIST_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_MESSAGE_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SUBSCRIBE_TO_TASK_METHOD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.a2aproject.sdk.grpc.SendMessageResponse;
import org.a2aproject.sdk.jsonrpc.common.json.InvalidParamsJsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2ARequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CancelTaskRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTaskPushNotificationConfigsRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendMessageRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SubscribeToTaskRequest;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.JSONParseError;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.TaskNotFoundError;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.spec.util.ErrorDetail;
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
                "tenant": "",
                "id": "config-456",
                "url": "https://example.com/callback",
                "authentication": {
                  "scheme": "jwt"
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
        assertEquals("https://example.com/callback", config.url());
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
        assertEquals("task-123", getRequest.getParams().taskId());
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
        assertEquals(ERROR_MESSAGE.formatted("invalid_field in message lf.a2a.v1.TaskPushNotificationConfig"), exception.getMessage());
    }

    @Test
    public void testParseNumericalTimestampThrowsInvalidParamsJsonMappingException() {
        String validRequest = """
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
            A2ARequest<?> request = JSONRPCUtils.parseRequestBody(validRequest, null);
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
        String unknownFieldMessage=  """
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
            () -> JSONRPCUtils.parseRequestBody(unknownFieldMessage, null)
        );
        assertEquals(ERROR_MESSAGE.formatted("unknown in message lf.a2a.v1.Message"), exception.getMessage());
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
        String responseJson = """
            {
              "jsonrpc": "2.0",
              "id": 1,
              "result": {
                "tenant": "tenant",
                "taskId": "task-123",
                "id": "config-456",
                "url": "https://example.com/callback"
              }
            }
            """;

        CreateTaskPushNotificationConfigResponse response =
            (CreateTaskPushNotificationConfigResponse) JSONRPCUtils.parseResponseBody(responseJson, SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertNotNull(response.getResult());
        assertEquals("task-123", response.getResult().taskId());
        assertEquals("https://example.com/callback", response.getResult().url());
    }

    @Test
    public void testGenerateGetTaskPushNotificationConfigResponse_Success() throws Exception {
        String responseJson = """
            {
              "jsonrpc": "2.0",
              "id": 2,
              "result": {
                "tenant": "tenant",
                "taskId": "task-123",
                "id": "config-456",
                "url": "https://example.com/callback"
              }
            }
            """;

        GetTaskPushNotificationConfigResponse response =
            (GetTaskPushNotificationConfigResponse) JSONRPCUtils.parseResponseBody(responseJson, GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

        assertNotNull(response);
        assertEquals(2, response.getId());
        assertNotNull(response.getResult());
        assertEquals("task-123", response.getResult().taskId());
        assertEquals("https://example.com/callback", response.getResult().url());
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

    @Test
    public void testToJsonRPCErrorResponse_KnownErrorCode_ProducesDataArray() {
        TaskNotFoundError error = new TaskNotFoundError();

        String json = JSONRPCUtils.toJsonRPCErrorResponse("req-1", error);

        var jsonObject = JsonParser.parseString(json).getAsJsonObject();
        var errorObj = jsonObject.getAsJsonObject("error");
        assertTrue(errorObj.has("data"), "error should have a 'data' field");
        assertTrue(errorObj.get("data").isJsonArray(), "'data' field should be a JSON array");
        JsonArray dataArray = errorObj.getAsJsonArray("data");
        assertEquals(1, dataArray.size());
        var detail = dataArray.get(0).getAsJsonObject();
        assertEquals(ErrorDetail.ERROR_INFO_TYPE, detail.get("@type").getAsString());
        assertEquals("TASK_NOT_FOUND", detail.get("reason").getAsString());
        assertEquals(ErrorDetail.ERROR_DOMAIN, detail.get("domain").getAsString());
    }

    @Test
    public void testProcessError_ArrayFormData_ExtractsFirstElement() throws Exception {
        String errorResponse = """
            {
              "jsonrpc": "2.0",
              "id": "8",
              "error": {
                "code": -32001,
                "message": "Task not found",
                "data": [
                  {
                    "@type": "type.googleapis.com/google.rpc.ErrorInfo",
                    "reason": "TASK_NOT_FOUND",
                    "domain": "a2a-protocol.org",
                    "metadata": {}
                  }
                ]
              }
            }
            """;

        CreateTaskPushNotificationConfigResponse response =
            (CreateTaskPushNotificationConfigResponse) JSONRPCUtils.parseResponseBody(errorResponse, SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

        assertNotNull(response);
        assertInstanceOf(TaskNotFoundError.class, response.getError());
        assertEquals(-32001, response.getError().getCode());
        assertEquals("Task not found", response.getError().getMessage());
    }

    @Test
    public void testProcessError_ArrayFormData_NonObjectElement_DoesNotThrow() throws Exception {
        // Verifies that a non-object first array element does not cause a ClassCastException
        String errorResponse = """
            {
              "jsonrpc": "2.0",
              "id": "9",
              "error": {
                "code": -32001,
                "message": "Task not found",
                "data": ["unexpected-string-element"]
              }
            }
            """;

        CreateTaskPushNotificationConfigResponse response =
            (CreateTaskPushNotificationConfigResponse) JSONRPCUtils.parseResponseBody(errorResponse, SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

        assertNotNull(response);
        assertInstanceOf(TaskNotFoundError.class, response.getError());
        // details should be empty since the array element was not an object
        assertTrue(response.getError().getDetails().isEmpty());
    }

    @Test
    public void testToJsonRPCErrorResponse_RoundTrip() throws Exception {
        TaskNotFoundError original = new TaskNotFoundError("Custom message", null);

        String json = JSONRPCUtils.toJsonRPCErrorResponse("req-rt", original);
        CreateTaskPushNotificationConfigResponse response =
            (CreateTaskPushNotificationConfigResponse) JSONRPCUtils.parseResponseBody(
                json,
                SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD);

        assertNotNull(response);
        assertInstanceOf(TaskNotFoundError.class, response.getError());
        assertEquals(-32001, response.getError().getCode());
        assertEquals("Custom message", response.getError().getMessage());
    }

    @Test
    public void testToJsonRPCRequest_doesNotHtmlEscapeAngleBrackets() {
        // Reproduce issue #892: HTML special characters in message text were escaped
        // to Unicode sequences (< for <, > for >) by JsonFormat.printer().
        MessageSendParams params = new MessageSendParams(
            Message.builder()
                .role(Message.Role.ROLE_USER)
                .parts(Collections.singletonList(new TextPart("<event-topic>")))
                .contextId("context-1234")
                .messageId("message-1234")
                .build(),
            null, null
        );
        org.a2aproject.sdk.grpc.SendMessageRequest protoRequest =
            ProtoUtils.ToProto.sendMessageRequest(params);

        String json = JSONRPCUtils.toJsonRPCRequest("test-id", SEND_MESSAGE_METHOD, protoRequest);

        assertTrue(json.contains("<event-topic>"),
            "JSON should preserve literal '<event-topic>' but got: " + json);
        assertFalse(json.contains("\\u003c"),
            "JSON must not contain HTML-escaped '<' (\\u003c) but got: " + json);
        assertFalse(json.contains("\\u003e"),
            "JSON must not contain HTML-escaped '>' (\\u003e) but got: " + json);
    }

    @Test
    public void testToJsonRPCResultResponse_doesNotHtmlEscapeAngleBrackets() {
        // Reproduce issue #892: server responses must also send literal angle brackets.
        Message message = Message.builder()
            .role(Message.Role.ROLE_AGENT)
            .parts(Collections.singletonList(new TextPart("<result>")))
            .contextId("context-1234")
            .messageId("message-5678")
            .build();
        SendMessageResponse protoResponse = ProtoUtils.ToProto.taskOrMessage(message);

        String json = JSONRPCUtils.toJsonRPCResultResponse("test-id", protoResponse);

        assertTrue(json.contains("<result>"),
            "JSON should preserve literal '<result>' but got: " + json);
        assertFalse(json.contains("\\u003c"),
            "JSON must not contain HTML-escaped '<' (\\u003c) but got: " + json);
        assertFalse(json.contains("\\u003e"),
            "JSON must not contain HTML-escaped '>' (\\u003e) but got: " + json);
    }

    // --- Tenant fallback injection tests ---

    @Test
    public void testTenantFallback_SendMessage_usesPathTenantWhenBodyOmitsTenant() throws JsonProcessingException {
        String request = """
            {
              "jsonrpc": "2.0",
              "method": "SendMessage",
              "id": "1",
              "params": {
                "message": {
                  "messageId": "msg-1",
                  "contextId": "ctx-1",
                  "role": "ROLE_USER",
                  "parts": [{"text": "hi"}]
                }
              }
            }
            """;

        A2ARequest<?> parsed = JSONRPCUtils.parseRequestBody(request, "my-org");

        assertInstanceOf(SendMessageRequest.class, parsed);
        assertEquals("my-org", ((SendMessageRequest) parsed).getParams().tenant());
    }

    @Test
    public void testTenantMismatch_urlAndBodyDiffer_throwsInvalidParams() {
        String request = """
            {
              "jsonrpc": "2.0",
              "method": "SendMessage",
              "id": "2",
              "params": {
                "tenant": "body-tenant",
                "message": {
                  "messageId": "msg-2",
                  "contextId": "ctx-2",
                  "role": "ROLE_USER",
                  "parts": [{"text": "hi"}]
                }
              }
            }
            """;

        assertThrows(InvalidParamsJsonMappingException.class,
            () -> JSONRPCUtils.parseRequestBody(request, "path-tenant"));
    }

    @Test
    public void testTenantMatch_urlAndBodySame_succeeds() throws JsonProcessingException {
        String request = """
            {
              "jsonrpc": "2.0",
              "method": "SendMessage",
              "id": "2b",
              "params": {
                "tenant": "acme",
                "message": {
                  "messageId": "msg-2b",
                  "contextId": "ctx-2b",
                  "role": "ROLE_USER",
                  "parts": [{"text": "hi"}]
                }
              }
            }
            """;

        A2ARequest<?> parsed = JSONRPCUtils.parseRequestBody(request, "acme");

        assertInstanceOf(SendMessageRequest.class, parsed);
        assertEquals("acme", ((SendMessageRequest) parsed).getParams().tenant());
    }

    @Test
    public void testTenantFallback_SendMessage_blankBodyTenantUsesPathTenant() throws JsonProcessingException {
        String request = """
            {
              "jsonrpc": "2.0",
              "method": "SendMessage",
              "id": "3",
              "params": {
                "tenant": "",
                "message": {
                  "messageId": "msg-3",
                  "contextId": "ctx-3",
                  "role": "ROLE_USER",
                  "parts": [{"text": "hi"}]
                }
              }
            }
            """;

        A2ARequest<?> parsed = JSONRPCUtils.parseRequestBody(request, "path-tenant");

        assertInstanceOf(SendMessageRequest.class, parsed);
        assertEquals("path-tenant", ((SendMessageRequest) parsed).getParams().tenant());
    }

    @Test
    public void testTenantFallback_SendMessage_noPathTenantAndNoBodyTenant_tenantIsNull() throws JsonProcessingException {
        String request = """
            {
              "jsonrpc": "2.0",
              "method": "SendMessage",
              "id": "4",
              "params": {
                "message": {
                  "messageId": "msg-4",
                  "contextId": "ctx-4",
                  "role": "ROLE_USER",
                  "parts": [{"text": "hi"}]
                }
              }
            }
            """;

        A2ARequest<?> parsed = JSONRPCUtils.parseRequestBody(request, null);

        assertInstanceOf(SendMessageRequest.class, parsed);
        assertNull(((SendMessageRequest) parsed).getParams().tenant());
    }

    @Test
    public void testTenantFallback_GetTask_usesPathTenantWhenBodyOmitsTenant() throws JsonProcessingException {
        String request = """
            {
              "jsonrpc": "2.0",
              "method": "GetTask",
              "id": "5",
              "params": {
                "id": "task-abc"
              }
            }
            """;

        A2ARequest<?> parsed = JSONRPCUtils.parseRequestBody(request, "acme");

        assertInstanceOf(GetTaskRequest.class, parsed);
        assertEquals("acme", ((GetTaskRequest) parsed).getParams().tenant());
    }

    @Test
    public void testTenantFallback_CancelTask_usesPathTenantWhenBodyOmitsTenant() throws JsonProcessingException {
        String request = """
            {
              "jsonrpc": "2.0",
              "method": "CancelTask",
              "id": "6",
              "params": {
                "id": "task-abc"
              }
            }
            """;

        A2ARequest<?> parsed = JSONRPCUtils.parseRequestBody(request, "acme");

        assertInstanceOf(CancelTaskRequest.class, parsed);
        assertEquals("acme", ((CancelTaskRequest) parsed).getParams().tenant());
    }

    @Test
    public void testTenantFallback_ListTasks_usesPathTenantWhenBodyOmitsTenant() throws JsonProcessingException {
        String request = """
            {
              "jsonrpc": "2.0",
              "method": "ListTasks",
              "id": "7",
              "params": {}
            }
            """;

        A2ARequest<?> parsed = JSONRPCUtils.parseRequestBody(request, "acme");

        assertInstanceOf(ListTasksRequest.class, parsed);
        assertEquals("acme", ((ListTasksRequest) parsed).getParams().tenant());
    }

    @Test
    public void testTenantFallback_SubscribeToTask_usesPathTenantWhenBodyOmitsTenant() throws JsonProcessingException {
        String request = """
            {
              "jsonrpc": "2.0",
              "method": "SubscribeToTask",
              "id": "8",
              "params": {
                "id": "task-abc"
              }
            }
            """;

        A2ARequest<?> parsed = JSONRPCUtils.parseRequestBody(request, "acme");

        assertInstanceOf(SubscribeToTaskRequest.class, parsed);
        assertEquals("acme", ((SubscribeToTaskRequest) parsed).getParams().tenant());
    }

    @Test
    public void testTenantFallback_GetTaskPushNotificationConfig_usesPathTenantWhenBodyOmitsTenant() throws JsonProcessingException {
        String request = """
            {
              "jsonrpc": "2.0",
              "method": "GetTaskPushNotificationConfig",
              "id": "9",
              "params": {
                "taskId": "task-abc",
                "id": "cfg-1"
              }
            }
            """;

        A2ARequest<?> parsed = JSONRPCUtils.parseRequestBody(request, "acme");

        assertInstanceOf(GetTaskPushNotificationConfigRequest.class, parsed);
        assertEquals("acme", ((GetTaskPushNotificationConfigRequest) parsed).getParams().tenant());
    }

    @Test
    public void testTenantMismatch_GetTask_urlAndBodyDiffer_throwsInvalidParams() {
        String request = """
            {
              "jsonrpc": "2.0",
              "method": "GetTask",
              "id": "gt-mismatch",
              "params": {
                "id": "task-xyz",
                "tenant": "body-tenant"
              }
            }
            """;

        assertThrows(InvalidParamsJsonMappingException.class,
            () -> JSONRPCUtils.parseRequestBody(request, "path-tenant"));
    }

    @Test
    public void testTenantFallback_SetTaskPushNotificationConfig_usesPathTenantWhenBodyOmitsTenant() throws JsonProcessingException {
        String request = """
            {
              "jsonrpc": "2.0",
              "method": "CreateTaskPushNotificationConfig",
              "id": "10",
              "params": {
                "id": "cfg-1",
                "url": "https://example.com/hook"
              }
            }
            """;

        A2ARequest<?> parsed = JSONRPCUtils.parseRequestBody(request, "acme");

        assertInstanceOf(CreateTaskPushNotificationConfigRequest.class, parsed);
        assertEquals("acme", ((CreateTaskPushNotificationConfigRequest) parsed).getParams().tenant());
    }

    @Test
    public void testTenantFallback_DeleteTaskPushNotificationConfig_usesPathTenantWhenBodyOmitsTenant() throws JsonProcessingException {
        String request = """
            {
              "jsonrpc": "2.0",
              "method": "DeleteTaskPushNotificationConfig",
              "id": "11",
              "params": {
                "taskId": "task-abc",
                "id": "cfg-1"
              }
            }
            """;

        A2ARequest<?> parsed = JSONRPCUtils.parseRequestBody(request, "acme");

        assertInstanceOf(DeleteTaskPushNotificationConfigRequest.class, parsed);
        assertEquals("acme", ((DeleteTaskPushNotificationConfigRequest) parsed).getParams().tenant());
    }

    @Test
    public void testTenantFallback_ListTaskPushNotificationConfigs_usesPathTenantWhenBodyOmitsTenant() throws JsonProcessingException {
        String request = """
            {
              "jsonrpc": "2.0",
              "method": "ListTaskPushNotificationConfigs",
              "id": "12",
              "params": {
                "taskId": "task-abc"
              }
            }
            """;

        A2ARequest<?> parsed = JSONRPCUtils.parseRequestBody(request, "acme");

        assertInstanceOf(ListTaskPushNotificationConfigsRequest.class, parsed);
        assertEquals("acme", ((ListTaskPushNotificationConfigsRequest) parsed).getParams().tenant());
    }

}
