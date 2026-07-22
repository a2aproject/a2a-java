package org.a2aproject.sdk.spec.util;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for creating defensive copies of collection fields.
 * <p>
 * The helpers make the immutability and null-handling choices explicit at call sites:
 * <ul>
 *     <li>{@code immutable*} methods are intended for record components and return immutable collections.</li>
 *     <li>{@code nullable*} methods preserve the {@code null} value of optional components.</li>
 *     <li>{@code *AllowingNulls} methods are intended for JSON-like maps such as metadata,
 *     params, and headers, where {@code null} values may be valid protocol data.</li>
 *     <li>{@code mutable*CopyOrEmpty} methods are intended for builders that need mutable copies.</li>
 * </ul>
 */
public final class CollectionCopies {

    private CollectionCopies() {
    }

    /**
     * Creates an immutable defensive copy of a non-null list.
     * <p>
     * This method uses {@link List#copyOf(java.util.Collection)} and therefore preserves
     * its null-handling semantics: null elements are not allowed.
     *
     * @param list the source list, must not be {@code null}
     * @param <E> the element type
     * @return an immutable defensive copy
     */
    public static <E> List<E> immutableList(List<E> list) {
        return List.copyOf(list);
    }

    /**
     * Creates an immutable defensive copy of a nullable list.
     * <p>
     * If the source list is {@code null}, this method returns {@code null}. Otherwise it
     * uses {@link List#copyOf(java.util.Collection)} and therefore does not allow null elements.
     *
     * @param list the source list, or {@code null}
     * @param <E> the element type
     * @return {@code null} if the source is {@code null}, otherwise an immutable defensive copy
     */
    public static <E> @Nullable List<E> immutableNullableList(@Nullable List<E> list) {
        return list == null ? null : List.copyOf(list);
    }

    /**
     * Creates an immutable defensive copy of a nullable list, defaulting to an empty immutable list.
     * <p>
     * This method is intended for fields where a missing list should be normalized to an empty list
     * rather than preserving {@code null}. It uses {@link List#copyOf(java.util.Collection)} for
     * non-null input and therefore does not allow null elements.
     *
     * @param list the source list, or {@code null}
     * @param <E> the element type
     * @return an immutable empty list if the source is {@code null}, otherwise an immutable defensive copy
     */
    public static <E> List<E> immutableListOrEmpty(@Nullable List<E> list) {
        return list == null ? List.of() : List.copyOf(list);
    }

    /**
     * Creates an immutable defensive copy of a non-null map.
     * <p>
     * This method uses {@link Map#copyOf(Map)} and is intended for typed maps where
     * null keys and null values are not valid data.
     *
     * @param map the source map, must not be {@code null}
     * @param <K> the key type
     * @param <V> the value type
     * @return an immutable defensive copy
     */
    public static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
        return Map.copyOf(map);
    }

    /**
     * Creates an immutable defensive copy of a nullable map.
     * <p>
     * If the source map is {@code null}, this method returns {@code null}. Otherwise it
     * uses {@link Map#copyOf(Map)} and is intended for typed maps where null keys and
     * null values are not valid data.
     *
     * @param map the source map, or {@code null}
     * @param <K> the key type
     * @param <V> the value type
     * @return {@code null} if the source is {@code null}, otherwise an immutable defensive copy
     */
    public static <K, V> @Nullable Map<K, V> immutableNullableMap(@Nullable Map<K, V> map) {
        return map == null ? null : Map.copyOf(map);
    }

    /**
     * Creates an unmodifiable shallow defensive copy of a non-null map while preserving null values.
     * <p>
     * This method is intended for JSON-like maps such as metadata, params, and headers,
     * where null values may be valid protocol data. It intentionally does not use
     * {@link Map#copyOf(Map)}, because {@code Map.copyOf} throws {@link NullPointerException}
     * when the source map contains null keys or values.
     * <p>
     * The returned map cannot be structurally modified, but this method does not deep-copy
     * mutable keys or values.
     *
     * @param map the source map, must not be {@code null}
     * @param <K> the key type
     * @param <V> the value type
     * @return an unmodifiable shallow copy preserving null values
     */
    public static <K, V> Map<K, V> unmodifiableShallowMap(Map<K, V> map) {
        return Collections.unmodifiableMap(new HashMap<>(map));
    }

    /**
     * Creates an unmodifiable shallow defensive copy of a nullable map while preserving null values.
     * <p>
     * If the source map is {@code null}, this method returns {@code null}. Otherwise it behaves like
     * {@link #unmodifiableShallowMap(Map)}.
     *
     * @param map the source map, or {@code null}
     * @param <K> the key type
     * @param <V> the value type
     * @return {@code null} if the source is {@code null}, otherwise an unmodifiable shallow copy
     * preserving null values
     */
    public static <K, V> @Nullable Map<K, V> unmodifiableNullableShallowMap(@Nullable Map<K, V> map) {
        return map == null ? null : unmodifiableShallowMap(map);
    }

    /**
     * Creates a mutable defensive copy of a nullable list.
     * <p>
     * This method is intended for builders that need mutable collection state.
     *
     * @param list the source list, or {@code null}
     * @param <E> the element type
     * @return a mutable defensive copy, or an empty mutable list if the source is {@code null}
     */
    public static <E> List<E> mutableListCopyOrEmpty(@Nullable List<E> list) {
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    /**
     * Creates a mutable defensive copy of a nullable map.
     * <p>
     * This method is intended for builders that need mutable collection state.
     *
     * @param map the source map, or {@code null}
     * @param <K> the key type
     * @param <V> the value type
     * @return a mutable defensive copy, or an empty mutable map if the source is {@code null}
     */
    public static <K, V> Map<K, V> mutableMapCopyOrEmpty(@Nullable Map<K, V> map) {
        return map == null ? new HashMap<>() : new HashMap<>(map);
    }
}
