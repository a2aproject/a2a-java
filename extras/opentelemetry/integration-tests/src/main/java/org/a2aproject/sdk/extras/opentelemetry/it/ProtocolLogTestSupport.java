package org.a2aproject.sdk.extras.opentelemetry.it;

import com.google.gson.Gson;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.vertx.ext.web.Router;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.a2aproject.sdk.extras.opentelemetry.ProtocolLogEmitter;
import org.a2aproject.sdk.extras.opentelemetry.ProtocolPayloadFormatter;

/** Test-only log pipeline and endpoint for verifying real server instrumentation. */
@Singleton
public class ProtocolLogTestSupport {
    private final InMemoryLogRecordExporter exporter = InMemoryLogRecordExporter.create();

    @Produces
    @Singleton
    InMemoryLogRecordExporter logRecordExporter() {
        return exporter;
    }

    @Produces
    @Singleton
    ProtocolLogEmitter protocolLogEmitter(OpenTelemetry openTelemetry) {
        return new ProtocolLogEmitter(openTelemetry.getLogsBridge().get("a2a-test"),
                new ProtocolPayloadFormatter(4096, value -> value));
    }

    void setup(@Observes Router router, OpenTelemetry openTelemetry) {
        router.get("/test/protocol-logs").handler(ctx -> {
            List<Map<String, Object>> records = exporter.getFinishedLogRecordItems().stream()
                    .filter(record -> record.getEventName() != null && record.getEventName().startsWith("a2a."))
                    .map(record -> {
                        Map<String, Object> fields = new LinkedHashMap<>();
                        fields.put("event", record.getEventName());
                        fields.put("traceId", record.getSpanContext().getTraceId());
                        fields.put("spanId", record.getSpanContext().getSpanId());
                        fields.put("sampled", record.getSpanContext().isSampled());
                        fields.put("severity", record.getSeverityText());
                        fields.put("body", record.getBody().asString());
                        record.getAttributes().forEach((key, value) -> fields.put("attr_" + key.getKey(), value));
                        return fields;
                    }).toList();
            ctx.response().putHeader("Content-Type", "application/json").end(new Gson().toJson(records));
        });
        router.delete("/test/protocol-logs").blockingHandler(ctx -> {
            if (openTelemetry instanceof OpenTelemetrySdk sdk) {
                sdk.getSdkLoggerProvider().forceFlush().join(5, TimeUnit.SECONDS);
            }
            exporter.reset();
            ctx.response().setStatusCode(204).end();
        });
    }
}
