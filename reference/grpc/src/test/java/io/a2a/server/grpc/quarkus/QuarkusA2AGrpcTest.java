package io.a2a.server.grpc.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.a2a.grpc.A2AServiceGrpc;
import io.a2a.grpc.CancelTaskRequest;
import io.a2a.grpc.CreateTaskPushNotificationConfigRequest;
import io.a2a.grpc.DeleteTaskPushNotificationConfigRequest;
import io.a2a.grpc.GetTaskPushNotificationConfigRequest;
import io.a2a.grpc.GetTaskRequest;
import io.a2a.grpc.ListTaskPushNotificationConfigRequest;
import io.a2a.grpc.ListTaskPushNotificationConfigResponse;
import io.a2a.grpc.SendMessageRequest;
import io.a2a.grpc.SendMessageResponse;
import io.a2a.grpc.StreamResponse;
import io.a2a.grpc.TaskSubscriptionRequest;
import io.a2a.grpc.utils.ProtoUtils;
import io.a2a.grpc.GetAgentCardRequest;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.Event;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.Task;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.util.Utils;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class QuarkusA2AGrpcTest {


    private static final Task MINIMAL_TASK = new Task.Builder()
            .id("task-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    private static final Task CANCEL_TASK = new Task.Builder()
            .id("cancel-task-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    private static final Task CANCEL_TASK_NOT_SUPPORTED = new Task.Builder()
            .id("cancel-task-not-supported-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    private static final Task SEND_MESSAGE_NOT_SUPPORTED = new Task.Builder()
            .id("task-not-supported-123")
            .contextId("session-xyz")
            .status(new TaskStatus(TaskState.SUBMITTED))
            .build();

    private static final Message MESSAGE = new Message.Builder()
            .messageId("111")
            .role(Message.Role.AGENT)
            .parts(new TextPart("test message"))
            .build();
    public static final String APPLICATION_JSON = "application/json";

    @GrpcClient("a2-a-service")
    A2AServiceGrpc.A2AServiceBlockingStub client;

    private final int serverPort = 8081;
    @Test
    public void testTaskStoreMethodsSanityTest() throws Exception {
        Task task = new Task.Builder(MINIMAL_TASK).id("abcde").build();
        saveTaskInTaskStore(task);
        Task saved = getTaskFromTaskStore(task.getId());
        assertEquals(task.getId(), saved.getId());
        assertEquals(task.getContextId(), saved.getContextId());
        assertEquals(task.getStatus().state(), saved.getStatus().state());

        deleteTaskInTaskStore(task.getId());
        Task saved2 = getTaskFromTaskStore(task.getId());
        assertNull(saved2);
    }

    @Test
    public void testGetTaskSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            GetTaskRequest request = GetTaskRequest.newBuilder()
                    .setName("tasks/" + MINIMAL_TASK.getId())
                    .build();
            io.a2a.grpc.Task response = client.getTask(request);
            assertEquals("task-123", response.getId());
            assertEquals("session-xyz", response.getContextId());
            assertEquals(io.a2a.grpc.TaskState.TASK_STATE_SUBMITTED, response.getStatus().getState());
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testGetTaskNotFound() throws Exception {
        assertTrue(getTaskFromTaskStore("non-existent-task") == null);
        GetTaskRequest request = GetTaskRequest.newBuilder()
                .setName("tasks/non-existent-task")
                .build();
        try {
            client.getTask(request);
            // Should not reach here
            assertTrue(false, "Expected StatusRuntimeException but method returned normally");
        } catch (StatusRuntimeException e) {
            assertEquals(Status.NOT_FOUND.getCode(), e.getStatus().getCode());
            String description = e.getStatus().getDescription();
            assertTrue(description != null && description.contains("TaskNotFoundError"));
        }
    }

    @Test
    public void testCancelTaskSuccess() throws Exception {
        saveTaskInTaskStore(CANCEL_TASK);
        try {
            CancelTaskRequest request = CancelTaskRequest.newBuilder()
                    .setName("tasks/" + CANCEL_TASK.getId())
                    .build();
            io.a2a.grpc.Task response = client.cancelTask(request);
            assertEquals(CANCEL_TASK.getId(), response.getId());
            assertEquals(CANCEL_TASK.getContextId(), response.getContextId());
            assertEquals(io.a2a.grpc.TaskState.TASK_STATE_CANCELLED, response.getStatus().getState());
        } finally {
            deleteTaskInTaskStore(CANCEL_TASK.getId());
        }
    }

    @Test
    public void testCancelTaskNotFound() throws Exception {
        CancelTaskRequest request = CancelTaskRequest.newBuilder()
                .setName("tasks/non-existent-task")
                .build();
        try {
            client.cancelTask(request);
            // Should not reach here
            assertTrue(false, "Expected StatusRuntimeException but method returned normally");
        } catch (StatusRuntimeException e) {
            assertEquals(Status.NOT_FOUND.getCode(), e.getStatus().getCode());
            String description = e.getStatus().getDescription();
            assertTrue(description != null && description.contains("TaskNotFoundError"));
        }
    }

    @Test
    public void testCancelTaskNotSupported() throws Exception {
        saveTaskInTaskStore(CANCEL_TASK_NOT_SUPPORTED);
        try {
            CancelTaskRequest request = CancelTaskRequest.newBuilder()
                    .setName("tasks/" + CANCEL_TASK_NOT_SUPPORTED.getId())
                    .build();
            try {
                client.cancelTask(request);
                // Should not reach here
                assertTrue(false, "Expected StatusRuntimeException but method returned normally");
            } catch (StatusRuntimeException e) {
                assertEquals(Status.UNIMPLEMENTED.getCode(), e.getStatus().getCode());
                String description = e.getStatus().getDescription();
                assertTrue(description != null && description.contains("UnsupportedOperationError"));
            }
        } finally {
            deleteTaskInTaskStore(CANCEL_TASK_NOT_SUPPORTED.getId());
        }
    }

    @Test
    public void testSendMessageNewMessageSuccess() throws Exception {
        assertTrue(getTaskFromTaskStore(MINIMAL_TASK.getId()) == null);
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = SendMessageRequest.newBuilder()
                .setRequest(ProtoUtils.ToProto.message(message))
                .build();
        SendMessageResponse response = client.sendMessage(request);
        assertTrue(response.hasMsg());
        io.a2a.grpc.Message grpcMessage = response.getMsg();
        // Convert back to spec Message for easier assertions
        Message messageResponse = ProtoUtils.FromProto.message(grpcMessage);
        assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
        assertEquals(MESSAGE.getRole(), messageResponse.getRole());
        Part<?> part = messageResponse.getParts().get(0);
        assertEquals(Part.Kind.TEXT, part.getKind());
        assertEquals("test message", ((TextPart) part).getText());
    }

    @Test
    public void testSendMessageExistingTaskSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            Message message = new Message.Builder(MESSAGE)
                    .taskId(MINIMAL_TASK.getId())
                    .contextId(MINIMAL_TASK.getContextId())
                    .build();

            SendMessageRequest request = SendMessageRequest.newBuilder()
                    .setRequest(ProtoUtils.ToProto.message(message))
                    .build();
            SendMessageResponse response = client.sendMessage(request);

            assertTrue(response.hasMsg());
            io.a2a.grpc.Message grpcMessage = response.getMsg();
            // Convert back to spec Message for easier assertions
            Message messageResponse = ProtoUtils.FromProto.message(grpcMessage);
            assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
            assertEquals(MESSAGE.getRole(), messageResponse.getRole());
            Part<?> part = messageResponse.getParts().get(0);
            assertEquals(Part.Kind.TEXT, part.getKind());
            assertEquals("test message", ((TextPart) part).getText());
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testError() throws Exception {
        Message message = new Message.Builder(MESSAGE)
                .taskId(SEND_MESSAGE_NOT_SUPPORTED.getId())
                .contextId(SEND_MESSAGE_NOT_SUPPORTED.getContextId())
                .build();

        SendMessageRequest request = SendMessageRequest.newBuilder()
                .setRequest(ProtoUtils.ToProto.message(message))
                .build();

        try {
            client.sendMessage(request);
            // Should not reach here
            assertTrue(false, "Expected StatusRuntimeException but method returned normally");
        } catch (StatusRuntimeException e) {
            assertEquals(Status.UNIMPLEMENTED.getCode(), e.getStatus().getCode());
            String description = e.getStatus().getDescription();
            assertTrue(description != null && description.contains("UnsupportedOperationError"));
        }
    }

    @Test
    public void testGetAgentCard() throws Exception {
        // Test gRPC getAgentCard method
        GetAgentCardRequest request = GetAgentCardRequest.newBuilder().build();
        
        io.a2a.grpc.AgentCard grpcAgentCard = client.getAgentCard(request);
        
        // Verify the expected agent card fields directly on the gRPC response
        assertNotNull(grpcAgentCard);
        assertEquals("test-card", grpcAgentCard.getName());
        assertEquals("A test agent card", grpcAgentCard.getDescription());
        assertEquals("http://localhost:" + serverPort, grpcAgentCard.getUrl()); // Use dynamic port
        assertEquals("1.0", grpcAgentCard.getVersion());
        assertEquals("http://example.com/docs", grpcAgentCard.getDocumentationUrl());
        assertTrue(grpcAgentCard.getCapabilities().getPushNotifications());
        assertTrue(grpcAgentCard.getCapabilities().getStreaming());
        // Note: stateTransitionHistory is not present in gRPC AgentCapabilities
        assertTrue(grpcAgentCard.getSkillsList().isEmpty());
    }

    @Test
    public void testGetExtendAgentCardNotSupported() {
        // NOTE: This test is not applicable to gRPC since extended agent card retrieval
        // is an HTTP/REST-specific feature that tests the /agent/authenticatedExtendedCard endpoint.
        // gRPC handles agent capabilities differently through service definitions.
        
        // This stub is maintained to preserve method order compatibility with AbstractA2AServerTest
        // for future migration when extending that base class.
    }

    @Test
    public void testMalformedJSONRPCRequest() {
        // NOTE: This test is not applicable to gRPC since it tests JSON-RPC protocol-specific
        // JSON parsing errors. gRPC uses Protocol Buffers for serialization and has its own
        // parsing and validation mechanisms.
        
        // This stub is maintained to preserve method order compatibility with AbstractA2AServerTest
        // for future migration when extending that base class.
    }

    @Test
    public void testInvalidParamsJSONRPCRequest() {
        // NOTE: This test is not applicable to gRPC since it tests JSON-RPC protocol-specific
        // parameter validation errors. gRPC uses strongly-typed Protocol Buffer messages
        // which provide built-in type safety and validation.
        
        // This stub is maintained to preserve method order compatibility with AbstractA2AServerTest
        // for future migration when extending that base class.
    }

    @Test
    public void testInvalidJSONRPCRequestMissingJsonrpc() {
        // NOTE: This test is not applicable to gRPC since it tests JSON-RPC protocol-specific
        // validation of the "jsonrpc" field. gRPC does not use JSON-RPC protocol elements.
        
        // This stub is maintained to preserve method order compatibility with AbstractA2AServerTest
        // for future migration when extending that base class.
    }

    @Test
    public void testInvalidJSONRPCRequestMissingMethod() {
        // NOTE: This test is not applicable to gRPC since it tests JSON-RPC protocol-specific
        // validation of the "method" field. gRPC methods are defined in the service definition
        // and invoked directly, not through JSON-RPC method names.
        
        // This stub is maintained to preserve method order compatibility with AbstractA2AServerTest
        // for future migration when extending that base class.
    }

    @Test
    public void testInvalidJSONRPCRequestInvalidId() {
        // NOTE: This test is not applicable to gRPC since it tests JSON-RPC protocol-specific
        // validation of the "id" field. gRPC handles request/response correlation differently
        // through its streaming mechanisms.
        
        // This stub is maintained to preserve method order compatibility with AbstractA2AServerTest
        // for future migration when extending that base class.
    }

    @Test
    public void testInvalidJSONRPCRequestNonExistentMethod() {
        // NOTE: This test is not applicable to gRPC since it tests JSON-RPC protocol-specific
        // method not found errors. gRPC method resolution is handled at the service definition
        // level and unknown methods result in different error types.
        
        // This stub is maintained to preserve method order compatibility with AbstractA2AServerTest
        // for future migration when extending that base class.
    }

    @Test
    public void testNonStreamingMethodWithAcceptHeader() throws Exception {
        // NOTE: This test is not applicable to gRPC since HTTP Accept headers 
        // are an HTTP/REST-specific concept and do not apply to gRPC protocol.
        // gRPC uses Protocol Buffers for message encoding and doesn't use HTTP content negotiation.
        
        // This stub is maintained to preserve method order compatibility with AbstractA2AServerTest
        // for future migration when extending that base class.
    }

    @Test
    public void testSetPushNotificationSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            // Create a PushNotificationConfig with an ID (needed for gRPC conversion)
            PushNotificationConfig pushConfig = new PushNotificationConfig.Builder()
                    .url("http://example.com")
                    .id(MINIMAL_TASK.getId()) // Using task ID as config ID for simplicity
                    .build();
            TaskPushNotificationConfig taskPushConfig = 
                    new TaskPushNotificationConfig(MINIMAL_TASK.getId(), pushConfig);

            CreateTaskPushNotificationConfigRequest request = CreateTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .setConfigId(MINIMAL_TASK.getId())
                    .setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig))
                    .build();

            io.a2a.grpc.TaskPushNotificationConfig response = client.createTaskPushNotificationConfig(request);

            // Convert back to spec for easier assertions
            TaskPushNotificationConfig responseConfig = ProtoUtils.FromProto.taskPushNotificationConfig(response);
            assertEquals(MINIMAL_TASK.getId(), responseConfig.taskId());
            assertEquals("http://example.com", responseConfig.pushNotificationConfig().url());
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testGetPushNotificationSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            // First, create a push notification config (same as previous test)
            PushNotificationConfig pushConfig = new PushNotificationConfig.Builder()
                    .url("http://example.com")
                    .id(MINIMAL_TASK.getId())
                    .build();
            TaskPushNotificationConfig taskPushConfig = 
                    new TaskPushNotificationConfig(MINIMAL_TASK.getId(), pushConfig);
            
            CreateTaskPushNotificationConfigRequest createRequest = CreateTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .setConfigId(MINIMAL_TASK.getId())
                    .setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig))
                    .build();
            
            io.a2a.grpc.TaskPushNotificationConfig createResponse = client.createTaskPushNotificationConfig(createRequest);
            assertNotNull(createResponse);
            
            // Now, get the push notification config
            GetTaskPushNotificationConfigRequest getRequest = GetTaskPushNotificationConfigRequest.newBuilder()
                    .setName("tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs/" + MINIMAL_TASK.getId())
                    .build();
            
            io.a2a.grpc.TaskPushNotificationConfig getResponse = client.getTaskPushNotificationConfig(getRequest);
            
            // Convert back to spec for easier assertions
            TaskPushNotificationConfig responseConfig = ProtoUtils.FromProto.taskPushNotificationConfig(getResponse);
            assertEquals(MINIMAL_TASK.getId(), responseConfig.taskId());
            assertEquals("http://example.com", responseConfig.pushNotificationConfig().url());
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testListPushNotificationConfigWithConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            // Create first push notification config
            PushNotificationConfig pushConfig1 = new PushNotificationConfig.Builder()
                    .url("http://example.com")
                    .id("config1")
                    .build();
            TaskPushNotificationConfig taskPushConfig1 = 
                    new TaskPushNotificationConfig(MINIMAL_TASK.getId(), pushConfig1);

            CreateTaskPushNotificationConfigRequest createRequest1 = CreateTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .setConfigId("config1")
                    .setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig1))
                    .build();
            client.createTaskPushNotificationConfig(createRequest1);

            // Create second push notification config
            PushNotificationConfig pushConfig2 = new PushNotificationConfig.Builder()
                    .url("http://example.com")
                    .id("config2")
                    .build();
            TaskPushNotificationConfig taskPushConfig2 = 
                    new TaskPushNotificationConfig(MINIMAL_TASK.getId(), pushConfig2);

            CreateTaskPushNotificationConfigRequest createRequest2 = CreateTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .setConfigId("config2")
                    .setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig2))
                    .build();
            client.createTaskPushNotificationConfig(createRequest2);

            // Now, list all push notification configs for the task
            ListTaskPushNotificationConfigRequest listRequest = ListTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .build();

            ListTaskPushNotificationConfigResponse listResponse = client.listTaskPushNotificationConfig(listRequest);

            // Verify the response
            assertEquals(2, listResponse.getConfigsCount());

            // Convert back to spec for easier assertions
            TaskPushNotificationConfig config1 = ProtoUtils.FromProto.taskPushNotificationConfig(listResponse.getConfigs(0));
            TaskPushNotificationConfig config2 = ProtoUtils.FromProto.taskPushNotificationConfig(listResponse.getConfigs(1));

            assertEquals(MINIMAL_TASK.getId(), config1.taskId());
            assertEquals("http://example.com", config1.pushNotificationConfig().url());
            assertEquals("config1", config1.pushNotificationConfig().id());

            assertEquals(MINIMAL_TASK.getId(), config2.taskId());
            assertEquals("http://example.com", config2.pushNotificationConfig().url());
            assertEquals("config2", config2.pushNotificationConfig().id());
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config1");
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config2");
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testListPushNotificationConfigWithoutConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            // Create first push notification config without explicit ID (will use task ID as default)
            PushNotificationConfig pushConfig1 = new PushNotificationConfig.Builder()
                    .url("http://1.example.com")
                    .id(MINIMAL_TASK.getId()) // Use task ID as config ID
                    .build();
            TaskPushNotificationConfig taskPushConfig1 = 
                    new TaskPushNotificationConfig(MINIMAL_TASK.getId(), pushConfig1);

            CreateTaskPushNotificationConfigRequest createRequest1 = CreateTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .setConfigId(MINIMAL_TASK.getId())
                    .setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig1))
                    .build();
            client.createTaskPushNotificationConfig(createRequest1);

            // Create second push notification config with same ID (will overwrite the previous one)
            PushNotificationConfig pushConfig2 = new PushNotificationConfig.Builder()
                    .url("http://2.example.com")
                    .id(MINIMAL_TASK.getId()) // Same ID, will overwrite
                    .build();
            TaskPushNotificationConfig taskPushConfig2 = 
                    new TaskPushNotificationConfig(MINIMAL_TASK.getId(), pushConfig2);

            CreateTaskPushNotificationConfigRequest createRequest2 = CreateTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .setConfigId(MINIMAL_TASK.getId())
                    .setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig2))
                    .build();
            client.createTaskPushNotificationConfig(createRequest2);

            // Now, list all push notification configs for the task
            ListTaskPushNotificationConfigRequest listRequest = ListTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .build();

            ListTaskPushNotificationConfigResponse listResponse = client.listTaskPushNotificationConfig(listRequest);

            // Verify only 1 config exists (second one overwrote the first)
            assertEquals(1, listResponse.getConfigsCount());

            // Convert back to spec for easier assertions
            TaskPushNotificationConfig config = ProtoUtils.FromProto.taskPushNotificationConfig(listResponse.getConfigs(0));

            assertEquals(MINIMAL_TASK.getId(), config.taskId());
            assertEquals("http://2.example.com", config.pushNotificationConfig().url());
            assertEquals(MINIMAL_TASK.getId(), config.pushNotificationConfig().id());
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testListPushNotificationConfigTaskNotFound() throws Exception {
        ListTaskPushNotificationConfigRequest listRequest = ListTaskPushNotificationConfigRequest.newBuilder()
                .setParent("tasks/non-existent-task")
                .build();

        try {
            client.listTaskPushNotificationConfig(listRequest);
            // Should not reach here
            assertTrue(false, "Expected StatusRuntimeException but method returned normally");
        } catch (StatusRuntimeException e) {
            assertEquals(Status.NOT_FOUND.getCode(), e.getStatus().getCode());
            String description = e.getStatus().getDescription();
            assertTrue(description != null && description.contains("TaskNotFoundError"));
        }
    }

    @Test
    public void testListPushNotificationConfigEmptyList() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            // List configs for a task that has no configs
            ListTaskPushNotificationConfigRequest listRequest = ListTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .build();

            ListTaskPushNotificationConfigResponse listResponse = client.listTaskPushNotificationConfig(listRequest);

            // Verify empty list
            assertEquals(0, listResponse.getConfigsCount());
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testDeletePushNotificationConfigWithValidConfigId() throws Exception {
        // Create a second task for testing cross-task isolation
        Task secondTask = new Task.Builder()
                .id("task-456")
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build();

        saveTaskInTaskStore(MINIMAL_TASK);
        saveTaskInTaskStore(secondTask);
        try {
            // Create config1 and config2 for MINIMAL_TASK
            PushNotificationConfig pushConfig1 = new PushNotificationConfig.Builder()
                    .url("http://example.com")
                    .id("config1")
                    .build();
            TaskPushNotificationConfig taskPushConfig1 = 
                    new TaskPushNotificationConfig(MINIMAL_TASK.getId(), pushConfig1);

            CreateTaskPushNotificationConfigRequest createRequest1 = CreateTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .setConfigId("config1")
                    .setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig1))
                    .build();
            client.createTaskPushNotificationConfig(createRequest1);

            PushNotificationConfig pushConfig2 = new PushNotificationConfig.Builder()
                    .url("http://example.com")
                    .id("config2")
                    .build();
            TaskPushNotificationConfig taskPushConfig2 = 
                    new TaskPushNotificationConfig(MINIMAL_TASK.getId(), pushConfig2);

            CreateTaskPushNotificationConfigRequest createRequest2 = CreateTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .setConfigId("config2")
                    .setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig2))
                    .build();
            client.createTaskPushNotificationConfig(createRequest2);

            // Create config1 for secondTask
            TaskPushNotificationConfig taskPushConfig3 = 
                    new TaskPushNotificationConfig(secondTask.getId(), pushConfig1);

            CreateTaskPushNotificationConfigRequest createRequest3 = CreateTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + secondTask.getId())
                    .setConfigId("config1")
                    .setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig3))
                    .build();
            client.createTaskPushNotificationConfig(createRequest3);

            // Delete config1 from MINIMAL_TASK
            DeleteTaskPushNotificationConfigRequest deleteRequest = DeleteTaskPushNotificationConfigRequest.newBuilder()
                    .setName("tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs/config1")
                    .build();

            com.google.protobuf.Empty deleteResponse = client.deleteTaskPushNotificationConfig(deleteRequest);
            assertNotNull(deleteResponse); // Should return Empty, not null

            // Verify MINIMAL_TASK now has only 1 config (config2)
            ListTaskPushNotificationConfigRequest listRequest1 = ListTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .build();
            ListTaskPushNotificationConfigResponse listResponse1 = client.listTaskPushNotificationConfig(listRequest1);
            assertEquals(1, listResponse1.getConfigsCount());

            TaskPushNotificationConfig remainingConfig = ProtoUtils.FromProto.taskPushNotificationConfig(listResponse1.getConfigs(0));
            assertEquals("config2", remainingConfig.pushNotificationConfig().id());

            // Verify secondTask remains unchanged (still has config1)
            ListTaskPushNotificationConfigRequest listRequest2 = ListTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + secondTask.getId())
                    .build();
            ListTaskPushNotificationConfigResponse listResponse2 = client.listTaskPushNotificationConfig(listRequest2);
            assertEquals(1, listResponse2.getConfigsCount());

            TaskPushNotificationConfig secondTaskConfig = ProtoUtils.FromProto.taskPushNotificationConfig(listResponse2.getConfigs(0));
            assertEquals("config1", secondTaskConfig.pushNotificationConfig().id());

        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config1");
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config2");
            deletePushNotificationConfigInStore(secondTask.getId(), "config1");
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
            deleteTaskInTaskStore(secondTask.getId());
        }
    }

    @Test
    public void testDeletePushNotificationConfigWithNonExistingConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            // Create config1 and config2
            PushNotificationConfig pushConfig1 = new PushNotificationConfig.Builder()
                    .url("http://example.com")
                    .id("config1")
                    .build();
            TaskPushNotificationConfig taskPushConfig1 = 
                    new TaskPushNotificationConfig(MINIMAL_TASK.getId(), pushConfig1);

            CreateTaskPushNotificationConfigRequest createRequest1 = CreateTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .setConfigId("config1")
                    .setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig1))
                    .build();
            client.createTaskPushNotificationConfig(createRequest1);

            PushNotificationConfig pushConfig2 = new PushNotificationConfig.Builder()
                    .url("http://example.com")
                    .id("config2")
                    .build();
            TaskPushNotificationConfig taskPushConfig2 = 
                    new TaskPushNotificationConfig(MINIMAL_TASK.getId(), pushConfig2);

            CreateTaskPushNotificationConfigRequest createRequest2 = CreateTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .setConfigId("config2")
                    .setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig2))
                    .build();
            client.createTaskPushNotificationConfig(createRequest2);

            // Try to delete non-existent config (should succeed silently)
            DeleteTaskPushNotificationConfigRequest deleteRequest = DeleteTaskPushNotificationConfigRequest.newBuilder()
                    .setName("tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs/non-existent-config-id")
                    .build();

            com.google.protobuf.Empty deleteResponse = client.deleteTaskPushNotificationConfig(deleteRequest);
            assertNotNull(deleteResponse); // Should return Empty, not throw error

            // Verify both configs remain unchanged
            ListTaskPushNotificationConfigRequest listRequest = ListTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .build();
            ListTaskPushNotificationConfigResponse listResponse = client.listTaskPushNotificationConfig(listRequest);
            assertEquals(2, listResponse.getConfigsCount());

        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config1");
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config2");
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testDeletePushNotificationConfigTaskNotFound() throws Exception {
        DeleteTaskPushNotificationConfigRequest deleteRequest = DeleteTaskPushNotificationConfigRequest.newBuilder()
                .setName("tasks/non-existent-task/pushNotificationConfigs/non-existent-config-id")
                .build();

        try {
            client.deleteTaskPushNotificationConfig(deleteRequest);
            // Should not reach here
            assertTrue(false, "Expected StatusRuntimeException but method returned normally");
        } catch (StatusRuntimeException e) {
            assertEquals(Status.NOT_FOUND.getCode(), e.getStatus().getCode());
            String description = e.getStatus().getDescription();
            assertTrue(description != null && description.contains("TaskNotFoundError"));
        }
    }

    @Test
    public void testDeletePushNotificationConfigSetWithoutConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            // Create first config without explicit ID (will use task ID as default)
            PushNotificationConfig pushConfig1 = new PushNotificationConfig.Builder()
                    .url("http://1.example.com")
                    .id(MINIMAL_TASK.getId())
                    .build();
            TaskPushNotificationConfig taskPushConfig1 = 
                    new TaskPushNotificationConfig(MINIMAL_TASK.getId(), pushConfig1);

            CreateTaskPushNotificationConfigRequest createRequest1 = CreateTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .setConfigId(MINIMAL_TASK.getId())
                    .setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig1))
                    .build();
            client.createTaskPushNotificationConfig(createRequest1);

            // Create second config with same ID (will overwrite the previous one)
            PushNotificationConfig pushConfig2 = new PushNotificationConfig.Builder()
                    .url("http://2.example.com")
                    .id(MINIMAL_TASK.getId())
                    .build();
            TaskPushNotificationConfig taskPushConfig2 = 
                    new TaskPushNotificationConfig(MINIMAL_TASK.getId(), pushConfig2);

            CreateTaskPushNotificationConfigRequest createRequest2 = CreateTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .setConfigId(MINIMAL_TASK.getId())
                    .setConfig(ProtoUtils.ToProto.taskPushNotificationConfig(taskPushConfig2))
                    .build();
            client.createTaskPushNotificationConfig(createRequest2);

            // Delete the config using task ID
            DeleteTaskPushNotificationConfigRequest deleteRequest = DeleteTaskPushNotificationConfigRequest.newBuilder()
                    .setName("tasks/" + MINIMAL_TASK.getId() + "/pushNotificationConfigs/" + MINIMAL_TASK.getId())
                    .build();

            com.google.protobuf.Empty deleteResponse = client.deleteTaskPushNotificationConfig(deleteRequest);
            assertNotNull(deleteResponse); // Should return Empty

            // Verify no configs remain
            ListTaskPushNotificationConfigRequest listRequest = ListTaskPushNotificationConfigRequest.newBuilder()
                    .setParent("tasks/" + MINIMAL_TASK.getId())
                    .build();
            ListTaskPushNotificationConfigResponse listResponse = client.listTaskPushNotificationConfig(listRequest);
            assertEquals(0, listResponse.getConfigsCount());

        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testSendMessageStreamExistingTaskSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            // Build message for existing task
            Message message = new Message.Builder(MESSAGE)
                    .taskId(MINIMAL_TASK.getId())
                    .contextId(MINIMAL_TASK.getContextId())
                    .build();

            // Create gRPC streaming request
            SendMessageRequest request = SendMessageRequest.newBuilder()
                    .setRequest(ProtoUtils.ToProto.message(message))
                    .build();

            // Use blocking iterator to consume stream responses
            java.util.Iterator<StreamResponse> responseIterator = client.sendStreamingMessage(request);

            // Collect responses - expect at least one
            java.util.List<StreamResponse> responses = new java.util.ArrayList<>();
            while (responseIterator.hasNext()) {
                StreamResponse response = responseIterator.next();
                responses.add(response);

                // For this test, we expect to get the message back - stop after first response
                if (response.hasMsg()) {
                    break;
                }
            }

            // Verify we got at least one response
            assertTrue(responses.size() >= 1, "Expected at least one response from streaming call");

            // Find the message response
            StreamResponse messageResponse = null;
            for (StreamResponse response : responses) {
                if (response.hasMsg()) {
                    messageResponse = response;
                    break;
                }
            }

            assertNotNull(messageResponse, "Expected to receive a message response");

            // Verify the message content
            io.a2a.grpc.Message grpcMessage = messageResponse.getMsg();
            Message responseMessage = ProtoUtils.FromProto.message(grpcMessage);
            assertEquals(MESSAGE.getMessageId(), responseMessage.getMessageId());
            assertEquals(MESSAGE.getRole(), responseMessage.getRole());
            Part<?> part = responseMessage.getParts().get(0);
            assertEquals(Part.Kind.TEXT, part.getKind());
            assertEquals("test message", ((TextPart) part).getText());

        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testStreamingMethodWithAcceptHeader() throws Exception {
        // NOTE: This test is not applicable to gRPC since HTTP Accept headers 
        // are an HTTP/REST-specific concept and do not apply to gRPC protocol.
        // gRPC uses Protocol Buffers for message encoding and doesn't use HTTP content negotiation.
        
        // This stub is maintained to preserve method order compatibility with AbstractA2AServerTest
        // for future migration when extending that base class.
    }

    @Test
    public void testSendMessageStreamNewMessageSuccess() throws Exception {
        // Ensure no task exists initially (test creates new task via streaming)
        assertTrue(getTaskFromTaskStore(MINIMAL_TASK.getId()) == null, "Task should not exist initially");

        try {
            // Build message for new task (no pre-existing task)
            Message message = new Message.Builder(MESSAGE)
                    .taskId(MINIMAL_TASK.getId())
                    .contextId(MINIMAL_TASK.getContextId())
                    .build();

            // Create gRPC streaming request
            SendMessageRequest request = SendMessageRequest.newBuilder()
                    .setRequest(ProtoUtils.ToProto.message(message))
                    .build();

            // Use blocking iterator to consume stream responses
            java.util.Iterator<StreamResponse> responseIterator = client.sendStreamingMessage(request);

            // Collect responses - expect at least one
            java.util.List<StreamResponse> responses = new java.util.ArrayList<>();
            while (responseIterator.hasNext()) {
                StreamResponse response = responseIterator.next();
                responses.add(response);

                // For this test, we expect to get the message back - stop after first response
                if (response.hasMsg()) {
                    break;
                }
            }

            // Verify we got at least one response
            assertTrue(responses.size() >= 1, "Expected at least one response from streaming call");

            // Find the message response
            StreamResponse messageResponse = null;
            for (StreamResponse response : responses) {
                if (response.hasMsg()) {
                    messageResponse = response;
                    break;
                }
            }

            assertNotNull(messageResponse, "Expected to receive a message response");

            // Verify the message content
            io.a2a.grpc.Message grpcMessage = messageResponse.getMsg();
            Message responseMessage = ProtoUtils.FromProto.message(grpcMessage);
            assertEquals(MESSAGE.getMessageId(), responseMessage.getMessageId());
            assertEquals(MESSAGE.getRole(), responseMessage.getRole());
            Part<?> part = responseMessage.getParts().get(0);
            assertEquals(Part.Kind.TEXT, part.getKind());
            assertEquals("test message", ((TextPart) part).getText());

        } finally {
            // Clean up any task that may have been created (ignore if task doesn't exist)
            try {
                deleteTaskInTaskStore(MINIMAL_TASK.getId());
            } catch (RuntimeException e) {
                // Ignore if task doesn't exist (404 error)
                if (!e.getMessage().contains("404")) {
                    throw e;
                }
            }
        }
    }

    @Test
    public void testResubscribeExistingTaskSuccess() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        saveTaskInTaskStore(MINIMAL_TASK);
        
        try {
            // Ensure queue for task exists (required for resubscription)
            ensureQueueForTask(MINIMAL_TASK.getId());
            
            CountDownLatch taskResubscriptionRequestSent = new CountDownLatch(1);
            CountDownLatch taskResubscriptionResponseReceived = new CountDownLatch(2);
            AtomicReference<StreamResponse> firstResponse = new AtomicReference<>();
            AtomicReference<StreamResponse> secondResponse = new AtomicReference<>();
            
            // Create gRPC task subscription request
            TaskSubscriptionRequest subscriptionRequest = TaskSubscriptionRequest.newBuilder()
                    .setName("tasks/" + MINIMAL_TASK.getId())
                    .build();
            
            // Count down the latch when the gRPC streaming subscription is established
            awaitStreamingSubscription()
                    .whenComplete((unused, throwable) -> taskResubscriptionRequestSent.countDown());
            
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            
            // Start the subscription in a separate thread
            executorService.submit(() -> {
                try {
                    java.util.Iterator<StreamResponse> responseIterator = client.taskSubscription(subscriptionRequest);
                    
                    while (responseIterator.hasNext()) {
                        StreamResponse response = responseIterator.next();
                        
                        if (taskResubscriptionResponseReceived.getCount() == 2) {
                            firstResponse.set(response);
                        } else {
                            secondResponse.set(response);
                        }
                        taskResubscriptionResponseReceived.countDown();
                        
                        if (taskResubscriptionResponseReceived.getCount() == 0) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    errorRef.set(e);
                    // Count down both latches to unblock the test
                    taskResubscriptionRequestSent.countDown();
                    while (taskResubscriptionResponseReceived.getCount() > 0) {
                        taskResubscriptionResponseReceived.countDown();
                    }
                }
            });
            
            // Wait for subscription to be established
            assertTrue(taskResubscriptionRequestSent.await(10, TimeUnit.SECONDS), "Subscription should be established");
            
            // Inject events into the server's event queue
            java.util.List<Event> events = java.util.List.of(
                    new TaskArtifactUpdateEvent.Builder()
                            .taskId(MINIMAL_TASK.getId())
                            .contextId(MINIMAL_TASK.getContextId())
                            .artifact(new Artifact.Builder()
                                    .artifactId("11")
                                    .parts(new TextPart("text"))
                                    .build())
                            .build(),
                    new TaskStatusUpdateEvent.Builder()
                            .taskId(MINIMAL_TASK.getId())
                            .contextId(MINIMAL_TASK.getContextId())
                            .status(new TaskStatus(TaskState.COMPLETED))
                            .isFinal(true)
                            .build());
            
            for (Event event : events) {
                enqueueEventOnServer(event);
            }
            
            // Wait for the client to receive the responses
            assertTrue(taskResubscriptionResponseReceived.await(20, TimeUnit.SECONDS), "Should receive both responses");
            
            // Check for errors
            if (errorRef.get() != null) {
                throw new RuntimeException("Error in subscription thread", errorRef.get());
            }
            
            // Verify first response (TaskArtifactUpdateEvent)
            assertNotNull(firstResponse.get(), "Should receive first response");
            StreamResponse firstStreamResponse = firstResponse.get();
            assertTrue(firstStreamResponse.hasArtifactUpdate(), "First response should be artifact update");
            
            io.a2a.grpc.TaskArtifactUpdateEvent artifactUpdate = firstStreamResponse.getArtifactUpdate();
            assertEquals(MINIMAL_TASK.getId(), artifactUpdate.getTaskId());
            assertEquals(MINIMAL_TASK.getContextId(), artifactUpdate.getContextId());
            assertEquals("11", artifactUpdate.getArtifact().getArtifactId());
            assertEquals("text", artifactUpdate.getArtifact().getParts(0).getText());
            
            // Verify second response (TaskStatusUpdateEvent)
            assertNotNull(secondResponse.get(), "Should receive second response");
            StreamResponse secondStreamResponse = secondResponse.get();
            assertTrue(secondStreamResponse.hasStatusUpdate(), "Second response should be status update");
            
            io.a2a.grpc.TaskStatusUpdateEvent statusUpdate = secondStreamResponse.getStatusUpdate();
            assertEquals(MINIMAL_TASK.getId(), statusUpdate.getTaskId());
            assertEquals(MINIMAL_TASK.getContextId(), statusUpdate.getContextId());
            assertEquals(io.a2a.grpc.TaskState.TASK_STATE_COMPLETED, statusUpdate.getStatus().getState());
            assertTrue(statusUpdate.getFinal(), "Final status update should be marked as final");
            
        } finally {
        deleteTaskInTaskStore(MINIMAL_TASK.getId());
            executorService.shutdown();
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        }
    }

    @Test
    public void testResubscribeNoExistingTaskError() throws Exception {
        // Try to resubscribe to a non-existent task - should get TaskNotFoundError
        TaskSubscriptionRequest request = TaskSubscriptionRequest.newBuilder()
                .setName("tasks/non-existent-task")
                .build();

        try {
            // Use blocking iterator to consume stream responses  
            java.util.Iterator<StreamResponse> responseIterator = client.taskSubscription(request);
            
            // Try to get first response - should throw StatusRuntimeException
            if (responseIterator.hasNext()) {
                responseIterator.next();
            }
            
            // Should not reach here
            assertTrue(false, "Expected StatusRuntimeException but method returned normally");
        } catch (StatusRuntimeException e) {
            // Verify this is a TaskNotFoundError mapped to NOT_FOUND status
            assertEquals(Status.NOT_FOUND.getCode(), e.getStatus().getCode());
            String description = e.getStatus().getDescription();
            assertTrue(description != null && description.contains("TaskNotFoundError"));
        }
    }


    protected void saveTaskInTaskStore(Task task) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/task"))
                .POST(HttpRequest.BodyPublishers.ofString(Utils.OBJECT_MAPPER.writeValueAsString(task)))
                .header("Content-Type", APPLICATION_JSON)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    String.format("Saving task failed! Status: %d, Body: %s", response.statusCode(), response.body()));
        }
    }

    protected Task getTaskFromTaskStore(String taskId) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/task/" + taskId))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format("Getting task failed! Status: %d, Body: %s", response.statusCode(), response.body()));
        }
        return Utils.OBJECT_MAPPER.readValue(response.body(), Task.TYPE_REFERENCE);
    }

    protected void deleteTaskInTaskStore(String taskId) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(("http://localhost:" + serverPort + "/test/task/" + taskId)))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.statusCode() + ": Deleting task failed!" + response.body());
        }
    }

    protected void ensureQueueForTask(String taskId) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/queue/ensure/" + taskId))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format("Ensuring queue failed! Status: %d, Body: %s", response.statusCode(), response.body()));
        }
    }

    protected void enqueueEventOnServer(Event event) throws Exception {
        String path;
        if (event instanceof TaskArtifactUpdateEvent e) {
            path = "test/queue/enqueueTaskArtifactUpdateEvent/" + e.getTaskId();
        } else if (event instanceof TaskStatusUpdateEvent e) {
            path = "test/queue/enqueueTaskStatusUpdateEvent/" + e.getTaskId();
        } else {
            throw new RuntimeException("Unknown event type " + event.getClass() + ". If you need the ability to" +
                    " handle more types, please add the REST endpoints.");
        }
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/" + path))
                .header("Content-Type", APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(Utils.OBJECT_MAPPER.writeValueAsString(event)))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.statusCode() + ": Queueing event failed!" + response.body());
        }
    }

    private CompletableFuture<Void> awaitStreamingSubscription() {
        int cnt = getStreamingSubscribedCount();
        AtomicInteger initialCount = new AtomicInteger(cnt);

        return CompletableFuture.runAsync(() -> {
            try {
                boolean done = false;
                long end = System.currentTimeMillis() + 15000;
                while (System.currentTimeMillis() < end) {
                    int count = getStreamingSubscribedCount();
                    if (count > initialCount.get()) {
                        done = true;
                        break;
                    }
                    Thread.sleep(500);
                }
                if (!done) {
                    throw new RuntimeException("Timed out waiting for subscription");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted");
            }
        });
    }

    private int getStreamingSubscribedCount() {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/streamingSubscribedCount"))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String body = response.body().trim();
            return Integer.parseInt(body);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void deletePushNotificationConfigInStore(String taskId, String configId) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(("http://localhost:" + serverPort + "/test/task/" + taskId + "/config/" + configId)))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.statusCode() + ": Deleting task failed!" + response.body());
        }
    }

    protected void savePushNotificationConfigInStore(String taskId, PushNotificationConfig notificationConfig) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/test/task/" + taskId))
                .POST(HttpRequest.BodyPublishers.ofString(Utils.OBJECT_MAPPER.writeValueAsString(notificationConfig)))
                .header("Content-Type", APPLICATION_JSON)
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new RuntimeException(response.statusCode() + ": Creating task push notification config failed! " + response.body());
        }
    }

}
