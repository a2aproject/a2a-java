package org.a2aproject.sdk.server.util;

import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;

/**
 * CDI utility methods shared across server modules.
 */
public final class CdiUtils {

    private static final Logger LOGGER = Logger.getLogger(CdiUtils.class.getName());

    private CdiUtils() {
    }

    /**
     * Resolves a bean from an optional CDI {@link Instance}.
     *
     * @param instance the CDI instance, may be {@code null}
     * @param <T>      the bean type
     * @return the resolved bean, or {@code null} if the instance is absent or not uniquely resolvable
     */
    public static <T> @Nullable T getIfResolvable(@Nullable Instance<T> instance) {
        if (instance != null && instance.isResolvable()) {
            return instance.get();
        }
        return null;
    }

    /**
     * Finds the single default bean of a given type, excluding beans that carry a specific
     * qualifier and optionally requiring another. Warns on duplicate defaults and validates
     * that the bean scope is {@code @ApplicationScoped} or {@code @Singleton}.
     *
     * @param allBeans          the CDI instance containing all beans of type {@code T}
     * @param excludeQualifier  qualifier whose presence causes a bean to be skipped
     * @param requiredQualifier qualifier that the default bean must carry, or {@code null} to accept any
     *                          non-excluded bean
     * @param beanTypeName      human-readable label used in warning messages (e.g. {@code "@ExtendedAgentCard"})
     * @param <T>               the bean type
     * @return the resolved default bean, or {@code null} if none was found
     */
    public static <T> @Nullable T resolveDefaultBean(
            Instance<T> allBeans,
            Class<? extends Annotation> excludeQualifier,
            @Nullable Class<? extends Annotation> requiredQualifier,
            String beanTypeName) {
        T defaultBean = null;
        for (Instance.Handle<T> handle : allBeans.handles()) {
            Set<Annotation> qualifiers = handle.getBean().getQualifiers();
            if (qualifiers.stream().anyMatch(excludeQualifier::isInstance)) {
                continue;
            }
            if (requiredQualifier != null && qualifiers.stream().noneMatch(requiredQualifier::isInstance)) {
                continue;
            }
            if (defaultBean != null) {
                throw new IllegalStateException(String.format(
                        "Multiple default %s beans detected — found %s but already resolved one; "
                                + "ensure only one %s bean without the excluded qualifier exists",
                        beanTypeName, handle.getBean().getBeanClass().getName(), beanTypeName));
            } else {
                Class<? extends Annotation> scope = handle.getBean().getScope();
                if (!ApplicationScoped.class.equals(scope) && !Singleton.class.equals(scope)) {
                    LOGGER.log(Level.WARNING,
                            "Default {0} bean {1} has scope {2} — only @ApplicationScoped or @Singleton is safe here",
                            new Object[] { beanTypeName, handle.getBean().getBeanClass().getName(),
                                    scope.getSimpleName() });
                }
                defaultBean = handle.get();
            }
        }
        return defaultBean;
    }
}
