package io.a2a.server.apps.common;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.wildfly.common.Assert.assertNotNull;
import static org.wildfly.common.Assert.assertTrue;

import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.a2a.client.A2AClient;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.AuthenticatedExtendedCardNotConfiguredError;
import io.a2a.spec.CancelTaskRequest;
import io.a2a.spec.CancelTaskResponse;
import io.a2a.spec.DeleteTaskPushNotificationConfigResponse;
import io.a2a.spec.Event;
import io.a2a.spec.GetAuthenticatedExtendedCardRequest;
import io.a2a.spec.GetAuthenticatedExtendedCardResponse;
import io.a2a.spec.GetTaskPushNotificationConfigParams;
import io.a2a.spec.GetTaskPushNotificationConfigRequest;
import io.a2a.spec.GetTaskPushNotificationConfigResponse;
import io.a2a.spec.GetTaskRequest;
import io.a2a.spec.GetTaskResponse;
import io.a2a.spec.InvalidParamsError;
import io.a2a.spec.InvalidRequestError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCErrorResponse;
import io.a2a.spec.ListTaskPushNotificationConfigResponse;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.MethodNotFoundError;
import io.a2a.spec.Part;
import io.a2a.spec.PushNotificationConfig;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.SetTaskPushNotificationConfigRequest;
import io.a2a.spec.SetTaskPushNotificationConfigResponse;
import io.a2a.spec.StreamingJSONRPCRequest;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskNotFoundError;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskResubscriptionRequest;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
import io.a2a.util.Utils;
import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * This test requires doing some work on the server to add/get/delete tasks, and enqueue events. This is exposed via REST,
 * which delegates to {@link TestUtilsBean}.
 */
public abstract class AbstractA2AServerTest {

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

    private final int serverPort;
    private A2AClient client;

