package org.a2aproject.sdk.extras.opentelemetry.it;

import static io.restassured.RestAssured.get;

import io.restassured.common.mapper.TypeRef;
import java.util.List;
import java.util.Map;

public class BaseTest {

    protected List<Map<String, Object>> getSpans() {
        return get("/export").body().as(new TypeRef<>() {});
    }

    protected List<Map<String, Object>> getMetrics() {
        return get("/export-metrics").body().as(new TypeRef<>() {});
    }

    protected void buildGlobalTelemetryInstance() {
        // Do nothing in JVM mode
    }
}
