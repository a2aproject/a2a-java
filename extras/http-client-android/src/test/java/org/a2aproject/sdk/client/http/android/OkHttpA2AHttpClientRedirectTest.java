package org.a2aproject.sdk.client.http.android;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.AbstractA2AHttpClientRedirectTest;

public class OkHttpA2AHttpClientRedirectTest extends AbstractA2AHttpClientRedirectTest {

    @Override
    protected A2AHttpClient createClient() {
        return new OkHttpA2AHttpClient();
    }
}
