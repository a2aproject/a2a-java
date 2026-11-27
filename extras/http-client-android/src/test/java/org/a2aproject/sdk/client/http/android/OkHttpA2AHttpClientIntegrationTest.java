package org.a2aproject.sdk.client.http.android;

import java.net.InetSocketAddress;
import java.net.Proxy;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.AbstractA2AHttpClientIntegrationTest;
import org.a2aproject.sdk.client.http.HttpsDefaultPortFixture;
import org.a2aproject.sdk.client.http.SSEParserConfig;
import org.junit.jupiter.api.Test;

public class OkHttpA2AHttpClientIntegrationTest extends AbstractA2AHttpClientIntegrationTest {

    @Override
    protected A2AHttpClient createClient() {
        return new OkHttpA2AHttpClient();
    }

    @Override
    protected A2AHttpClient createClientWithProxy(InetSocketAddress proxy) {
        return new OkHttpA2AHttpClient(SSEParserConfig.DEFAULT, new Proxy(Proxy.Type.HTTP, proxy), null);
    }

    @Test
    void normalizedHttpsDefaultPortPreservesHostHeader() throws Exception {
        HttpsDefaultPortFixture.assertHostPreserved(connection -> new OkHttpA2AHttpClient(SSEParserConfig.DEFAULT,
                new Proxy(Proxy.Type.HTTP, connection.proxy()), connection));
    }
}
