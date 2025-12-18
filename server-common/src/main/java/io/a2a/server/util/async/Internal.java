package io.a2a.server.util.async;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.inject.Qualifier;

/**
 * CDI qualifier for internal async executor beans.
 * <p>
 * This qualifier is used to distinguish internal async executors from
 * application-level executors when multiple executor beans exist.
 * </p>
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Internal {
}
