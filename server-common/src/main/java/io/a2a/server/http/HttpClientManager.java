package io.a2a.server.http;

import io.a2a.client.http.HttpClient;
import io.a2a.util.Assert;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@ApplicationScoped
public class HttpClientManager {

    private final Map<Endpoint, HttpClient> clients = new ConcurrentHashMap<>();

    public HttpClient getOrCreate(String url) {
        Assert.checkNotNullParam("url", url);

        try {
            return clients.computeIfAbsent(Endpoint.from(URI.create(url).toURL()), new Function<Endpoint, HttpClient>() {
                @Override
                public HttpClient apply(Endpoint edpt) {
                    return HttpClient.createHttpClient(url);
                }
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("URL is malformed: [" + url + "]");
        }
    }

    private static class Endpoint {
        private final String host;
        private final int port;

        public Endpoint(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public static Endpoint from(URL url) {
            return new Endpoint(url.getHost(), url.getPort() != -1 ? url.getPort() : url.getDefaultPort());
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Endpoint endpoint = (Endpoint) o;
            return port == endpoint.port && Objects.equals(host, endpoint.host);
        }

        @Override
        public int hashCode() {
            return Objects.hash(host, port);
        }
    }
}
