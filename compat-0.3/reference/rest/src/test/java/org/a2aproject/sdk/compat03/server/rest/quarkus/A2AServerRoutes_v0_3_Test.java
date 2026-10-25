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
import java.util.stream.Stream;

import jakarta.enterprise.inject.Instance;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.jspecify.annotations.Nullable;
import org.a2aproject.sdk.compat03.conversion.Convert_v0_3_To10RequestHandler;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.transport.rest.handler.RestHandler_v0_3;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.GetTaskPushNotificationConfigParams;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
                assertError(422, "InvalidParamsError", "bad history_length or historyLength");
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
        assertError(422, "InvalidParamsError", "bad config name");
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

    @ParameterizedTest(name = "taskId={0}, history_length={1}, historyLength={2}")
    @MethodSource("taskAndAliasValues")
    public void testGetTaskForwardsRawPathAndQueryValues(@Nullable String taskId, @Nullable String snake,
            @Nullable String camel) {
        fixture(true);
        RestHandler_v0_3 transport = mock(RestHandler_v0_3.class);
        routes.jsonRestHandler = transport;
        when(rc.pathParam("id")).thenReturn(taskId);
        when(rc.pathParam("param0")).thenReturn(taskId);
        if (snake != null) {
            params.set("history_length", snake);
        }
        if (camel != null) {
            params.set("historyLength", camel);
        }
        RestHandler_v0_3.HTTPRestResponse error = rawForwardingError();
        when(transport.getTask(eq(taskId), eq(snake), eq(camel), any())).thenReturn(error);

        routes.getTask(rc);

        verify(transport).getTask(eq(taskId), eq(snake), eq(camel), any());
        verifyNoMoreInteractions(transport);
        assertRawForwardingResponse(error);
    }

    @ParameterizedTest(name = "taskId={0}")
    @MethodSource("taskIds")
    public void testCancelTaskForwardsRawPathValue(@Nullable String taskId) {
        fixture(true);
        RestHandler_v0_3 transport = mock(RestHandler_v0_3.class);
        routes.jsonRestHandler = transport;
        setTaskId(taskId);
        RestHandler_v0_3.HTTPRestResponse error = rawForwardingError();
        when(transport.cancelTask(eq(taskId), any())).thenReturn(error);

        routes.cancelTask(rc);

        verify(transport).cancelTask(eq(taskId), any());
        verifyNoMoreInteractions(transport);
        assertRawForwardingResponse(error);
    }

    @ParameterizedTest(name = "taskId={0}")
    @MethodSource("taskIds")
    public void testResubscribeTaskForwardsRawPathValue(@Nullable String taskId) {
        fixture(true);
        RestHandler_v0_3 transport = mock(RestHandler_v0_3.class);
        routes.jsonRestHandler = transport;
        setTaskId(taskId);
        RestHandler_v0_3.HTTPRestResponse error = rawForwardingError();
        when(transport.resubscribeTask(eq(taskId), any())).thenReturn(error);

        routes.resubscribeTask(rc);

        verify(transport).resubscribeTask(eq(taskId), any());
        verifyNoMoreInteractions(transport);
        assertRawForwardingResponse(error);
    }

    @ParameterizedTest(name = "taskId={0}")
    @MethodSource("taskIds")
    public void testSetPushConfigForwardsRawPathValue(@Nullable String taskId) {
        fixture(true);
        RestHandler_v0_3 transport = mock(RestHandler_v0_3.class);
        routes.jsonRestHandler = transport;
        setTaskId(taskId);
        RestHandler_v0_3.HTTPRestResponse error = rawForwardingError();
        when(transport.setTaskPushNotificationConfiguration(eq(taskId), eq("{broken"), any())).thenReturn(error);

        routes.setTaskPushNotificationConfiguration("{broken", rc);

        verify(transport).setTaskPushNotificationConfiguration(eq(taskId), eq("{broken"), any());
        verifyNoMoreInteractions(transport);
        assertRawForwardingResponse(error);
    }

    @ParameterizedTest(name = "taskId={0}, configId={1}")
    @MethodSource("taskAndConfigIds")
    public void testGetPushConfigForwardsRawPathValues(@Nullable String taskId, @Nullable String configId) {
        fixture(true);
        RestHandler_v0_3 transport = mock(RestHandler_v0_3.class);
        routes.jsonRestHandler = transport;
        setTaskId(taskId);
        when(rc.pathParam("configId")).thenReturn(configId);
        RestHandler_v0_3.HTTPRestResponse error = rawForwardingError();
        when(transport.getTaskPushNotificationConfiguration(eq(taskId), eq(configId), any())).thenReturn(error);

        routes.getTaskPushNotificationConfiguration(rc);

        verify(transport).getTaskPushNotificationConfiguration(eq(taskId), eq(configId), any());
        verifyNoMoreInteractions(transport);
        assertRawForwardingResponse(error);
    }

    @ParameterizedTest(name = "taskId={0}")
    @MethodSource("taskIds")
    public void testListPushConfigsForwardsRawPathValue(@Nullable String taskId) {
        fixture(true);
        RestHandler_v0_3 transport = mock(RestHandler_v0_3.class);
        routes.jsonRestHandler = transport;
        setTaskId(taskId);
        RestHandler_v0_3.HTTPRestResponse error = rawForwardingError();
        when(transport.listTaskPushNotificationConfigurations(eq(taskId), any())).thenReturn(error);

        routes.listTaskPushNotificationConfigurations(rc);

        verify(transport).listTaskPushNotificationConfigurations(eq(taskId), any());
        verifyNoMoreInteractions(transport);
        assertRawForwardingResponse(error);
    }

    @ParameterizedTest(name = "taskId={0}, configId={1}")
    @MethodSource("taskAndConfigIds")
    public void testDeletePushConfigForwardsRawPathValues(@Nullable String taskId, @Nullable String configId) {
        fixture(true);
        RestHandler_v0_3 transport = mock(RestHandler_v0_3.class);
        routes.jsonRestHandler = transport;
        setTaskId(taskId);
        when(rc.pathParam("configId")).thenReturn(configId);
        RestHandler_v0_3.HTTPRestResponse error = rawForwardingError();
        when(transport.deleteTaskPushNotificationConfiguration(eq(taskId), eq(configId), any())).thenReturn(error);

        routes.deleteTaskPushNotificationConfiguration(rc);

        verify(transport).deleteTaskPushNotificationConfiguration(eq(taskId), eq(configId), any());
        verifyNoMoreInteractions(transport);
        assertRawForwardingResponse(error);
    }

    private static Stream<Arguments> taskIds() {
        return Stream.of(Arguments.of((Object) null), Arguments.of(""), Arguments.of("task"));
    }

    private static Stream<Arguments> taskAndConfigIds() {
        return Stream.of((String) null, "", "task")
                .flatMap(taskId -> Stream.of((String) null, "", "config")
                        .map(configId -> Arguments.of(taskId, configId)));
    }

    private static Stream<Arguments> taskAndAliasValues() {
        String[][] aliases = {{null, null}, {"", null}, {null, ""}, {"bad", null}, {null, "bad"},
                {"2", null}, {null, "2"}, {"", "bad"}};
        return Stream.of((String) null, "", "task")
                .flatMap(taskId -> Stream.of(aliases)
                        .map(alias -> Arguments.of(taskId, alias[0], alias[1])));
    }

    private void setTaskId(@Nullable String taskId) {
        when(rc.pathParam("id")).thenReturn(taskId);
        when(rc.pathParam("param0")).thenReturn(taskId);
    }

    private RestHandler_v0_3.HTTPRestResponse rawForwardingError() {
        return new RestHandler_v0_3.HTTPRestResponse(422, "application/json", "{\"transportError\":true}");
    }

    private void assertRawForwardingResponse(RestHandler_v0_3.HTTPRestResponse error) {
        verify(response).setStatusCode(422);
        verify(response).putHeader(CONTENT_TYPE, "application/json");
        verify(response).end(error.getBody());
        verifyNoMoreInteractions(response);
    }
}
