package org.a2aproject.sdk.tests.multiversion.jsonrpc;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.a2aproject.sdk.compat03.client.ClientBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransport_v0_3;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransportConfigBuilder_v0_3;
import org.a2aproject.sdk.compat03.client.transport.spi.interceptors.auth.AuthInterceptor_v0_3;
import org.a2aproject.sdk.compat03.conversion.AbstractA2AServerWithAuthTest_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;

@QuarkusTest
@TestProfile(AuthTestProfile.class)
public class MultiVersionJSONRPC_v0_3_WithAuthTest extends AbstractA2AServerWithAuthTest_v0_3 {

    public MultiVersionJSONRPC_v0_3_WithAuthTest() {
        super(8081);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol_v0_3.JSONRPC.asString();
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

        builder.withTransport(JSONRPCTransport_v0_3.class,
                new JSONRPCTransportConfigBuilder_v0_3()
                        .addInterceptor(authInterceptor));
    }

    @Override
    protected void configureTransport(ClientBuilder_v0_3 builder) {
        builder.withTransport(JSONRPCTransport_v0_3.class, new JSONRPCTransportConfigBuilder_v0_3());
    }

}
