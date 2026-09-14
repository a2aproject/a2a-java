package org.a2aproject.sdk.client.http;

public class JdkA2AHttpClientSSETest extends AbstractA2AHttpClientSSETest {

    @Override
    protected A2AHttpClient createClient() {
        return new JdkA2AHttpClient();
    }

    @Override
    protected A2AHttpClient createClient(SSEParserConfig sseParserConfig) {
        return JdkA2AHttpClient.withSseConfig(sseParserConfig);
    }
}
