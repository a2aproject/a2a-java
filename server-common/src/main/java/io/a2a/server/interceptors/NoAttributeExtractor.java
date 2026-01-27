package io.a2a.server.interceptors;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

public class NoAttributeExtractor implements AttributeExtractor {

    @Override
    public Function<InvocationContext, Map<String, String>> get() {
        return ctx -> Collections.emptyMap();
    }
}
