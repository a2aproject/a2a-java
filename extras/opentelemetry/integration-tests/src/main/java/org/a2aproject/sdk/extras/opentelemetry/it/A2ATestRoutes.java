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
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
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
import java.util.concurrent.atomic.AtomicInteger;
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

    private final AtomicInteger webhookCallCount = new AtomicInteger(0);

    @Inject
    TestUtilsBean testUtilsBean;
    @Inject
    InMemorySpanExporter inMemorySpanExporter;
    @Inject
    InMemoryMetricExporter inMemoryMetricExporter;
    @Inject
    OpenTelemetry openTelemetry;

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

        router.get("/export-metrics")
            .produces(APPLICATION_JSON)
            .handler(ctx -> exportMetrics(ctx));

        router.get("/reset")
            .produces(TEXT_PLAIN)
            .handler(ctx -> reset(ctx));

        router.get("/reset-metrics")
            .produces(TEXT_PLAIN)
            .handler(ctx -> resetMetrics(ctx));

        router.post("/test/webhook")
            .handler(BodyHandler.create())
            .handler(ctx -> receiveWebhook(ctx));

        router.get("/test/webhook/count")
            .produces(TEXT_PLAIN)
            .handler(ctx -> getWebhookCount(ctx));
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

            jsonObject.addProperty("events_count", spanData.getEvents().size());
            spanData.getEvents().forEach(event -> {
                JsonObject eventObject = new JsonObject();
                eventObject.addProperty("name", event.getName());
                event.getAttributes().forEach((k, v) -> eventObject.addProperty("attr_" + k.getKey(), v.toString()));
                jsonObject.add("event_" + event.getName(), eventObject);
            });

            jsonObject.addProperty("links_count", spanData.getLinks().size());
            if (!spanData.getLinks().isEmpty()) {
                jsonObject.addProperty("link_0_spanId", spanData.getLinks().get(0).getSpanContext().getSpanId());
                jsonObject.addProperty("link_0_traceId", spanData.getLinks().get(0).getSpanContext().getTraceId());
            }

            spans.add(jsonObject);
        }

        return spans;
    }

    public void exportMetrics(RoutingContext rc) {
        if (openTelemetry instanceof OpenTelemetrySdk sdk) {
            sdk.getSdkMeterProvider().forceFlush();
        }
        List<MetricData> metrics = inMemoryMetricExporter.getFinishedMetricItems().stream()
                .filter(m -> m.getName().startsWith("gen_ai."))
                .collect(Collectors.toList());
        rc.response()
                .setStatusCode(200)
                .putHeader(CONTENT_TYPE, APPLICATION_JSON)
                .end(gson.toJson(serializeMetrics(metrics)));
    }

    JsonElement serializeMetrics(List<MetricData> metricDataList) {
        JsonArray result = new JsonArray(metricDataList.size());
        for (MetricData m : metricDataList) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", m.getName());
            obj.addProperty("description", m.getDescription());
            obj.addProperty("unit", m.getUnit());
            obj.addProperty("type", m.getType().name());
            if (m.getType() == MetricDataType.HISTOGRAM) {
                HistogramData histogram = m.getHistogramData();
                obj.addProperty("data_count", histogram.getPoints().size());
                JsonArray points = new JsonArray(histogram.getPoints().size());
                histogram.getPoints().forEach(point -> {
                    JsonObject pointObj = new JsonObject();
                    pointObj.addProperty("count", point.getCount());
                    pointObj.addProperty("sum", point.getSum());
                    point.getAttributes().forEach((k, v) -> pointObj.addProperty("attr_" + k.getKey(), v.toString()));
                    points.add(pointObj);
                });
                obj.add("points", points);
            }
            result.add(obj);
        }
        return result;
    }

    public void resetMetrics(RoutingContext rc) {
        inMemoryMetricExporter.reset();
        rc.response().setStatusCode(200).end();
    }

    public void receiveWebhook(RoutingContext rc) {
        webhookCallCount.incrementAndGet();
        rc.response().setStatusCode(200).end();
    }

    public void getWebhookCount(RoutingContext rc) {
        rc.response()
                .setStatusCode(200)
                .putHeader(CONTENT_TYPE, TEXT_PLAIN)
                .end(String.valueOf(webhookCallCount.get()));
    }

    public void reset(RoutingContext rc) {
        inMemorySpanExporter.reset();
        webhookCallCount.set(0);
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

    @ApplicationScoped
    static class InMemoryMetricExporterProducer {

        @Produces
        @Singleton
        InMemoryMetricExporter inMemoryMetricExporter() {
            return InMemoryMetricExporter.create();
        }
    }
}
