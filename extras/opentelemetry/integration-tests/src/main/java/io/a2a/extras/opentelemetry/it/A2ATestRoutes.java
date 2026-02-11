package io.a2a.extras.opentelemetry.it;



import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.vertx.web.Body;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Test routes for OpenTelemetry integration testing.
 * Exposes test utilities via REST endpoints.
 */
@Singleton
public class A2ATestRoutes {

    private static final String APPLICATION_JSON = "application/json";
    private static final String TEXT_PLAIN = "text/plain";
    private static final Gson gson = new GsonBuilder().create();

    @Inject
    TestUtilsBean testUtilsBean;
    @Inject
    InMemorySpanExporter inMemorySpanExporter;

    @Inject
    Tracer tracer;

    @Route(path = "/test/task", methods = {Route.HttpMethod.POST}, consumes = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void saveTask(@Body String body, RoutingContext rc) {
        try {
            Task task = JsonUtil.fromJson(body, Task.class);
            testUtilsBean.saveTask(task);
            rc.response()
                    .setStatusCode(200)
                    .end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/task/:taskId", methods = {Route.HttpMethod.GET}, produces = {APPLICATION_JSON}, type = Route.HandlerType.BLOCKING)
    public void getTask(@Param String taskId, RoutingContext rc) {
        try {
            Task task = testUtilsBean.getTask(taskId);
            if (task == null) {
                rc.response()
                        .setStatusCode(404)
                        .end();
                return;
            }
            rc.response()
                    .setStatusCode(200)
                    .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .end(JsonUtil.toJson(task));

        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/task/:taskId", methods = {Route.HttpMethod.DELETE}, type = Route.HandlerType.BLOCKING)
    public void deleteTask(@Param String taskId, RoutingContext rc) {
        try {
            Task task = testUtilsBean.getTask(taskId);
            if (task == null) {
                rc.response()
                        .setStatusCode(404)
                        .end();
                return;
            }
            testUtilsBean.deleteTask(taskId);
            rc.response()
                    .setStatusCode(200)
                    .end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/queue/ensure/:taskId", methods = {Route.HttpMethod.POST})
    public void ensureTaskQueue(@Param String taskId, RoutingContext rc) {
        try {
            testUtilsBean.ensureQueue(taskId);
            rc.response()
                    .setStatusCode(200)
                    .end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/queue/enqueueTaskStatusUpdateEvent/:taskId", methods = {Route.HttpMethod.POST})
    public void enqueueTaskStatusUpdateEvent(@Param String taskId, @Body String body, RoutingContext rc) {
        try {
            TaskStatusUpdateEvent event = JsonUtil.fromJson(body, TaskStatusUpdateEvent.class);
            testUtilsBean.enqueueEvent(taskId, event);
            rc.response()
                    .setStatusCode(200)
                    .end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/queue/enqueueTaskArtifactUpdateEvent/:taskId", methods = {Route.HttpMethod.POST})
    public void enqueueTaskArtifactUpdateEvent(@Param String taskId, @Body String body, RoutingContext rc) {
        try {
            TaskArtifactUpdateEvent event = JsonUtil.fromJson(body, TaskArtifactUpdateEvent.class);
            testUtilsBean.enqueueEvent(taskId, event);
            rc.response()
                    .setStatusCode(200)
                    .end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    @Route(path = "/test/queue/childCount/:taskId", methods = {Route.HttpMethod.GET}, produces = {TEXT_PLAIN})
    public void getChildQueueCount(@Param String taskId, RoutingContext rc) {
        int count = testUtilsBean.getChildQueueCount(taskId);
        rc.response()
                .setStatusCode(200)
                .end(String.valueOf(count));
    }

    @Route(path = "/hello", methods = {Route.HttpMethod.GET}, produces = {TEXT_PLAIN})
    public void hello(RoutingContext rc) {
        Span span = tracer.spanBuilder("hello").startSpan();
        try (Scope scope = span.makeCurrent()) {
            rc.response()
                    .setStatusCode(200)
                    .putHeader(CONTENT_TYPE, TEXT_PLAIN)
                    .end("Hello from Quarkus REST");
        } finally {
            span.end();
        }
    }

    @Route(path = "/export", methods = {Route.HttpMethod.GET}, produces = {APPLICATION_JSON})
    public void exportSpans(@Param String taskId, RoutingContext rc) {
        List<SpanData> spans = inMemorySpanExporter.getFinishedSpanItems()
                .stream()
                .filter(sd -> !sd.getName().contains("export") && !sd.getName().contains("reset"))
                .collect(Collectors.toList());
        String json = gson.toJson(serialize(spans));
        rc.response()
                .setStatusCode(200)
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .end(json);
    }

    private JsonElement serialize(List<SpanData> spanDatas) {
        JsonArray spans = new JsonArray(spanDatas.size());
        for (SpanData spanData : spanDatas) {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("spanId", spanData.getSpanId());
            jsonObject.addProperty("traceId", spanData.getTraceId());
            jsonObject.addProperty("name", spanData.getName());
            jsonObject.addProperty("kind", spanData.getKind().name());
            jsonObject.addProperty("ended", spanData.hasEnded());

            jsonObject.addProperty("parentSpanId", spanData.getParentSpanContext().getSpanId());
            jsonObject.addProperty("parent_spanId", spanData.getParentSpanContext().getSpanId());
            jsonObject.addProperty("parent_traceId", spanData.getParentSpanContext().getTraceId());
            jsonObject.addProperty("parent_remote", spanData.getParentSpanContext().isRemote());
            jsonObject.addProperty("parent_valid", spanData.getParentSpanContext().isValid());

            spanData.getAttributes().forEach((k, v) -> {
                jsonObject.addProperty("attr_" + k.getKey(), v.toString());
            });

            spanData.getResource().getAttributes().forEach((k, v) -> {
                jsonObject.addProperty("resource_" + k.getKey(), v.toString());
            });
            spans.add(jsonObject);
        }

        return spans;
    }

    @Route(path = "/reset", methods = {Route.HttpMethod.GET}, produces = {TEXT_PLAIN})
    public void reset(@Param String taskId, RoutingContext rc) {
        inMemorySpanExporter.reset();
        rc.response().setStatusCode(200).end();
    }

    private void errorResponse(Throwable t, RoutingContext rc) {
        t.printStackTrace();
        rc.response()
                .setStatusCode(500)
                .putHeader(CONTENT_TYPE, TEXT_PLAIN)
                .end();
    }

    @ApplicationScoped
    static class InMemorySpanExporterProducer {

        @Produces
        @Singleton
        InMemorySpanExporter inMemorySpanExporter() {
            return InMemorySpanExporter.create();
        }
    }
}
