package org.a2aproject.sdk.client.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/** Validation and canonicalization of HTTP endpoint identities used for credentials. */
public final class HttpEndpoint {
    private HttpEndpoint() {
    }

    /** Validates and encodes an identifier for use as one HTTP path segment.
     * @param value identifier to encode
     * @return UTF-8 encoded path segment
     * @throws IllegalArgumentException if the identifier is empty or ambiguous
     */
    public static String pathSegment(@Nullable String value) {
        checkPathSegment(value);
        String identifier = Objects.requireNonNull(value);
        StringBuilder encoded = new StringBuilder();
        for (byte octet : identifier.getBytes(StandardCharsets.UTF_8)) {
            int c = octet & 0xff;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.' || c == '~'
                    || c == '@' || c == '!' || c == '$' || c == '&' || c == '\''
                    || c == '(' || c == ')' || c == '*' || c == '+' || c == ',' || c == '=') {
                encoded.append((char) c);
            } else {
                encoded.append('%');
                encoded.append("0123456789ABCDEF".charAt(c >>> 4));
                encoded.append("0123456789ABCDEF".charAt(c & 15));
            }
        }
        return encoded.toString();
    }

    /**
     * Validates an identifier without encoding it.
     *
     * @param value identifier to validate
     * @throws IllegalArgumentException if the identifier is empty or ambiguous
     */
    public static void checkPathSegment(@Nullable String value) {
        if (value == null || value.isEmpty() || value.equals(".") || value.equals("..")) {
            throw new IllegalArgumentException("Invalid path identifier");
        }
        if (value.indexOf('/') >= 0 || value.indexOf('\\') >= 0
                || value.indexOf('?') >= 0 || value.indexOf('#') >= 0) {
            throw new IllegalArgumentException("Invalid path identifier");
        }
    }

    /**
     * Returns the canonical REST base URL used for credential lookup, removing its trailing slash.
     * Pass the configured base endpoint. For tenant-scoped requests, pass the tenant separately
     * to the credential service.
     *
     * @param endpoint REST base URL
     * @return canonical REST credential destination
     * @throws IllegalArgumentException if the endpoint is invalid or ambiguous
     */
    public static String normalizeRest(String endpoint) {
        String normalized = normalize(endpoint);
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    /**
     * Returns the JSON-RPC endpoint key. An empty path and the root path both address {@code /}.
     *
     * @param endpoint JSON-RPC URL
     * @return canonical JSON-RPC endpoint
     */
    public static String normalizeJsonRpc(String endpoint) {
        String normalized = normalize(endpoint);
        int pathStart = normalized.indexOf('/', normalized.indexOf("://") + 3);
        return pathStart == normalized.length() - 1 ? normalized.substring(0, pathStart) : normalized;
    }

    /**
     * Validates the endpoint and normalizes its authority while preserving its path.
     * For credential keys, use {@link #normalizeJsonRpc(String)} or {@link #normalizeRest(String)}.
     * The host must be a DNS hostname or IP literal that {@link URI#getHost()} recognizes.
     * A DNS root-label dot at the end of the hostname is omitted.
     *
     * @param endpoint absolute HTTP or HTTPS endpoint without query or fragment
     * @return canonical endpoint
     * @throws IllegalArgumentException if the endpoint is invalid or ambiguous
     */
    public static String normalize(String endpoint) {
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint must not be null");
        }
        try {
            URI uri = new URI(endpoint);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || uri.getRawUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null
                    || uri.getRawAuthority() == null || uri.getRawAuthority().endsWith(":")
                    || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("Invalid HTTP endpoint");
            }
            if (host == null) {
                throw new IllegalArgumentException("HTTP endpoint must have a URI-compatible hostname");
            }
            if (host.endsWith(".")) {
                host = host.substring(0, host.length() - 1);
            }
            int port = uri.getPort();
            if (port == 0 || port > 65535) {
                throw new IllegalArgumentException("Invalid HTTP endpoint port");
            }
            String path = uri.getRawPath();
            if (path == null) {
                path = "";
            }
            String lowerPath = path.toLowerCase(Locale.ROOT);
            if (lowerPath.contains("%2e") || lowerPath.contains("%2f") || lowerPath.contains("%5c")
                    || lowerPath.contains("%25") || path.contains("//")) {
                throw new IllegalArgumentException("Ambiguous HTTP endpoint path");
            }
            for (String segment : path.split("/", -1)) {
                if (segment.equals(".") || segment.equals("..")) {
                    throw new IllegalArgumentException("Ambiguous HTTP endpoint path");
                }
            }
            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            return normalizedScheme + "://" + normalizedHost + ":"
                    + (port == -1 ? (normalizedScheme.equals("https") ? 443 : 80) : port) + path;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid HTTP endpoint", e);
        }
    }
}
