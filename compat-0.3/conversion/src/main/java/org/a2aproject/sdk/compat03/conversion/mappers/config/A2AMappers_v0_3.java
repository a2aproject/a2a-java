package org.a2aproject.sdk.compat03.conversion.mappers.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton factory for MapStruct mapper instances in the v0.3 to v1.0 conversion layer.
 * <p>
 * This factory provides centralized access to mapper implementations, ensuring that
 * each mapper interface has exactly one instance (singleton pattern). MapStruct
 * generates implementation classes with an "Impl" suffix at compile time, and this
 * factory uses reflection to instantiate and cache them.
 * <p>
 * Example usage:
 * <pre>{@code
 * public interface TaskStateMapper {
 *     TaskStateMapper INSTANCE = A03Mappers.getMapper(TaskStateMapper.class);
 *
 *     org.a2aproject.sdk.spec.TaskState toV10(
 *         org.a2aproject.sdk.compat03.spec.TaskState v03);
 * }
 * }</pre>
 *
 * Thread Safety: This factory uses {@link ConcurrentHashMap} to ensure thread-safe
 * lazy initialization of mapper instances.
 *
 * @see A03ToV10MapperConfig
 */
public final class A2AMappers_v0_3 {

    /**
     * Cache of instantiated mapper instances.
     * Key: Mapper interface class, Value: Singleton mapper instance
     */
    private static final Map<Class<?>, Object> MAPPERS = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation.
     */
    private A2AMappers_v0_3() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Returns the singleton instance of the specified mapper interface.
     * <p>
     * This method uses reflection to load the MapStruct-generated implementation class
     * (interface name + "Impl" suffix) and caches it for future use. If the implementation
     * class cannot be loaded or instantiated, a {@link RuntimeException} is thrown.
     *
     * @param mapperInterface the mapper interface class (e.g., {@code TaskStateMapper.class})
     * @param <T> the mapper type
     * @return the singleton instance of the mapper
     * @throws RuntimeException if the mapper implementation cannot be loaded or instantiated
     */
    @SuppressWarnings("unchecked")
    public static <T> T getMapper(Class<T> mapperInterface) {
        return (T) MAPPERS.computeIfAbsent(mapperInterface, clazz -> {
            try {
                // MapStruct generates implementation with "Impl" suffix
                String implName = clazz.getName() + "Impl";
                Class<?> implClass = Class.forName(implName);
                return implClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to load mapper: " + clazz.getName(), e);
            }
        });
    }
}
