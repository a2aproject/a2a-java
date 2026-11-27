package org.a2aproject.sdk.grpc.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.protobuf.Empty;
import org.a2aproject.sdk.grpc.GetTaskRequest;
import org.junit.jupiter.api.Test;

class RequestTenantTest {
    @Test
    void setsAndClearsRoutedTenant() {
        var request = GetTaskRequest.newBuilder();
        RequestTenant.set(request, "tenant-a");
        assertEquals("tenant-a", request.getTenant());
        RequestTenant.set(request, null);
        assertEquals("", request.getTenant());
    }

    @Test
    void rejectsBuildersWithoutATenantField() {
        assertThrows(IllegalArgumentException.class, () -> RequestTenant.set(Empty.newBuilder(), "tenant-a"));
    }

    @Test
    void invalidInterceptorPayloadFailsWithOrWithoutATenant() {
        var scoped = assertThrows(IllegalArgumentException.class,
                () -> RequestTenant.requireUnchanged("invalid payload", "tenant-a"));
        var unscoped = assertThrows(IllegalArgumentException.class,
                () -> RequestTenant.requireUnchanged("invalid payload", null));
        assertEquals("Interceptor payload must be a protobuf request", scoped.getMessage());
        assertEquals(scoped.getMessage(), unscoped.getMessage());
    }
}
