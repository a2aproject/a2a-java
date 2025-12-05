package io.a2a.grpc.mapper;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class A2AMappers {
    private static final String IMPLEMENTATION_SUFFIX = "Impl";

    static <T> T getMapper(Class<T> iface) {
        try {
            @SuppressWarnings( "unchecked" )
            Class<T> implementation =
                    (Class<T>) iface.getClassLoader().loadClass( iface.getName() + IMPLEMENTATION_SUFFIX );
            Constructor<T> constructor = implementation.getDeclaredConstructor();
            constructor.setAccessible( true );

            return constructor.newInstance();

        } catch (ClassNotFoundException |
                 NoSuchMethodException |
                 InstantiationException |
                 IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
