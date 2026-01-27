package io.a2a.server.interceptors;

import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Framework-agnostic annotation for method tracing.
 * Works with both Jakarta EE CDI interceptors and Spring AOP.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
public @interface Trace {
    @Nonbinding
    Kind kind() default Kind.SERVER;
    @Nonbinding
    Class<? extends AttributeExtractor> extractor() default NoAttributeExtractor.class;
}
