package io.a2a.server.apps.quarkus;

import static io.a2a.spec.AgentCard.CURRENT_PROTOCOL_VERSION;
import static io.a2a.transport.jsonrpc.context.JSONRPCContextKeys.METHOD_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

import jakarta.enterprise.inject.Instance;

import io.a2a.server.ServerCallContext;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AuthenticationInfo;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
import io.a2a.spec.DeleteTaskPushNotificationConfigResponse;
import io.a2a.spec.GetAuthenticatedExtendedCardRequest;
import io.a2a.spec.GetAuthenticatedExtendedCardResponse;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.ListTaskPushNotificationConfigRequest;
import io.a2a.spec.ListTaskPushNotificationConfigResponse;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.SubscribeToTaskRequest;
import io.a2a.spec.Task;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

        // Setup common mock behavior
        when(mockCallContextFactory.isUnsatisfied()).thenReturn(true);
        when(mockRoutingContext.request()).thenReturn(mockRequest);
        when(mockRoutingContext.response()).thenReturn(mockHttpResponse);
        when(mockRoutingContext.user()).thenReturn(null);
        when(mockRequest.headers()).thenReturn(mockHeaders);
        when(mockRoutingContext.body()).thenReturn(mockRequestBody);

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
                 "blocking": true
              },
              "metadata": {}
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with a Task
        Task responseTask = Task.builder()
                .id("task-123")
                .contextId("context-1234")
                .status(new TaskStatus(TaskState.SUBMITTED))
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
        assertEquals(SendMessageRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
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
                "blocking": true
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
        assertEquals(SendStreamingMessageRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
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
              "name": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64",
              "historyLength": 10
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with a Task
        Task responseTask = Task.builder()
                .id("de38c76d-d54c-436c-8b9f-4c2703648d64")
                .contextId("context-1234")
                .status(new TaskStatus(TaskState.SUBMITTED))
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
        assertEquals(GetTaskRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
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
              "name": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64"
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with a Task
        Task responseTask = Task.builder()
                .id("de38c76d-d54c-436c-8b9f-4c2703648d64")
                .contextId("context-1234")
                .status(new TaskStatus(TaskState.CANCELED))
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
        assertEquals(CancelTaskRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
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
              "name": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64"
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
        assertEquals(SubscribeToTaskRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testSetTaskPushNotificationConfig_MethodNameSetInContext() {
        // Arrange - using protobuf JSON format
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "SetTaskPushNotificationConfig",
             "params": {
              "parent": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64",
              "configId": "config-123",
              "config": {
               "name": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64/pushNotificationConfigs/config-123",
               "pushNotificationConfig": {
                "url": "https://example.com/callback",
                "authentication": {
                 "schemes": ["jwt"]
                }
               }
              }
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with a TaskPushNotificationConfig
        TaskPushNotificationConfig responseConfig = new TaskPushNotificationConfig(
                "de38c76d-d54c-436c-8b9f-4c2703648d64",
                PushNotificationConfig.builder()
                        .id("config-123")
                        .url("https://example.com/callback")
                        .authentication(new AuthenticationInfo(Collections.singletonList("jwt"), null))
                        .build(),
                "tenant");

        SetTaskPushNotificationConfigResponse realResponse = new SetTaskPushNotificationConfigResponse("1", responseConfig);
        when(mockJsonRpcHandler.setPushNotificationConfig(any(SetTaskPushNotificationConfigRequest.class),
                any(ServerCallContext.class))).thenReturn(realResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).setPushNotificationConfig(any(SetTaskPushNotificationConfigRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(SetTaskPushNotificationConfigRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
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
              "name": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64/pushNotificationConfigs/config-456"
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with a TaskPushNotificationConfig
        TaskPushNotificationConfig responseConfig = new TaskPushNotificationConfig(
                "de38c76d-d54c-436c-8b9f-4c2703648d64",
                PushNotificationConfig.builder()
                        .id("config-456")
                        .url("https://example.com/callback")
                        .build(),
                null
        );
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
        assertEquals(GetTaskPushNotificationConfigRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testListTaskPushNotificationConfig_MethodNameSetInContext() {
        // Arrange - using protobuf JSON format
        String jsonRpcRequest = """
            {
             "jsonrpc": "2.0",
             "id": "cd4c76de-d54c-436c-8b9f-4c2703648d64",
             "method": "ListTaskPushNotificationConfig",
             "params": {
              "parent": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64"
             }
            }""";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with a list of TaskPushNotificationConfig
        TaskPushNotificationConfig config = new TaskPushNotificationConfig(
                "de38c76d-d54c-436c-8b9f-4c2703648d64",
                PushNotificationConfig.builder()
                        .id("config-123")
                        .url("https://example.com/callback")
                        .build(),
                null
        );
        ListTaskPushNotificationConfigResponse realResponse = new ListTaskPushNotificationConfigResponse("1", Collections.singletonList(config));
        when(mockJsonRpcHandler.listPushNotificationConfig(any(ListTaskPushNotificationConfigRequest.class),
                any(ServerCallContext.class))).thenReturn(realResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).listPushNotificationConfig(any(ListTaskPushNotificationConfigRequest.class),
                contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(ListTaskPushNotificationConfigRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
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
              "name": "tasks/de38c76d-d54c-436c-8b9f-4c2703648d64/pushNotificationConfigs/config-456"
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
        assertEquals(DeleteTaskPushNotificationConfigRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
    }

    @Test
    public void testGetAuthenticatedExtendedCard_MethodNameSetInContext() {
        // Arrange
        String jsonRpcRequest = "{\"jsonrpc\":\"2.0\",\"id\":\"5\",\"method\":\"" + GetAuthenticatedExtendedCardRequest.METHOD
                + "\",\"id\":1}";
        when(mockRequestBody.asString()).thenReturn(jsonRpcRequest);

        // Create a real response with an AgentCard
        AgentCard agentCard = AgentCard.builder()
                .name("Test Agent")
                .description("Test agent description")
                .version("1.0.0")
                .protocolVersion(CURRENT_PROTOCOL_VERSION)
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.emptyList())
                .supportedInterfaces(Collections.singletonList(new AgentInterface("jsonrpc", "http://localhost:9999")))
                .build();
        GetAuthenticatedExtendedCardResponse realResponse = new GetAuthenticatedExtendedCardResponse(1, agentCard);
        when(mockJsonRpcHandler.onGetAuthenticatedExtendedCardRequest(
                any(GetAuthenticatedExtendedCardRequest.class), any(ServerCallContext.class)))
                .thenReturn(realResponse);

        ArgumentCaptor<ServerCallContext> contextCaptor = ArgumentCaptor.forClass(ServerCallContext.class);

        // Act
        routes.invokeJSONRPCHandler(jsonRpcRequest, mockRoutingContext);

        // Assert
        verify(mockJsonRpcHandler).onGetAuthenticatedExtendedCardRequest(
                any(GetAuthenticatedExtendedCardRequest.class), contextCaptor.capture());
        ServerCallContext capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(GetAuthenticatedExtendedCardRequest.METHOD, capturedContext.getState().get(METHOD_NAME_KEY));
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
}
