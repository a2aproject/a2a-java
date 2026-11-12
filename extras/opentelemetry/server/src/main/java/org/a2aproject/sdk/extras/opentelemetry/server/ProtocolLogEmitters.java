package org.a2aproject.sdk.extras.opentelemetry.server;

import jakarta.enterprise.inject.Instance;
import java.util.function.Supplier;
import org.a2aproject.sdk.extras.opentelemetry.ProtocolLogEmitter;
import org.jspecify.annotations.Nullable;

/** Resolves optional protocol log emitters without allowing CDI failures to affect requests. */
final class ProtocolLogEmitters {
    private ProtocolLogEmitters() {}

    /**
     * Returns a memoized, thread-safe {@link Supplier} that resolves the optional protocol log
     * emitter from a CDI {@link Instance} at most once. CDI failures are silently ignored and
     * treated as "no emitter".
     *
     * <p>Callers should hold the returned supplier in a {@code volatile} field and initialise
     * it on first access via a double-checked locking pattern (see usages in this package).
     *
     * @param emitters the CDI instance for {@link ProtocolLogEmitter}; may be {@code null}
     * @return a thread-safe lazily-resolved supplier
     */
    static Supplier<@Nullable ProtocolLogEmitter> lazy(
            @Nullable Instance<ProtocolLogEmitter> emitters) {
        return new LazyEmitter(emitters);
    }

    private static final class LazyEmitter implements Supplier<@Nullable ProtocolLogEmitter> {
        private final @Nullable Instance<ProtocolLogEmitter> emitters;
        private volatile boolean resolved;
        // written only inside the synchronized block; read after the volatile resolved flag
        private @Nullable ProtocolLogEmitter value;

        LazyEmitter(@Nullable Instance<ProtocolLogEmitter> emitters) {
            this.emitters = emitters;
        }

        @Override
        public @Nullable ProtocolLogEmitter get() {
            if (!resolved) {
                synchronized (this) {
                    if (!resolved) {
                        try {
                            value = emitters != null && emitters.isResolvable()
                                    ? emitters.get()
                                    : null;
                        } catch (RuntimeException ignored) {
                            value = null;
                        }
                        resolved = true;
                    }
                }
            }
            return value;
        }
    }
}
