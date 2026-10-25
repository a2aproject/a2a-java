package org.a2aproject.sdk.server.apps.quarkus;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Collections.singletonList;
import static org.a2aproject.sdk.spec.A2AMethods.CANCEL_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_MESSAGE_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SEND_STREAMING_MESSAGE_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD;
import static org.a2aproject.sdk.spec.A2AMethods.SUBSCRIBE_TO_TASK_METHOD;
import static org.a2aproject.sdk.transport.jsonrpc.context.JSONRPCContextKeys.METHOD_NAME_KEY;
import static org.a2aproject.sdk.transport.jsonrpc.context.JSONRPCContextKeys.TENANT_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import jakarta.enterprise.inject.Instance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CancelTaskRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CancelTaskResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetExtendedAgentCardRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetExtendedAgentCardResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTaskPushNotificationConfigsRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTaskPushNotificationConfigsResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendMessageRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendMessageResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendStreamingMessageRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SendStreamingMessageResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.SubscribeToTaskRequest;
import org.a2aproject.sdk.server.AgentCardCacheMetadata;
import org.a2aproject.sdk.server.FixedInstance;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.multitenancy.AgentCardRouter;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AuthenticationInfo;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.transport.jsonrpc.handler.JSONRPCHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/**
 * Unit test for JSON-RPC A2AServerRoutes that verifies the method names are properly set
 * in the ServerCallContext for all request types.
 */
public class A2AServerRoutesTest {

    private A2AServerRoutes routes;
    private JSONRPCHandler mockJsonRpcHandler;
    private Executor mockExecutor;
    private Instance<CallContextFactory> mockCallContextFactory;
    private RoutingContext mockRoutingContext;
    private HttpServerRequest mockRequest;
    private HttpServerResponse mockHttpResponse;
    private MultiMap mockHeaders;
    private RequestBody mockRequestBody;

