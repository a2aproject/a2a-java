package io.a2a.grpc.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.StreamingEventKind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Test;

public class StreamResponseMapperTest {

    @Test
    void testConvertTask_ToProto() {
        // Arrange
        Task task = Task.builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus(TaskState.COMPLETED))
                .build();

        // Act
        io.a2a.grpc.StreamResponse result = StreamResponseMapper.INSTANCE.toProto(task);

        // Assert
        assertNotNull(result);
        assertEquals(io.a2a.grpc.StreamResponse.PayloadCase.TASK, result.getPayloadCase());
        assertEquals("task-123", result.getTask().getId());
        assertEquals("context-456", result.getTask().getContextId());
        assertEquals(io.a2a.grpc.TaskState.TASK_STATE_COMPLETED, result.getTask().getStatus().getState());
    }

    @Test
    void testConvertTask_FromProto() {
        // Arrange
        io.a2a.grpc.StreamResponse proto = io.a2a.grpc.StreamResponse.newBuilder()
                .setTask(io.a2a.grpc.Task.newBuilder()
                        .setId("task-123")
                        .setContextId("context-456")
                        .setStatus(io.a2a.grpc.TaskStatus.newBuilder()
                                .setState(io.a2a.grpc.TaskState.TASK_STATE_COMPLETED)
                                .build())
                        .build())
                .build();

        // Act
        StreamingEventKind result = StreamResponseMapper.INSTANCE.fromProto(proto);

        // Assert
        assertNotNull(result);
        assertInstanceOf(Task.class, result);
        Task task = (Task) result;
        assertEquals("task-123", task.id());
        assertEquals("context-456", task.contextId());
        assertEquals(TaskState.COMPLETED, task.status().state());
    }

    @Test
    void testConvertMessage_ToProto() {
        // Arrange
        Message message = Message.builder()
                .messageId("msg-123")
                .contextId("context-456")
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("Hello")))
                .build();

        // Act
        io.a2a.grpc.StreamResponse result = StreamResponseMapper.INSTANCE.toProto(message);

        // Assert
        assertNotNull(result);
        assertEquals(io.a2a.grpc.StreamResponse.PayloadCase.MESSAGE, result.getPayloadCase());
        assertEquals("msg-123", result.getMessage().getMessageId());
        assertEquals("context-456", result.getMessage().getContextId());
        assertEquals(io.a2a.grpc.Role.ROLE_USER, result.getMessage().getRole());
    }

    @Test
    void testConvertMessage_FromProto() {
        // Arrange
        io.a2a.grpc.StreamResponse proto = io.a2a.grpc.StreamResponse.newBuilder()
                .setMessage(io.a2a.grpc.Message.newBuilder()
                        .setMessageId("msg-123")
                        .setContextId("context-456")
                        .setRole(io.a2a.grpc.Role.ROLE_USER)
                        .addParts(io.a2a.grpc.Part.newBuilder()
                                .setText("Hello")
                                .build())
                        .build())
                .build();

        // Act
        StreamingEventKind result = StreamResponseMapper.INSTANCE.fromProto(proto);

        // Assert
        assertNotNull(result);
        assertInstanceOf(Message.class, result);
        Message message = (Message) result;
        assertEquals("msg-123", message.messageId());
        assertEquals("context-456", message.contextId());
        assertEquals(Message.Role.USER, message.role());
    }

    @Test
    void testConvertTaskStatusUpdateEvent_ToProto() {
        // Arrange
        TaskStatusUpdateEvent event = TaskStatusUpdateEvent.builder()
                .taskId("task-123")
                .contextId("context-456")
                .status(new TaskStatus(TaskState.WORKING))
                .build();

        // Act
        io.a2a.grpc.StreamResponse result = StreamResponseMapper.INSTANCE.toProto(event);

        // Assert
        assertNotNull(result);
        assertEquals(io.a2a.grpc.StreamResponse.PayloadCase.STATUS_UPDATE, result.getPayloadCase());
        assertEquals("task-123", result.getStatusUpdate().getTaskId());
        assertEquals("context-456", result.getStatusUpdate().getContextId());
        assertEquals(io.a2a.grpc.TaskState.TASK_STATE_WORKING, result.getStatusUpdate().getStatus().getState());
    }

    @Test
    void testConvertTaskStatusUpdateEvent_FromProto() {
        // Arrange
        io.a2a.grpc.StreamResponse proto = io.a2a.grpc.StreamResponse.newBuilder()
                .setStatusUpdate(io.a2a.grpc.TaskStatusUpdateEvent.newBuilder()
                        .setTaskId("task-123")
                        .setContextId("context-456")
                        .setStatus(io.a2a.grpc.TaskStatus.newBuilder()
                                .setState(io.a2a.grpc.TaskState.TASK_STATE_WORKING)
                                .build())
                        .build())
                .build();

        // Act
        StreamingEventKind result = StreamResponseMapper.INSTANCE.fromProto(proto);

        // Assert
        assertNotNull(result);
        assertInstanceOf(TaskStatusUpdateEvent.class, result);
        TaskStatusUpdateEvent event = (TaskStatusUpdateEvent) result;
        assertEquals("task-123", event.taskId());
        assertEquals("context-456", event.contextId());
        assertEquals(TaskState.WORKING, event.status().state());
        assertEquals(false, event.isFinal());
    }

    @Test
    void testConvertTaskArtifactUpdateEvent_ToProto() {
        // Arrange
        TaskArtifactUpdateEvent event = TaskArtifactUpdateEvent.builder()
                .taskId("task-123")
                .contextId("context-456")
                .artifact(Artifact.builder()
                        .artifactId("artifact-1")
                        .name("result")
                        .parts(new TextPart("Result text"))
                        .build())
                .build();

        // Act
        io.a2a.grpc.StreamResponse result = StreamResponseMapper.INSTANCE.toProto(event);

        // Assert
        assertNotNull(result);
        assertEquals(io.a2a.grpc.StreamResponse.PayloadCase.ARTIFACT_UPDATE, result.getPayloadCase());
        assertEquals("task-123", result.getArtifactUpdate().getTaskId());
        assertEquals("context-456", result.getArtifactUpdate().getContextId());
        assertEquals("artifact-1", result.getArtifactUpdate().getArtifact().getArtifactId());
        assertEquals("result", result.getArtifactUpdate().getArtifact().getName());
    }

    @Test
    void testConvertTaskArtifactUpdateEvent_FromProto() {
        // Arrange
        io.a2a.grpc.StreamResponse proto = io.a2a.grpc.StreamResponse.newBuilder()
                .setArtifactUpdate(io.a2a.grpc.TaskArtifactUpdateEvent.newBuilder()
                        .setTaskId("task-123")
                        .setContextId("context-456")
                        .setArtifact(io.a2a.grpc.Artifact.newBuilder()
                                .setArtifactId("artifact-1")
                                .setName("result")
                                .addParts(io.a2a.grpc.Part.newBuilder()
                                        .setText("Result text")
                                        .build())
                                .build())
                        .build())
                .build();

        // Act
        StreamingEventKind result = StreamResponseMapper.INSTANCE.fromProto(proto);

        // Assert
        assertNotNull(result);
        assertInstanceOf(TaskArtifactUpdateEvent.class, result);
        TaskArtifactUpdateEvent event = (TaskArtifactUpdateEvent) result;
        assertEquals("task-123", event.taskId());
        assertEquals("context-456", event.contextId());
        assertEquals("artifact-1", event.artifact().artifactId());
        assertEquals("result", event.artifact().name());
    }

    @Test
    void testConvertStreamResponse_FromProto_PayloadNotSet_ThrowsException() {
        // Arrange
        io.a2a.grpc.StreamResponse proto = io.a2a.grpc.StreamResponse.newBuilder().build();

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            StreamResponseMapper.INSTANCE.fromProto(proto);
        });
        assertEquals("StreamResponse payload oneof field not set", exception.getMessage());
    }

    @Test
    void testConvertStreamResponse_Roundtrip_Task() {
        // Arrange
        Task originalTask = Task.builder()
                .id("task-123")
                .contextId("context-456")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        // Act
        io.a2a.grpc.StreamResponse proto = StreamResponseMapper.INSTANCE.toProto(originalTask);
        StreamingEventKind result = StreamResponseMapper.INSTANCE.fromProto(proto);

        // Assert
        assertNotNull(result);
        assertInstanceOf(Task.class, result);
        Task roundtrippedTask = (Task) result;
        assertEquals(originalTask.id(), roundtrippedTask.id());
        assertEquals(originalTask.contextId(), roundtrippedTask.contextId());
        assertEquals(originalTask.status().state(), roundtrippedTask.status().state());
    }

    @Test
    void testConvertStreamResponse_Roundtrip_Message() {
        // Arrange
        Message originalMessage = Message.builder()
                .messageId("msg-123")
                .contextId("context-456")
                .role(Message.Role.AGENT)
                .parts(Collections.singletonList(new TextPart("Response")))
                .build();

        // Act
        io.a2a.grpc.StreamResponse proto = StreamResponseMapper.INSTANCE.toProto(originalMessage);
        StreamingEventKind result = StreamResponseMapper.INSTANCE.fromProto(proto);

        // Assert
        assertNotNull(result);
        assertInstanceOf(Message.class, result);
        Message roundtrippedMessage = (Message) result;
        assertEquals(originalMessage.messageId(), roundtrippedMessage.messageId());
        assertEquals(originalMessage.contextId(), roundtrippedMessage.contextId());
        assertEquals(originalMessage.role(), roundtrippedMessage.role());
    }
}
