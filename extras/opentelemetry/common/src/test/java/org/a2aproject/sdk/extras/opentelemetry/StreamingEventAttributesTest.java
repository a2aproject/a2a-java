package org.a2aproject.sdk.extras.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StreamingEventAttributesTest {

    @Test
    void recordDuration_noOpWhenHistogramIsNull() {
        StreamingEventAttributes.recordDuration(null, System.nanoTime(), "test-op", true);
        // must not throw
    }

    @Test
    void recordDuration_recordsPositiveValueForSubSecondOperation() {
        DoubleHistogram histogram = mock(DoubleHistogram.class);
        long startNanos = System.nanoTime() - 500_000L; // simulate 500 µs elapsed

        StreamingEventAttributes.recordDuration(histogram, startNanos, "test-op", true);

        ArgumentCaptor<Double> valueCaptor = ArgumentCaptor.forClass(Double.class);
        verify(histogram).record(valueCaptor.capture(), any(Attributes.class));
        double recorded = valueCaptor.getValue();
        assertTrue(recorded > 0.0, "recorded duration must be positive, got: " + recorded);
        assertTrue(recorded < 1.0, "recorded duration should be sub-second, got: " + recorded);
    }

    @Test
    void recordDuration_setsErrorTypeAttributeOnFailure() {
        DoubleHistogram histogram = mock(DoubleHistogram.class);

        StreamingEventAttributes.recordDuration(histogram, System.nanoTime() - 500_000L, "test-op", false);

        ArgumentCaptor<Attributes> attrsCaptor = ArgumentCaptor.forClass(Attributes.class);
        verify(histogram).record(anyDouble(), attrsCaptor.capture());
        assertNotNull(attrsCaptor.getValue().get(AttributeKey.stringKey(A2AObservabilityNames.ERROR_TYPE)));
    }

    @Test
    void recordDuration_omitsErrorTypeAttributeOnSuccess() {
        DoubleHistogram histogram = mock(DoubleHistogram.class);

        StreamingEventAttributes.recordDuration(histogram, System.nanoTime() - 500_000L, "test-op", true);

        ArgumentCaptor<Attributes> attrsCaptor = ArgumentCaptor.forClass(Attributes.class);
        verify(histogram).record(anyDouble(), attrsCaptor.capture());
        assertNull(attrsCaptor.getValue().get(AttributeKey.stringKey(A2AObservabilityNames.ERROR_TYPE)));
    }
}
