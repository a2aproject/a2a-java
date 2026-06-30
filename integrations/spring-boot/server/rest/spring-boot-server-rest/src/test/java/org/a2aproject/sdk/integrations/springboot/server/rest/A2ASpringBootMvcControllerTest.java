package org.a2aproject.sdk.integrations.springboot.server.rest;

import static org.a2aproject.sdk.common.A2AHeaders.A2A_VERSION;
import static org.a2aproject.sdk.spec.TransportProtocol.HTTP_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.SubmissionPublisher;

import jakarta.servlet.http.HttpServletRequest;

import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.AgentExtension;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AuthenticationInfo;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.ExtendedAgentCardNotConfiguredError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class A2ASpringBootMvcControllerTest {

    private final RequestHandler requestHandler = mock(RequestHandler.class);
    private final A2ASpringBootHttpResponseMapper responseMapper = new A2ASpringBootHttpResponseMapper();
    private final A2APushNotificationConfigRequestMapper pushNotificationConfigRequestMapper =
            new A2APushNotificationConfigRequestMapper();
    private final ObjectProvider<StreamingSubscriptionObserver> streamingSubscriptionObserver =
            new StaticListableBeanFactory().getBeanProvider(StreamingSubscriptionObserver.class);
    private final A2ASpringBootMvcController controller =
            new A2ASpringBootMvcController(
                    agentCard(),
                    emptyExtendedAgentCardProvider(),
                    requestHandler,
                    responseMapper,
                    pushNotificationConfigRequestMapper,
                    streamingSubscriptionObserver);

    @Test
    void servesAgentCardAsJson() {
        ResponseEntity<String> response = controller.getAgentCard();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertTrue(response.getBody().contains("\"Spring Boot Test Agent\""));
    }

    @Test
    void routesSendMessageThroughRequestHandler() throws Exception {
        Message requestMessage = Message.builder()
                .role(Message.Role.ROLE_USER)
                .messageId("msg-1")
                .parts(new TextPart("hello"))
                .build();
        MessageSendParams params = MessageSendParams.builder()
                .message(requestMessage)
                .metadata(Map.of("traceId", "trace-1"))
                .build();
        String body = toJson(params);

        Message responseMessage = Message.builder()
                .role(Message.Role.ROLE_AGENT)
                .messageId("msg-2")
                .parts(new TextPart("ok"))
                .build();
        when(requestHandler.onMessageSend(any(MessageSendParams.class), any(ServerCallContext.class))).thenReturn(responseMessage);

        ResponseEntity<String> response = controller.sendMessage("tenant-a", body, httpRequest());

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertTrue(response.getBody().contains("\"msg-2\""));

        var paramsCaptor = org.mockito.ArgumentCaptor.forClass(MessageSendParams.class);
        var contextCaptor = org.mockito.ArgumentCaptor.forClass(ServerCallContext.class);
        verify(requestHandler).onMessageSend(paramsCaptor.capture(), contextCaptor.capture());

        assertEquals("tenant-a", paramsCaptor.getValue().tenant());
        assertEquals("trace-1", paramsCaptor.getValue().metadata().get("traceId"));
        assertEquals(HTTP_JSON, contextCaptor.getValue().getState().get("transport"));
        assertEquals("SendMessage", contextCaptor.getValue().getState().get("method"));
    }

    @Test
    void routesGetTaskThroughRequestHandler() {
        Task task = Task.builder()
                .id("task-123")
                .contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build();
        when(requestHandler.onGetTask(any(TaskQueryParams.class), any(ServerCallContext.class))).thenReturn(task);

        ResponseEntity<String> response = controller.getTask("tenant-a", "task-123", 2, httpRequest());

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("\"task-123\""));

        var paramsCaptor = org.mockito.ArgumentCaptor.forClass(TaskQueryParams.class);
        var contextCaptor = org.mockito.ArgumentCaptor.forClass(ServerCallContext.class);
        verify(requestHandler).onGetTask(paramsCaptor.capture(), contextCaptor.capture());

        assertEquals("task-123", paramsCaptor.getValue().id());
        assertEquals(2, paramsCaptor.getValue().historyLength());
        assertEquals("tenant-a", paramsCaptor.getValue().tenant());
        assertEquals("GetTask", contextCaptor.getValue().getState().get("method"));
    }

    @Test
    void routesCancelTaskThroughRequestHandler() {
        Task task = Task.builder()
                .id("task-123")
                .contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_CANCELED))
                .build();
        when(requestHandler.onCancelTask(any(CancelTaskParams.class), any(ServerCallContext.class))).thenReturn(task);

        ResponseEntity<String> response = controller.cancelTask("tenant-a", "task-123", "{\"metadata\":{\"reason\":\"user_requested\"}}", httpRequest());

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("\"task-123\""));

        var paramsCaptor = org.mockito.ArgumentCaptor.forClass(CancelTaskParams.class);
        var contextCaptor = org.mockito.ArgumentCaptor.forClass(ServerCallContext.class);
        verify(requestHandler).onCancelTask(paramsCaptor.capture(), contextCaptor.capture());

        assertEquals("task-123", paramsCaptor.getValue().id());
        assertEquals("tenant-a", paramsCaptor.getValue().tenant());
        assertEquals("user_requested", paramsCaptor.getValue().metadata().get("reason"));
        assertEquals("CancelTask", contextCaptor.getValue().getState().get("method"));
    }

    @Test
    void routesCancelTaskWithBlankBodyThroughRequestHandler() {
        Task task = Task.builder()
                .id("task-123")
                .contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_CANCELED))
                .build();
        when(requestHandler.onCancelTask(any(CancelTaskParams.class), any(ServerCallContext.class))).thenReturn(task);

        ResponseEntity<String> response = controller.cancelTask("tenant-a", "task-123", "   ", httpRequest());

        assertEquals(200, response.getStatusCodeValue());
        var paramsCaptor = org.mockito.ArgumentCaptor.forClass(CancelTaskParams.class);
        verify(requestHandler).onCancelTask(paramsCaptor.capture(), any(ServerCallContext.class));
        assertEquals(Map.of(), paramsCaptor.getValue().metadata());
    }

    @Test
    void routesListTasksThroughRequestHandler() {
        when(requestHandler.onListTasks(any(), any())).thenReturn(new ListTasksResult(List.of(task("task-123"))));

        ResponseEntity<String> response = controller.listTasks(
                "tenant-a",
                "ctx-1",
                "task_state_submitted",
                10,
                "token-1",
                2,
                "2026-01-01T00:00:00Z",
                true,
                httpRequest());

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("\"task-123\""));

        var paramsCaptor = org.mockito.ArgumentCaptor.forClass(ListTasksParams.class);
        verify(requestHandler).onListTasks(paramsCaptor.capture(), any(ServerCallContext.class));
        assertEquals("tenant-a", paramsCaptor.getValue().tenant());
        assertEquals("ctx-1", paramsCaptor.getValue().contextId());
        assertEquals(TaskState.TASK_STATE_SUBMITTED, paramsCaptor.getValue().status());
        assertEquals(10, paramsCaptor.getValue().pageSize());
        assertEquals("token-1", paramsCaptor.getValue().pageToken());
        assertEquals(2, paramsCaptor.getValue().historyLength());
        assertEquals(true, paramsCaptor.getValue().includeArtifacts());
    }

    @Test
    void throwsForBadListStatus() {
        assertThrows(IllegalArgumentException.class, () -> controller.listTasks(
                "tenant-a",
                null,
                "not-a-status",
                null,
                null,
                null,
                null,
                null,
                httpRequest()));
    }

    @Test
    void rejectsBlankSendMessageBody() {
        assertThrows(InvalidRequestError.class, () -> controller.sendMessage("tenant-a", " ", httpRequest()));
    }

    @Test
    void rejectsNullJsonSendMessageBody() {
        assertThrows(InvalidRequestError.class, () -> controller.sendMessage("tenant-a", "null", httpRequest()));
    }

    @Test
    void routesStreamingMessageThroughRequestHandler() throws Exception {
        SubmissionPublisher<StreamingEventKind> publisher = new SubmissionPublisher<>();
        when(requestHandler.onMessageSendStream(any(MessageSendParams.class), any(ServerCallContext.class))).thenReturn(publisher);

        SseEmitter response = controller.sendMessageStream("tenant-a",
                toJson(MessageSendParams.builder()
                        .message(Message.builder()
                                .role(Message.Role.ROLE_USER)
                                .messageId("msg-1")
                                .parts(new TextPart("hello"))
                                .build())
                        .build()),
                httpRequest());

        assertInstanceOf(SseEmitter.class, response);
    }

    @Test
    void throwsWhenStreamingCapabilityDisabled() {
        A2ASpringBootMvcController nonStreamingController = new A2ASpringBootMvcController(
                agentCard(false, false, false),
                emptyExtendedAgentCardProvider(),
                requestHandler,
                responseMapper,
                pushNotificationConfigRequestMapper,
                streamingSubscriptionObserver);

        assertThrows(InvalidRequestError.class, () -> nonStreamingController.sendMessageStream("tenant-a",
                toJson(MessageSendParams.builder()
                        .message(Message.builder()
                                .role(Message.Role.ROLE_USER)
                                .messageId("msg-1")
                                .parts(new TextPart("hello"))
                                .build())
                        .build()),
                httpRequest()));
    }

    @Test
    void routesTaskSubscriptionThroughRequestHandler() {
        SubmissionPublisher<StreamingEventKind> publisher = new SubmissionPublisher<>();
        when(requestHandler.onSubscribeToTask(any(), any())).thenReturn(publisher);

        SseEmitter response = controller.subscribeToTask("tenant-a", "task-123", httpRequest());

        assertInstanceOf(SseEmitter.class, response);
    }

    @Test
    void throwsWhenSubscribeStreamingCapabilityDisabled() {
        A2ASpringBootMvcController nonStreamingController = new A2ASpringBootMvcController(
                agentCard(false, false, false),
                emptyExtendedAgentCardProvider(),
                requestHandler,
                responseMapper,
                pushNotificationConfigRequestMapper,
                streamingSubscriptionObserver);

        assertThrows(InvalidRequestError.class, () -> nonStreamingController.subscribeToTask("tenant-a", "task-123", httpRequest()));
    }

    @Test
    void routesCreatePushNotificationConfigurationThroughRequestHandler() {
        TaskPushNotificationConfig storedConfig = TaskPushNotificationConfig.builder()
                .id("config-1")
                .taskId("task-123")
                .url("https://example.com/webhook")
                .token("token-1")
                .authentication(new AuthenticationInfo("Bearer", "secret"))
                .tenant("tenant-a")
                .build();
        when(requestHandler.onCreateTaskPushNotificationConfig(any(TaskPushNotificationConfig.class), any(ServerCallContext.class)))
                .thenReturn(storedConfig);

        ResponseEntity<String> response = controller.createTaskPushNotificationConfiguration(
                "tenant-a",
                "task-123",
                "{\"url\":\"https://example.com/webhook\",\"token\":\"token-1\",\"authentication\":{\"scheme\":\"Bearer\",\"credentials\":\"secret\"}}",
                httpRequest());

        assertEquals(201, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("\"config-1\""));

        var paramsCaptor = org.mockito.ArgumentCaptor.forClass(TaskPushNotificationConfig.class);
        verify(requestHandler).onCreateTaskPushNotificationConfig(paramsCaptor.capture(), any(ServerCallContext.class));
        assertEquals("task-123", paramsCaptor.getValue().taskId());
        assertEquals("tenant-a", paramsCaptor.getValue().tenant());
        assertEquals("https://example.com/webhook", paramsCaptor.getValue().url());
    }

    @Test
    void routesGetPushNotificationConfigurationThroughRequestHandler() {
        TaskPushNotificationConfig storedConfig = TaskPushNotificationConfig.builder()
                .id("config-1")
                .taskId("task-123")
                .url("https://example.com/webhook")
                .tenant("tenant-a")
                .build();
        when(requestHandler.onGetTaskPushNotificationConfig(any(GetTaskPushNotificationConfigParams.class), any(ServerCallContext.class)))
                .thenReturn(storedConfig);

        ResponseEntity<String> response = controller.getTaskPushNotificationConfiguration(
                "tenant-a", "task-123", "config-1", httpRequest());

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("\"config-1\""));
        verify(requestHandler).onGetTaskPushNotificationConfig(eq(new GetTaskPushNotificationConfigParams("task-123", "config-1", "tenant-a")),
                any(ServerCallContext.class));
    }

    @Test
    void routesListPushNotificationConfigurationsThroughRequestHandler() {
        TaskPushNotificationConfig storedConfig = TaskPushNotificationConfig.builder()
                .id("config-1")
                .taskId("task-123")
                .url("https://example.com/webhook")
                .tenant("tenant-a")
                .build();
        when(requestHandler.onListTaskPushNotificationConfigs(any(ListTaskPushNotificationConfigsParams.class), any(ServerCallContext.class)))
                .thenReturn(new ListTaskPushNotificationConfigsResult(List.of(storedConfig), "next-token"));

        ResponseEntity<String> response = controller.listTaskPushNotificationConfigurations(
                "tenant-a", "task-123", 10, "token-1", httpRequest());

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("\"config-1\""));
        verify(requestHandler).onListTaskPushNotificationConfigs(
                eq(new ListTaskPushNotificationConfigsParams("task-123", 10, "token-1", "tenant-a")),
                any(ServerCallContext.class));
    }

    @Test
    void routesDeletePushNotificationConfigurationThroughRequestHandler() {
        ResponseEntity<?> response = controller.deleteTaskPushNotificationConfiguration(
                "tenant-a", "task-123", "config-1", httpRequest());

        assertEquals(204, response.getStatusCodeValue());
        verify(requestHandler).onDeleteTaskPushNotificationConfig(
                eq(new DeleteTaskPushNotificationConfigParams("task-123", "config-1", "tenant-a")),
                any(ServerCallContext.class));
    }

    @Test
    void servesExtendedAgentCardWhenAvailable() {
        AgentCard extended = agentCard(true, true, true);
        A2ASpringBootMvcController extendedCardController = new A2ASpringBootMvcController(
                agentCard(true, true, true),
                singleExtendedAgentCardProvider(extended),
                requestHandler,
                responseMapper,
                pushNotificationConfigRequestMapper,
                streamingSubscriptionObserver);

        ResponseEntity<String> response = extendedCardController.getExtendedAgentCard("tenant-a", httpRequest());

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("\"Spring Boot Test Agent\""));
    }

    @Test
    void throwsWhenExtendedAgentCardConfiguredButBeanMissing() {
        A2ASpringBootMvcController extendedCardController = new A2ASpringBootMvcController(
                agentCard(true, true, true),
                emptyExtendedAgentCardProvider(),
                requestHandler,
                responseMapper,
                pushNotificationConfigRequestMapper,
                streamingSubscriptionObserver);

        assertThrows(ExtendedAgentCardNotConfiguredError.class,
                () -> extendedCardController.getExtendedAgentCard("tenant-a", httpRequest()));
    }

    @Test
    void throwsWhenExtendedAgentCardCapabilityDisabled() {
        A2ASpringBootMvcController extendedCardController = new A2ASpringBootMvcController(
                agentCard(true, true, false),
                emptyExtendedAgentCardProvider(),
                requestHandler,
                responseMapper,
                pushNotificationConfigRequestMapper,
                streamingSubscriptionObserver);

        assertThrows(UnsupportedOperationError.class,
                () -> extendedCardController.getExtendedAgentCard("tenant-a", httpRequest()));
    }

    private AgentCard agentCard() {
        return agentCard(true, true, false);
    }

    private AgentCard agentCard(boolean streaming, boolean pushNotifications, boolean extendedAgentCard) {
        return AgentCard.builder()
                .name("Spring Boot Test Agent")
                .description("Test agent for Spring Boot MVC transport")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(streaming)
                        .pushNotifications(pushNotifications)
                        .extendedAgentCard(extendedAgentCard)
                        .extensions(List.of(AgentExtension.builder()
                                .description("Test extension")
                                .uri("trace")
                                .required(false)
                                .build()))
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .supportedInterfaces(List.of(new AgentInterface(HTTP_JSON.asString(), "http://localhost:8080")))
                .build();
    }

    private Task task(String taskId) {
        return Task.builder()
                .id(taskId)
                .contextId(taskId + "-context")
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build();
    }

    private HttpServletRequest httpRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Map<String, List<String>> headers = Map.of(
                A2A_VERSION, List.of("1.0"),
                "A2A-Extensions", List.of("trace"),
                "X-Trace-Id", List.of("trace-1"));
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(headers.keySet()));
        when(request.getHeader(any(String.class))).thenAnswer(invocation -> {
            String headerName = invocation.getArgument(0, String.class);
            List<String> values = headers.get(headerName);
            return values == null || values.isEmpty() ? null : values.get(0);
        });
        when(request.getHeaders(any(String.class))).thenAnswer(invocation -> {
            String headerName = invocation.getArgument(0, String.class);
            List<String> values = headers.get(headerName);
            return Collections.enumeration(values == null ? List.of() : values);
        });
        when(request.getUserPrincipal()).thenReturn((Principal) () -> "alice");
        return request;
    }

    private String toJson(Object value) throws Exception {
        return JsonUtil.toJson(value);
    }

    private ObjectProvider<AgentCard> emptyExtendedAgentCardProvider() {
        return new StaticListableBeanFactory().getBeanProvider(AgentCard.class);
    }

    private ObjectProvider<AgentCard> singleExtendedAgentCardProvider(AgentCard agentCard) {
        return new StaticListableBeanFactory(Map.of("extendedAgentCard", agentCard)).getBeanProvider(AgentCard.class);
    }
}
