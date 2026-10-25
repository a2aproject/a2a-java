package org.a2aproject.sdk.grpc.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.a2aproject.sdk.grpc.ListTasksRequest;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.junit.jupiter.api.Test;

public class ListTasksParamsMapperTest {

    @Test
    void toProtoWithDefaultBuilder() {
        ListTasksRequest request = ListTasksParamsMapper.INSTANCE.toProto(ListTasksParams.builder().build());

        assertEquals("", request.getTenant());
    }

    @Test
    void toProtoWithDefaultConstructor() {
        ListTasksRequest request = ListTasksParamsMapper.INSTANCE.toProto(new ListTasksParams());

        assertEquals("", request.getTenant());
    }

    @Test
    void toProtoWithTenant() {
        ListTasksRequest request = ListTasksParamsMapper.INSTANCE.toProto(
                ListTasksParams.builder().tenant("tenant-123").build());

        assertEquals("tenant-123", request.getTenant());
    }

    @Test
    void fromProtoWithoutTenant() {
        ListTasksParams params = ListTasksParamsMapper.INSTANCE.fromProto(ListTasksRequest.newBuilder().build());

        assertNull(params.tenant());
    }

    @Test
    void fromProtoWithTenant() {
        ListTasksParams params = ListTasksParamsMapper.INSTANCE.fromProto(
                ListTasksRequest.newBuilder().setTenant("tenant-123").build());

        assertEquals("tenant-123", params.tenant());
    }

    @Test
    void nullTenantRoundTrip() {
        ListTasksRequest request = ListTasksParamsMapper.INSTANCE.toProto(ListTasksParams.builder().build());
        ListTasksParams params = ListTasksParamsMapper.INSTANCE.fromProto(request);

        assertNull(params.tenant());
    }
}
