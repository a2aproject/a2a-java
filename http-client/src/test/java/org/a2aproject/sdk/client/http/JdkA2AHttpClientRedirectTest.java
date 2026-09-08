package org.a2aproject.sdk.client.http;

import java.net.http.HttpClient;

public class JdkA2AHttpClientRedirectTest extends AbstractA2AHttpClientRedirectTest {

    @Override
    protected A2AHttpClient createClient() {
        return new JdkA2AHttpClient();
    }

    @Override
    protected A2AHttpClient createRedirectCapableClient() {
        return new JdkA2AHttpClient(HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build());
    }
}
