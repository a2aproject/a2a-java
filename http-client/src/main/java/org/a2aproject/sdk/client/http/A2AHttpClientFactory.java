package org.a2aproject.sdk.client.http;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.a2aproject.sdk.util.Assert;

/**
 * Factory for creating {@link A2AHttpClient} instances using the ServiceLoader mechanism.
 *
 * <p>
 * This factory discovers available {@link A2AHttpClientProvider} implementations at runtime,
 * tries them in descending priority order, and returns the first one that succeeds.
 * If no provider can be instantiated, it throws an {@link IllegalStateException}.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Get the default client (highest available priority provider)
 * A2AHttpClient client = A2AHttpClientFactory.create(A2AHttpClientConfiguration.DEFAULT);
 *
 * // Use with try-with-resources if the client implements AutoCloseable
 * try (A2AHttpClient client = A2AHttpClientFactory.create(A2AHttpClientConfiguration.DEFAULT)) {
 *     A2AHttpResponse response = client.createGet()
 *         .url("https://example.com")
 *         .get();
 * }
 * }</pre>
 *
 * <h2>Priority System</h2>
 * <p>
 * Providers are tried in descending priority order. If a provider's {@code create()} throws,
 * the factory logs the failure at {@code WARNING} level and falls back to the next provider:
 * <ul>
 * <li>CdiA2AHttpClient: priority 200 (CDI-provided bean, when a container and bean are active)</li>
 * <li>AndroidA2AHttpClient: priority 110 (Android runtime only)</li>
 * <li>VertxA2AHttpClient: priority 100 (when {@code vertx-web-client} is on the classpath)</li>
 * <li>JdkA2AHttpClient: priority 0 (always available, last resort)</li>
 * </ul>
 *
 * <h2>Custom Providers</h2>
 * <p>
 * To add a custom provider, implement {@link A2AHttpClientProvider} and register it
 * in {@code META-INF/services/org.a2aproject.sdk.client.http.A2AHttpClientProvider}.
 */
public final class A2AHttpClientFactory {

    private static final Logger LOGGER = Logger.getLogger(A2AHttpClientFactory.class.getName());
    private static final List<A2AHttpClientProvider> PROVIDERS;

    static {
        PROVIDERS =
                StreamSupport.stream(
                                ServiceLoader.load(
                                                A2AHttpClientProvider.class,
                                                A2AHttpClientProvider.class.getClassLoader())
                                        .spliterator(),
                                false)
                        .sorted(Comparator.comparingInt(A2AHttpClientProvider::priority).reversed())
                        .toList();
    }

    private A2AHttpClientFactory() {
        // Utility class
    }

    /**
     * Creates a new A2AHttpClient instance using the highest available priority provider.
     *
     * <p>
     * Providers are tried in descending priority order. If a provider's {@code create()}
     * throws, it is skipped and the next provider is tried. Failures are logged at
     * {@code WARNING} level.
     *
     * @return a new A2AHttpClient instance
     * @throws IllegalStateException if no provider found or all providers failed to instantiate
     * @deprecated use {@link #create(A2AHttpClientConfiguration)} with
     *     {@link A2AHttpClientConfiguration#DEFAULT}
     */
    @Deprecated(forRemoval = false)
    public static A2AHttpClient create() {
        return create(A2AHttpClientConfiguration.DEFAULT);
    }

    /**
     * Creates a client while excluding the named providers.
     *
     * <p>This overload is retained for source and binary compatibility. Use
     * {@link #create(A2AHttpClientConfiguration)} with
     * {@link A2AHttpClientConfiguration.Builder#excludedProviderNames(List)} for new code.
     *
     * @param excludedProviderNames names of providers to skip
     * @return a client from the highest-priority non-excluded provider
     * @throws IllegalStateException if no eligible provider could be instantiated
     * @deprecated use {@link #create(A2AHttpClientConfiguration)}
     */
    @Deprecated(forRemoval = false)
    public static A2AHttpClient createExcluding(String... excludedProviderNames) {
        return create(
                A2AHttpClientConfiguration.builder()
                        .excludedProviderNames(List.of(excludedProviderNames))
                        .build());
    }

    /**
     * Creates a client with custom SSE parser settings.
     *
     * <p>This overload is retained for source and binary compatibility. Use
     * {@link #create(A2AHttpClientConfiguration)} with
     * {@link A2AHttpClientConfiguration.Builder#sseParserConfig(SSEParserConfig)} for new code.
     *
     * @param sseParserConfig SSE parser settings, or {@code null} for defaults
     * @return a client from the highest-priority provider that supports the settings
     * @throws IllegalStateException if no eligible provider could be instantiated
     * @deprecated use {@link #create(A2AHttpClientConfiguration)}
     */
    @Deprecated(forRemoval = false)
    public static A2AHttpClient createWithSseConfig(SSEParserConfig sseParserConfig) {
        return create(
                A2AHttpClientConfiguration.builder().sseParserConfig(sseParserConfig).build());
    }

    /**
     * Creates a client from the named provider.
     *
     * <p>This overload is retained for source and binary compatibility. New code should use
     * {@link #create(A2AHttpClientConfiguration)} and exclude unwanted providers instead.
     *
     * @param providerName the provider name
     * @return a client from the named provider
     * @throws IllegalArgumentException if the name is null, empty, or unknown
     * @deprecated use {@link #create(A2AHttpClientConfiguration)}
     */
    @Deprecated(forRemoval = false)
    public static A2AHttpClient create(String providerName) {
        if (providerName == null || providerName.isEmpty()) {
            throw new IllegalArgumentException("Provider name must not be null or empty");
        }
        return PROVIDERS.stream()
                .filter(provider -> providerName.equals(provider.name()))
                .findFirst()
                .map(provider -> provider.create(A2AHttpClientConfiguration.DEFAULT))
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "No A2AHttpClientProvider found with name: "
                                                + providerName));
    }

    /**
     * Creates a new A2AHttpClient using the supplied selection and client configuration.
     *
     * <p>When SSE parser limits or a redirect-header policy are configured, providers that do
     * not support them are skipped. This ensures configured values are never silently ignored.
     *
     * @param configuration provider exclusions and optional client settings
     * @return a configured client from the highest-priority eligible provider
     * @throws IllegalStateException if no eligible provider could be instantiated
     */
    public static A2AHttpClient create(A2AHttpClientConfiguration configuration) {
        Assert.checkNotNullParam("configuration", configuration);
        return createInternal(
                p ->
                        !configuration.excludedProviderNames().contains(p.name())
                                && supports(p, configuration),
                configuration);
    }

    private static boolean supports(
            A2AHttpClientProvider provider, A2AHttpClientConfiguration configuration) {
        return (configuration.sseParserConfig() == null || provider.supportsSseConfig())
                && (configuration.redirectHeaderPolicy() == null
                        || provider.supportsRedirectHeaderPolicy());
    }

    private static A2AHttpClient createInternal(
            Predicate<A2AHttpClientProvider> filter, A2AHttpClientConfiguration configuration) {
        return PROVIDERS.stream()
                .filter(filter)
                .flatMap(
                        p -> {
                            try {
                                return Stream.of(p.create(configuration));
                            } catch (Exception e) {
                                LOGGER.log(
                                        Level.WARNING,
                                        e,
                                        () -> "Provider " + p.name() + " skipped");
                                return Stream.empty();
                            }
                        })
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No A2AHttpClientProvider could be instantiated"));
    }
}
