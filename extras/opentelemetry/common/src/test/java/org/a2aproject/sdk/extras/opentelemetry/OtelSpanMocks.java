package org.a2aproject.sdk.extras.opentelemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/** Shared Mockito helpers for OTel span builder stubs used in client and server tests. */
public final class OtelSpanMocks {

    private OtelSpanMocks() {
    }

    /**
     * Stubs {@code tracer.spanBuilder(spanName)} to return a builder that produces {@code closingSpan},
     * with status and scope stubs that cover both success and error paths.
     */
    public static SpanBuilder mockClosingSpanBuilder(Tracer tracer, String spanName, Span closingSpan) {
        SpanBuilder closingSpanBuilder = mock(SpanBuilder.class);
        when(tracer.spanBuilder(spanName)).thenReturn(closingSpanBuilder);
        when(closingSpanBuilder.setSpanKind(any(SpanKind.class))).thenReturn(closingSpanBuilder);
        when(closingSpanBuilder.addLink(any(SpanContext.class))).thenReturn(closingSpanBuilder);
        when(closingSpanBuilder.startSpan()).thenReturn(closingSpan);
        // lenient: success/error path coverage varies per test
        lenient().when(closingSpan.setStatus(any(StatusCode.class))).thenReturn(closingSpan);
        lenient().when(closingSpan.setStatus(any(StatusCode.class), anyString())).thenReturn(closingSpan);
        when(closingSpan.makeCurrent()).thenReturn(mock(Scope.class));
        return closingSpanBuilder;
    }

    /**
     * Stubs {@code tracer.spanBuilder(spanName)} to return a builder that produces {@code eventSpan},
     * with attribute and scope stubs for per-event spans.
     */
    public static SpanBuilder mockEventSpanBuilder(Tracer tracer, String spanName, Span eventSpan) {
        SpanBuilder eventSpanBuilder = mock(SpanBuilder.class);
        when(tracer.spanBuilder(spanName)).thenReturn(eventSpanBuilder);
        when(eventSpanBuilder.setSpanKind(any(SpanKind.class))).thenReturn(eventSpanBuilder);
        when(eventSpanBuilder.addLink(any(SpanContext.class))).thenReturn(eventSpanBuilder);
        when(eventSpanBuilder.setAttribute(anyString(), anyString())).thenReturn(eventSpanBuilder);
        when(eventSpanBuilder.startSpan()).thenReturn(eventSpan);
        when(eventSpan.makeCurrent()).thenReturn(mock(Scope.class));
        return eventSpanBuilder;
    }
}
