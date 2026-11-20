package org.a2aproject.sdk.client.transport.rest;

import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.CANCEL_TASK_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.GET_TASK_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.LIST_TASKS_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.LIST_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.SEND_MESSAGE_STREAMING_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.SEND_MESSAGE_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.Message;
import com.sun.net.httpserver.HttpServer;

import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.GetExtendedAgentCardParams;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class RestInterceptorWireTest {
    @Test
    void mutableAndImmutableReplacementsDriveAllResourcePaths() throws Exception {
        for (boolean immutable : List.of(false, true)) {
            var queue = new LinkedBlockingQueue<String>();
            var header = new java.util.concurrent.atomic.AtomicReference<String>();
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext(
                    "/",
                    exchange -> {
                        String path = exchange.getRequestURI().getRawPath();
                        header.set(exchange.getRequestHeaders().getFirst("X-Probe"));
                        queue.add(
                                exchange.getRequestMethod()
                                        + " "
                                        + exchange.getRequestURI().toString());
                        String body;
                        if (path.endsWith(":subscribe"))
                            body = TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE;
                        else if (path.endsWith("/message:send")) body = SEND_MESSAGE_TEST_RESPONSE;
                        else if (path.endsWith("/message:stream"))
                            body = SEND_MESSAGE_STREAMING_TEST_RESPONSE;
                        else if (path.endsWith("/extendedAgentCard"))
                            body =
                                    "{\"name\":\"agent\",\"description\":\"d\",\"version\":\"1\",\"capabilities\":{},\"defaultInputModes\":[],\"defaultOutputModes\":[],\"skills\":[],\"supportedInterfaces\":[]}";
                        else if (exchange.getRequestMethod().equals("DELETE")) body = "";
                        else if (path.endsWith("/pushNotificationConfigs"))
                            body =
                                    exchange.getRequestMethod().equals("POST")
                                            ? SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE
                                            : LIST_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
                        else if (path.contains("/pushNotificationConfigs/"))
                            body = GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
                        else if (path.endsWith(":cancel")) body = CANCEL_TASK_TEST_RESPONSE;
                        else if (path.endsWith("/tasks")) body = LIST_TASKS_TEST_RESPONSE;
                        else body = GET_TASK_TEST_RESPONSE;
                        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders()
                                .add(
                                        "Content-Type",
                                        (path.endsWith(":subscribe")
                                                        || path.endsWith("/message:stream"))
                                                ? "text/event-stream"
                                                : "application/json");
                        exchange.sendResponseHeaders(200, bytes.length);
                        try (var out = exchange.getResponseBody()) {
                            out.write(bytes);
                        }
                    });
            server.start();
            try {
                var iface =
                        new AgentInterface(
                                "HTTP+JSON",
                                "http://localhost:" + server.getAddress().getPort(),
                                "defaulttenant");
                var card =
                        AgentCard.builder()
                                .name("test")
                                .description("test")
                                .version("1.0")
                                .supportedInterfaces(List.of(iface))
                                .capabilities(AgentCapabilities.builder().build())
                                .defaultInputModes(List.of("text"))
                                .defaultOutputModes(List.of("text"))
                                .skills(List.of())
                                .build();
                ClientCallInterceptor interceptor =
                        new ClientCallInterceptor() {
                            @Override
                            public PayloadAndHeaders intercept(
                                    String method,
                                    Object payload,
                                    Map<String, String> headers,
                                    AgentCard c,
                                    ClientCallContext ctx) {
                                if (payload
                                        instanceof
                                        org.a2aproject.sdk.grpc.SendMessageRequest.Builder b) {
                                    b.setTenant("override");
                                }
                                if (payload
                                        instanceof
                                        org.a2aproject.sdk.grpc.GetExtendedAgentCardRequest.Builder
                                                b) {
                                    b.setTenant("override");
                                }
                                if (payload
                                        instanceof
                                        org.a2aproject.sdk.grpc.GetTaskRequest.Builder b) {
                                    b.setId("changed").setHistoryLength(0);
                                    if (immutable) b.setTenant("override");
                                }
                                if (payload
                                        instanceof
                                        org.a2aproject.sdk.grpc.CancelTaskRequest.Builder b) {
                                    b.setId("changed");
                                }
                                if (payload
                                        instanceof
                                        org.a2aproject.sdk.grpc.SubscribeToTaskRequest.Builder b) {
                                    b.setId("changed");
                                }
                                if (payload
                                        instanceof
                                        org.a2aproject.sdk.grpc.ListTasksRequest.Builder b) {
                                    b.setContextId("filtered")
                                            .setHistoryLength(0)
                                            .setIncludeArtifacts(false);
                                }
                                if (payload
                                        instanceof
                                        org.a2aproject.sdk.grpc.TaskPushNotificationConfig.Builder
                                                b) {
                                    b.setTaskId("changed");
                                }
                                if (payload
                                        instanceof
                                        org.a2aproject.sdk.grpc.GetTaskPushNotificationConfigRequest
                                                        .Builder
                                                b) {
                                    b.setTaskId("changed").setId("config2");
                                }
                                if (payload
                                        instanceof
                                        org.a2aproject.sdk.grpc
                                                        .ListTaskPushNotificationConfigsRequest
                                                        .Builder
                                                b) {
                                    b.setTaskId("changed").setPageSize(3);
                                }
                                if (payload
                                        instanceof
                                        org.a2aproject.sdk.grpc
                                                        .DeleteTaskPushNotificationConfigRequest
                                                        .Builder
                                                b) {
                                    b.setTaskId("changed").setId("config2");
                                }
                                return new PayloadAndHeaders(
                                        immutable && payload instanceof Message.Builder b
                                                ? b.build()
                                                : payload,
                                        headers);
                            }
                        };
                var c = new RestTransport(null, card, iface, List.of(interceptor));
                var message =
                        org.a2aproject.sdk.spec.Message.builder()
                                .role(org.a2aproject.sdk.spec.Message.Role.ROLE_USER)
                                .messageId("m1")
                                .parts(List.of(new TextPart("hello")))
                                .build();
                var send = MessageSendParams.builder().message(message).build();
                c.sendMessage(send, null);
                assertEquals("POST /override/message:send", queue.poll(5, TimeUnit.SECONDS));
                c.sendMessageStreaming(send, event -> {}, error -> {}, null);
                assertEquals("POST /override/message:stream", queue.poll(5, TimeUnit.SECONDS));
                c.getExtendedAgentCard(new GetExtendedAgentCardParams(null), null);
                assertEquals("GET /override/extendedAgentCard", queue.poll(5, TimeUnit.SECONDS));
                c.getTask(new TaskQueryParams("orig"), null);
                assertEquals(
                        "GET /"
                                + (immutable ? "override" : "defaulttenant")
                                + "/tasks/changed?historyLength=0",
                        queue.poll(5, TimeUnit.SECONDS));
                c.cancelTask(new CancelTaskParams("orig"), null);
                assertEquals(
                        "POST /defaulttenant/tasks/changed:cancel",
                        queue.poll(5, TimeUnit.SECONDS));
                c.subscribeToTask(new TaskIdParams("orig"), event -> {}, error -> {}, null);
                assertEquals(
                        "POST /defaulttenant/tasks/changed:subscribe",
                        queue.poll(5, TimeUnit.SECONDS));
                c.listTasks(new ListTasksParams(), null);
                assertEquals(
                        "GET /defaulttenant/tasks?contextId=filtered&historyLength=0&includeArtifacts=false",
                        queue.poll(5, TimeUnit.SECONDS));
                c.getTaskPushNotificationConfiguration(
                        new GetTaskPushNotificationConfigParams("orig", "config1"), null);
                assertEquals(
                        "GET /defaulttenant/tasks/changed/pushNotificationConfigs/config2",
                        queue.poll(5, TimeUnit.SECONDS));
                c.listTaskPushNotificationConfigurations(
                        new ListTaskPushNotificationConfigsParams("orig"), null);
                assertEquals(
                        "GET /defaulttenant/tasks/changed/pushNotificationConfigs?pageSize=3",
                        queue.poll(5, TimeUnit.SECONDS));
                c.deleteTaskPushNotificationConfigurations(
                        new DeleteTaskPushNotificationConfigParams("orig", "config1"), null);
                assertEquals(
                        "DELETE /defaulttenant/tasks/changed/pushNotificationConfigs/config2",
                        queue.poll(5, TimeUnit.SECONDS));
                c.createTaskPushNotificationConfiguration(
                        TaskPushNotificationConfig.builder()
                                .taskId("orig")
                                .url("https://example.com")
                                .build(),
                        null);
                assertEquals(
                        "POST /defaulttenant/tasks/changed/pushNotificationConfigs",
                        queue.poll(5, TimeUnit.SECONDS));
                ClientCallInterceptor clear =
                        new ClientCallInterceptor() {
                            @Override
                            public PayloadAndHeaders intercept(
                                    String method,
                                    Object payload,
                                    Map<String, String> headers,
                                    AgentCard agent,
                                    ClientCallContext ctx) {
                                if (payload
                                        instanceof org.a2aproject.sdk.grpc.GetTaskRequest.Builder b)
                                    b.clearHistoryLength();
                                if (payload
                                        instanceof
                                        org.a2aproject.sdk.grpc.ListTasksRequest.Builder b)
                                    b.clearHistoryLength().clearIncludeArtifacts();
                                return new PayloadAndHeaders(payload, headers);
                            }
                        };
                var cleared = new RestTransport(null, card, iface, List.of(clear));
                cleared.getTask(new TaskQueryParams("orig", 5), null);
                assertEquals("GET /defaulttenant/tasks/orig", queue.poll(5, TimeUnit.SECONDS));
                var list = new ListTasksParams(null, null, null, null, 5, null, true, null);
                cleared.listTasks(list, null);
                assertEquals("GET /defaulttenant/tasks", queue.poll(5, TimeUnit.SECONDS));
                var plain = new RestTransport(null, card, iface, null);
                var explicitDefaults =
                        new ListTasksParams(
                                "",
                                org.a2aproject.sdk.spec.TaskState.TASK_STATE_UNSPECIFIED,
                                null,
                                "",
                                null,
                                null,
                                null,
                                null);
                plain.listTasks(explicitDefaults, null);
                assertEquals(
                        "GET /defaulttenant/tasks?contextId=&status=TASK_STATE_UNSPECIFIED&pageToken=",
                        queue.poll(5, TimeUnit.SECONDS));
                ClientCallInterceptor nullPayload =
                        new ClientCallInterceptor() {
                            @Override
                            public PayloadAndHeaders intercept(
                                    String method,
                                    Object payload,
                                    Map<String, String> headers,
                                    AgentCard agent,
                                    ClientCallContext ctx) {
                                return new PayloadAndHeaders(null, Map.of("X-Probe", "yes"));
                            }
                        };
                var withNull = new RestTransport(null, card, iface, List.of(nullPayload));
                withNull.getTask(new TaskQueryParams("orig", 5), null);
                assertEquals(
                        "GET /defaulttenant/tasks/orig?historyLength=5",
                        queue.poll(5, TimeUnit.SECONDS));
                withNull.getExtendedAgentCard(new GetExtendedAgentCardParams(null), null);
                assertEquals(
                        "GET /defaulttenant/extendedAgentCard", queue.poll(5, TimeUnit.SECONDS));
                assertEquals("yes", header.get());
                withNull.sendMessage(send, null);
                assertEquals("POST /defaulttenant/message:send", queue.poll(5, TimeUnit.SECONDS));
            } finally {
                server.stop(0);
            }
        }
    }
}
