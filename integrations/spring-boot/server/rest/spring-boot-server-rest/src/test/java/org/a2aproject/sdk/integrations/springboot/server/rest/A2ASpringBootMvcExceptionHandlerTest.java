package org.a2aproject.sdk.integrations.springboot.server.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.format.DateTimeParseException;

import org.a2aproject.sdk.spec.ExtendedAgentCardNotConfiguredError;
import org.junit.jupiter.api.Test;

class A2ASpringBootMvcExceptionHandlerTest {

    private final A2ASpringBootMvcExceptionHandler handler =
            new A2ASpringBootMvcExceptionHandler(new A2ASpringBootHttpResponseMapper());

    @Test
    void mapsA2AErrorsThroughResponseMapper() {
        var response = handler.handleA2AError(new ExtendedAgentCardNotConfiguredError(null, "Extended Card not configured", null));

        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("Extended Card not configured"));
    }

    @Test
    void mapsInvalidParamsExceptionsToInvalidParamsError() {
        var response = handler.handleInvalidParams(new DateTimeParseException("bad timestamp", "abc", 0));

        assertEquals(422, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("bad timestamp"));
    }

    @Test
    void mapsThrowableToInternalError() {
        var response = handler.handleThrowable(new RuntimeException("boom"));

        assertEquals(500, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("boom"));
    }
}
