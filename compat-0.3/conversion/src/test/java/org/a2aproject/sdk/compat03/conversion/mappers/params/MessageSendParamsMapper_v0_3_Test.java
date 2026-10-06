package org.a2aproject.sdk.compat03.conversion.mappers.params;

import java.util.List;

import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageSendParamsMapper_v0_3_Test {

    @Test
    void roundTripPreservesMessageAndAddsOnlyDefaultTenant() {
        Message_v0_3 message = new Message_v0_3(
            Message_v0_3.Role.USER, List.of(new TextPart_v0_3("hello")), "message", "context",
            null, null, null, null);
        MessageSendParams_v0_3 legacy = new MessageSendParams_v0_3(message, null, null);

        MessageSendParams current = MessageSendParamsMapper_v0_3.INSTANCE.toV10(legacy);

        assertEquals("", current.tenant());
        assertEquals(legacy, MessageSendParamsMapper_v0_3.INSTANCE.fromV10(current));
    }
}
