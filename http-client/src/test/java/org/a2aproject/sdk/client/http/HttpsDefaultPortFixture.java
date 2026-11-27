package org.a2aproject.sdk.client.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.mockserver.configuration.Configuration;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.socket.tls.KeyAndCertificateFactoryFactory;

/** Runs HTTPS requests at the default logical port through an unprivileged loopback proxy. */
public final class HttpsDefaultPortFixture {
    private static final String HOST = "example.test";
    private static final int TIMEOUT_MS = 5_000;

    private HttpsDefaultPortFixture() {
    }

    /** TLS and proxy settings for an individual test client. */
    public record Connection(InetSocketAddress proxy, SSLContext tls, TrustManagerFactory trustManagers) {
        public X509TrustManager trustManager() {
            return (X509TrustManager) trustManagers.getTrustManagers()[0];
        }
    }

    /**
     * Compares the Host headers received inside the TLS tunnel before and after normalization.
     *
     * @param factory creates a client using the fixture's proxy and TLS trust
     */
    public static void assertHostPreserved(Function<Connection, A2AHttpClient> factory) throws Exception {
        Configuration configuration = Configuration.configuration().sslCertificateDomainName(HOST)
                .sslSubjectAlternativeNameDomains(HOST);
        var certificates = KeyAndCertificateFactoryFactory.createKeyAndCertificateFactory(configuration,
                new MockServerLogger(HttpsDefaultPortFixture.class));
        certificates.buildAndSavePrivateKeyAndX509Certificate();
        var credentials = KeyStore.getInstance("PKCS12");
        credentials.load(null);
        char[] password = "test-only".toCharArray();
        credentials.setKeyEntry("server", certificates.privateKey(), password,
                certificates.certificateChain().toArray(Certificate[]::new));
        var keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keys.init(credentials, password);
        var serverTls = SSLContext.getInstance("TLS");
        serverTls.init(keys.getKeyManagers(), null, null);

        var trustedRoots = KeyStore.getInstance("PKCS12");
        trustedRoots.load(null);
        trustedRoots.setCertificateEntry("test-ca", certificates.certificateAuthorityX509Certificate());
        var trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trust.init(trustedRoots);
        var clientTls = SSLContext.getInstance("TLS");
        clientTls.init(null, trust.getTrustManagers(), null);

        var executor = Executors.newSingleThreadExecutor();
        try (var listener = new ServerSocket()) {
            listener.bind(new InetSocketAddress("127.0.0.1", 0));
            listener.setSoTimeout(TIMEOUT_MS);
            var hosts = executor.submit(() -> List.of(serveRequest(listener, serverTls), serveRequest(listener, serverTls)));
            var client = factory.apply(new Connection((InetSocketAddress) listener.getLocalSocketAddress(), clientTls, trust));
            try {
                String endpoint = "https://" + HOST + "/agent";
                String normalized = HttpEndpoint.normalize(endpoint);
                assertEquals("https://" + HOST + ":443/agent", normalized);
                assertEquals(200, client.createGet().url(endpoint).get().status());
                assertEquals(200, client.createGet().url(normalized).get().status());
                assertEquals(List.of(HOST, HOST), hosts.get(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            } finally {
                if (client instanceof AutoCloseable closeable) {
                    closeable.close();
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static String serveRequest(ServerSocket listener, SSLContext serverTls) throws IOException {
        try (Socket connection = listener.accept()) {
            connection.setSoTimeout(TIMEOUT_MS);
            BufferedReader proxy = reader(connection);
            String connect = proxy.readLine();
            assertEquals("CONNECT " + HOST + ":443", connect.substring(0, connect.lastIndexOf(' ')));
            // CONNECT clients wait for a response before starting TLS.
            drainHeaders(proxy);
            write(connection, "HTTP/1.1 200 Connection Established\r\n\r\n");
            try (var tls = (SSLSocket) serverTls.getSocketFactory().createSocket(connection, HOST, 443, false)) {
                tls.setUseClientMode(false);
                BufferedReader request = reader(tls);
                assertEquals("GET /agent HTTP/1.1", request.readLine());
                String host = drainHeaders(request);
                write(tls, "HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n");
                return host;
            }
        }
    }

    private static BufferedReader reader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
    }

    private static String drainHeaders(BufferedReader request) throws IOException {
        String host = "";
        for (String line; (line = request.readLine()) != null && !line.isEmpty();) {
            if (line.regionMatches(true, 0, "Host:", 0, 5)) {
                host = line.substring(5).trim();
            }
        }
        return host;
    }

    private static void write(Socket socket, String response) throws IOException {
        socket.getOutputStream().write(response.getBytes(StandardCharsets.US_ASCII));
        socket.getOutputStream().flush();
    }
}
