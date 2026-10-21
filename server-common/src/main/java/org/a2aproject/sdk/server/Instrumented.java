package org.a2aproject.sdk.server;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * CDI qualifier for beans that have been wrapped with observability
 * instrumentation (e.g. distributed tracing, context propagation, metrics).
 *
 * <p>When an instrumentation module (such as the OpenTelemetry server extra) is
 * on the classpath, it may produce:
 * <ul>
 *   <li>An {@code @Instrumented} {@link org.a2aproject.sdk.client.http.A2AHttpClient}
 *       that wraps the application's own HTTP client (or the default factory-created one)
 *       with trace-context header injection.</li>
 *   <li>An {@code @Instrumented} {@link java.util.concurrent.Executor} that wraps a
 *       user-provided {@link jakarta.inject.Named @Named} executor (or a default
 *       thread pool) with OpenTelemetry context propagation.</li>
 * </ul>
 *
 * <p>{@link org.a2aproject.sdk.server.tasks.BasePushNotificationSender} prefers
 * {@code @Instrumented} beans when available, falling back to unqualified /
 * {@code @Named} beans, and finally to built-in defaults.
 *
 * <p>This qualifier lets instrumentation modules add behaviour transparently,
 * without replacing the application's custom configuration (mTLS, proxy,
 * authentication, custom thread pools, etc.).
 *
 * @see org.a2aproject.sdk.server.tasks.BasePushNotificationSender
 */
@Qualifier
@Retention(RUNTIME)
@Target({FIELD, TYPE, METHOD, PARAMETER})
public @interface Instrumented {

    /**
     * Supports programmatic CDI lookup of {@code @Instrumented} beans via
     * {@code CDI.current().select(A2AHttpClient.class, Instrumented.Literal.INSTANCE)}.
     */
    final class Literal extends AnnotationLiteral<Instrumented> implements Instrumented {
        public static final Literal INSTANCE = new Literal();
    }
}
