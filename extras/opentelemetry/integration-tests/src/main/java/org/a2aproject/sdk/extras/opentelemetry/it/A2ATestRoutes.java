package org.a2aproject.sdk.extras.opentelemetry.it;



import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.a2aproject.sdk.jsonrpc.common.json.JsonUtil;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskArtifactUpdateEvent;
import org.a2aproject.sdk.spec.TaskStatusUpdateEvent;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
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

    void setupRoutes(@Observes Router router) {
        router.post("/test/task")
            .consumes(APPLICATION_JSON)
            .handler(BodyHandler.create())
            .blockingHandler(ctx -> saveTask(ctx.body().asString(), ctx));

        router.get("/test/task/:taskId")
            .produces(APPLICATION_JSON)
            .blockingHandler(ctx -> getTask(ctx.pathParam("taskId"), ctx));

        router.delete("/test/task/:taskId")
            .blockingHandler(ctx -> deleteTask(ctx.pathParam("taskId"), ctx));

        router.post("/test/queue/ensure/:taskId")
            .handler(ctx -> ensureTaskQueue(ctx.pathParam("taskId"), ctx));

        router.post("/test/queue/enqueueTaskStatusUpdateEvent/:taskId")
            .handler(BodyHandler.create())
            .handler(ctx -> enqueueTaskStatusUpdateEvent(ctx.pathParam("taskId"), ctx.body().asString(), ctx));

        router.post("/test/queue/enqueueTaskArtifactUpdateEvent/:taskId")
            .handler(BodyHandler.create())
            .handler(ctx -> enqueueTaskArtifactUpdateEvent(ctx.pathParam("taskId"), ctx.body().asString(), ctx));

        router.get("/test/queue/childCount/:taskId")
            .produces(TEXT_PLAIN)
            .handler(ctx -> getChildQueueCount(ctx.pathParam("taskId"), ctx));

        router.get("/hello")
            .produces(TEXT_PLAIN)
            .handler(ctx -> hello(ctx));

        router.get("/export")
            .produces(APPLICATION_JSON)
            .handler(ctx -> exportSpans(ctx));

        router.get("/reset")
            .produces(TEXT_PLAIN)
            .handler(ctx -> reset(ctx));
    }

    public void saveTask(String body, RoutingContext rc) {
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

    public void getTask(String taskId, RoutingContext rc) {
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

    public void deleteTask(String taskId, RoutingContext rc) {
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

    public void ensureTaskQueue(String taskId, RoutingContext rc) {
        try {
            testUtilsBean.ensureQueue(taskId);
            rc.response()
                    .setStatusCode(200)
                    .end();
        } catch (Throwable t) {
            errorResponse(t, rc);
        }
    }

    public void enqueueTaskStatusUpdateEvent(String taskId, String body, RoutingContext rc) {
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

    public void enqueueTaskArtifactUpdateEvent(String taskId, String body, RoutingContext rc) {
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

    public void getChildQueueCount(String taskId, RoutingContext rc) {
        int count = testUtilsBean.getChildQueueCount(taskId);
        rc.response()
                .setStatusCode(200)
                .end(String.valueOf(count));
    }

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

    public void exportSpans(RoutingContext rc) {
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

    public void reset(RoutingContext rc) {
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
