package org.a2aproject.sdk.compat03.server.rest.quarkus;

import org.a2aproject.sdk.compat03.client.ClientBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.rest.RestTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.auth.AuthInterceptor_v0_3;
import org.a2aproject.sdk.compat03.conversion.AbstractA2AServerWithAuthTest_v0_3;
import org.a2aproject.sdk.compat03.conversion.AndroidA2AHttpClient_v0_3;
import org.a2aproject.sdk.compat03.conversion.AuthTestProfile_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(AuthTestProfile_v0_3.class)
public class QuarkusA2ARest_v0_3_WithAuthAndroidTest extends AbstractA2AServerWithAuthTest_v0_3 {

    public QuarkusA2ARest_v0_3_WithAuthAndroidTest() {
        super(8081);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol_v0_3.HTTP_JSON.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:8081";
    }

    @Override
    protected void configureTransportWithAuth(ClientBuilder_v0_3 builder) {
        AuthInterceptor_v0_3 authInterceptor = new AuthInterceptor_v0_3(
                (schemeName, context) -> BASIC_AUTH_SCHEME_NAME.equals(schemeName) ? getEncodedCredentials() : null
        );

        builder.withTransport(RestTransport_v0_3.class,
                new RestTransportConfigBuilder_v0_3()
                        .httpClient(new AndroidA2AHttpClient_v0_3())
                        .addInterceptor(authInterceptor));
    }

    @Override
    protected void configureTransport(ClientBuilder_v0_3 builder) {
        builder.withTransport(RestTransport_v0_3.class,
                new RestTransportConfigBuilder_v0_3().httpClient(new AndroidA2AHttpClient_v0_3()));
    }

    @Test
    @Override
    public void testBasicAuthWorksViaHttp() throws Exception {
        saveTaskInTaskStore(MINIMAL_TASK);

        givenAuthenticated()
                .get("/v1/tasks/" + MINIMAL_TASK.getId())
                .then()
                .statusCode(200);

        deleteTaskInTaskStore(MINIMAL_TASK.getId());
    }
}
