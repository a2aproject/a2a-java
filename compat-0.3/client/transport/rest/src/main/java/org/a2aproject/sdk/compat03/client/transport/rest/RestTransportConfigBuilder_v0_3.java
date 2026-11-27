package org.a2aproject.sdk.compat03.client.transport.rest;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.compat03.client.transport.spi.ClientTransportConfigBuilder_v0_3;
import org.jspecify.annotations.Nullable;

public class RestTransportConfigBuilder_v0_3 extends ClientTransportConfigBuilder_v0_3<RestTransportConfig_v0_3, RestTransportConfigBuilder_v0_3> {

    private @Nullable A2AHttpClient httpClient;

    public RestTransportConfigBuilder_v0_3 httpClient(A2AHttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    @Override
    public RestTransportConfig_v0_3 build() {
        RestTransportConfig_v0_3 config = new RestTransportConfig_v0_3(httpClient);
        config.setInterceptors(this.interceptors);
        return config;
    }
}
