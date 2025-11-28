package io.a2a.server.interceptors;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class NoAttributeExtractor implements Supplier<Function<InvocationContext, Map<String, String>>> {

    @Override
    public Function<InvocationContext, Map<String, String>> get() {
        return ctx -> Collections.emptyMap();
    }
}
