package org.a2aproject.sdk.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Type inspection shared by the protocol JSON map adapters. */
public final class MapTypeUtils {
    private MapTypeUtils() {}

    /**
     * Whether map values are declared as Object, including unresolved raw-map type variables.
     * Typed maps must retain their declared value adapters when serialized.
     */
    public static boolean hasObjectValues(Type type) {
        Type valueType = mapValueType(type, Map.of());
        return valueType == Object.class || valueType instanceof TypeVariable<?>;
    }

    private static @Nullable Type mapValueType(Type type, Map<TypeVariable<?>, Type> inherited) {
        Class<?> raw;
        Map<TypeVariable<?>, Type> bindings = new HashMap<>(inherited);
        if (type instanceof ParameterizedType parameterized) {
            raw = (Class<?>) parameterized.getRawType();
            TypeVariable<?>[] variables = raw.getTypeParameters();
            Type[] arguments = parameterized.getActualTypeArguments();
            for (int i = 0; i < variables.length; i++) {
                bindings.put(variables[i], resolveVariable(arguments[i], inherited));
            }
        } else if (type instanceof Class<?> clazz) {
            raw = clazz;
        } else {
            return null;
        }
        if (!Map.class.isAssignableFrom(raw)) {
            return null;
        }
        if (raw == Map.class) {
            return resolveVariable(raw.getTypeParameters()[1], bindings);
        }
        for (Type parent : raw.getGenericInterfaces()) {
            Type valueType = mapValueType(parent, bindings);
            if (valueType != null) {
                return valueType;
            }
        }
        Type parent = raw.getGenericSuperclass();
        return parent == null ? null : mapValueType(parent, bindings);
    }

    private static Type resolveVariable(Type type, Map<TypeVariable<?>, Type> bindings) {
        while (type instanceof TypeVariable<?> variable) {
            Type resolved = bindings.get(variable);
            if (resolved == null || resolved.equals(type)) {
                break;
            }
            type = resolved;
        }
        return type;
    }
}
