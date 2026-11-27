package org.a2aproject.sdk.compat03.client.transport.rest;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfig_v0_3;
import org.jspecify.annotations.Nullable;

/**
 * Configures a v0.3 REST transport. Each transport built without an explicit HTTP client
 * selects one from the default providers. Providers determine whether the transport owns
 * the selected client. An explicitly supplied client remains owned by the application.
 */
public class RestTransportConfig_v0_3 extends ClientTransportConfig_v0_3<RestTransport_v0_3> {

    private final @Nullable A2AHttpClient httpClient;

    public RestTransportConfig_v0_3() {
        this.httpClient = null;
    }

    public RestTransportConfig_v0_3(@Nullable A2AHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Returns the configured HTTP client.
     *
     * @return the injected client, or {@code null} if the transport creates the default client
     */
    public @Nullable A2AHttpClient getHttpClient() {
        return httpClient;
    }
}