    protected AbstractA2AServerTest(int serverPort) {
        this.serverPort = serverPort;
        this.client = new A2AClient("http://localhost:" + serverPort);
    }

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
        testGetTask();
    }

    private void testGetTask() throws Exception {
        testGetTask(null);
    }

    private void testGetTask(String mediaType) throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams(MINIMAL_TASK.getId()));
            RequestSpecification requestSpecification = RestAssured.given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request);
            if (mediaType != null) {
                requestSpecification = requestSpecification.accept(mediaType);
            }
            GetTaskResponse response = requestSpecification
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(GetTaskResponse.class);
            assertEquals("1", response.getId());
            assertEquals("task-123", response.getResult().getId());
            assertEquals("session-xyz", response.getResult().getContextId());
            assertEquals(TaskState.SUBMITTED, response.getResult().getStatus().state());
            assertNull(response.getError());
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testGetTaskNotFound() throws Exception {
        assertTrue(getTaskFromTaskStore("non-existent-task") == null);
        GetTaskRequest request = new GetTaskRequest("1", new TaskQueryParams("non-existent-task"));
        GetTaskResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(GetTaskResponse.class);
        assertEquals("1", response.getId());
        // this should be an instance of TaskNotFoundError, see https://github.com/a2aproject/a2a-java/issues/23
        assertInstanceOf(JSONRPCError.class, response.getError());
        assertEquals(new TaskNotFoundError().getCode(), response.getError().getCode());
        assertNull(response.getResult());
    }

    @Test
    public void testCancelTaskSuccess() throws Exception {
        saveTaskInTaskStore(CANCEL_TASK);
        try {
            CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams(CANCEL_TASK.getId()));
            CancelTaskResponse response = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(CancelTaskResponse.class);
            assertNull(response.getError());
            assertEquals(request.getId(), response.getId());
            Task task = response.getResult();
            assertEquals(CANCEL_TASK.getId(), task.getId());
            assertEquals(CANCEL_TASK.getContextId(), task.getContextId());
            assertEquals(TaskState.CANCELED, task.getStatus().state());
        } catch (Exception e) {
        } finally {
            deleteTaskInTaskStore(CANCEL_TASK.getId());
        }
    }

    @Test
    public void testCancelTaskNotSupported() throws Exception {
        saveTaskInTaskStore(CANCEL_TASK_NOT_SUPPORTED);
        try {
            CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams(CANCEL_TASK_NOT_SUPPORTED.getId()));
            CancelTaskResponse response = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(CancelTaskResponse.class);
            assertEquals(request.getId(), response.getId());
            assertNull(response.getResult());
            // this should be an instance of UnsupportedOperationError, see https://github.com/a2aproject/a2a-java/issues/23
            assertInstanceOf(JSONRPCError.class, response.getError());
            assertEquals(new UnsupportedOperationError().getCode(), response.getError().getCode());
        } catch (Exception e) {
        } finally {
            deleteTaskInTaskStore(CANCEL_TASK_NOT_SUPPORTED.getId());
        }
    }

    @Test
    public void testCancelTaskNotFound() {
        CancelTaskRequest request = new CancelTaskRequest("1", new TaskIdParams("non-existent-task"));
        CancelTaskResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(CancelTaskResponse.class)
                ;
        assertEquals(request.getId(), response.getId());
        assertNull(response.getResult());
        // this should be an instance of UnsupportedOperationError, see https://github.com/a2aproject/a2a-java/issues/23
        assertInstanceOf(JSONRPCError.class, response.getError());
        assertEquals(new TaskNotFoundError().getCode(), response.getError().getCode());
    }

    @Test
    public void testSendMessageNewMessageSuccess() throws Exception {
        assertTrue(getTaskFromTaskStore(MINIMAL_TASK.getId()) == null);
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
        SendMessageResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(SendMessageResponse.class);
        assertNull(response.getError());
        Message messageResponse =  (Message) response.getResult();
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
            SendMessageRequest request = new SendMessageRequest("1", new MessageSendParams(message, null, null));
            SendMessageResponse response = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(SendMessageResponse.class);
            assertNull(response.getError());
            Message messageResponse = (Message) response.getResult();
            assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
            assertEquals(MESSAGE.getRole(), messageResponse.getRole());
            Part<?> part = messageResponse.getParts().get(0);
            assertEquals(Part.Kind.TEXT, part.getKind());
            assertEquals("test message", ((TextPart) part).getText());
        } catch (Exception e) {
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testSetPushNotificationSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            TaskPushNotificationConfig taskPushConfig =
                    new TaskPushNotificationConfig(
                            MINIMAL_TASK.getId(), new PushNotificationConfig.Builder().url("http://example.com").build());
            SetTaskPushNotificationConfigRequest request = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
            SetTaskPushNotificationConfigResponse response = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(SetTaskPushNotificationConfigResponse.class);
            assertNull(response.getError());
            assertEquals(request.getId(), response.getId());
            TaskPushNotificationConfig config = response.getResult();
            assertEquals(MINIMAL_TASK.getId(), config.taskId());
            assertEquals("http://example.com", config.pushNotificationConfig().url());
        } catch (Exception e) {
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testGetPushNotificationSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            TaskPushNotificationConfig taskPushConfig =
                    new TaskPushNotificationConfig(
                            MINIMAL_TASK.getId(), new PushNotificationConfig.Builder().url("http://example.com").build());

            SetTaskPushNotificationConfigRequest setTaskPushNotificationRequest = new SetTaskPushNotificationConfigRequest("1", taskPushConfig);
            SetTaskPushNotificationConfigResponse setTaskPushNotificationResponse = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(setTaskPushNotificationRequest)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(SetTaskPushNotificationConfigResponse.class);
            assertNotNull(setTaskPushNotificationResponse);

            GetTaskPushNotificationConfigRequest request =
                    new GetTaskPushNotificationConfigRequest("111", new GetTaskPushNotificationConfigParams(MINIMAL_TASK.getId()));
            GetTaskPushNotificationConfigResponse response = given()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .when()
                    .post("/")
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(GetTaskPushNotificationConfigResponse.class);
            assertNull(response.getError());
            assertEquals(request.getId(), response.getId());
            TaskPushNotificationConfig config = response.getResult();
            assertEquals(MINIMAL_TASK.getId(), config.taskId());
            assertEquals("http://example.com", config.pushNotificationConfig().url());
        } catch (Exception e) {
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testError() {
        Message message = new Message.Builder(MESSAGE)
                .taskId(SEND_MESSAGE_NOT_SUPPORTED.getId())
                .contextId(SEND_MESSAGE_NOT_SUPPORTED.getContextId())
                .build();
        SendMessageRequest request = new SendMessageRequest(
                "1", new MessageSendParams(message, null, null));
        SendMessageResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(SendMessageResponse.class);
        assertEquals(request.getId(), response.getId());
        assertNull(response.getResult());
        // this should be an instance of UnsupportedOperationError, see https://github.com/a2aproject/a2a-java/issues/23
        assertInstanceOf(JSONRPCError.class, response.getError());
        assertEquals(new UnsupportedOperationError().getCode(), response.getError().getCode());
    }

    @Test
    public void testGetAgentCard() {
        AgentCard agentCard = given()
                .contentType(MediaType.APPLICATION_JSON)
                .when()
                .get("/.well-known/agent-card.json")
                .then()
                .statusCode(200)
                .extract()
                .as(AgentCard.class);
        assertNotNull(agentCard);
        assertEquals("test-card", agentCard.name());
        assertEquals("A test agent card", agentCard.description());
        assertEquals("http://localhost:8081", agentCard.url());
        assertEquals("1.0", agentCard.version());
        assertEquals("http://example.com/docs", agentCard.documentationUrl());
        assertTrue(agentCard.capabilities().pushNotifications());
        assertTrue(agentCard.capabilities().streaming());
        assertTrue(agentCard.capabilities().stateTransitionHistory());
        assertTrue(agentCard.skills().isEmpty());
    }

    @Test
    public void testGetExtendAgentCardNotSupported() {
        GetAuthenticatedExtendedCardRequest request = new GetAuthenticatedExtendedCardRequest("1");
        GetAuthenticatedExtendedCardResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(GetAuthenticatedExtendedCardResponse.class);
        assertEquals("1", response.getId());
        assertInstanceOf(JSONRPCError.class, response.getError());
        assertEquals(new AuthenticatedExtendedCardNotConfiguredError().getCode(), response.getError().getCode());
        assertNull(response.getResult());
    }

    @Test
    public void testMalformedJSONRPCRequest() {
        // missing closing bracket
        String malformedRequest = "{\"jsonrpc\": \"2.0\", \"method\": \"message/send\", \"params\": {\"foo\": \"bar\"}";
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(malformedRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new JSONParseError().getCode(), response.getError().getCode());
    }

    @Test
    public void testInvalidParamsJSONRPCRequest() {
        String invalidParamsRequest = """
            {"jsonrpc": "2.0", "method": "message/send", "params": "not_a_dict", "id": "1"}
            """;
        testInvalidParams(invalidParamsRequest);

        invalidParamsRequest = """
            {"jsonrpc": "2.0", "method": "message/send", "params": {"message": {"parts": "invalid"}}, "id": "1"}
            """;
        testInvalidParams(invalidParamsRequest);
    }

    private void testInvalidParams(String invalidParamsRequest) {
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidParamsRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new InvalidParamsError().getCode(), response.getError().getCode());
        assertEquals("1", response.getId());
    }

    @Test
    public void testInvalidJSONRPCRequestMissingJsonrpc() {
        String invalidRequest = """
            {
             "method": "message/send",
             "params": {}
            }
            """;
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new InvalidRequestError().getCode(), response.getError().getCode());
    }

    @Test
    public void testInvalidJSONRPCRequestMissingMethod() {
        String invalidRequest = """
            {"jsonrpc": "2.0", "params": {}}
            """;
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new InvalidRequestError().getCode(), response.getError().getCode());
    }

    @Test
    public void testInvalidJSONRPCRequestInvalidId() {
        String invalidRequest = """
            {"jsonrpc": "2.0", "method": "message/send", "params": {}, "id": {"bad": "type"}}
            """;
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new InvalidRequestError().getCode(), response.getError().getCode());
    }

    @Test
    public void testInvalidJSONRPCRequestNonExistentMethod() {
        String invalidRequest = """
            {"jsonrpc": "2.0", "method" : "nonexistent/method", "params": {}}
            """;
        JSONRPCErrorResponse response = given()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .when()
                .post("/")
                .then()
                .statusCode(200)
                .extract()
                .as(JSONRPCErrorResponse.class);
        assertNotNull(response.getError());
        assertEquals(new MethodNotFoundError().getCode(), response.getError().getCode());
    }

    @Test
    public void testNonStreamingMethodWithAcceptHeader() throws Exception {
        testGetTask(MediaType.APPLICATION_JSON);
    }


    @Test
    public void testSendMessageStreamExistingTaskSuccess() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            Message message = new Message.Builder(MESSAGE)
                    .taskId(MINIMAL_TASK.getId())
                    .contextId(MINIMAL_TASK.getContextId())
                    .build();
            SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                    "1", new MessageSendParams(message, null, null));

            CompletableFuture<HttpResponse<Stream<String>>> responseFuture = initialiseStreamingRequest(request, null);

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            responseFuture.thenAccept(response -> {
                if (response.statusCode() != 200) {
                    //errorRef.set(new IllegalStateException("Status code was " + response.statusCode()));
                    throw new IllegalStateException("Status code was " + response.statusCode());
                }
                response.body().forEach(line -> {
                    try {
                        SendStreamingMessageResponse jsonResponse = extractJsonResponseFromSseLine(line);
                        if (jsonResponse != null) {
                            assertNull(jsonResponse.getError());
                            Message messageResponse = (Message) jsonResponse.getResult();
                            assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
                            assertEquals(MESSAGE.getRole(), messageResponse.getRole());
                            Part<?> part = messageResponse.getParts().get(0);
                            assertEquals(Part.Kind.TEXT, part.getKind());
                            assertEquals("test message", ((TextPart) part).getText());
                            latch.countDown();
                        }
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
            }).exceptionally(t -> {
                if (!isStreamClosedError(t)) {
                    errorRef.set(t);
                }
                latch.countDown();
                return null;
            });

            boolean dataRead = latch.await(20, TimeUnit.SECONDS);
            Assertions.assertTrue(dataRead);
            Assertions.assertNull(errorRef.get());
        } catch (Exception e) {
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void testResubscribeExistingTaskSuccess() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        saveTaskInTaskStore(MINIMAL_TASK);

        try {
            // attempting to send a streaming message instead of explicitly calling queueManager#createOrTap
            // does not work because after the message is sent, the queue becomes null but task resubscription
            // requires the queue to still be active
            ensureQueueForTask(MINIMAL_TASK.getId());

            CountDownLatch taskResubscriptionRequestSent = new CountDownLatch(1);
            CountDownLatch taskResubscriptionResponseReceived = new CountDownLatch(2);
            AtomicReference<SendStreamingMessageResponse> firstResponse = new AtomicReference<>();
            AtomicReference<SendStreamingMessageResponse> secondResponse = new AtomicReference<>();

            // resubscribe to the task, requires the task and its queue to still be active
            TaskResubscriptionRequest taskResubscriptionRequest = new TaskResubscriptionRequest("1", new TaskIdParams(MINIMAL_TASK.getId()));

            // Count down the latch when the MultiSseSupport on the server has started subscribing
            awaitStreamingSubscription()
                    .whenComplete((unused, throwable) -> taskResubscriptionRequestSent.countDown());

            CompletableFuture<HttpResponse<Stream<String>>> responseFuture = initialiseStreamingRequest(taskResubscriptionRequest, null);

            AtomicReference<Throwable> errorRef = new AtomicReference<>();

            responseFuture.thenAccept(response -> {

                if (response.statusCode() != 200) {
                    throw new IllegalStateException("Status code was " + response.statusCode());
                }
                try {
                    response.body().forEach(line -> {
                        try {
                            SendStreamingMessageResponse jsonResponse = extractJsonResponseFromSseLine(line);
                            if (jsonResponse != null) {
                                SendStreamingMessageResponse sendStreamingMessageResponse = Utils.OBJECT_MAPPER.readValue(line.substring("data: ".length()).trim(), SendStreamingMessageResponse.class);
                                if (taskResubscriptionResponseReceived.getCount() == 2) {
                                    firstResponse.set(sendStreamingMessageResponse);
                                } else {
                                    secondResponse.set(sendStreamingMessageResponse);
                                }
                                taskResubscriptionResponseReceived.countDown();
                                if (taskResubscriptionResponseReceived.getCount() == 0) {
                                    throw new BreakException();
                                }
                            }
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (BreakException e) {
                }
            }).exceptionally(t -> {
                if (!isStreamClosedError(t)) {
                    errorRef.set(t);
                }
                return null;
            });

            try {
                taskResubscriptionRequestSent.await();
                List<Event> events = List.of(
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
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // wait for the client to receive the responses
            taskResubscriptionResponseReceived.await();

            assertNotNull(firstResponse.get());
            SendStreamingMessageResponse sendStreamingMessageResponse = firstResponse.get();
            assertNull(sendStreamingMessageResponse.getError());
            TaskArtifactUpdateEvent taskArtifactUpdateEvent = (TaskArtifactUpdateEvent) sendStreamingMessageResponse.getResult();
            assertEquals(MINIMAL_TASK.getId(), taskArtifactUpdateEvent.getTaskId());
            assertEquals(MINIMAL_TASK.getContextId(), taskArtifactUpdateEvent.getContextId());
            Part<?> part = taskArtifactUpdateEvent.getArtifact().parts().get(0);
            assertEquals(Part.Kind.TEXT, part.getKind());
            assertEquals("text", ((TextPart) part).getText());

            assertNotNull(secondResponse.get());
            sendStreamingMessageResponse = secondResponse.get();
            assertNull(sendStreamingMessageResponse.getError());
            TaskStatusUpdateEvent taskStatusUpdateEvent = (TaskStatusUpdateEvent) sendStreamingMessageResponse.getResult();
            assertEquals(MINIMAL_TASK.getId(), taskStatusUpdateEvent.getTaskId());
            assertEquals(MINIMAL_TASK.getContextId(), taskStatusUpdateEvent.getContextId());
            assertEquals(TaskState.COMPLETED, taskStatusUpdateEvent.getStatus().state());
            assertNotNull(taskStatusUpdateEvent.getStatus().timestamp());
        } finally {
            //setStreamingSubscribedRunnable(null);
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
            executorService.shutdown();
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        }
    }

    @Test
    public void testResubscribeNoExistingTaskError() throws Exception {
        TaskResubscriptionRequest request = new TaskResubscriptionRequest("1", new TaskIdParams("non-existent-task"));

        CompletableFuture<HttpResponse<Stream<String>>> responseFuture = initialiseStreamingRequest(request, null);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        responseFuture.thenAccept(response -> {
            if (response.statusCode() != 200) {
                //errorRef.set(new IllegalStateException("Status code was " + response.statusCode()));
                throw new IllegalStateException("Status code was " + response.statusCode());
            }
            response.body().forEach(line -> {
                try {
                    SendStreamingMessageResponse jsonResponse = extractJsonResponseFromSseLine(line);
                    if (jsonResponse != null) {
                        assertEquals(request.getId(), jsonResponse.getId());
                        assertNull(jsonResponse.getResult());
                        // this should be an instance of TaskNotFoundError, see https://github.com/a2aproject/a2a-java/issues/23
                        assertInstanceOf(JSONRPCError.class, jsonResponse.getError());
                        assertEquals(new TaskNotFoundError().getCode(), jsonResponse.getError().getCode());
                        latch.countDown();
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }).exceptionally(t -> {
            if (!isStreamClosedError(t)) {
                errorRef.set(t);
            }
            latch.countDown();
            return null;
        });

        boolean dataRead = latch.await(20, TimeUnit.SECONDS);
        Assertions.assertTrue(dataRead);
        Assertions.assertNull(errorRef.get());
    }

    @Test
    public void testStreamingMethodWithAcceptHeader() throws Exception {
        testSendStreamingMessage(MediaType.SERVER_SENT_EVENTS);
    }

    @Test
    public void testSendMessageStreamNewMessageSuccess() throws Exception {
        testSendStreamingMessage(null);
    }

    private void testSendStreamingMessage(String mediaType) throws Exception {
        Message message = new Message.Builder(MESSAGE)
                .taskId(MINIMAL_TASK.getId())
                .contextId(MINIMAL_TASK.getContextId())
                .build();
        SendStreamingMessageRequest request = new SendStreamingMessageRequest(
                "1", new MessageSendParams(message, null, null));

        CompletableFuture<HttpResponse<Stream<String>>> responseFuture = initialiseStreamingRequest(request, mediaType);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        responseFuture.thenAccept(response -> {
            if (response.statusCode() != 200) {
                //errorRef.set(new IllegalStateException("Status code was " + response.statusCode()));
                throw new IllegalStateException("Status code was " + response.statusCode());
            }
            response.body().forEach(line -> {
                try {
                    SendStreamingMessageResponse jsonResponse = extractJsonResponseFromSseLine(line);
                    if (jsonResponse != null) {
                        assertNull(jsonResponse.getError());
                        Message messageResponse =  (Message) jsonResponse.getResult();
                        assertEquals(MESSAGE.getMessageId(), messageResponse.getMessageId());
                        assertEquals(MESSAGE.getRole(), messageResponse.getRole());
                        Part<?> part = messageResponse.getParts().get(0);
                        assertEquals(Part.Kind.TEXT, part.getKind());
                        assertEquals("test message", ((TextPart) part).getText());
                        latch.countDown();
                    }
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }).exceptionally(t -> {
            if (!isStreamClosedError(t)) {
                errorRef.set(t);
            }
            latch.countDown();
            return null;
        });


        boolean dataRead = latch.await(20, TimeUnit.SECONDS);
        Assertions.assertTrue(dataRead);
        Assertions.assertNull(errorRef.get());

    }

    @Test
    public void testListPushNotificationConfigWithConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        PushNotificationConfig notificationConfig1 =
                new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id("config1")
                        .build();
        PushNotificationConfig notificationConfig2 =
                new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id("config2")
                        .build();
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig1);
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig2);

        try {
            ListTaskPushNotificationConfigResponse listResponse = client.listTaskPushNotificationConfig("111", MINIMAL_TASK.getId());
            assertEquals("111", listResponse.getId());
            assertEquals(2, listResponse.getResult().size());
            assertEquals(new TaskPushNotificationConfig(MINIMAL_TASK.getId(), notificationConfig1), listResponse.getResult().get(0));
            assertEquals(new TaskPushNotificationConfig(MINIMAL_TASK.getId(), notificationConfig2), listResponse.getResult().get(1));
        } catch (Exception e) {
            fail();
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config1");
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config2");
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testListPushNotificationConfigWithoutConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        PushNotificationConfig notificationConfig1 =
                new PushNotificationConfig.Builder()
                        .url("http://1.example.com")
                        .build();
        PushNotificationConfig notificationConfig2 =
                new PushNotificationConfig.Builder()
                        .url("http://2.example.com")
                        .build();
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig1);

        // will overwrite the previous one
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig2);
        try {
            ListTaskPushNotificationConfigResponse listResponse = client.listTaskPushNotificationConfig("111", MINIMAL_TASK.getId());
            assertEquals("111", listResponse.getId());
            assertEquals(1, listResponse.getResult().size());

            PushNotificationConfig expectedNotificationConfig = new PushNotificationConfig.Builder()
                    .url("http://2.example.com")
                    .id(MINIMAL_TASK.getId())
                    .build();
            assertEquals(new TaskPushNotificationConfig(MINIMAL_TASK.getId(), expectedNotificationConfig),
                    listResponse.getResult().get(0));
        } catch (Exception e) {
            fail();
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testListPushNotificationConfigTaskNotFound() {
        try {
            client.listTaskPushNotificationConfig("111", "non-existent-task");
            fail();
        } catch (A2AServerException e) {
            assertInstanceOf(TaskNotFoundError.class, e.getCause());
        }
    }

    @Test
    public void testListPushNotificationConfigEmptyList() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        try {
            ListTaskPushNotificationConfigResponse listResponse = client.listTaskPushNotificationConfig("111", MINIMAL_TASK.getId());
            assertEquals("111", listResponse.getId());
            assertEquals(0, listResponse.getResult().size());
        } catch (Exception e) {
            fail();
        } finally {
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testDeletePushNotificationConfigWithValidConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        saveTaskInTaskStore(new Task.Builder()
                .id("task-456")
                .contextId("session-xyz")
                .status(new TaskStatus(TaskState.SUBMITTED))
                .build());

        PushNotificationConfig notificationConfig1 =
                new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id("config1")
                        .build();
        PushNotificationConfig notificationConfig2 =
                new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id("config2")
                        .build();
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig1);
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig2);
        savePushNotificationConfigInStore("task-456", notificationConfig1);

        try {
            // specify the config ID to delete
            DeleteTaskPushNotificationConfigResponse deleteResponse = client.deleteTaskPushNotificationConfig(MINIMAL_TASK.getId(),
                    "config1");
            assertNull(deleteResponse.getError());
            assertNull(deleteResponse.getResult());

            // should now be 1 left
            ListTaskPushNotificationConfigResponse listResponse = client.listTaskPushNotificationConfig(MINIMAL_TASK.getId());
            assertEquals(1, listResponse.getResult().size());

            // should remain unchanged, this is a different task
            listResponse = client.listTaskPushNotificationConfig("task-456");
            assertEquals(1, listResponse.getResult().size());
        } catch (Exception e) {
            fail();
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config1");
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config2");
            deletePushNotificationConfigInStore("task-456", "config1");
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
            deleteTaskInTaskStore("task-456");
        }
    }

    @Test
    public void testDeletePushNotificationConfigWithNonExistingConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        PushNotificationConfig notificationConfig1 =
                new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id("config1")
                        .build();
        PushNotificationConfig notificationConfig2 =
                new PushNotificationConfig.Builder()
                        .url("http://example.com")
                        .id("config2")
                        .build();
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig1);
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig2);

        try {
            DeleteTaskPushNotificationConfigResponse deleteResponse = client.deleteTaskPushNotificationConfig(MINIMAL_TASK.getId(),
                    "non-existent-config-id");
            assertNull(deleteResponse.getError());
            assertNull(deleteResponse.getResult());

            // should remain unchanged
            ListTaskPushNotificationConfigResponse listResponse = client.listTaskPushNotificationConfig(MINIMAL_TASK.getId());
            assertEquals(2, listResponse.getResult().size());
        } catch (Exception e) {
            fail();
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config1");
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), "config2");
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    @Test
    public void testDeletePushNotificationConfigTaskNotFound() {
        try {
            client.deleteTaskPushNotificationConfig("non-existent-task", "non-existent-config-id");
            fail();
        } catch (A2AServerException e) {
            assertInstanceOf(TaskNotFoundError.class, e.getCause());
        }
    }

    @Test
    public void testDeletePushNotificationConfigSetWithoutConfigId() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);
        PushNotificationConfig notificationConfig1 =
                new PushNotificationConfig.Builder()
                        .url("http://1.example.com")
                        .build();
        PushNotificationConfig notificationConfig2 =
                new PushNotificationConfig.Builder()
                        .url("http://2.example.com")
                        .build();
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig1);

        // this one will overwrite the previous one
        savePushNotificationConfigInStore(MINIMAL_TASK.getId(), notificationConfig2);

        try {
            DeleteTaskPushNotificationConfigResponse deleteResponse = client.deleteTaskPushNotificationConfig(MINIMAL_TASK.getId(),
                    MINIMAL_TASK.getId());
            assertNull(deleteResponse.getError());
            assertNull(deleteResponse.getResult());

            // should now be 0
            ListTaskPushNotificationConfigResponse listResponse = client.listTaskPushNotificationConfig(MINIMAL_TASK.getId());
            assertEquals(0, listResponse.getResult().size());
        } catch (Exception e) {
            fail();
        } finally {
            deletePushNotificationConfigInStore(MINIMAL_TASK.getId(), MINIMAL_TASK.getId());
            deleteTaskInTaskStore(MINIMAL_TASK.getId());
        }
    }

    private SendStreamingMessageResponse extractJsonResponseFromSseLine(String line) throws JsonProcessingException {
        line = extractSseData(line);
        if (line != null) {
            return Utils.OBJECT_MAPPER.readValue(line, SendStreamingMessageResponse.class);
        }
        return null;
    }

    private static String extractSseData(String line) {
        if (line.startsWith("data:")) {
            line =  line.substring(5).trim();
            return line;
        }
        return null;
    }

    private boolean isStreamClosedError(Throwable throwable) {
        // Unwrap the CompletionException
        Throwable cause = throwable;

        while (cause != null) {
            if (cause instanceof EOFException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private CompletableFuture<HttpResponse<Stream<String>>> initialiseStreamingRequest(
            StreamingJSONRPCRequest<?> request, String mediaType) throws Exception {

        // Create the client
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        // Create the request
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + serverPort + "/"))
                .POST(HttpRequest.BodyPublishers.ofString(Utils.OBJECT_MAPPER.writeValueAsString(request)))
                .header("Content-Type", APPLICATION_JSON);
        if (mediaType != null) {
            builder.header("Accept", mediaType);
        }
        HttpRequest httpRequest = builder.build();


        // Send request async and return the CompletableFuture
        return client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofLines());
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
throw new RuntimeException(String.format("Saving task failed! Status: %d, Body: %s", response.statusCode(), response.body()));
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

    private static class BreakException extends RuntimeException {

    }
}
