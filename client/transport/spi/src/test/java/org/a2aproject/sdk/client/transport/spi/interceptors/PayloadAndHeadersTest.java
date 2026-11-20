package org.a2aproject.sdk.client.transport.spi.interceptors;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PayloadAndHeadersTest {

    @Test
    void rejectsNullPayload() {
        assertThrows(IllegalArgumentException.class, () -> new PayloadAndHeaders(null, null));
    }
}
