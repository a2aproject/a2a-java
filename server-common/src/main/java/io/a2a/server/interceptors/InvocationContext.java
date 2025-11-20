package io.a2a.server.interceptors;

import java.lang.reflect.Method;

public record InvocationContext(Object target, Method method, Object[] parameters) {

    public static InvocationContext create(Object target, Object[] parameters) {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

        Method callingMethod = walker.walk(frames
                -> frames.skip(1) // Skip the create method itself
                        .findFirst()
                        .map(frame -> {
                            try {
                                Class<?> declaringClass = frame.getDeclaringClass();
                                String methodName = frame.getMethodName();
                                // Find the method by name and parameter count
                                for (Method m : declaringClass.getDeclaredMethods()) {
                                    if (m.getName().equals(methodName) && m.getParameterCount() == parameters.length) {
                                        return m;
                                    }
                                }
                                return null;
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .orElse(null)
        );

        return new InvocationContext(target, callingMethod, parameters);
    }

    public Object proceed() throws Exception {
        if (method != null) {
            return method.invoke(target, parameters);
        }
        return null;
    }
}
