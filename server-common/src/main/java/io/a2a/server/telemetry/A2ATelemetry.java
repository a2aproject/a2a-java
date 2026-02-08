package io.a2a.server.telemetry;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.function.Supplier;

/**
 * @author Sandeep Belgavi
 */
@ApplicationScoped
public class A2ATelemetry {

    @Inject
    Tracer tracer;

    public <T> T withSpan(String name, Supplier<T> operation) {
        Span span = tracer.spanBuilder(name).startSpan();
        try (Scope scope = span.makeCurrent()) {
            return operation.get();
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    public void withSpan(String name, Runnable operation) {
        Span span = tracer.spanBuilder(name).startSpan();
        try (Scope scope = span.makeCurrent()) {
            operation.run();
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
