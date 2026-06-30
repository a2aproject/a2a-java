package org.a2aproject.sdk.integrations.springboot.server.rest;

import static org.a2aproject.sdk.common.A2AHeaders.A2A_VERSION;
import static org.a2aproject.sdk.spec.TransportProtocol.HTTP_JSON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.AuthenticationInfo;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = A2ASpringBootIntegrationTest.TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class A2ASpringBootIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestHandler requestHandler;

    @Autowired
    private AgentCard agentCard;

    @Test
    void servesAgentCardAsJson() throws Exception {
        MvcResult result = mockMvc.perform(get("/.well-known/agent-card.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("\"name\":\"Spring Boot Test Agent\""));
        assertTrue(responseBody.contains("\"version\":\"1.0.0\""));
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

        MvcResult result = mockMvc.perform(post("/tenant-a/message:send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(A2A_VERSION, agentCard.supportedInterfaces().get(0).protocolVersion())
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("\"msg-2\""));
        assertTrue(responseBody.contains("\"message\""));

        var paramsCaptor = org.mockito.ArgumentCaptor.forClass(MessageSendParams.class);
        var contextCaptor = org.mockito.ArgumentCaptor.forClass(ServerCallContext.class);
        verify(requestHandler).onMessageSend(paramsCaptor.capture(), contextCaptor.capture());

        assertEquals("tenant-a", paramsCaptor.getValue().tenant());
        assertEquals("trace-1", paramsCaptor.getValue().metadata().get("traceId"));
        assertEquals(HTTP_JSON, contextCaptor.getValue().getState().get("transport"));
        assertEquals("SendMessage", contextCaptor.getValue().getState().get("method"));
    }

    @Test
    void routesStreamingMessageThroughRequestHandler() throws Exception {
        Flow.Publisher<StreamingEventKind> publisher = subscriber -> subscriber.onSubscribe(new Flow.Subscription() {
            private boolean emitted;

            @Override
            public void request(long n) {
                if (!emitted) {
                    emitted = true;
                    subscriber.onNext(Message.builder()
                            .role(Message.Role.ROLE_AGENT)
                            .messageId("stream-1")
                            .parts(new TextPart("ok"))
                            .build());
                    subscriber.onComplete();
                }
            }

            @Override
            public void cancel() {
            }
        });
        when(requestHandler.onMessageSendStream(any(MessageSendParams.class), any(ServerCallContext.class))).thenReturn(publisher);

        MessageSendParams params = MessageSendParams.builder()
                .message(Message.builder()
                        .role(Message.Role.ROLE_USER)
                        .messageId("msg-1")
                        .parts(new TextPart("hello"))
                        .build())
                .build();

        MvcResult result = mockMvc.perform(post("/tenant-a/message:stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(A2A_VERSION, agentCard.supportedInterfaces().get(0).protocolVersion())
                        .content(toJson(params)))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult dispatched = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        String responseBody = dispatched.getResponse().getContentAsString();
        assertTrue(responseBody.contains("stream-1"));
    }

    @Test
    void routesTaskLookupThroughRequestHandler() throws Exception {
        Task task = Task.builder()
                .id("task-123")
                .contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build();
        when(requestHandler.onGetTask(any(), any())).thenReturn(task);

        MvcResult result = mockMvc.perform(get("/tenant-a/tasks/task-123")
                        .param("historyLength", "2")
                        .header(A2A_VERSION, agentCard.supportedInterfaces().get(0).protocolVersion()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("\"task-123\""));
    }

    @Test
    void routesTaskListThroughRequestHandler() throws Exception {
        when(requestHandler.onListTasks(any(), any())).thenReturn(new ListTasksResult(List.of(task("task-123", TaskState.TASK_STATE_SUBMITTED))));

        MvcResult result = mockMvc.perform(get("/tenant-a/tasks")
                        .param("status", "task_state_submitted")
                        .param("pageSize", "10")
                        .param("includeArtifacts", "true")
                        .header(A2A_VERSION, agentCard.supportedInterfaces().get(0).protocolVersion()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("\"task-123\""));
    }

    @Test
    void routesCancelTaskThroughRequestHandler() throws Exception {
        when(requestHandler.onCancelTask(any(), any())).thenReturn(task("task-123", TaskState.TASK_STATE_CANCELED));

        MvcResult result = mockMvc.perform(post("/tenant-a/tasks/task-123:cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(A2A_VERSION, agentCard.supportedInterfaces().get(0).protocolVersion())
                        .content("{\"metadata\":{\"reason\":\"user_requested\"}}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("\"task-123\""));
    }

    @Test
    void routesTaskSubscriptionThroughRequestHandler() throws Exception {
        Flow.Publisher<StreamingEventKind> publisher = subscriber -> subscriber.onSubscribe(new Flow.Subscription() {
            private boolean emitted;

            @Override
            public void request(long n) {
                if (!emitted) {
                    emitted = true;
                    subscriber.onNext(Message.builder()
                            .role(Message.Role.ROLE_AGENT)
                            .messageId("stream-2")
                            .parts(new TextPart("subscribed"))
                            .build());
                    subscriber.onComplete();
                }
            }

            @Override
            public void cancel() {
            }
        });
        when(requestHandler.onSubscribeToTask(any(), any())).thenReturn(publisher);

        MvcResult result = mockMvc.perform(post("/tenant-a/tasks/task-123:subscribe")
                        .header(A2A_VERSION, agentCard.supportedInterfaces().get(0).protocolVersion()))
                .andExpect(request().asyncStarted())
                .andReturn();

        MvcResult dispatched = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        String responseBody = dispatched.getResponse().getContentAsString();
        assertTrue(responseBody.contains("stream-2"));
    }

    @Test
    void routesPushNotificationEndpointsThroughRequestHandler() throws Exception {
        TaskPushNotificationConfig config = TaskPushNotificationConfig.builder()
                .id("config-1")
                .taskId("task-123")
                .url("https://example.com/webhook")
                .token("token-1")
                .authentication(new AuthenticationInfo("Bearer", "secret"))
                .tenant("tenant-a")
                .build();
        when(requestHandler.onCreateTaskPushNotificationConfig(any(), any())).thenReturn(config);
        when(requestHandler.onGetTaskPushNotificationConfig(any(GetTaskPushNotificationConfigParams.class), any()))
                .thenReturn(config);
        when(requestHandler.onListTaskPushNotificationConfigs(any(ListTaskPushNotificationConfigsParams.class), any()))
                .thenReturn(new ListTaskPushNotificationConfigsResult(List.of(config), "next-token"));

        mockMvc.perform(post("/tenant-a/tasks/task-123/pushNotificationConfigs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(A2A_VERSION, agentCard.supportedInterfaces().get(0).protocolVersion())
                        .content("""
                                {
                                  "url": "https://example.com/webhook",
                                  "token": "token-1",
                                  "authentication": {
                                    "scheme": "Bearer",
                                    "credentials": "secret"
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("\"config-1\"")));

        mockMvc.perform(get("/tenant-a/tasks/task-123/pushNotificationConfigs/config-1")
                        .header(A2A_VERSION, agentCard.supportedInterfaces().get(0).protocolVersion()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("\"config-1\"")));

        mockMvc.perform(get("/tenant-a/tasks/task-123/pushNotificationConfigs")
                        .param("pageSize", "10")
                        .param("pageToken", "token-1")
                        .header(A2A_VERSION, agentCard.supportedInterfaces().get(0).protocolVersion()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("\"config-1\"")));

        mockMvc.perform(delete("/tenant-a/tasks/task-123/pushNotificationConfigs/config-1")
                        .header(A2A_VERSION, agentCard.supportedInterfaces().get(0).protocolVersion()))
                .andExpect(status().isNoContent());
    }

    @Test
    void servesExtendedAgentCardWhenConfigured() throws Exception {
        MvcResult result = mockMvc.perform(get("/tenant-a/extendedAgentCard")
                        .header(A2A_VERSION, agentCard.supportedInterfaces().get(0).protocolVersion()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertTrue(responseBody.contains("\"Spring Boot Extended Agent\""));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        AgentCard agentCard() {
            return AgentCard.builder()
                    .name("Spring Boot Test Agent")
                    .description("Test agent for Spring Boot transport integration")
                    .version("1.0.0")
                    .capabilities(AgentCapabilities.builder()
                            .streaming(true)
                            .pushNotifications(true)
                            .extendedAgentCard(true)
                            .build())
                    .defaultInputModes(List.of("text"))
                    .defaultOutputModes(List.of("text"))
                    .skills(List.of())
                    .supportedInterfaces(List.of(new AgentInterface(HTTP_JSON.asString(), "http://localhost:8080")))
                    .build();
        }

        @Bean("extendedAgentCard")
        AgentCard extendedAgentCard() {
            return AgentCard.builder()
                    .name("Spring Boot Extended Agent")
                    .description("Extended test agent for Spring Boot transport integration")
                    .version("1.0.0")
                    .capabilities(AgentCapabilities.builder()
                            .streaming(true)
                            .pushNotifications(true)
                            .extendedAgentCard(true)
                            .build())
                    .defaultInputModes(List.of("text"))
                    .defaultOutputModes(List.of("text"))
                    .skills(List.of())
                    .supportedInterfaces(List.of(new AgentInterface(HTTP_JSON.asString(), "http://localhost:8080")))
                    .build();
        }
    }

    private String toJson(Object value) throws Exception {
        return JsonUtil.toJson(value);
    }

    private Task task(String id, TaskState state) {
        return Task.builder()
                .id(id)
                .contextId("ctx-1")
                .status(new TaskStatus(state))
                .build();
    }
}
