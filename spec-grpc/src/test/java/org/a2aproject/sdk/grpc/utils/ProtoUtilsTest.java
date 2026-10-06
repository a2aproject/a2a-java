package org.a2aproject.sdk.grpc.utils;

import static org.a2aproject.sdk.grpc.Role.ROLE_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.a2aproject.sdk.grpc.SendMessageResponse;
import org.a2aproject.sdk.grpc.StreamResponse;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.GetExtendedAgentCardParams;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsResult;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendConfiguration;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import org.a2aproject.sdk.spec.TextPart;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.junit.jupiter.api.Test;

/**
 * Additional coverage for {@link ProtoUtils}, complementing {@link ToProtoTest}: the
 * streaming/oneof conversions and the {@link ProtoUtils.FromProto} direction, including
 * round-trip conversions for the request and response wrappers.
 */
public class ProtoUtilsTest {

    private static final Message SIMPLE_MESSAGE = Message.builder()
            .role(Message.Role.ROLE_USER)
            .parts(Collections.singletonList(new TextPart("tell me a joke", Map.of())))
            .contextId("context-1234")
            .messageId("message-1234")
            .metadata(Map.of())
            .build();

    private static final Task SIMPLE_TASK = Task.builder()
            .id("task-1")
            .contextId("ctx-1")
            .status(new TaskStatus(TaskState.TASK_STATE_WORKING))
            .metadata(Map.of())
            .build();

    @Test
    public void streamResponseWithTask() {
        StreamResponse response = ProtoUtils.ToProto.streamResponse(SIMPLE_TASK);
        assertTrue(response.hasTask());
        assertEquals("task-1", response.getTask().getId());

        StreamingEventKind converted = ProtoUtils.FromProto.streamingEventKind(response);
        assertTrue(converted instanceof Task);
        assertEquals(SIMPLE_TASK, converted);
    }

    @Test
    public void streamResponseWithMessage() {
        StreamResponse response = ProtoUtils.ToProto.streamResponse(SIMPLE_MESSAGE);
        assertTrue(response.hasMessage());
        assertEquals("message-1234", response.getMessage().getMessageId());

        StreamingEventKind converted = ProtoUtils.FromProto.streamingEventKind(response);
        assertTrue(converted instanceof Message);
        assertEquals(SIMPLE_MESSAGE, converted);
    }

    @Test
    public void streamResponseWithStatusUpdate() {
        TaskStatusUpdateEvent event = new TaskStatusUpdateEvent(
                "task-1",
                new TaskStatus(TaskState.TASK_STATE_WORKING),
                "ctx-1",
                null);
        StreamResponse response = ProtoUtils.ToProto.streamResponse(event);
        assertTrue(response.hasStatusUpdate());
        assertEquals("task-1", response.getStatusUpdate().getTaskId());
        assertEquals("ctx-1", response.getStatusUpdate().getContextId());
        assertEquals(org.a2aproject.sdk.grpc.TaskState.TASK_STATE_WORKING,
                response.getStatusUpdate().getStatus().getState());

        StreamingEventKind converted = ProtoUtils.FromProto.streamingEventKind(response);
        assertTrue(converted instanceof TaskStatusUpdateEvent);
        assertEquals("task-1", ((TaskStatusUpdateEvent) converted).taskId());
        assertEquals(TaskState.TASK_STATE_WORKING, ((TaskStatusUpdateEvent) converted).status().state());
        assertEquals("ctx-1", ((TaskStatusUpdateEvent) converted).contextId());
    }

    @Test
    public void streamResponseWithArtifactUpdate() {
        TaskArtifactUpdateEvent event = TaskArtifactUpdateEvent.builder()
                .taskId("task-1")
                .contextId("ctx-1")
                .artifact(Artifact.builder()
                        .artifactId("artifact-1")
                        .parts(new TextPart("text", Map.of()))
                        .build())
                .build();
        StreamResponse response = ProtoUtils.ToProto.streamResponse(event);
        assertTrue(response.hasArtifactUpdate());
        assertEquals("task-1", response.getArtifactUpdate().getTaskId());
        assertEquals("artifact-1", response.getArtifactUpdate().getArtifact().getArtifactId());

        StreamingEventKind converted = ProtoUtils.FromProto.streamingEventKind(response);
        assertTrue(converted instanceof TaskArtifactUpdateEvent);
        assertEquals("task-1", ((TaskArtifactUpdateEvent) converted).taskId());
        assertEquals("ctx-1", ((TaskArtifactUpdateEvent) converted).contextId());
        assertEquals("artifact-1", ((TaskArtifactUpdateEvent) converted).artifact().artifactId());
    }

