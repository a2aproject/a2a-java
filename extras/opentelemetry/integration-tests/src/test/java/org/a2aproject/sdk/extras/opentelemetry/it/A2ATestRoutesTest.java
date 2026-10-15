package org.a2aproject.sdk.extras.opentelemetry.it;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.HistogramData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class A2ATestRoutesTest {

    @Mock
    private MetricData metricData;
    @Mock
    private HistogramData histogramData;
    @Mock
    private HistogramPointData point1;
    @Mock
    private HistogramPointData point2;

    private final A2ATestRoutes routes = new A2ATestRoutes();

    @Test
    void serializeMetrics_singleHistogramPoint_returnsPointsArray() {
        when(metricData.getName()).thenReturn("gen_ai.agent.a2a.streaming.duration");
        when(metricData.getDescription()).thenReturn("desc");
        when(metricData.getUnit()).thenReturn("s");
        when(metricData.getType()).thenReturn(MetricDataType.HISTOGRAM);
        when(metricData.getHistogramData()).thenReturn(histogramData);
        when(histogramData.getPoints()).thenReturn(List.of(point1));
        when(point1.getCount()).thenReturn(1L);
        when(point1.getSum()).thenReturn(0.5);
        when(point1.getAttributes()).thenReturn(Attributes.empty());

        JsonElement result = routes.serializeMetrics(List.of(metricData));

        JsonObject metric = result.getAsJsonArray().get(0).getAsJsonObject();
        assertTrue(metric.has("points"), "Should have 'points' JsonArray");
        assertFalse(metric.has("point"), "Should not have single-overwriting 'point' key");
        assertEquals(1, metric.getAsJsonArray("points").size());
    }

    @Test
    void serializeMetrics_multipleHistogramPoints_allPointsSerialized() {
        when(metricData.getName()).thenReturn("gen_ai.agent.a2a.streaming.duration");
        when(metricData.getDescription()).thenReturn("desc");
        when(metricData.getUnit()).thenReturn("s");
        when(metricData.getType()).thenReturn(MetricDataType.HISTOGRAM);
        when(metricData.getHistogramData()).thenReturn(histogramData);
        when(histogramData.getPoints()).thenReturn(List.of(point1, point2));
        when(point1.getCount()).thenReturn(1L);
        when(point1.getSum()).thenReturn(0.5);
        when(point1.getAttributes()).thenReturn(Attributes.empty());
        when(point2.getCount()).thenReturn(3L);
        when(point2.getSum()).thenReturn(2.0);
        when(point2.getAttributes()).thenReturn(Attributes.empty());

        JsonElement result = routes.serializeMetrics(List.of(metricData));

        JsonObject metric = result.getAsJsonArray().get(0).getAsJsonObject();
        JsonArray points = metric.getAsJsonArray("points");
        assertEquals(2, points.size(), "Both histogram data points must be serialized");
        assertEquals(1L, points.get(0).getAsJsonObject().get("count").getAsLong());
        assertEquals(0.5, points.get(0).getAsJsonObject().get("sum").getAsDouble());
        assertEquals(3L, points.get(1).getAsJsonObject().get("count").getAsLong());
        assertEquals(2.0, points.get(1).getAsJsonObject().get("sum").getAsDouble());
    }

    @Test
    void serializeMetrics_nonHistogramMetric_hasNoPointsKey() {
        when(metricData.getName()).thenReturn("gen_ai.client.operation.duration");
        when(metricData.getDescription()).thenReturn("desc");
        when(metricData.getUnit()).thenReturn("s");
        when(metricData.getType()).thenReturn(MetricDataType.LONG_SUM);

        JsonElement result = routes.serializeMetrics(List.of(metricData));

        JsonObject metric = result.getAsJsonArray().get(0).getAsJsonObject();
        assertFalse(metric.has("points"), "Non-histogram metric should not have a 'points' key");
        assertFalse(metric.has("point"), "Non-histogram metric should not have a 'point' key");
    }
}
