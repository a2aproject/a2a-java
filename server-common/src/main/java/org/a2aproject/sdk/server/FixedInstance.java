package org.a2aproject.sdk.server;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Iterator;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.UnsatisfiedResolutionException;
import jakarta.enterprise.util.TypeLiteral;

import org.jspecify.annotations.Nullable;

/**
 * A static {@link Instance} wrapper holding a pre-resolved (or absent) value,
 * for use in non-CDI contexts such as convenience constructors and tests.
 * A {@code null} value represents an unsatisfied instance.
 *
 * <p>Usage example (wrapping a concrete value):
 * <pre>{@code
 * Instance<AgentCard> instance = new FixedInstance<>(myAgentCard);
 * }</pre>
 *
 * <p>Usage example (empty / unsatisfied):
 * <pre>{@code
 * Instance<AgentCard> empty = FixedInstance.empty();
 * }</pre>
 *
 * @param <T> the bean type
 */
public class FixedInstance<T> implements Instance<T> {

    @SuppressWarnings("rawtypes")
    private static final FixedInstance EMPTY = new FixedInstance<>(null);

    private final @Nullable T value;

    public FixedInstance(@Nullable T value) {
        this.value = value;
    }

    /**
     * Returns a {@code FixedInstance} with no value (unsatisfied).
     *
     * @param <T> the bean type
     * @return an empty instance
     */
    @SuppressWarnings("unchecked")
    public static <T> FixedInstance<T> empty() {
        return (FixedInstance<T>) EMPTY;
    }

    @Override
    public T get() {
        if (value == null) {
            throw new UnsatisfiedResolutionException("No value available");
        }
        return value;
    }

    @Override
    public boolean isUnsatisfied() {
        return value == null;
    }

    @Override
    public boolean isAmbiguous() {
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return value != null ? Collections.singleton(value).iterator() : Collections.emptyIterator();
    }

    @Override
    public void destroy(T instance) {
    }

    @Override
    public Instance<T> select(Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... qualifiers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Handle<T> getHandle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterable<? extends Handle<T>> handles() {
        throw new UnsupportedOperationException();
    }
}
