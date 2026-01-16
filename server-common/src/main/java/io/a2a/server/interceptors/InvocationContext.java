package io.a2a.server.interceptors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Represents the context of a method invocation for interceptors.
 * <p>
 * This record captures the target object, method being invoked, and parameters
 * passed to the method. It's used by interceptors to extract attributes and
 * execute the actual method invocation.
 * <p>
 * The parameters array is defensively copied to prevent external modification,
 * ensuring immutability of the InvocationContext after construction.
 *
 * @param target the object on which the method is being invoked (never null)
 * @param method the method being invoked (may be null if method resolution fails)
 * @param parameters the parameters passed to the method (may be null or empty)
 */
public record InvocationContext(@NonNull Object target, @NonNull Method method, Object @Nullable[] parameters) {

    /**
     * Compact constructor with validation and defensive copying.
     */
    public InvocationContext {
        if (target == null) {
            throw new IllegalArgumentException("target cannot be null");
        }
        if (method == null) {
            throw new IllegalArgumentException("method cannot be null");
        }
    }

    /**
     * Invokes the method on the target with the provided parameters.
     *
     * @return the result of the method invocation
     * @throws IllegalStateException if method is null
     * @throws IllegalAccessException if the method is not accessible
     * @throws IllegalArgumentException if parameters don't match method signature
     * @throws InvocationTargetException if the invoked method throws an exception
     */
    public @Nullable Object proceed() throws IllegalAccessException,
                                              IllegalArgumentException,
                                              InvocationTargetException {
        return method.invoke(target, parameters);
    }
}