    @Test
    public void taskOrMessageWithTask() {
        SendMessageResponse response = ProtoUtils.ToProto.taskOrMessage(SIMPLE_TASK);
        assertTrue(response.hasTask());
        assertEquals("task-1", response.getTask().getId());
        assertEquals(SIMPLE_TASK, ProtoUtils.FromProto.task(response.getTask()));
    }

    @Test
    public void taskOrMessageWithMessage() {
        SendMessageResponse response = ProtoUtils.ToProto.taskOrMessage(SIMPLE_MESSAGE);
        assertTrue(response.hasMessage());
        assertEquals("message-1234", response.getMessage().getMessageId());
        assertEquals(SIMPLE_MESSAGE, ProtoUtils.FromProto.message(response.getMessage()));
    }

    @Test
    public void getTaskRequestRoundTrip() {
        TaskQueryParams params = new TaskQueryParams("task-123", 5, "tenant-1");
        org.a2aproject.sdk.grpc.GetTaskRequest request = ProtoUtils.ToProto.getTaskRequest(params);
        assertEquals("task-123", request.getId());
        assertEquals(params, ProtoUtils.FromProto.taskQueryParams(request));
    }

    @Test
    public void cancelTaskRequestRoundTrip() {
        CancelTaskParams params = new CancelTaskParams("task-123", "tenant-1", Map.of("key", "value"));
        org.a2aproject.sdk.grpc.CancelTaskRequest request = ProtoUtils.ToProto.cancelTaskRequest(params);
        assertEquals("task-123", request.getId());
        assertEquals(params, ProtoUtils.FromProto.cancelTaskParams(request));
    }

    @Test
    public void subscribeToTaskRequestRoundTrip() {
        TaskIdParams params = new TaskIdParams("task-123", "tenant-1");
        org.a2aproject.sdk.grpc.SubscribeToTaskRequest request = ProtoUtils.ToProto.subscribeToTaskRequest(params);
        assertEquals("task-123", request.getId());
        assertEquals(params, ProtoUtils.FromProto.taskIdParams(request));
    }

    @Test
    public void sendMessageRequestRoundTrip() {
        MessageSendConfiguration configuration = MessageSendConfiguration.builder()
                .acceptedOutputModes(List.of("text"))
                .returnImmediately(true)
                .build();
        MessageSendParams params = new MessageSendParams(
                SIMPLE_MESSAGE, configuration, Map.of("key", "value"), "tenant-1");
        org.a2aproject.sdk.grpc.SendMessageRequest request = ProtoUtils.ToProto.sendMessageRequest(params);

        assertEquals("message-1234", request.getMessage().getMessageId());
        assertEquals(params, ProtoUtils.FromProto.messageSendParams(request));
    }

    @Test
    public void getTaskPushNotificationConfigRequestRoundTrip() {
        GetTaskPushNotificationConfigParams params = new GetTaskPushNotificationConfigParams(
                "task-123", "config-456", "tenant-1");
        org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest request =
                ProtoUtils.ToProto.getTaskPushNotificationConfigRequest(params);
        assertEquals("task-123", request.getTaskId());
        assertEquals(params, ProtoUtils.FromProto.getTaskPushNotificationConfigParams(request));
    }

    @Test
    public void listTaskPushNotificationConfigsRequestRoundTrip() {
        ListTaskPushNotificationConfigsParams params = new ListTaskPushNotificationConfigsParams(
                "task-123", 10, "token-1", "tenant-1");
        org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsRequest request =
                ProtoUtils.ToProto.listTaskPushNotificationConfigsRequest(params);
        assertEquals("task-123", request.getTaskId());
        assertEquals(params, ProtoUtils.FromProto.listTaskPushNotificationConfigsParams(request));
    }

    @Test
    public void listTaskPushNotificationConfigsResultRoundTrip() {
        TaskPushNotificationConfig config = TaskPushNotificationConfig.builder()
                .id("config-1")
                .taskId("task-1")
                .url("http://example.com")
                .build();
        ListTaskPushNotificationConfigsResult result = new ListTaskPushNotificationConfigsResult(
                List.of(config), "next-token-1");

        org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsResponse response =
                ProtoUtils.ToProto.listTaskPushNotificationConfigsResponse(result);
        assertEquals(1, response.getConfigsCount());
        assertEquals("next-token-1", response.getNextPageToken());
        assertEquals(result, ProtoUtils.FromProto.listTaskPushNotificationConfigsResult(response));
    }

