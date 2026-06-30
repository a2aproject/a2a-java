package org.a2aproject.sdk.integrations.springboot.server.rest;

import java.time.format.DateTimeParseException;

import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception mapping for the Spring MVC transport adapter.
 *
 * <p>This handler translates protocol and parsing errors into the A2A JSON error envelope rather
 * than letting Spring Boot fall back to its default error response format.
 */
@RestControllerAdvice
public class A2ASpringBootMvcExceptionHandler {

    private final A2ASpringBootHttpResponseMapper responseMapper;

    public A2ASpringBootMvcExceptionHandler(A2ASpringBootHttpResponseMapper responseMapper) {
        this.responseMapper = responseMapper;
    }

    @ExceptionHandler(A2AError.class)
    public ResponseEntity<String> handleA2AError(A2AError error) {
        return responseMapper.error(error);
    }

    @ExceptionHandler({JsonProcessingException.class, IllegalArgumentException.class, DateTimeParseException.class})
    public ResponseEntity<String> handleInvalidParams(Exception exception) {
        return responseMapper.error(new InvalidParamsError(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleThrowable(Exception throwable) {
        return responseMapper.error(throwable);
    }
}
