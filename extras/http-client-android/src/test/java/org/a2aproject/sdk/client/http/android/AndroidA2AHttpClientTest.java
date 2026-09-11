package org.a2aproject.sdk.client.http.android;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.AbstractA2AHttpClientBasicTest;

public class AndroidA2AHttpClientTest extends AbstractA2AHttpClientBasicTest {

    @Override
    protected A2AHttpClient createClient() {
        return new AndroidA2AHttpClient();
    }
}
