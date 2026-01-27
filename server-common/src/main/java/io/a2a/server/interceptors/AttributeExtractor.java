
package io.a2a.server.interceptors;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;


public interface AttributeExtractor extends Supplier<Function<InvocationContext, Map<String, String>>> {
    default boolean extractRequest() {
        return Boolean.getBoolean("io.a2a.server.extract.request");
    }

    default boolean extractResponse() {
        return Boolean.getBoolean("io.a2a.server.extract.response");
    }
}
