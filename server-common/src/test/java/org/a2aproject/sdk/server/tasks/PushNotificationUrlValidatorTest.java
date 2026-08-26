package org.a2aproject.sdk.server.tasks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.a2aproject.sdk.server.config.A2AConfigProvider;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class PushNotificationUrlValidatorTest {

    private DefaultPushNotificationUrlValidator createValidator() {
        return new DefaultPushNotificationUrlValidator();
    }

    private DefaultPushNotificationUrlValidator createValidatorWithConfig(String allowedSchemes,
                                                                          String allowPrivate) {
        return createValidatorWithConfig(allowedSchemes, allowPrivate, null);
    }

    private DefaultPushNotificationUrlValidator createValidatorWithConfig(String allowedSchemes,
                                                                          String allowPrivate,
                                                                          @Nullable String dnsCacheTtl) {
        DefaultPushNotificationUrlValidator validator = new DefaultPushNotificationUrlValidator();
        validator.configProvider = new A2AConfigProvider() {
            @Override
            public String getValue(String name) {
                return getOptionalValue(name).orElseThrow(() ->
                        new IllegalArgumentException("No value for " + name));
            }

            @Override
            public Optional<String> getOptionalValue(String name) {
                if (DefaultPushNotificationUrlValidator.ALLOWED_SCHEMES_KEY.equals(name)) {
                    return Optional.ofNullable(allowedSchemes);
                }
                if (DefaultPushNotificationUrlValidator.ALLOW_PRIVATE_KEY.equals(name)) {
                    return Optional.ofNullable(allowPrivate);
                }
                if (DefaultPushNotificationUrlValidator.DNS_CACHE_TTL_KEY.equals(name)) {
                    return Optional.ofNullable(dnsCacheTtl);
                }
                return Optional.empty();
            }
        };
        return validator;
    }

    @Test
    public void httpsPublicUrlIsAllowed() {
        DefaultPushNotificationUrlValidator validator = createValidator();
        assertDoesNotThrow(() -> validator.validate("https://example.com/webhook"));
    }

    @Test
    public void httpSchemeIsRejectedByDefault() {
        DefaultPushNotificationUrlValidator validator = createValidator();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate("http://example.com/webhook"));
        assertTrue(ex.getMessage().contains("scheme"));
    }

    @Test
    public void httpSchemeAllowedWhenConfigured() {
        DefaultPushNotificationUrlValidator validator = createValidatorWithConfig("http,https", "false");
        assertDoesNotThrow(() -> validator.validate("http://example.com/webhook"));
    }

    @ParameterizedTest(name = "blocked scheme: {0}")
    @ValueSource(strings = {"ftp://example.com/file", "file:///etc/passwd", "gopher://evil.com/"})
    public void nonHttpSchemesAreRejected(String url) {
        DefaultPushNotificationUrlValidator validator = createValidatorWithConfig("http,https", "false");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(url));
    }

    @Test
    public void malformedUrlIsRejected() {
        DefaultPushNotificationUrlValidator validator = createValidator();
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("not a valid url }{"));
    }

    @Test
    public void urlWithoutHostIsRejected() {
        DefaultPushNotificationUrlValidator validator = createValidator();
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("https:///path-only"));
    }

    static Stream<Arguments> ssrfVectors() {
        return Stream.of(
                Arguments.of("loopback IPv4", "https://127.0.0.1/secret"),
                Arguments.of("loopback IPv4 alt", "https://127.0.0.2/secret"),
                Arguments.of("localhost", "https://localhost/secret"),
                Arguments.of("link-local / metadata", "https://169.254.169.254/latest/meta-data/"),
                Arguments.of("private 10.x", "https://10.0.0.1/internal"),
                Arguments.of("private 172.16.x", "https://172.16.0.1/internal"),
                Arguments.of("private 192.168.x", "https://192.168.1.1/internal"),
                Arguments.of("IPv6 loopback", "https://[::1]/secret"),
                Arguments.of("any-local 0.0.0.0", "https://0.0.0.0/secret"));
    }

    @ParameterizedTest(name = "SSRF blocked: {0}")
    @MethodSource("ssrfVectors")
    public void privateNetworkAddressesAreBlocked(String description, String url) {
        DefaultPushNotificationUrlValidator validator = createValidatorWithConfig("https", "false");
        assertThrows(IllegalArgumentException.class, () -> validator.validate(url),
                description + " should be blocked");
    }

    @Test
    public void privateNetworkAllowedWhenConfigured() {
        DefaultPushNotificationUrlValidator validator = createValidatorWithConfig("https", "true");
        assertDoesNotThrow(() -> validator.validate("https://127.0.0.1/webhook"));
    }

    @Test
    public void isBlockedAddress_loopback() throws UnknownHostException {
        assertTrue(DefaultPushNotificationUrlValidator.isBlockedAddress(
                InetAddress.getByName("127.0.0.1")));
    }

    @Test
    public void isBlockedAddress_linkLocal() throws UnknownHostException {
        assertTrue(DefaultPushNotificationUrlValidator.isBlockedAddress(
                InetAddress.getByName("169.254.169.254")));
    }

    @Test
    public void isBlockedAddress_siteLocal() throws UnknownHostException {
        assertTrue(DefaultPushNotificationUrlValidator.isBlockedAddress(
                InetAddress.getByName("10.0.0.1")));
        assertTrue(DefaultPushNotificationUrlValidator.isBlockedAddress(
                InetAddress.getByName("172.16.0.1")));
        assertTrue(DefaultPushNotificationUrlValidator.isBlockedAddress(
                InetAddress.getByName("192.168.0.1")));
    }

    @Test
    public void isBlockedAddress_rfc6598SharedSpace() throws UnknownHostException {
        assertTrue(DefaultPushNotificationUrlValidator.isBlockedAddress(
                InetAddress.getByName("100.64.0.1")));
        assertTrue(DefaultPushNotificationUrlValidator.isBlockedAddress(
                InetAddress.getByName("100.127.255.254")));
    }

    @Test
    public void isBlockedAddress_ipv6UniqueLocal() throws UnknownHostException {
        assertTrue(DefaultPushNotificationUrlValidator.isBlockedAddress(
                InetAddress.getByAddress(new byte[]{
                        (byte) 0xFD, 0, 0, 0, 0, 0, 0, 0,
                        0, 0, 0, 0, 0, 0, 0, 1})));
    }

    @Test
    public void isBlockedAddress_ipv4MappedIpv6Loopback() throws UnknownHostException {
        // ::ffff:127.0.0.1
        InetAddress addr = InetAddress.getByAddress(new byte[]{
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, (byte) 0xFF, (byte) 0xFF,
                127, 0, 0, 1});
        assertTrue(DefaultPushNotificationUrlValidator.isBlockedAddress(addr));
    }

    @Test
    public void isBlockedAddress_multicast() throws UnknownHostException {
        assertTrue(DefaultPushNotificationUrlValidator.isBlockedAddress(
                InetAddress.getByName("224.0.0.1")));
    }

    @Test
    public void dnsCacheServesSubsequentCalls() {
        DefaultPushNotificationUrlValidator validator = createValidatorWithConfig("https", "false", "60");
        assertDoesNotThrow(() -> validator.validate("https://example.com/webhook"));
        assertDoesNotThrow(() -> validator.validate("https://example.com/webhook"));
    }

    @Test
    public void dnsCacheDisabledWhenTtlIsZero() {
        DefaultPushNotificationUrlValidator validator = createValidatorWithConfig("https", "false", "0");
        assertDoesNotThrow(() -> validator.validate("https://example.com/webhook"));
    }

    @Test
    public void dnsCacheUsesDefaultTtlWithoutConfig() {
        DefaultPushNotificationUrlValidator validator = createValidator();
        assertDoesNotThrow(() -> validator.validate("https://example.com/webhook"));
        assertDoesNotThrow(() -> validator.validate("https://example.com/webhook"));
    }

    @Test
    public void dnsCacheInvalidTtlFallsBackToDefault() {
        DefaultPushNotificationUrlValidator validator = createValidatorWithConfig("https", "false", "not-a-number");
        assertDoesNotThrow(() -> validator.validate("https://example.com/webhook"));
    }

    @Test
    public void dnsCacheNegativeTtlFallsBackToDefault() {
        DefaultPushNotificationUrlValidator validator = createValidatorWithConfig("https", "false", "-5");
        assertDoesNotThrow(() -> validator.validate("https://example.com/webhook"));
    }

    @Test
    public void senderRejectsPrivateUrlAndNeverConnects() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        CountDownLatch hit = new CountDownLatch(1);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
            hit.countDown();
        });
        server.start();
        int port = server.getAddress().getPort();

        try {
            InMemoryPushNotificationConfigStore configStore = new InMemoryPushNotificationConfigStore();
            DefaultPushNotificationUrlValidator validator = createValidatorWithConfig("http,https", "false");
            BasePushNotificationSender sender = new BasePushNotificationSender(configStore, validator);

            String taskId = "ssrf-test";
            configStore.setInfo(TaskPushNotificationConfig.builder()
                    .id("cfg-ssrf")
                    .taskId(taskId)
                    .url("http://127.0.0.1:" + port + "/latest/meta-data/iam/security-credentials/")
                    .build());

            Task task = Task.builder()
                    .id(taskId)
                    .contextId("ctx")
                    .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
                    .build();

            sender.sendNotification(task, null);

            assertFalse(hit.await(1, TimeUnit.SECONDS),
                    "SSRF: server-side request reached the internal listener — the URL was not blocked");
        } finally {
            server.stop(0);
        }
    }
}