    @Test
    public void listTaskPushNotificationConfigsResultEmptyPageTokenBecomesNull() {
        ListTaskPushNotificationConfigsResult result = new ListTaskPushNotificationConfigsResult(
                List.of(), null);

        org.a2aproject.sdk.grpc.ListTaskPushNotificationConfigsResponse response =
                ProtoUtils.ToProto.listTaskPushNotificationConfigsResponse(result);

        // The proto default for an unset string is the empty string; the spec convention is null
        assertNull(ProtoUtils.FromProto.listTaskPushNotificationConfigsResult(response).nextPageToken());
    }

    @Test
    public void listTasksResultRoundTrip() {
        ListTasksResult result = new ListTasksResult(List.of(SIMPLE_TASK), 3, 1, "next-token-1");

        org.a2aproject.sdk.grpc.ListTasksResponse response = ProtoUtils.ToProto.listTasksResult(result);
        assertEquals(1, response.getTasksCount());
        assertEquals(3, response.getTotalSize());
        assertEquals(1, response.getPageSize());
        assertEquals("next-token-1", response.getNextPageToken());
        assertEquals(result, ProtoUtils.FromProto.listTasksResult(response));
    }

    @Test
    public void listTasksParamsRoundTrip() {
        ListTasksParams params = new ListTasksParams(
                "ctx-1", TaskState.TASK_STATE_WORKING, 10, "token-1", 5, null, true, "tenant-1");
        org.a2aproject.sdk.grpc.ListTasksRequest request = ProtoUtils.ToProto.listTasksParams(params);
        assertEquals("ctx-1", request.getContextId());
        assertEquals(params, ProtoUtils.FromProto.listTasksParams(request));
    }

    @Test
    public void getExtendedAgentCardParamsNormalizesEmptyTenantToNull() {
        GetExtendedAgentCardParams params = ProtoUtils.FromProto.getExtendedAgentCardParams(
                org.a2aproject.sdk.grpc.GetExtendedAgentCardRequest.getDefaultInstance());
        assertNull(params.tenant());

        params = ProtoUtils.FromProto.getExtendedAgentCardParams(
                org.a2aproject.sdk.grpc.GetExtendedAgentCardRequest.newBuilder().setTenant("tenant-1").build());
        assertEquals(new GetExtendedAgentCardParams("tenant-1"), params);
    }

    @Test
    public void fromProtoMessageRequiresMessageId() {
        assertThrows(InvalidParamsError.class,
                () -> ProtoUtils.FromProto.message(org.a2aproject.sdk.grpc.Message.getDefaultInstance()));
    }

    @Test
    public void agentCardRoundTrip() {
        // Collections are built with canonical empty values: the converters normalize absent
        // collections and maps to their empty forms when converting back from proto
        AgentCard agentCard = AgentCard.builder()
                .name("Hello World Agent")
                .description("Just a hello world agent")
                .supportedInterfaces(Collections.singletonList(new AgentInterface("jsonrpc", "http://localhost:9999")))
                .version("1.0.0")
                .documentationUrl("http://example.com/docs")
                .capabilities(AgentCapabilities.builder()
                        .streaming(true)
                        .pushNotifications(true)
                        .extensions(List.of())
                        .build())
                .defaultInputModes(List.of("text", "application/json"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder()
                        .id("hello_world")
                        .name("Returns hello world")
                        .description("just returns hello world")
                        .tags(List.of("hello world"))
                        .examples(List.of("hi", "hello world"))
                        .inputModes(List.of())
                        .outputModes(List.of())
                        .securityRequirements(List.of())
                        .build()))
                .securitySchemes(Map.of())
                .securityRequirements(List.of())
                .signatures(List.of())
                .build();

        org.a2aproject.sdk.grpc.AgentCard protoCard = ProtoUtils.ToProto.agentCard(agentCard);
        assertEquals(agentCard, ProtoUtils.FromProto.agentCard(protoCard));
    }

    @Test
    public void taskRoundTripWithArtifactsAndHistory() {
        Task task = Task.builder()
                .id("task-1")
                .contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED,
                        SIMPLE_MESSAGE, java.time.OffsetDateTime.parse("2026-09-21T00:00:00Z")))
                .artifacts(List.of(Artifact.builder()
                        .artifactId("artifact-1")
                        .name("artifact")
                        .parts(new TextPart("text", Map.of()))
                        .metadata(Map.of())
                        .build()))
                .history(List.of(SIMPLE_MESSAGE))
                .metadata(Map.of("key", "value"))
                .build();

        org.a2aproject.sdk.grpc.Task protoTask = ProtoUtils.ToProto.task(task);
        assertEquals(task, ProtoUtils.FromProto.task(protoTask));
    }
}
