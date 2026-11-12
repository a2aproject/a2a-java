package org.a2aproject.sdk.extras.opentelemetry.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.function.Supplier;
import org.a2aproject.sdk.extras.opentelemetry.ProtocolLogEmitter;
import org.a2aproject.sdk.server.tasks.PushNotificationDeliveryObserver;
import org.jspecify.annotations.Nullable;

/** Bridges per-destination delivery outcomes to the optional protocol log emitter. */
@ApplicationScoped
public class OpenTelemetryPushDeliveryObserver implements PushNotificationDeliveryObserver {

    /** CDI instance used to lazily resolve the optional protocol log emitter. */
    @Inject
    private Instance<ProtocolLogEmitter> emitters;

    /**
     * Memoized supplier for the optional protocol log emitter.
     * {@code null} until first call to {@link #onDelivery}; initialised via outer DCL.
     */
    @Nullable
    private volatile Supplier<@Nullable ProtocolLogEmitter> emitterSupplier;

    @Override
    public void onDelivery(@Nullable String taskId, @Nullable String configId, boolean success) {
        Supplier<@Nullable ProtocolLogEmitter> s = emitterSupplier;
        if (s == null) {
            synchronized (this) {
                if ((s = emitterSupplier) == null) {
                    emitterSupplier = s = ProtocolLogEmitters.lazy(emitters);
                }
            }
        }
        ProtocolLogEmitter emitter = s.get();
        if (emitter != null) {
            emitter.emitNotificationDelivery(taskId, configId, success);
        }
    }
}
