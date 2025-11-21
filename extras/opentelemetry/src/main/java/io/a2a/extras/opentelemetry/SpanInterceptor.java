package io.a2a.extras.opentelemetry;

import io.a2a.server.interceptors.Kind;
import io.a2a.server.interceptors.NoAttributeExtractor;
import io.a2a.server.interceptors.Trace;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Jakarta EE CDI interceptor for @Trace annotation.
 * Integrates with OpenTelemetry to create spans for traced methods.
 */
@Trace()
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class SpanInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpanInterceptor.class);

    @Inject
    private Tracer tracer;

    @AroundInvoke
    public Object trace(jakarta.interceptor.InvocationContext jakartaContext) throws Exception {
        // Convert Jakarta InvocationContext to our custom InvocationContext
        io.a2a.server.interceptors.InvocationContext customContext
                = new io.a2a.server.interceptors.InvocationContext(
                        jakartaContext.getTarget(),
                        jakartaContext.getMethod(),
                        jakartaContext.getParameters()
                );

        Kind kind = jakartaContext
                .getMethod()
                .getAnnotation(Trace.class)
                .kind();
        Class<? extends Supplier<Function<io.a2a.server.interceptors.InvocationContext, Map<String, String>>>> extractorClass
                = jakartaContext.getMethod()
                        .getAnnotation(Trace.class)
                        .extractor();

        String name = jakartaContext.getTarget().getClass().getName();
        if (name != null && name.endsWith("_Subclass")) {
            name = name.substring(0, name.length() - "_Subclass".length());
        }
        name = name + '#' + jakartaContext.getMethod().getName();
        SpanBuilder spanBuilder = tracer.spanBuilder(name)
                .setSpanKind(SpanKind.valueOf(kind.toString()));

        if (extractorClass != null && !extractorClass.equals(NoAttributeExtractor.class)) {
            try {
                Supplier<Function<io.a2a.server.interceptors.InvocationContext, Map<String, String>>> supplier
                        = extractorClass.getDeclaredConstructor().newInstance();
                Map<String, String> attributes = supplier.get().apply(customContext);
                for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                    spanBuilder.setAttribute(attribute.getKey(), attribute.getValue());
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to instantiate attribute extractor {}: {}",
                        extractorClass.getName(), e.getMessage(), e);
            }
        }

        Span span = spanBuilder.startSpan();
        try (Scope scope = span.makeCurrent()) {
            Object ret = jakartaContext.proceed();
            span.setStatus(StatusCode.OK);
            if (ret != null) {
                span.setAttribute("response", ret.toString());
            }
            return ret;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }
}
