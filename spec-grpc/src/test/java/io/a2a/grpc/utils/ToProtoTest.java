package io.a2a.grpc.utils;

import static io.a2a.grpc.Role.ROLE_AGENT;
import static io.a2a.grpc.Role.ROLE_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.a2a.grpc.SendMessageConfiguration;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.Artifact;
import io.a2a.spec.HTTPAuthSecurityScheme;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendConfiguration;
import io.a2a.spec.AuthenticationInfo;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ToProtoTest {

    private static final Message SIMPLE_MESSAGE = new Message.Builder()
            .role(Message.Role.USER)
            .parts(Collections.singletonList(new TextPart("tell me a joke")))
            .contextId("context-1234")
            .messageId("message-1234")
            .build();

    @Test
    public void convertAgentCard() {
        AgentCard agentCard = new AgentCard.Builder()
                .name("Hello World Agent")
                .description("Just a hello world agent")
                .url("http://localhost:9999")
                .version("1.0.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill.Builder()
                        .id("hello_world")
                        .name("Returns hello world")
                        .description("just returns hello world")
                        .tags(Collections.singletonList("hello world"))
                        .examples(List.of("hi", "hello world"))
                        .build()))
                .protocolVersion("0.2.5")
                .build();
        io.a2a.grpc.AgentCard result = ProtoUtils.ToProto.agentCard(agentCard);
        assertEquals("Hello World Agent", result.getName());
        assertEquals("Just a hello world agent", result.getDescription());
        assertEquals("http://localhost:9999", result.getUrl());
        assertEquals("1.0.0", result.getVersion());
        assertEquals("http://example.com/docs", result.getDocumentationUrl());
        assertEquals(1, result.getDefaultInputModesCount());
        assertEquals("text", result.getDefaultInputModes(0));
        assertEquals(1, result.getDefaultOutputModesCount());
        assertEquals("text", result.getDefaultOutputModes(0));
        assertEquals("0.2.5", result.getProtocolVersion());
        agentCard = new AgentCard.Builder()
                .name("Hello World Agent")
                .description("Just a hello world agent")
                .url("http://localhost:9999")
                .version("1.0.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill.Builder()
                        .id("hello_world")
                        .name("Returns hello world")
                        .description("just returns hello world")
                        .tags(Collections.singletonList("hello world"))
                        .examples(List.of("hi", "hello world"))
                        .build()))
                .preferredTransport("HTTP+JSON")
