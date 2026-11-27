package org.a2aproject.sdk.client.transport.rest;

import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.CANCEL_TASK_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.GET_TASK_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.LIST_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
import static org.a2aproject.sdk.client.transport.rest.JsonRestMessages.TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.CancelTaskParams;
import org.a2aproject.sdk.spec.DeleteTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.ListTaskPushNotificationConfigsParams;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.junit.jupiter.api.Test;

class RestResourcePathTest {
    @Test
    void resourceIdsAreEncodedAsSinglePathSegments() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        var paths = new LinkedBlockingQueue<String>();
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getRawPath();
            paths.add(exchange.getRequestMethod() + " " + path +
                    (exchange.getRequestURI().getRawQuery() == null ? "" : "?" + exchange.getRequestURI().getRawQuery()));
            String body;
            if (path.endsWith(":subscribe")) body = TASK_RESUBSCRIPTION_REQUEST_TEST_RESPONSE;
            else if (exchange.getRequestMethod().equals("DELETE")) body = "";
            else if (path.endsWith("/pushNotificationConfigs")) body = exchange.getRequestMethod().equals("POST")
                    ? SET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE : LIST_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
            else if (path.contains("/pushNotificationConfigs/")) body = GET_TASK_PUSH_NOTIFICATION_CONFIG_TEST_RESPONSE;
            else if (exchange.getRequestMethod().equals("POST")) body = CANCEL_TASK_TEST_RESPONSE;
            else body = GET_TASK_TEST_RESPONSE;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", path.endsWith(":subscribe") ? "text/event-stream" : "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        try {
            AgentInterface iface = new AgentInterface("HTTP+JSON", "http://localhost:" + server.getAddress().getPort());
            AgentCard card = AgentCard.builder().name("test").description("test").version("1.0")
                    .supportedInterfaces(List.of(iface)).capabilities(AgentCapabilities.builder().build())
                    .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text")).skills(List.of()).build();
            RestTransport client = new RestTransport(null, card, iface, null);
            String[][] cases = {
                    {"normal-id_~", "normal-id_~"}, {"a b", "a%20b"}, {"a/b", "a%2Fb"},
                    {"a%b", "a%25b"}, {"foo:subscribe", "foo%3Asubscribe"},
                    {"中?x=1", "%E4%B8%AD%3Fx%3D1"}, {".", "%2E"}, {"..", "%2E%2E"}
            };
            for (String[] test : cases) {
                client.getTask(new TaskQueryParams(test[0]), null);
                assertEquals("GET /tasks/" + test[1], paths.poll(5, TimeUnit.SECONDS));
            }
            client.getTask(new TaskQueryParams("foo?bar", 5), null);
            assertEquals("GET /tasks/foo%3Fbar?historyLength=5", paths.poll(5, TimeUnit.SECONDS));
            String taskId = "task/a?x=1";
            String configId = "cfg:一%";
            String taskPath = "/tasks/task%2Fa%3Fx%3D1";
            String configPath = taskPath + "/pushNotificationConfigs/cfg%3A%E4%B8%80%25";
            client.cancelTask(new CancelTaskParams(taskId), null);
            assertEquals("POST " + taskPath + ":cancel", paths.poll(5, TimeUnit.SECONDS));
            client.subscribeToTask(new TaskIdParams(taskId), event -> {}, error -> {}, null);
            assertEquals("POST " + taskPath + ":subscribe", paths.poll(5, TimeUnit.SECONDS));
            client.getTaskPushNotificationConfiguration(new GetTaskPushNotificationConfigParams(taskId, configId), null);
            assertEquals("GET " + configPath, paths.poll(5, TimeUnit.SECONDS));
            client.getTaskPushNotificationConfiguration(new GetTaskPushNotificationConfigParams(taskId), null);
            assertEquals("GET " + taskPath + "/pushNotificationConfigs/", paths.poll(5, TimeUnit.SECONDS));
            client.deleteTaskPushNotificationConfigurations(new DeleteTaskPushNotificationConfigParams(taskId, configId), null);
            assertEquals("DELETE " + configPath, paths.poll(5, TimeUnit.SECONDS));
            client.listTaskPushNotificationConfigurations(new ListTaskPushNotificationConfigsParams(taskId), null);
            assertEquals("GET " + taskPath + "/pushNotificationConfigs", paths.poll(5, TimeUnit.SECONDS));
            client.createTaskPushNotificationConfiguration(TaskPushNotificationConfig.builder().taskId(taskId).url("https://example.com").build(), null);
            assertEquals("POST " + taskPath + "/pushNotificationConfigs", paths.poll(5, TimeUnit.SECONDS));
        } finally {
            server.stop(0);
        }
    }
}
