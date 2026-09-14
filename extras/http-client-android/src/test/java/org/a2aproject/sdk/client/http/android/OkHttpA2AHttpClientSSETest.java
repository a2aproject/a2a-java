package org.a2aproject.sdk.client.http.android;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.AbstractA2AHttpClientSSETest;
import org.a2aproject.sdk.client.http.SSEParserConfig;

public class OkHttpA2AHttpClientSSETest extends AbstractA2AHttpClientSSETest {

    @Override
    protected A2AHttpClient createClient() {
        return new OkHttpA2AHttpClient();
    }

    @Override
    protected A2AHttpClient createClient(SSEParserConfig sseParserConfig) {
        return new OkHttpA2AHttpClient(sseParserConfig);
    }
}
