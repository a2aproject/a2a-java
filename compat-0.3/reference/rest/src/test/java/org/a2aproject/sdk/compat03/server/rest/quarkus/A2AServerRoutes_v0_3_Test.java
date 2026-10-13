package org.a2aproject.sdk.compat03.server.rest.quarkus;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Consumer;

import jakarta.enterprise.inject.Instance;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.a2aproject.sdk.compat03.conversion.Convert_v0_3_To10RequestHandler;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.transport.rest.handler.RestHandler_v0_3;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Wire parity and raw delegation at the compat REST route boundary. */
public class A2AServerRoutes_v0_3_Test {
    private A2AServerRoutes_v0_3 routes;
    private RequestHandler handler;
    private RoutingContext rc;
    private HttpServerResponse response;
    private MultiMap params;

    private void fixture(boolean capabilities) {
        routes = new A2AServerRoutes_v0_3();
        handler = mock(RequestHandler.class);
        AgentCard_v0_3 card = new AgentCard_v0_3.Builder().name("validation").description("Validation baseline")
                .url("http://localhost:9999").version("1").protocolVersion("0.3")
                .capabilities(new AgentCapabilities_v0_3(capabilities, capabilities, false, null))
                .defaultInputModes(List.of("text")).defaultOutputModes(List.of("text")).skills(List.of()).build();
        routes.jsonRestHandler = new RestHandler_v0_3(card, Runnable::run, new Convert_v0_3_To10RequestHandler(handler));
        routes.callContextFactory = mock(Instance.class);
        when(routes.callContextFactory.isUnsatisfied()).thenReturn(true);
        rc = mock(RoutingContext.class);
        HttpServerRequest request = mock(HttpServerRequest.class);
        response = mock(HttpServerResponse.class);
        params = MultiMap.caseInsensitiveMultiMap();
        when(rc.request()).thenReturn(request);
        when(rc.response()).thenReturn(response);
        when(rc.pathParam("id")).thenReturn("task");
        when(rc.pathParam("param0")).thenReturn("task");
        when(rc.pathParam("configId")).thenReturn("config");
        when(request.params()).thenReturn(params);
        when(request.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(response.putHeader(any(CharSequence.class), any(CharSequence.class))).thenReturn(response);
        when(response.end(anyString())).thenReturn(Future.succeededFuture());
    }

    private void assertError(int status, String type, String message) {
        verify(response).setStatusCode(status);
        verify(response).putHeader(CONTENT_TYPE, "application/json");
        verify(response).end("{\"error\": \"org.a2aproject.sdk.compat03.spec." + type
                + "_v0_3\", \"message\": \"" + message + "\"}");
        verifyNoMoreInteractions(response);
    }

    private void assertBadPath(Consumer<RoutingContext> operation) {
        for (String id : new String[] {null, ""}) {
            for (boolean capabilities : new boolean[] {true, false}) {
                fixture(capabilities);
                when(rc.pathParam("id")).thenReturn(id);
                when(rc.pathParam("param0")).thenReturn(id);
                when(rc.pathParam("configId")).thenReturn("");
                params.set("history_length", "bad").set("historyLength", "bad");
                operation.accept(rc);
                assertError(422, "InvalidParamsError", "bad task id");
                verifyNoInteractions(handler);
            }
        }
    }

    @Test
    public void testGetTaskRawAliasValidationOrder() {
        assertBadPath(context -> routes.getTask(context));
        for (String id : new String[] {null, ""}) {
            fixture(true);
            when(rc.pathParam("id")).thenReturn(id);
            params.set("historyLength", "bad");
            routes.getTask(rc);
            assertError(422, "InvalidParamsError", "bad task id");
            verifyNoInteractions(handler);
        }
        for (String snake : List.of("", "bad", "1")) {
            fixture(true);
            params.set("history_length", snake).set("historyLength", "bad");
            routes.getTask(rc);
            assertError(422, "InvalidParamsError", "Only one of 'history_length' or 'historyLength' may be specified");
            verifyNoInteractions(handler);
        }
        for (String alias : List.of("history_length", "historyLength")) {
            for (String value : List.of("bad", "", "2147483648", "-1")) {
                fixture(true);
                params.set(alias, value);
                routes.getTask(rc);
                assertError(value.equals("-1") ? 500 : 422,
                        value.equals("-1") ? "InternalError" : "InvalidParamsError",
                        value.equals("-1") ? "Invalid history length" : "bad history_length or historyLength");
                verifyNoInteractions(handler);
            }
        }
        for (String alias : new String[] {null, "history_length", "historyLength"}) {
            fixture(true);
            if (alias != null) {
                params.set(alias, "2");
            }
            routes.getTask(rc);
            ArgumentCaptor<TaskQueryParams> query = ArgumentCaptor.forClass(TaskQueryParams.class);
            verify(handler).onGetTask(query.capture(), any());
            assertEquals(alias == null ? null : Integer.valueOf(2), query.getValue().historyLength());
            assertError(404, "TaskNotFoundError", "Task not found");
        }
    }

    @Test
    public void testCancelTaskPathValidationOrder() {
        assertBadPath(context -> routes.cancelTask(context));
    }

    @Test
    public void testResubscribeTaskPathValidationOrder() {
        assertBadPath(context -> routes.resubscribeTask(context));
    }

    @Test
    public void testSetPushConfigPathValidationOrder() {
        assertBadPath(context -> routes.setTaskPushNotificationConfiguration("{broken", context));
        fixture(true);
        routes.setTaskPushNotificationConfiguration("{\"config\":{\"name\":\"short\"}}", rc);
        assertError(500, "InternalError", "Index 1 out of bounds for length 1");
        verifyNoInteractions(handler);
    }

    @Test
    public void testGetPushConfigPathValidationOrder() {
        assertBadPath(context -> routes.getTaskPushNotificationConfiguration(context));
        for (String configId : new String[] {null, ""}) {
            fixture(true);
            when(rc.pathParam("configId")).thenReturn(configId);
            when(handler.onGetTaskPushNotificationConfig(any(), any())).thenThrow(new InternalError("delegated"));
            routes.getTaskPushNotificationConfiguration(rc);
            ArgumentCaptor<GetTaskPushNotificationConfigParams> query =
                    ArgumentCaptor.forClass(GetTaskPushNotificationConfigParams.class);
            verify(handler).onGetTaskPushNotificationConfig(query.capture(), any());
            assertEquals(configId == null ? "task" : "", query.getValue().id());
            assertError(500, "InternalError", "delegated");
        }
    }

    @Test
    public void testListPushConfigsPathValidationOrder() {
        assertBadPath(context -> routes.listTaskPushNotificationConfigurations(context));
    }

    @Test
    public void testDeletePushConfigPathValidationOrder() {
        assertBadPath(context -> routes.deleteTaskPushNotificationConfiguration(context));
        for (String configId : new String[] {null, ""}) {
            for (boolean capabilities : new boolean[] {true, false}) {
                fixture(capabilities);
                when(rc.pathParam("configId")).thenReturn(configId);
                routes.deleteTaskPushNotificationConfiguration(rc);
                assertError(422, "InvalidParamsError", "bad config id");
                verifyNoInteractions(handler);
            }
        }
    }

    @Test
    public void testRawPathAndQueryValuesForwardedForAllOperations() {
        for (String taskId : new String[] {null, "", "task"}) {
            for (String configId : new String[] {null, "", "config"}) {
                for (String[] aliases : new String[][] {{null, null}, {"", null}, {null, ""},
                        {"bad", null}, {null, "bad"}, {"2", null}, {null, "2"}, {"", "bad"}}) {
                    for (int operation = 0; operation < 7; operation++) {
                        fixture(true);
                        RestHandler_v0_3 transport = mock(RestHandler_v0_3.class);
                        routes.jsonRestHandler = transport;
                        when(rc.pathParam("id")).thenReturn(taskId);
                        when(rc.pathParam("param0")).thenReturn(taskId);
                        when(rc.pathParam("configId")).thenReturn(configId);
                        if (aliases[0] != null) {
                            params.set("history_length", aliases[0]);
                        }
                        if (aliases[1] != null) {
                            params.set("historyLength", aliases[1]);
                        }
                        RestHandler_v0_3.HTTPRestResponse error =
                                new RestHandler_v0_3.HTTPRestResponse(422, "application/json", "{\"transportError\":true}");
                        switch (operation) {
                            case 0 -> {
                                when(transport.getTask(eq(taskId), eq(aliases[0]), eq(aliases[1]), any())).thenReturn(error);
                                routes.getTask(rc);
                                verify(transport).getTask(eq(taskId), eq(aliases[0]), eq(aliases[1]), any());
                            }
                            case 1 -> {
                                when(transport.cancelTask(eq(taskId), any())).thenReturn(error);
                                routes.cancelTask(rc);
                                verify(transport).cancelTask(eq(taskId), any());
                            }
                            case 2 -> {
                                when(transport.resubscribeTask(eq(taskId), any())).thenReturn(error);
                                routes.resubscribeTask(rc);
                                verify(transport).resubscribeTask(eq(taskId), any());
                            }
                            case 3 -> {
                                when(transport.setTaskPushNotificationConfiguration(eq(taskId), eq("{broken"), any())).thenReturn(error);
                                routes.setTaskPushNotificationConfiguration("{broken", rc);
                                verify(transport).setTaskPushNotificationConfiguration(eq(taskId), eq("{broken"), any());
                            }
                            case 4 -> {
                                when(transport.getTaskPushNotificationConfiguration(eq(taskId), eq(configId), any())).thenReturn(error);
                                routes.getTaskPushNotificationConfiguration(rc);
                                verify(transport).getTaskPushNotificationConfiguration(eq(taskId), eq(configId), any());
                            }
                            case 5 -> {
                                when(transport.listTaskPushNotificationConfigurations(eq(taskId), any())).thenReturn(error);
                                routes.listTaskPushNotificationConfigurations(rc);
                                verify(transport).listTaskPushNotificationConfigurations(eq(taskId), any());
                            }
                            case 6 -> {
                                when(transport.deleteTaskPushNotificationConfiguration(eq(taskId), eq(configId), any())).thenReturn(error);
                                routes.deleteTaskPushNotificationConfiguration(rc);
                                verify(transport).deleteTaskPushNotificationConfiguration(eq(taskId), eq(configId), any());
                            }
                            default -> throw new AssertionError(operation);
                        }
                        verifyNoMoreInteractions(transport);
                        verify(response).setStatusCode(422);
                        verify(response).putHeader(CONTENT_TYPE, "application/json");
                        verify(response).end(error.getBody());
                        verifyNoMoreInteractions(response);
                    }
                }
            }
        }
    }
}