//                .iconUrl("http://example.com/icon.svg")
                .securitySchemes(Map.of("basic", new HTTPAuthSecurityScheme.Builder().scheme("basic").description("Basic Auth").build()))
                .security(List.of(Map.of("oauth", List.of("read"))))
                .protocolVersion("0.2.5")
                .build();
       result = ProtoUtils.ToProto.agentCard(agentCard);
        assertEquals("Hello World Agent", result.getName());
        assertEquals("Just a hello world agent", result.getDescription());
        assertEquals("http://localhost:9999", result.getUrl());
        assertEquals("1.0.0", result.getVersion());
        assertEquals("http://example.com/docs", result.getDocumentationUrl());
        assertEquals(1, result.getDefaultInputModesCount());
        assertEquals("text", result.getDefaultInputModes(0));
        assertEquals(1, result.getDefaultOutputModesCount());
        assertEquals("text", result.getDefaultOutputModes(0));
        assertEquals("0.2.5", result.getProtocolVersion());
        assertEquals("HTTP+JSON", result.getPreferredTransport());
        assertEquals(1, result.getSecurityCount());
        assertEquals(1, result.getSecurity(0).getSchemesMap().size());
        assertEquals(true, result.getSecurity(0).getSchemesMap().containsKey("oauth"));
        assertEquals(1, result.getSecurity(0).getSchemesMap().get("oauth").getListCount());
        assertEquals("read", result.getSecurity(0).getSchemesMap().get("oauth").getList(0));
        assertEquals(1, result.getSecuritySchemesMap().size());
        assertEquals(true, result.getSecuritySchemesMap().containsKey("basic"));
        assertEquals(result.getSecuritySchemesMap().get("basic").getApiKeySecurityScheme().getDefaultInstanceForType(), result.getSecuritySchemesMap().get("basic").getApiKeySecurityScheme());
        assertEquals(result.getSecuritySchemesMap().get("basic").getOauth2SecurityScheme().getDefaultInstanceForType(), result.getSecuritySchemesMap().get("basic").getOauth2SecurityScheme());
        assertEquals("basic", result.getSecuritySchemesMap().get("basic").getHttpAuthSecurityScheme().getScheme());
        assertEquals("Basic Auth", result.getSecuritySchemesMap().get("basic").getHttpAuthSecurityScheme().getDescription());
    }

    @Test
    public void convertTask() {
        Task task = new Task.Builder().id("cancel-task-123")
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();
        io.a2a.grpc.Task result = ProtoUtils.ToProto.task(task);
        assertEquals("session-xyz", result.getContextId());
        assertEquals("cancel-task-123", result.getId());
        assertEquals(io.a2a.grpc.TaskState.TASK_STATE_SUBMITTED, result.getStatus().getState());
        assertEquals(0, result.getArtifactsCount());
        assertEquals(0, result.getHistoryCount());
        task = new Task.Builder().id("cancel-task-123")
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .artifacts(List.of(new Artifact.Builder()
                        .artifactId("11")
                        .name("artefact")
                        .parts(new TextPart("text"))
                        .build()))
                .history(List.of(SIMPLE_MESSAGE))
                .metadata(Collections.emptyMap())
                .build();
        result = ProtoUtils.ToProto.task(task);
        assertEquals("session-xyz", result.getContextId());
        assertEquals("cancel-task-123", result.getId());
        assertEquals(io.a2a.grpc.TaskState.TASK_STATE_SUBMITTED, result.getStatus().getState());
        assertEquals(1, result.getArtifactsCount());
        assertEquals("11", result.getArtifacts(0).getArtifactId());
        assertEquals("artefact", result.getArtifacts(0).getName());
        assertEquals(1, result.getArtifacts(0).getPartsCount());
        assertEquals(true, result.getArtifacts(0).getParts(0).hasText());
        assertEquals(false, result.getArtifacts(0).getParts(0).hasFile());
        assertEquals(false, result.getArtifacts(0).getParts(0).hasData());
        assertEquals("text", result.getArtifacts(0).getParts(0).getText());
        assertEquals(1, result.getHistoryCount());
                assertEquals("context-1234", result.getHistory(0).getContextId());
        assertEquals("message-1234", result.getHistory(0).getMessageId());
        assertEquals(ROLE_USER, result.getHistory(0).getRole());
        assertEquals(1, result.getHistory(0).getPartsCount());
        assertEquals("tell me a joke", result.getHistory(0).getParts(0).getText());
        assertEquals(io.a2a.grpc.FilePart.getDefaultInstance(), result.getHistory(0).getParts(0).getFile());
        assertEquals(io.a2a.grpc.DataPart.getDefaultInstance(), result.getHistory(0).getParts(0).getData());
    }

    @Test
    public void convertMessage() {
        io.a2a.grpc.Message result = ProtoUtils.ToProto.message(SIMPLE_MESSAGE);
        assertEquals("context-1234", result.getContextId());
        assertEquals("message-1234", result.getMessageId());
        assertEquals(ROLE_USER, result.getRole());
        assertEquals(1, result.getPartsCount());
        assertEquals("tell me a joke", result.getParts(0).getText());
        assertEquals(io.a2a.grpc.FilePart.getDefaultInstance(), result.getParts(0).getFile());
        assertEquals(io.a2a.grpc.DataPart.getDefaultInstance(), result.getParts(0).getData());
        Message message = new Message.Builder()
                .role(Message.Role.AGENT)
                .parts(Collections.singletonList(new TextPart("tell me a joke")))
                .messageId("message-1234")
                .build();
        result = ProtoUtils.ToProto.message(message);
        assertEquals("", result.getContextId());
        assertEquals("message-1234", result.getMessageId());
        assertEquals(ROLE_AGENT, result.getRole());
        assertEquals(1, result.getPartsCount());
        assertEquals("tell me a joke", result.getParts(0).getText());
        assertEquals(io.a2a.grpc.FilePart.getDefaultInstance(), result.getParts(0).getFile());
        assertEquals(io.a2a.grpc.DataPart.getDefaultInstance(), result.getParts(0).getData());
    }

    @Test
    public void convertTaskPushNotificationConfig() {
        TaskPushNotificationConfig taskPushConfig = new TaskPushNotificationConfig("push-task-123",
                new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id("xyz")
                        .build());
        io.a2a.grpc.TaskPushNotificationConfig result = ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig);
        assertEquals("tasks/push-task-123/pushNotificationConfigs/xyz", result.getName());
        assertNotNull(result.getPushNotificationConfig());
        assertEquals("http://example.com", result.getPushNotificationConfig().getUrl());
        assertEquals("xyz", result.getPushNotificationConfig().getId());
        assertEquals(false, result.getPushNotificationConfig().hasAuthentication());
        taskPushConfig
                = new TaskPushNotificationConfig("push-task-123",
                        new PushNotificationConfig.Builder()
                                .token("AAAAAA")
                                .authenticationInfo(new AuthenticationInfo(Collections.singletonList("jwt"), "credentials"))
                                .url("http://example.com")
                                .id("xyz")
                                .build());
        result = ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig);
        assertEquals("tasks/push-task-123/pushNotificationConfigs/xyz", result.getName());
        assertNotNull(result.getPushNotificationConfig());
        assertEquals("http://example.com", result.getPushNotificationConfig().getUrl());
        assertEquals("xyz", result.getPushNotificationConfig().getId());
        assertEquals("AAAAAA", result.getPushNotificationConfig().getToken());
        assertEquals(true, result.getPushNotificationConfig().hasAuthentication());
        assertEquals("credentials", result.getPushNotificationConfig().getAuthentication().getCredentials());
        assertEquals(1, result.getPushNotificationConfig().getAuthentication().getSchemesCount());
        assertEquals("jwt", result.getPushNotificationConfig().getAuthentication().getSchemes(0));
    }

    @Test
    public void convertTaskArtifactUpdateEvent() {
        TaskArtifactUpdateEvent task = new TaskArtifactUpdateEvent.Builder()
                .taskId("task-123")
                .contextId("session-123")
                .artifact(new Artifact.Builder()
                        .artifactId("11")
                        .parts(new TextPart("text"))
                        .build()).build();
        io.a2a.grpc.TaskArtifactUpdateEvent result = ProtoUtils.ToProto.taskArtifactUpdateEvent(task);
        assertEquals("task-123", result.getTaskId());
        assertEquals("session-123", result.getContextId());
        assertNotNull(result.getArtifact());
        assertEquals("11", result.getArtifact().getArtifactId());
        assertEquals(1, result.getArtifact().getPartsCount());
        assertEquals("text", result.getArtifact().getParts(0).getText());
    }

    @Test
    public void convertTaskStatusUpdateEvent() {
        TaskStatusUpdateEvent tsue = new TaskStatusUpdateEvent.Builder()
                .taskId("1234")
                .contextId("xyz")
                .status(new TaskStatus(TaskState.COMPLETED))
                .isFinal(true)
                .build();
        io.a2a.grpc.TaskStatusUpdateEvent result = ProtoUtils.ToProto.taskStatusUpdateEvent(tsue);
        assertEquals("1234", result.getTaskId());
        assertEquals("xyz", result.getContextId());
        assertEquals(true, result.getFinal());
        assertEquals(io.a2a.grpc.TaskState.TASK_STATE_COMPLETED, result.getStatus().getState());
    }

    @Test
    public void convertSendMessageConfiguration() {
        MessageSendConfiguration configuration = new MessageSendConfiguration.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(false)
                .build();
        SendMessageConfiguration result = ProtoUtils.ToProto.messageSendConfiguration(configuration);
        assertEquals(false, result.getBlocking());
        assertEquals(1, result.getAcceptedOutputModesCount());
        assertEquals("text", result.getAcceptedOutputModesBytes(0).toStringUtf8());
    }

    @Test
    public void convertTaskTimestampStatus() {
        OffsetDateTime expectedTimestamp = OffsetDateTime.parse("2024-10-05T12:34:56Z");
        TaskStatus testStatus = new TaskStatus(TaskState.COMPLETED, null, expectedTimestamp);
        Task task = new Task.Builder()
                .id("task-123")
                .contextId("context-456")
                .status(testStatus)
                .build();

        io.a2a.grpc.Task grpcTask = ProtoUtils.ToProto.task(task);
        task = ProtoUtils.FromProto.task(grpcTask);
        TaskStatus status = task.getStatus();
        assertEquals(TaskState.COMPLETED, status.state());
        assertNotNull(status.timestamp());
        assertEquals(expectedTimestamp, status.timestamp());
    }
}