    @BeforeEach
    public void setUp() {
        routes = new A2AServerRoutes();
        mockJsonRpcHandler = mock(JSONRPCHandler.class);
        mockExecutor = mock(Executor.class);
        mockCallContextFactory = mock(Instance.class);
        mockRoutingContext = mock(RoutingContext.class);
        mockRequest = mock(HttpServerRequest.class);
        mockHttpResponse = mock(HttpServerResponse.class);
        mockHeaders = MultiMap.caseInsensitiveMultiMap();
        mockRequestBody = mock(RequestBody.class);

        // Inject mocks via reflection since we can't use @InjectMocks
        setField(routes, "jsonRpcHandler", mockJsonRpcHandler);
        setField(routes, "executor", mockExecutor);
        setField(routes, "callContextFactory", mockCallContextFactory);
        setField(routes, "cacheMetadata", mock(AgentCardCacheMetadata.class));

        // Setup common mock behavior
        when(mockCallContextFactory.isUnsatisfied()).thenReturn(true);
        when(mockRoutingContext.request()).thenReturn(mockRequest);
        when(mockRoutingContext.response()).thenReturn(mockHttpResponse);
        when(mockRoutingContext.user()).thenReturn(null);
        when(mockRequest.headers()).thenReturn(mockHeaders);
        when(mockRoutingContext.body()).thenReturn(mockRequestBody);
        when(mockRoutingContext.normalizedPath()).thenReturn("/");

        // Chain the response methods properly
        when(mockHttpResponse.setStatusCode(any(Integer.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.putHeader(any(CharSequence.class), any(CharSequence.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.end(anyString())).thenReturn(null);
        when(mockHttpResponse.setChunked(any(Boolean.class))).thenReturn(mockHttpResponse);
        when(mockHttpResponse.headers()).thenReturn(mockHeaders);
    }

    @Test
    public void testSendMessage_MethodNameSetInContext() {
        // Arrange - using protobuf JSON format
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "SendMessage",
             "params": {
              "message": {
               "messageId": "message-1234",
               "contextId": "context-1234",
               "role": "ROLE_USER",
               "parts": [
                {
                 "text": "tell me a joke"
                }
               ],
               "metadata": {}
              },
              "configuration": {
                "acceptedOutputModes": ["text"],
                 "returnImmediately": false
              },
              "metadata": {}
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with a Task
        Task responseTask = Task.builder()
                .id("task-123")
                .contextId("context-1234")
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build();
        SendMessageResponse realResponse = new SendMessageResponse("1", responseTask);
        when(mockJsonRpcHandler.onMessageSend(any(SendMessageRequest.class), any(ServerCallContext.class)))
                .thenReturn(realResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onMessageSend(any(SendMessageRequest.class), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SEND_MESSAGE_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, APPLICATION_JSON);
    }

    @Test
    public void testSendStreamingMessage_MethodNameSetInContext() {
        // Arrange - using protobuf JSON format
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "SendStreamingMessage",
             "params": {
              "message": {
               "messageId": "message-1234",
               "contextId": "context-1234",
               "role": "ROLE_USER",
               "parts": [
                {
                 "text": "tell me a joke"
                }
               ],
               "metadata": {}
              },
              "configuration": {
                "acceptedOutputModes": ["text"],
                "returnImmediately": false
              },
              "metadata": {}
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        @SuppressWarnings("unchecked")
        Flow.Publisher<SendStreamingMessageResponse> mockPublisher = mock(Flow.Publisher.class);
        when(mockJsonRpcHandler.onMessageSendStream(any(SendStreamingMessageRequest.class),
                any(ServerCallContext.class))).thenReturn(mockPublisher);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onMessageSendStream(any(SendStreamingMessageRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SEND_STREAMING_MESSAGE_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testGetTask_MethodNameSetInContext() {
        // Arrange - using protobuf JSON format
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "GetTask",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "historyLength": 10
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with a Task
        Task responseTask = Task.builder()
                .id("de38c76d-d54c-436c-8b9f-4c2703648d64")
                .contextId("context-1234")
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build();
        GetTaskResponse realResponse = new GetTaskResponse("1", responseTask);
        when(mockJsonRpcHandler.onGetTask(any(GetTaskRequest.class), any(ServerCallContext.class)))
                .thenReturn(realResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onGetTask(any(GetTaskRequest.class), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(GET_TASK_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, APPLICATION_JSON);
    }

    @Test
    public void testGetTask_ForwardsTransportPolicyErrorAndResponseId() {
        String jsonRpcRequest = """
                {"jsonrpc":"2.0","id":"request-id","method":"GetTask","params":{"id":"task"}}
                """;
        GetTaskResponse policyResponse = new GetTaskResponse("policy-error-id",
                new org.a2aproject.sdk.spec.UnsupportedOperationError());
        when(mockJsonRpcHandler.onGetTask(any(GetTaskRequest.class), any(ServerCallContext.class)))
                .thenReturn(policyResponse);

        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        verify(mockJsonRpcHandler).onGetTask(any(GetTaskRequest.class), any(ServerCallContext.class));
        JsonObject response = captureResponse();
        assertEquals("policy-error-id", response.get("id").getAsString());
        assertTrue(response.has("error"));
    }

    @Test
    public void testCancelTask_MethodNameSetInContext() {
        // Arrange - using protobuf JSON format
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "CancelTask",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64"
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with a Task
        Task responseTask = Task.builder()
                .id("de38c76d-d54c-436c-8b9f-4c2703648d64")
                .contextId("context-1234")
                .status(new TaskStatus(TaskState.TASK_STATE_CANCELED))
                .build();
        CancelTaskResponse realResponse = new CancelTaskResponse("1", responseTask);
        when(mockJsonRpcHandler.onCancelTask(any(CancelTaskRequest.class), any(ServerCallContext.class)))
                .thenReturn(realResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onCancelTask(any(CancelTaskRequest.class), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(CANCEL_TASK_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, APPLICATION_JSON);
    }

    @Test
    public void testTaskResubscription_MethodNameSetInContext() {
        // Arrange - using protobuf JSON format
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "SubscribeToTask",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64"
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        @SuppressWarnings("unchecked")
        Flow.Publisher<SendStreamingMessageResponse> mockPublisher = mock(Flow.Publisher.class);
        when(mockJsonRpcHandler.onSubscribeToTask(any(SubscribeToTaskRequest.class),
                any(ServerCallContext.class))).thenReturn(mockPublisher);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onSubscribeToTask(any(SubscribeToTaskRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SUBSCRIBE_TO_TASK_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testCreateTaskPushNotificationConfig_MethodNameSetInContext() {
        // Arrange - using protobuf JSON format
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "CreateTaskPushNotificationConfig",
             "params": {
              "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "id": "config-123",
              "url": "https://example.com/callback",
              "authentication": {
               "scheme": "jwt"
              }
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with a TaskPushNotificationConfig
        TaskPushNotificationConfig responseConfig = TaskPushNotificationConfig.builder()
                .id("config-123")
                .taskId("de38c76d-d54c-436c-8b9f-4c2703648d64")
                .url("https://example.com/callback")
                .authentication(new AuthenticationInfo("jwt", null))
                .tenant("tenant")
                .build();

        CreateTaskPushNotificationConfigResponse realResponse = new CreateTaskPushNotificationConfigResponse("1", responseConfig);
        when(mockJsonRpcHandler.setPushNotificationConfig(any(CreateTaskPushNotificationConfigRequest.class),
                any(ServerCallContext.class))).thenReturn(realResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).setPushNotificationConfig(any(CreateTaskPushNotificationConfigRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, APPLICATION_JSON);
    }

    @Test
    public void testGetTaskPushNotificationConfig_MethodNameSetInContext() {
        // Arrange - using protobuf JSON format
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "GetTaskPushNotificationConfig",
             "params": {
              "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "id": "config-456"
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with a TaskPushNotificationConfig
        TaskPushNotificationConfig responseConfig = TaskPushNotificationConfig.builder()
                .id("config-456")
                .taskId("de38c76d-d54c-436c-8b9f-4c2703648d64")
                .url("https://example.com/callback")
                .build();
        GetTaskPushNotificationConfigResponse realResponse = new GetTaskPushNotificationConfigResponse("1", responseConfig);
        when(mockJsonRpcHandler.getPushNotificationConfig(any(GetTaskPushNotificationConfigRequest.class),
                any(ServerCallContext.class))).thenReturn(realResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).getPushNotificationConfig(any(GetTaskPushNotificationConfigRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(GET_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, APPLICATION_JSON);
    }

    @Test
    public void testListTaskPushNotificationConfigs_MethodNameSetInContext() {
        // Arrange - using protobuf JSON format
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "ListTaskPushNotificationConfigs",
             "params": {
              "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "pageSize": 0,
              "pageToken": ""
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with a list of TaskPushNotificationConfig
        TaskPushNotificationConfig config = TaskPushNotificationConfig.builder()
                .id("config-123")
                .taskId("de38c76d-d54c-436c-8b9f-4c2703648d64")
                .url("https://example.com/callback")
                .build();
        ListTaskPushNotificationConfigsResponse realResponse = new ListTaskPushNotificationConfigsResponse("1", new ListTaskPushNotificationConfigsResult(singletonList(config)));
        when(mockJsonRpcHandler.listPushNotificationConfigs(any(ListTaskPushNotificationConfigsRequest.class),
                any(ServerCallContext.class))).thenReturn(realResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).listPushNotificationConfigs(any(ListTaskPushNotificationConfigsRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(LIST_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, APPLICATION_JSON);
    }

    @Test
    public void testDeleteTaskPushNotificationConfig_MethodNameSetInContext() {
        // Arrange - using protobuf JSON format
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "DeleteTaskPushNotificationConfig",
             "params": {
              "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "id": "config-456"
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with id
        DeleteTaskPushNotificationConfigResponse realResponse = new DeleteTaskPushNotificationConfigResponse("1");
        when(mockJsonRpcHandler.deletePushNotificationConfig(any(DeleteTaskPushNotificationConfigRequest.class),
                any(ServerCallContext.class))).thenReturn(realResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).deletePushNotificationConfig(any(DeleteTaskPushNotificationConfigRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(DELETE_TASK_PUSH_NOTIFICATION_CONFIG_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, APPLICATION_JSON);
    }

    @Test
    public void testGetExtendedCard_MethodNameSetInContext() {
        // Arrange
        String jsonRpcRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"5\",\"method\":\"" + GET_EXTENDED_AGENT_CARD_METHOD
                + "\",\"id\":1}";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with an AgentCard
        AgentCard agentCard = AgentCard.builder()
                .name("Test Agent")
                .description("Test agent description")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.emptyList())
                .supportedInterfaces(Collections.singletonList(new AgentInterface("jsonrpc", "http://localhost:9999")))
                .build();
        GetExtendedAgentCardResponse realResponse = new GetExtendedAgentCardResponse(1, agentCard);
        when(mockJsonRpcHandler.onGetExtendedCardRequest(
                any(GetExtendedAgentCardRequest.class), any(ServerCallContext.class)))
                .thenReturn(realResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onGetExtendedCardRequest(
                any(GetExtendedAgentCardRequest.class), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(GET_EXTENDED_AGENT_CARD_METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, APPLICATION_JSON);
    }

    @Test
    public void testTenantExtraction_MultiSegmentPath_Rejected() {
        // Arrange - multi-segment tenant paths (containing '/') are rejected
        when(mockRoutingContext.normalizedPath()).thenReturn("/test/titi");
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "GetTask",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "historyLength": 10
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert - handler is NOT called; an error response is returned
        verify(mockJsonRpcHandler, never()).onGetTask(any(GetTaskRequest.class), any(ServerCallContext.class));
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, APPLICATION_JSON);
        verify(mockHttpResponse).end(anyString());
    }

    @Test
    public void testTenantExtraction_RootPath() {
        // Arrange - simulate request to /
        when(mockRoutingContext.normalizedPath()).thenReturn("/");
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "GetTask",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "historyLength": 10
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        Task responseTask = Task.builder()
                .id("de38c76d-d54c-436c-8b9f-4c2703648d64")
                .contextId("context-1234")
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build();
        GetTaskResponse realResponse = new GetTaskResponse("1", responseTask);
        when(mockJsonRpcHandler.onGetTask(any(GetTaskRequest.class), any(ServerCallContext.class)))
                .thenReturn(realResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onGetTask(any(GetTaskRequest.class), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals("", capturedContext.getState().get(TENANT_KEY));
    }

    @Test
    public void testTenantExtraction_SingleSegmentPath() {
        // Arrange - simulate request to /tenant1
        when(mockRoutingContext.normalizedPath()).thenReturn("/tenant1");
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "GetTask",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "historyLength": 10
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        Task responseTask = Task.builder()
                .id("de38c76d-d54c-436c-8b9f-4c2703648d64")
                .contextId("context-1234")
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build();
        GetTaskResponse realResponse = new GetTaskResponse("1", responseTask);
        when(mockJsonRpcHandler.onGetTask(any(GetTaskRequest.class), any(ServerCallContext.class)))
                .thenReturn(realResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onGetTask(any(GetTaskRequest.class), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals("tenant1", capturedContext.getState().get(TENANT_KEY));
    }

    @Test
    public void testTenantExtraction_ThreeSegmentPath_Rejected() {
        // Arrange - three-segment tenant paths (containing '/') are rejected
        when(mockRoutingContext.normalizedPath()).thenReturn("/tenant1/api/v1");
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "GetTask",
             "params": {
              "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
              "historyLength": 10
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert - handler is NOT called; an error response is returned
        verify(mockJsonRpcHandler, never()).onGetTask(any(GetTaskRequest.class), any(ServerCallContext.class));
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, APPLICATION_JSON);
        verify(mockHttpResponse).end(anyString());
    }

    @Test
    public void testTenantExtraction_StreamingRequest() {
        // Arrange - simulate streaming request to /myTenant
        when(mockRoutingContext.normalizedPath()).thenReturn("/myTenant");
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "SendStreamingMessage",
             "params": {
              "message": {
               "messageId": "message-1234",
               "contextId": "context-1234",
               "role": "ROLE_USER",
               "parts": [
                {
                 "text": "tell me a joke"
                }
               ],
               "metadata": {}
              },
              "configuration": {
                "acceptedOutputModes": ["text"],
                "returnImmediately": false
              },
              "metadata": {}
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        @SuppressWarnings("unchecked")
        Flow.Publisher<SendStreamingMessageResponse> mockPublisher = mock(Flow.Publisher.class);
        when(mockJsonRpcHandler.onMessageSendStream(any(SendStreamingMessageRequest.class),
                any(ServerCallContext.class))).thenReturn(mockPublisher);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onMessageSendStream(any(SendStreamingMessageRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals("myTenant", capturedContext.getState().get(TENANT_KEY));
    }

    @Test
    public void testJsonParseError_ContentTypeIsApplicationJson() {
        // Arrange - invalid JSON
        String invalidJson = "not valid json {{{";
        when(mockRequestBody.asString()).thenReturn(invalidJson);

        // Act
        routes.invokeJSONRPCHandler(invalidJson, mockRoutingContext);

        // Assert
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, APPLICATION_JSON);
    }

    @Test
    public void testMethodNotFound_ContentTypeIsApplicationJson() {
        // Arrange - unknown method
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "UnknownMethod",
             "params": {}
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, APPLICATION_JSON);
    }

    @Test
    public void testOmittedParams_ReturnsInvalidParamsError() {
        // Arrange - "params" is omitted, which JSON-RPC 2.0 allows
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": 1,
             "method": "SendMessage"
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert - missing message is reported as Invalid params, not as an internal error,
        // and the response keeps the request id
        JsonObject response = captureResponse();
        assertEquals(-32602, response.getAsJsonObject("error").get("code").getAsInt());
        assertEquals(1, response.get("id").getAsInt());
    }

    @Test
    public void testOmittedParams_ListTasksIsDispatched() {
        // Arrange - "params" is omitted and every ListTasks field is optional
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": 1,
             "method": "ListTasks"
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);
        when(mockJsonRpcHandler.onListTasks(any(ListTasksRequest.class), any(ServerCallContext.class)))
                .thenReturn(new ListTasksResponse(1, new ListTasksResult(Collections.emptyList())));

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert - the request reaches the handler and the response carries no error
        verify(mockJsonRpcHandler).onListTasks(any(ListTasksRequest.class), any(ServerCallContext.class));
        JsonObject response = captureResponse();
        assertFalse(response.has("error"));
        assertEquals(1, response.get("id").getAsInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "[]",
        "[{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"GetTask\", \"params\": {\"id\": \"task-1\"}}]",
        "\"SendMessage\"",
        "1",
        "true",
        "null"
    })
    public void testNonObjectBody_ReturnsInvalidRequestError(String jsonRpcRequest) {
        // Arrange - valid JSON, but not a JSON-RPC request object
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert - Invalid Request, and "id": null because no request id can be read
        JsonObject response = captureResponse();
        assertEquals(-32600, response.getAsJsonObject("error").get("code").getAsInt());
        assertTrue(response.has("id"));
        assertTrue(response.get("id").isJsonNull());
    }

    private JsonObject captureResponse() {
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockHttpResponse).end(bodyCaptor.capture());
        return JsonParser.parseString(bodyCaptor.getValue()).getAsJsonObject();
    }

    @Test
    public void testGetAgentCardReturnsNullWhenNoPublicCard() throws Exception {
        when(mockJsonRpcHandler.getAgentCard()).thenReturn(null);
        String result = routes.getAgentCard(mockRoutingContext);
        assertNull(result);
        verify(mockHttpResponse, never()).putHeader(any(CharSequence.class), any(CharSequence.class));
    }

    @Test
    public void testGetTenantAgentCardReturnsNullWhenNoPublicCard() throws Exception {
        when(mockRoutingContext.pathParam("tenant")).thenReturn("acme");
        when(mockJsonRpcHandler.getAgentCard("acme")).thenReturn(null);
        String result = routes.getTenantAgentCard(mockRoutingContext);
        assertNull(result);
        verify(mockHttpResponse, never()).putHeader(any(CharSequence.class), any(CharSequence.class));
    }

    @Test
    void malformedEnvelopeWinsOverUnknownTenantAndUnsupportedVersion() {
        AgentCardRouter router = tenant -> {
            throw new AssertionError("Envelope decoding must precede card policy");
        };
        JSONRPCHandler handler = new JSONRPCHandler(FixedInstance.empty(), null, mock(RequestHandler.class),
                mockExecutor, new FixedInstance<>(router));
        setField(routes, "jsonRpcHandler", handler);
        mockHeaders.set("A2A-Version", "2.0");
        when(mockRoutingContext.normalizedPath()).thenReturn("/unknown");

        routes.invokeJSONRPCHandler("[]", mockRoutingContext);

        assertEquals(-32600, captureResponse().getAsJsonObject("error").get("code").getAsInt());
    }

    @Test
    void tenantPublicCardInvalidNameReturns400Text() {
        when(mockRoutingContext.pathParam("tenant")).thenReturn("bad?tenant");

        publicCardRoute(true).handle(mockRoutingContext);

        verify(mockHttpResponse).setStatusCode(400);
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, "text/plain");
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mockHttpResponse).end(body.capture());
        assertEquals("Tenant contains invalid characters. Only a-zA-Z0-9_-. are allowed", body.getValue());
        org.mockito.Mockito.verifyNoInteractions(mockJsonRpcHandler);
    }

    @Test
    void unknownRoutedPublicCardReturns404() {
        AgentCardRouter router = tenant -> null;
        setField(routes, "jsonRpcHandler", new JSONRPCHandler(FixedInstance.empty(), null,
                mock(RequestHandler.class), mockExecutor, new FixedInstance<>(router)));
        when(mockRoutingContext.pathParam("tenant")).thenReturn("unknown");

        publicCardRoute(true).handle(mockRoutingContext);

        verify(mockHttpResponse).setStatusCode(404);
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, "text/plain");
        verify(mockHttpResponse).end("No public agent card registered for tenant: unknown");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void publicCardTransportConfigurationFailuresReturnSanitized500(boolean routed) {
        AgentCard invalid = AgentCard.builder()
                .name("invalid")
                .description("invalid transport configuration")
                .version("1.0")
                .supportedInterfaces(List.of(new AgentInterface("NOT_INSTALLED", "http://localhost")))
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();
        AgentCardRouter router = new AgentCardRouter() {
            @Override
            public AgentCard resolvePublicCard(String tenant) {
                return invalid;
            }

            @Override
            public AgentCard resolveExtendedCard(String tenant) {
                return null;
            }
        };
        setField(routes, "jsonRpcHandler", new JSONRPCHandler(new FixedInstance<>(invalid), null,
                mock(RequestHandler.class), mockExecutor, routed ? new FixedInstance<>(router) : null));
        when(mockRoutingContext.pathParam("tenant")).thenReturn("acme");

        publicCardRoute(routed).handle(mockRoutingContext);

        verify(mockHttpResponse).setStatusCode(500);
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, "text/plain");
        verify(mockHttpResponse).end("Internal Server Error");
    }

    private Handler<RoutingContext> publicCardRoute(boolean routed) {
        Router router = mock(Router.class);
        Route route = mock(Route.class, org.mockito.Mockito.RETURNS_SELF);
        Route publicRoute = mock(Route.class, org.mockito.Mockito.RETURNS_SELF);
        Route tenantRoute = mock(Route.class, org.mockito.Mockito.RETURNS_SELF);
        when(router.post(anyString())).thenReturn(route);
        when(router.postWithRegex(anyString())).thenReturn(route);
        when(router.get(anyString())).thenReturn(publicRoute);
        when(router.getWithRegex(anyString())).thenReturn(tenantRoute);
        routes.setupRoutes(router);
        ArgumentCaptor<Handler<RoutingContext>> captor = ArgumentCaptor.forClass(Handler.class);
        verify(routed ? tenantRoute : publicRoute).handler(captor.capture());
        return captor.getValue();
    }

    /**
     * Helper method to set a field via reflection for testing purposes.
     */
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    @Test
    public void testEmptyMessageIdDecodeErrorRetainsId() {
        routes.invokeJSONRPCHandler("""
                {"jsonrpc":"2.0","id":"empty-message","method":"SendMessage","params":{
                  "message":{"messageId":"","role":"ROLE_USER","parts":[{"text":"hello"}]}}}
                """, mockRoutingContext);
        verify(mockHttpResponse).setStatusCode(200);
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, APPLICATION_JSON);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mockHttpResponse).end(body.capture());
        assertEquals(JsonParser.parseString("""
                {"jsonrpc":"2.0","id":"empty-message","error":{"code":-32602,
                 "message":"Required field cannot be null or empty","data":[{
                 "@type":"type.googleapis.com/google.rpc.ErrorInfo","reason":"INVALID_PARAMS",
                 "domain":"a2a-protocol.org","metadata":{}}]}}
                """), JsonParser.parseString(body.getValue()));
        org.mockito.Mockito.verifyNoInteractions(mockJsonRpcHandler);
    }

    @Test
    public void testMalformedProtobufParamsDecodeErrorHasNullId() {
        routes.invokeJSONRPCHandler("""
                {"jsonrpc":"2.0","id":"protobuf-error","method":"GetTask",
                 "params":{"id":"task","historyLength":"bad"}}
                """, mockRoutingContext);
        verify(mockHttpResponse).setStatusCode(200);
        verify(mockHttpResponse).putHeader(CONTENT_TYPE, APPLICATION_JSON);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mockHttpResponse).end(body.capture());
        assertEquals(JsonParser.parseString("""
                {"jsonrpc":"2.0","id":null,"error":{"code":-32600,
                "message":"Invalid request content: Not an int32 value: \\"bad\\". Please verify the request matches the expected schema for this method.",
                "data":[{"@type":"type.googleapis.com/google.rpc.ErrorInfo","reason":"INVALID_REQUEST",
                "domain":"a2a-protocol.org","metadata":{}}]}}
                """), JsonParser.parseString(body.getValue()));
        org.mockito.Mockito.verifyNoInteractions(mockJsonRpcHandler);
    }

}
