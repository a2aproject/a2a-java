package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Verifies that HTTP transports reject unsafe or unsupported endpoints at construction. */
public abstract class AbstractHttpEndpointValidationTest {
    protected abstract void createTransport(String url);

    protected abstract void createThroughProvider(String url) throws Exception;

    protected abstract Class<? extends Exception> providerException();

    @ParameterizedTest
    @ValueSource(strings = {
            "https://user@example.com/agent",
            "https://example.com/agent?key=x",
            "https://example.com/agent#fragment",
            "https://example.com/a//b",
            "https://example.com/a/../b",
            "http://example.com:0/agent",
            "http://exa_mple.com/agent",
            "https://bücher.example/agent"
    })
    void rejectsInvalidEndpointsAtConstruction(String url) {
        assertThrows(IllegalArgumentException.class, () -> createTransport(url));
        Exception error = assertThrows(providerException(), () -> createThroughProvider(url));
        assertInstanceOf(IllegalArgumentException.class, error.getCause());
    }
}
