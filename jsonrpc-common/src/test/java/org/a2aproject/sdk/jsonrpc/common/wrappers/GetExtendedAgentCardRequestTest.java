package org.a2aproject.sdk.jsonrpc.common.wrappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.a2aproject.sdk.spec.A2AMethods;
import org.a2aproject.sdk.spec.GetExtendedAgentCardParams;
import org.junit.jupiter.api.Test;

class GetExtendedAgentCardRequestTest {

    @Test
    void constructorWithNullParams() {
        GetExtendedAgentCardRequest request = new GetExtendedAgentCardRequest("2.0", "1", null);

        assertEquals("1", request.getId());
        assertEquals(A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD, request.getMethod());
        assertNull(request.getParams());
    }

    @Test
    void convenienceConstructor() {
        GetExtendedAgentCardRequest request = new GetExtendedAgentCardRequest("1");

        assertEquals("1", request.getId());
        assertEquals(A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD, request.getMethod());
        assertNull(request.getParams());
    }

    @Test
    void builderWithParams() {
        GetExtendedAgentCardParams params = new GetExtendedAgentCardParams("acme");
        GetExtendedAgentCardRequest request = GetExtendedAgentCardRequest.builder()
                .id("1")
                .params(params)
                .build();

        assertEquals("1", request.getId());
        assertNotNull(request.getParams());
        assertEquals("acme", request.getParams().tenant());
    }

    @Test
    void builderAutoGeneratesId() {
        GetExtendedAgentCardRequest request = GetExtendedAgentCardRequest.builder().build();

        assertNotNull(request.getId());
        assertEquals(A2AMethods.GET_EXTENDED_AGENT_CARD_METHOD, request.getMethod());
    }

}
