package org.a2aproject.sdk.server;

import jakarta.enterprise.inject.Instance;

/**
 * Factory methods for {@link Instance} test doubles built on {@link FixedInstance}.
 */
public final class TestInstances {

    private TestInstances() {
    }

    /**
     * Returns an {@link Instance} that throws {@link AssertionError} on {@link Instance#get()},
     * useful for verifying that a constructor does not eagerly resolve the instance.
     *
     * @param <T> the bean type
     * @return an instance that throws on resolution
     */
    public static <T> Instance<T> throwOnGet() {
        return new FixedInstance<>(null) {
            @Override
            public T get() {
                throw new AssertionError("Instance.get() must not be called during construction");
            }

            @Override
            public boolean isUnsatisfied() {
                return false;
            }
        };
    }
}
