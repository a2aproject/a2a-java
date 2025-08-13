/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.a2a.client.transport;

import static io.a2a.client.transport.JsonRestMessages.CANCEL_TASK_TEST_REQUEST;
import static io.a2a.client.transport.JsonRestMessages.CANCEL_TASK_TEST_RESPONSE;
import static io.a2a.client.transport.JsonRestMessages.GET_TASK_TEST_RESPONSE;
import static io.a2a.client.transport.JsonRestMessages.SEND_MESSAGE_TEST_REQUEST;
import static io.a2a.client.transport.JsonRestMessages.SEND_MESSAGE_TEST_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import io.a2a.client.ClientCallContext;
import io.a2a.spec.Artifact;
import io.a2a.spec.EventKind;
import io.a2a.spec.FilePart;
import io.a2a.spec.FileWithBytes;
import io.a2a.spec.FileWithUri;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.Part.Kind;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskQueryParams;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import java.util.Collections;
import java.util.HashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.JsonBody;

public class JSONRestTransportTest {

    private ClientAndServer server;

    @BeforeEach
    public void setUp() {
        server = new ClientAndServer(4001);
    }

    @AfterEach
    public void tearDown() {
        server.stop();
    }

    public JSONRestTransportTest() {
    }

    /**
     * Test of sendMessage method, of class JSONRestTransport.
     */
    @Test
    public void testSendMessage() throws Exception {
        Message message = new Message.Builder()
                .role(Message.Role.USER)
                .parts(Collections.singletonList(new TextPart("tell me a joke")))
                .contextId("context-1234")
                .messageId("message-1234")
                .taskId("")
                .build();
        this.server.when(
                request()
                        .withMethod("POST")
                        .withPath("/v1/message:send")
                        .withBody(JsonBody.json(SEND_MESSAGE_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(SEND_MESSAGE_TEST_RESPONSE)
                );
        MessageSendParams messageSendParams = new MessageSendParams(message, null, null);
        ClientCallContext context = null;
        JSONRestTransport instance = new JSONRestTransport("http://localhost:4001");
        EventKind result = instance.sendMessage(messageSendParams, context);
        assertEquals("task", result.getKind());
        Task task = (Task) result;
        assertEquals("9b511af4-b27c-47fa-aecf-2a93c08a44f8", task.getId());
        assertEquals("context-1234", task.getContextId());
        assertEquals(TaskState.SUBMITTED, task.getStatus().state());
        assertNull(task.getStatus().message());
        assertNull(task.getMetadata());
        assertEquals(true, task.getArtifacts().isEmpty());
        assertEquals(1, task.getHistory().size());
        Message history = task.getHistory().get(0);
        assertEquals("message", history.getKind());
        assertEquals(Message.Role.USER, history.getRole());
        assertEquals("context-1234", history.getContextId());
        assertEquals("message-1234", history.getMessageId());
        assertEquals("9b511af4-b27c-47fa-aecf-2a93c08a44f8", history.getTaskId());
        assertEquals(1, history.getParts().size());
        assertEquals(Kind.TEXT, history.getParts().get(0).getKind());
        assertEquals("tell me a joke", ((TextPart) history.getParts().get(0)).getText());
        assertNull(history.getMetadata());
        assertNull(history.getReferenceTaskIds());
    }

    /**
     * Test of cancelTask method, of class JSONRestTransport.
     */
    @Test
    public void testCancelTask() throws Exception {
        this.server.when(
                request()
                        .withMethod("POST")
                        .withPath("/v1/tasks/de38c76d-d54c-436c-8b9f-4c2703648d64:cancel")
                        .withBody(JsonBody.json(CANCEL_TASK_TEST_REQUEST, MatchType.ONLY_MATCHING_FIELDS))
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(CANCEL_TASK_TEST_RESPONSE)
                );
        ClientCallContext context = null;
        JSONRestTransport instance = new JSONRestTransport("http://localhost:4001");
        Task task = instance.cancelTask(new TaskIdParams("de38c76d-d54c-436c-8b9f-4c2703648d64",
                new HashMap<>()), context);
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertEquals(TaskState.CANCELED, task.getStatus().state());
        assertNull(task.getStatus().message());
        assertNull(task.getMetadata());
    }

    /**
     * Test of getTask method, of class JSONRestTransport.
     */
    @Test
    public void testGetTask() throws Exception {
        this.server.when(
                request()
                        .withMethod("GET")
                        .withPath("/v1/tasks/de38c76d-d54c-436c-8b9f-4c2703648d64")
        )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(GET_TASK_TEST_RESPONSE)
                );
        ClientCallContext context = null;
        TaskQueryParams request = new TaskQueryParams("de38c76d-d54c-436c-8b9f-4c2703648d64", 10);
        JSONRestTransport instance = new JSONRestTransport("http://localhost:4001");
        Task task = instance.getTask(request, context);
        assertEquals("de38c76d-d54c-436c-8b9f-4c2703648d64", task.getId());
        assertEquals(TaskState.COMPLETED, task.getStatus().state());
        assertNull(task.getStatus().message());
        assertNull(task.getMetadata());
        assertEquals(false, task.getArtifacts().isEmpty());
        assertEquals(1, task.getArtifacts().size());
        Artifact artifact = task.getArtifacts().get(0);
        assertEquals("artifact-1", artifact.artifactId());
        assertEquals("", artifact.name());
        assertEquals(false, artifact.parts().isEmpty());
        assertEquals(Kind.TEXT, artifact.parts().get(0).getKind());
        assertEquals("Why did the chicken cross the road? To get to the other side!", ((TextPart)artifact.parts().get(0)).getText());
        assertEquals(1, task.getHistory().size());
        Message history = task.getHistory().get(0);
        assertEquals("message", history.getKind());
        assertEquals(Message.Role.USER, history.getRole());
        assertEquals("message-123", history.getMessageId());
        assertEquals(3, history.getParts().size());
        assertEquals(Kind.TEXT, history.getParts().get(0).getKind());
        assertEquals("tell me a joke", ((TextPart) history.getParts().get(0)).getText());
        assertEquals(Kind.FILE, history.getParts().get(1).getKind());
        FilePart part = (FilePart) history.getParts().get(1);
        assertEquals("text/plain", part.getFile().mimeType());
        assertEquals("file.txt",part.getFile().name());
        assertEquals("file:///path/to/file.txt", ((FileWithUri)part.getFile()).uri());
        part = (FilePart) history.getParts().get(2);
        assertEquals(Kind.FILE, part.getKind());
        assertEquals("hello.txt", part.getFile().name());
        assertEquals("text/plain", part.getFile().mimeType());
        assertEquals("hello", ((FileWithBytes)part.getFile()).bytes());
        assertNull(history.getMetadata());
        assertNull(history.getReferenceTaskIds());
    }
//
//
//    /**
//     * Test of sendMessageStreaming method, of class JSONRestTransport.
//     */
//    @Test
//    public void testSendMessageStreaming() throws Exception {
//        System.out.println("sendMessageStreaming");
//        MessageSendParams request = null;
//        Consumer<StreamingEventKind> eventConsumer = null;
//        Consumer<Throwable> errorConsumer = null;
//        ClientCallContext context = null;
//        JSONRestTransport instance =new JSONRestTransport("http://localhost:4001");
//        instance.sendMessageStreaming(request, eventConsumer, errorConsumer, context);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getTask method, of class JSONRestTransport.
//     */
//    @Test
//    public void testGetTask() throws Exception {
//        System.out.println("getTask");
//        TaskQueryParams request = null;
//        ClientCallContext context = null;
//        JSONRestTransport instance =new JSONRestTransport("http://localhost:4001");
//        Task expResult = null;
//        Task result = instance.getTask(request, context);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of cancelTask method, of class JSONRestTransport.
//     */
//    @Test
//    public void testCancelTask() throws Exception {
//        System.out.println("cancelTask");
//        TaskIdParams request = null;
//        ClientCallContext context = null;
//        JSONRestTransport instance =new JSONRestTransport("http://localhost:4001");
//        Task expResult = null;
//        Task result = instance.cancelTask(request, context);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of setTaskPushNotificationConfiguration method, of class JSONRestTransport.
//     */
//    @Test
//    public void testSetTaskPushNotificationConfiguration() throws Exception {
//        System.out.println("setTaskPushNotificationConfiguration");
//        TaskPushNotificationConfig request = null;
//        ClientCallContext context = null;
//        JSONRestTransport instance =new JSONRestTransport("http://localhost:4001");
//        TaskPushNotificationConfig expResult = null;
//        TaskPushNotificationConfig result = instance.setTaskPushNotificationConfiguration(request, context);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getTaskPushNotificationConfiguration method, of class JSONRestTransport.
//     */
//    @Test
//    public void testGetTaskPushNotificationConfiguration() throws Exception {
//        System.out.println("getTaskPushNotificationConfiguration");
//        GetTaskPushNotificationConfigParams request = null;
//        ClientCallContext context = null;
//        JSONRestTransport instance =new JSONRestTransport("http://localhost:4001");
//        TaskPushNotificationConfig expResult = null;
//        TaskPushNotificationConfig result = instance.getTaskPushNotificationConfiguration(request, context);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of listTaskPushNotificationConfigurations method, of class JSONRestTransport.
//     */
//    @Test
//    public void testListTaskPushNotificationConfigurations() throws Exception {
//        System.out.println("listTaskPushNotificationConfigurations");
//        ListTaskPushNotificationConfigParams request = null;
//        ClientCallContext context = null;
//        JSONRestTransport instance =new JSONRestTransport("http://localhost:4001");
//        List<TaskPushNotificationConfig> expResult = null;
//        List<TaskPushNotificationConfig> result = instance.listTaskPushNotificationConfigurations(request, context);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of deleteTaskPushNotificationConfigurations method, of class JSONRestTransport.
//     */
//    @Test
//    public void testDeleteTaskPushNotificationConfigurations() throws Exception {
//        System.out.println("deleteTaskPushNotificationConfigurations");
//        DeleteTaskPushNotificationConfigParams request = null;
//        ClientCallContext context = null;
//        JSONRestTransport instance =new JSONRestTransport("http://localhost:4001");
//        instance.deleteTaskPushNotificationConfigurations(request, context);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of resubscribe method, of class JSONRestTransport.
//     */
//    @Test
//    public void testResubscribe() throws Exception {
//        System.out.println("resubscribe");
//        TaskIdParams request = null;
//        Consumer<StreamingEventKind> eventConsumer = null;
//        Consumer<Throwable> errorConsumer = null;
//        ClientCallContext context = null;
//        JSONRestTransport instance =new JSONRestTransport("http://localhost:4001");
//        instance.resubscribe(request, eventConsumer, errorConsumer, context);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of getAgentCard method, of class JSONRestTransport.
//     */
//    @Test
//    public void testGetAgentCard() throws Exception {
//        System.out.println("getAgentCard");
//        ClientCallContext context = null;
//        JSONRestTransport instance =new JSONRestTransport("http://localhost:4001");
//        AgentCard expResult = null;
//        AgentCard result = instance.getAgentCard(context);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//
//    /**
//     * Test of close method, of class JSONRestTransport.
//     */
//    @Test
//    public void testClose() {
//        System.out.println("close");
//        JSONRestTransport instance =new JSONRestTransport("http://localhost:4001");
//        instance.close();
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
//    }
//    
}
