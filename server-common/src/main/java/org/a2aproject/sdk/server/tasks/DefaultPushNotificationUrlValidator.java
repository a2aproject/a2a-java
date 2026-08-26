package org.a2aproject.sdk.server.tasks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.a2aproject.sdk.server.config.A2AConfigProvider;
import org.jspecify.annotations.Nullable;

/**
 * Default SSRF-safe implementation of {@link PushNotificationUrlValidator}.
 * <p>
 * Enforces the following policies (configurable via {@link A2AConfigProvider}):
 * <ul>
 *   <li><b>Scheme restriction</b> — only {@code https} by default
 *       ({@value #ALLOWED_SCHEMES_KEY}, comma-separated)</li>
 *   <li><b>Private-network blocking</b> — loopback, link-local, site-local,
 *       cloud metadata (169.254.x.x), RFC 6598 shared space (100.64.0.0/10),
 *       IPv6 unique-local (fc00::/7), and IPv4-mapped IPv6 variants are all
 *       rejected unless {@value #ALLOW_PRIVATE_KEY} is {@code true}</li>
 *   <li><b>DNS resolution caching</b> — resolved addresses are cached for
 *       a configurable duration ({@value #DNS_CACHE_TTL_KEY}, default 30 s)
 *       to avoid blocking DNS lookups on every validation call. Set to
 *       {@code 0} to disable caching.</li>
 * </ul>
 *
 * <p><b>Known limitation — DNS rebinding:</b> hostname resolution happens at
 * validation time (when the push notification is about to be sent), not at
 * connection time. An attacker controlling a domain could register a URL that
 * resolves to a public IP (passes validation), then change the DNS record to
 * point to an internal address before the HTTP client opens the connection.
 * The DNS cache mitigates the window somewhat by reusing the validated
 * resolution, but does not eliminate the TOCTOU gap entirely. Deployments
 * with strict SSRF requirements should use a network-level firewall to block
 * outbound traffic to internal ranges.
 */
@ApplicationScoped
public class DefaultPushNotificationUrlValidator implements PushNotificationUrlValidator {

    static final String ALLOWED_SCHEMES_KEY = "a2a.push-notification.url.allowed-schemes";
    static final String ALLOW_PRIVATE_KEY = "a2a.push-notification.url.allow-private-network-targets";
    static final String DNS_CACHE_TTL_KEY = "a2a.push-notification.url.dns-cache-ttl-seconds";

    private static final Set<String> DEFAULT_ALLOWED_SCHEMES = Set.of("https");
    private static final long DEFAULT_DNS_CACHE_TTL_SECONDS = 30;

    // RFC 6598 shared address space: 100.64.0.0/10
    private static final int RFC6598_FIRST_OCTET = 100;
    private static final int RFC6598_SECOND_OCTET_MASK = 0xC0;
    private static final int RFC6598_SECOND_OCTET_VALUE = 0x40;

    // IPv6 unique-local address prefix: fc00::/7
    private static final int IPV6_ULA_MASK = 0xFE;
    private static final int IPV6_ULA_PREFIX = 0xFC;

    @Inject
    @Nullable A2AConfigProvider configProvider;

    private final Map<String, DnsCacheEntry> dnsCache = new ConcurrentHashMap<>();

    public DefaultPushNotificationUrlValidator() {
    }

    private record DnsCacheEntry(InetAddress[] addresses, long expiresAtNanos) {
        boolean isExpired() {
            return System.nanoTime() - expiresAtNanos >= 0;
        }
    }

    @Override
    public void validate(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid push notification URL: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        Set<String> allowed = getAllowedSchemes();
        if (scheme == null || !allowed.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Push notification URL scheme '" + scheme + "' is not allowed; allowed: " + allowed);
        }

        String host = uri.getHost();
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("Push notification URL must have a host");
        }

        if (!isPrivateNetworkAllowed()) {
            InetAddress[] addresses = resolveHost(host);

            for (InetAddress addr : addresses) {
                if (isBlockedAddress(addr)) {
                    throw new IllegalArgumentException(
                            "Push notification URL host resolves to a blocked network address: "
                                    + addr.getHostAddress());
                }
            }
        }
    }

    private Set<String> getAllowedSchemes() {
        if (configProvider == null) {
            return DEFAULT_ALLOWED_SCHEMES;
        }
        return configProvider.getOptionalValue(ALLOWED_SCHEMES_KEY)
                .map(value -> {
                    Set<String> schemes = new HashSet<>();
                    for (String s : value.split(",")) {
                        String trimmed = s.trim().toLowerCase(Locale.ROOT);
                        if (!trimmed.isEmpty()) {
                            schemes.add(trimmed);
                        }
                    }
                    return schemes.isEmpty() ? DEFAULT_ALLOWED_SCHEMES : Set.copyOf(schemes);
                })
                .orElse(DEFAULT_ALLOWED_SCHEMES);
    }

    private InetAddress[] resolveHost(String host) {
        long ttlSeconds = getDnsCacheTtlSeconds();
        if (ttlSeconds > 0) {
            DnsCacheEntry cached = dnsCache.get(host);
            if (cached != null && !cached.isExpired()) {
                return cached.addresses();
            }
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            dnsCache.remove(host);
            throw new IllegalArgumentException("Cannot resolve push notification URL host: " + host);
        }

        if (ttlSeconds > 0) {
            long expiresAt = System.nanoTime() + ttlSeconds * 1_000_000_000L;
            dnsCache.put(host, new DnsCacheEntry(Arrays.copyOf(addresses, addresses.length), expiresAt));
        }
        return addresses;
    }

    private long getDnsCacheTtlSeconds() {
        if (configProvider == null) {
            return DEFAULT_DNS_CACHE_TTL_SECONDS;
        }
        return configProvider.getOptionalValue(DNS_CACHE_TTL_KEY)
                .map(value -> {
                    try {
                        long ttl = Long.parseLong(value.trim());
                        return ttl < 0 ? DEFAULT_DNS_CACHE_TTL_SECONDS : ttl;
                    } catch (NumberFormatException e) {
                        return DEFAULT_DNS_CACHE_TTL_SECONDS;
                    }
                })
                .orElse(DEFAULT_DNS_CACHE_TTL_SECONDS);
    }

    private boolean isPrivateNetworkAllowed() {
        if (configProvider == null) {
            return false;
        }
        return configProvider.getOptionalValue(ALLOW_PRIVATE_KEY)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    static boolean isBlockedAddress(InetAddress addr) {
        if (addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isAnyLocalAddress()
                || addr.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = addr.getAddress();

        if (bytes.length == 4) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            if (first == RFC6598_FIRST_OCTET && (second & RFC6598_SECOND_OCTET_MASK) == RFC6598_SECOND_OCTET_VALUE) {
                return true;
            }
        }

        if (bytes.length == 16) {
            if ((bytes[0] & IPV6_ULA_MASK) == IPV6_ULA_PREFIX) {
                return true;
            }
            // IPv4-mapped IPv6 (::ffff:x.x.x.x) — re-check the embedded IPv4 portion
            if (isIPv4Mapped(bytes)) {
                byte[] ipv4 = {bytes[12], bytes[13], bytes[14], bytes[15]};
                try {
                    return isBlockedAddress(InetAddress.getByAddress(ipv4));
                } catch (UnknownHostException e) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isIPv4Mapped(byte[] bytes) {
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return (bytes[10] & 0xFF) == 0xFF && (bytes[11] & 0xFF) == 0xFF;
    }
}
