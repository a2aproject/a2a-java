package org.a2aproject.sdk.grpc.utils;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import org.jspecify.annotations.Nullable;

/** Reads and writes the tenant of a current-protocol request. */
public final class RequestTenant {
    private RequestTenant() {
    }

    /**
     * Reads the nonblank tenant from a protocol request or falls back to the default.
     *
     * @param request protobuf request or builder
     * @param defaultTenant tenant to use when absent, if any
     * @return the selected tenant, or null for an unscoped request
     */
    public static @Nullable String resolve(Object request, @Nullable String defaultTenant) {
        String tenant = read(request);
        if (!tenant.isBlank()) {
            return tenant;
        }
        return defaultTenant == null || defaultTenant.isBlank() ? null : defaultTenant;
    }

    /**
     * Sets the routed tenant on a request builder.
     *
     * @param request builder with a singular string tenant field
     * @param tenant tenant to set, or null to clear it
     * @throws IllegalArgumentException if the builder has no singular string tenant field
     */
    public static void set(Message.Builder request, @Nullable String tenant) {
        FieldDescriptor field = field(request);
        if (field == null) {
            throw new IllegalArgumentException("Request does not support a tenant");
        }
        request.setField(field, tenant == null ? "" : tenant);
    }

    /** Rejects tenant changes outside the routing phase. */
    public static void requireUnchanged(Object request, @Nullable String tenant) {
        if (!(request instanceof MessageOrBuilder message)) {
            throw new IllegalArgumentException("Interceptor payload must be a protobuf request");
        }
        FieldDescriptor field = field(message);
        if (field == null) {
            throw new IllegalArgumentException("Interceptor payload must have a string tenant field");
        }
        String expected = tenant == null ? "" : tenant;
        if (!expected.equals(message.getField(field))) {
            throw new IllegalArgumentException("Only a ClientRoutingInterceptor can change the request tenant");
        }
    }

    private static String read(Object request) {
        if (request instanceof MessageOrBuilder message) {
            FieldDescriptor field = field(message);
            if (field != null) {
                return (String) message.getField(field);
            }
        }
        return "";
    }

    private static @Nullable FieldDescriptor field(MessageOrBuilder request) {
        FieldDescriptor field = request.getDescriptorForType().findFieldByName("tenant");
        return field != null && !field.isRepeated() && field.getJavaType() == FieldDescriptor.JavaType.STRING ? field : null;
    }
}
