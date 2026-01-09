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
import org.jspecify.annotations.Nullable;
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

    /**
     * Common CDI proxy suffixes used by various CDI implementations.
     * These are appended to class names when CDI creates proxies for intercepted beans.
     */
    private static final String[] CDI_PROXY_SUFFIXES = {
        "_Subclass",        // Quarkus/Weld subclass proxies
        "_ClientProxy",     // Weld client proxies
        "$$_WeldSubclass",  // Weld subclass alternative
        "_$$_javassist_",   // Javassist-based proxies
        "$Proxy$_$$_"       // Other CDI proxy patterns
    };

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

        // Get the actual class name, stripping CDI proxy suffixes for cleaner span names
        // CDI implementations like Weld/Quarkus add suffixes to proxied classes
        String rawClassName = jakartaContext.getTarget().getClass().getName();
        String className = stripCdiProxySuffix(rawClassName);
        // Use raw class name as fallback if stripping returns null (shouldn't happen in practice)
        String name = (className != null ? className : rawClassName) + '#' + jakartaContext.getMethod().getName();
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
                span.setAttribute("gen_ai.agent.a2a.response", ret.toString());
            }
            return ret;
        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            throw ex;
        } finally {
            span.end();
        }
    }

    /**
     * Strips known CDI proxy suffixes from class names to get the original class name.
     * <p>
     * CDI implementations (Weld, Quarkus, etc.) create proxy classes for beans with interceptors,
     * appending various suffixes to the original class name. This method removes these suffixes
     * to provide cleaner, more readable span names in OpenTelemetry traces.
     * <p>
     * For example:
     * <ul>
     *   <li>{@code com.example.MyService_Subclass} → {@code com.example.MyService}</li>
     *   <li>{@code com.example.MyService_ClientProxy} → {@code com.example.MyService}</li>
     *   <li>{@code com.example.MyService$$_WeldSubclass} → {@code com.example.MyService}</li>
     * </ul>
     *
     * @param className the potentially proxied class name
     * @return the class name with CDI proxy suffixes removed, or the original name if no suffix found, or null if input is null
     */
    private @Nullable String stripCdiProxySuffix(@Nullable String className) {
        if (className == null) {
            return null;
        }

        // Check each known CDI proxy suffix and remove it if found
        for (String suffix : CDI_PROXY_SUFFIXES) {
            if (className.endsWith(suffix)) {
                return className.substring(0, className.length() - suffix.length());
            }
            // Also handle cases where the suffix appears in the middle (e.g., _$$_javassist_N)
            int suffixIndex = className.indexOf(suffix);
            if (suffixIndex > 0) {
                return className.substring(0, suffixIndex);
            }
        }

        return className;
    }
}
