package org.a2aproject.sdk.compat03.grpc.utils;

import static org.a2aproject.sdk.compat03.grpc.Role.ROLE_AGENT;
import static org.a2aproject.sdk.compat03.grpc.Role.ROLE_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.compat03.grpc.SendMessageConfiguration;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.compat03.spec.Artifact_v0_3;
import org.a2aproject.sdk.compat03.spec.HTTPAuthSecurityScheme_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendConfiguration_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationAuthenticationInfo_v0_3;
import org.a2aproject.sdk.compat03.spec.PushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskArtifactUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskPushNotificationConfig_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatus_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskStatusUpdateEvent_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.Test;

public class ToProto_v0_3_Test {

    private static final Message_v0_3 SIMPLE_MESSAGE = new Message_v0_3.Builder()
            .role(Message_v0_3.Role.USER)
            .parts(Collections.singletonList(new TextPart_v0_3("tell me a joke")))
            .contextId("context-1234")
            .messageId("message-1234")
            .build();

    @Test
    public void convertAgentCard() {
        AgentCard_v0_3 agentCard = new AgentCard_v0_3.Builder()
                .name("Hello World Agent")
                .description("Just a hello world agent")
                .url("http://localhost:9999")
                .version("1.0.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities_v0_3.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill_v0_3.Builder()
                        .id("hello_world")
                        .name("Returns hello world")
                        .description("just returns hello world")
                        .tags(Collections.singletonList("hello world"))
                        .examples(List.of("hi", "hello world"))
                        .build()))
                .protocolVersion("0.2.5")
                .build();
        org.a2aproject.sdk.compat03.grpc.AgentCard result = ProtoUtils_v0_3.ToProto.agentCard(agentCard);
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
        agentCard = new AgentCard_v0_3.Builder()
                .name("Hello World Agent")
                .description("Just a hello world agent")
                .url("http://localhost:9999")
                .version("1.0.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(new AgentCapabilities_v0_3.Builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .stateTransitionHistory(true)
                        .build())
                .defaultInputModes(Collections.singletonList("text"))
                .defaultOutputModes(Collections.singletonList("text"))
                .skills(Collections.singletonList(new AgentSkill_v0_3.Builder()
                        .id("hello_world")
                        .name("Returns hello world")
                        .description("just returns hello world")
                        .tags(Collections.singletonList("hello world"))
                        .examples(List.of("hi", "hello world"))
                        .build()))
                .preferredTransport("HTTP+JSON")
//                .iconUrl("http://example.com/icon.svg")
                .securitySchemes(Map.of("basic", new HTTPAuthSecurityScheme_v0_3.Builder().scheme("basic").description("Basic Auth").build()))
                .security(List.of(Map.of("oauth", List.of("read"))))
                .protocolVersion("0.2.5")
                .build();
       result = ProtoUtils_v0_3.ToProto.agentCard(agentCard);
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
        Task_v0_3 task = new Task_v0_3.Builder().id("cancel-task-123")
                .contextId("session-xyz")
                .status(new TaskStatus_v0_3(TaskState_v0_3.SUBMITTED))
                .build();
        org.a2aproject.sdk.compat03.grpc.Task result = ProtoUtils_v0_3.ToProto.task(task);
        assertEquals("session-xyz", result.getContextId());
        assertEquals("cancel-task-123", result.getId());
        assertEquals(org.a2aproject.sdk.compat03.grpc.TaskState.TASK_STATE_SUBMITTED, result.getStatus().getState());
        assertEquals(0, result.getArtifactsCount());
        assertEquals(0, result.getHistoryCount());
        task = new Task_v0_3.Builder().id("cancel-task-123")
                .contextId("session-xyz")
                .status(new TaskStatus_v0_3(TaskState_v0_3.SUBMITTED))
                .artifacts(List.of(new Artifact_v0_3.Builder()
                        .artifactId("11")
                        .name("artefact")
                        .parts(new TextPart_v0_3("text"))
                        .build()))
                .history(List.of(SIMPLE_MESSAGE))
                .metadata(Collections.emptyMap())
                .build();
        result = ProtoUtils_v0_3.ToProto.task(task);
        assertEquals("session-xyz", result.getContextId());
        assertEquals("cancel-task-123", result.getId());
        assertEquals(org.a2aproject.sdk.compat03.grpc.TaskState.TASK_STATE_SUBMITTED, result.getStatus().getState());
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
        assertEquals(1, result.getHistory(0).getContentCount());
        assertEquals("tell me a joke", result.getHistory(0).getContent(0).getText());
        assertEquals(org.a2aproject.sdk.compat03.grpc.FilePart.getDefaultInstance(), result.getHistory(0).getContent(0).getFile());
        assertEquals(org.a2aproject.sdk.compat03.grpc.DataPart.getDefaultInstance(), result.getHistory(0).getContent(0).getData());
    }

    @Test
    public void convertMessage() {
        org.a2aproject.sdk.compat03.grpc.Message result = ProtoUtils_v0_3.ToProto.message(SIMPLE_MESSAGE);
        assertEquals("context-1234", result.getContextId());
        assertEquals("message-1234", result.getMessageId());
        assertEquals(ROLE_USER, result.getRole());
        assertEquals(1, result.getContentCount());
        assertEquals("tell me a joke", result.getContent(0).getText());
        assertEquals(org.a2aproject.sdk.compat03.grpc.FilePart.getDefaultInstance(), result.getContent(0).getFile());
        assertEquals(org.a2aproject.sdk.compat03.grpc.DataPart.getDefaultInstance(), result.getContent(0).getData());
        Message_v0_3 message = new Message_v0_3.Builder()
                .role(Message_v0_3.Role.AGENT)
                .parts(Collections.singletonList(new TextPart_v0_3("tell me a joke")))
                .messageId("message-1234")
                .build();
        result = ProtoUtils_v0_3.ToProto.message(message);
        assertEquals("", result.getContextId());
        assertEquals("message-1234", result.getMessageId());
        assertEquals(ROLE_AGENT, result.getRole());
        assertEquals(1, result.getContentCount());
        assertEquals("tell me a joke", result.getContent(0).getText());
        assertEquals(org.a2aproject.sdk.compat03.grpc.FilePart.getDefaultInstance(), result.getContent(0).getFile());
        assertEquals(org.a2aproject.sdk.compat03.grpc.DataPart.getDefaultInstance(), result.getContent(0).getData());
    }

    @Test
    public void convertTaskPushNotificationConfig() {
        TaskPushNotificationConfig_v0_3 taskPushConfig = new TaskPushNotificationConfig_v0_3("push-task-123",
                new PushNotificationConfig_v0_3.Builder()
                        .url("http://example.com")
                        .id("xyz")
                        .build());
        org.a2aproject.sdk.compat03.grpc.TaskPushNotificationConfig result = ProtoUtils_v0_3.ToProto.taskPushNotificationConfig(taskPushConfig);
        assertEquals("tasks/push-task-123/pushNotificationConfigs/xyz", result.getName());
        assertNotNull(result.getPushNotificationConfig());
        assertEquals("http://example.com", result.getPushNotificationConfig().getUrl());
        assertEquals("xyz", result.getPushNotificationConfig().getId());
        assertEquals(false, result.getPushNotificationConfig().hasAuthentication());
        taskPushConfig
                = new TaskPushNotificationConfig_v0_3("push-task-123",
                        new PushNotificationConfig_v0_3.Builder()
                                .token("AAAAAA")
                                .authenticationInfo(new PushNotificationAuthenticationInfo_v0_3(Collections.singletonList("jwt"), "credentials"))
                                .url("http://example.com")
                                .id("xyz")
                                .build());
        result = ProtoUtils_v0_3.ToProto.taskPushNotificationConfig(taskPushConfig);
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
        TaskArtifactUpdateEvent_v0_3 task = new TaskArtifactUpdateEvent_v0_3.Builder()
                .taskId("task-123")
                .contextId("session-123")
                .artifact(new Artifact_v0_3.Builder()
                        .artifactId("11")
                        .parts(new TextPart_v0_3("text"))
                        .build()).build();
        org.a2aproject.sdk.compat03.grpc.TaskArtifactUpdateEvent result = ProtoUtils_v0_3.ToProto.taskArtifactUpdateEvent(task);
        assertEquals("task-123", result.getTaskId());
        assertEquals("session-123", result.getContextId());
        assertNotNull(result.getArtifact());
        assertEquals("11", result.getArtifact().getArtifactId());
        assertEquals(1, result.getArtifact().getPartsCount());
        assertEquals("text", result.getArtifact().getParts(0).getText());
    }

    @Test
    public void convertTaskStatusUpdateEvent() {
        TaskStatusUpdateEvent_v0_3 tsue = new TaskStatusUpdateEvent_v0_3.Builder()
                .taskId("1234")
                .contextId("xyz")
                .status(new TaskStatus_v0_3(TaskState_v0_3.COMPLETED))
                .isFinal(true)
                .build();
        org.a2aproject.sdk.compat03.grpc.TaskStatusUpdateEvent result = ProtoUtils_v0_3.ToProto.taskStatusUpdateEvent(tsue);
        assertEquals("1234", result.getTaskId());
        assertEquals("xyz", result.getContextId());
        assertEquals(true, result.getFinal());
        assertEquals(org.a2aproject.sdk.compat03.grpc.TaskState.TASK_STATE_COMPLETED, result.getStatus().getState());
    }

    @Test
    public void convertSendMessageConfiguration() {
        MessageSendConfiguration_v0_3 configuration = new MessageSendConfiguration_v0_3.Builder()
                .acceptedOutputModes(List.of("text"))
                .blocking(false)
                .build();
        SendMessageConfiguration result = ProtoUtils_v0_3.ToProto.messageSendConfiguration(configuration);
        assertEquals(false, result.getBlocking());
        assertEquals(1, result.getAcceptedOutputModesCount());
        assertEquals("text", result.getAcceptedOutputModesBytes(0).toStringUtf8());
    }

    @Test
    public void convertTaskTimestampStatus() {
        OffsetDateTime expectedTimestamp = OffsetDateTime.parse("2024-10-05T12:34:56Z");
        TaskStatus_v0_3 testStatus = new TaskStatus_v0_3(TaskState_v0_3.COMPLETED, null, expectedTimestamp);
        Task_v0_3 task = new Task_v0_3.Builder()
                .id("task-123")
                .contextId("context-456")
                .status(testStatus)
                .build();

        org.a2aproject.sdk.compat03.grpc.Task grpcTask = ProtoUtils_v0_3.ToProto.task(task);
        task = ProtoUtils_v0_3.FromProto.task(grpcTask);
        TaskStatus_v0_3 status = task.getStatus();
        assertEquals(TaskState_v0_3.COMPLETED, status.state());
        assertNotNull(status.timestamp());
        assertEquals(expectedTimestamp, status.timestamp());
    }
}
