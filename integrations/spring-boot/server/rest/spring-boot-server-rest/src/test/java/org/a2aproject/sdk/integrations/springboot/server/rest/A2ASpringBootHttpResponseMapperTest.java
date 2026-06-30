package org.a2aproject.sdk.integrations.springboot.server.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.SubmissionPublisher;

import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class A2ASpringBootHttpResponseMapperTest {

    private final A2ASpringBootHttpResponseMapper mapper = new A2ASpringBootHttpResponseMapper();

    @Test
    void serializesJsonBodies() {
        Task task = task("task-1");

        var response = mapper.okTask(task);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertTrue(response.getBody().contains("\"task-1\""));
    }

    @Test
    void serializesA2AErrors() {
        var response = mapper.error(new InvalidParamsError("bad request"));

        assertEquals(422, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertTrue(response.getBody().contains("\"bad request\""));
    }

    @Test
    void serializesThrowableWithoutMessageUsingFallbackClassName() {
        var response = mapper.error(new RuntimeException());

        assertEquals(500, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertTrue(response.getBody().contains(RuntimeException.class.getName()));
    }

    @Test
    void createsCreatedResponses() {
        var response = mapper.createdTaskPushNotificationConfig(
                org.a2aproject.sdk.spec.TaskPushNotificationConfig.builder()
                        .id("config-1")
                        .taskId("task-3")
                        .url("https://example.com/hook")
                        .build());

        assertEquals(201, response.getStatusCodeValue());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertTrue(response.getBody().contains("\"task-3\""));
    }

    @Test
    void createsNoContentResponses() {
        var response = mapper.noContent();

        assertEquals(204, response.getStatusCodeValue());
        assertEquals(null, response.getBody());
    }

    @Test
    void createsSseEmitter() {
        ServerCallContext context = serverCallContext();
        SubmissionPublisher<StreamingEventKind> publisher = new SubmissionPublisher<>();
        publisher.submit(Message.builder()
                .role(Message.Role.ROLE_AGENT)
                .messageId("msg-1")
                .parts(new TextPart("ok"))
                .build());
        publisher.close();

        var emitter = mapper.toSseEmitter(publisher, context);

        assertTrue(emitter != null);
    }

    @Test
    void mapsSendMessageResponses() {
        var taskResponse = mapper.okSendMessage(task("task-2"));
        var messageResponse = mapper.okSendMessage(Message.builder()
                .role(Message.Role.ROLE_AGENT)
                .messageId("msg-1")
                .parts(new TextPart("ok"))
                .build());

        assertEquals(200, taskResponse.getStatusCodeValue());
        assertEquals(200, messageResponse.getStatusCodeValue());
        assertTrue(taskResponse.getBody().contains("\"task\""));
        assertTrue(messageResponse.getBody().contains("\"message\""));
    }

    private Task task(String id) {
        return Task.builder()
                .id(id)
                .contextId("ctx-1")
                .status(new TaskStatus(TaskState.TASK_STATE_SUBMITTED))
                .build();
    }

    private ServerCallContext serverCallContext() {
        return new ServerCallContext(UnauthenticatedUser.INSTANCE, java.util.Map.of(), java.util.Set.of(), null);
    }
}
