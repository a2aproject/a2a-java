package org.a2aproject.sdk.client.http;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.a2aproject.sdk.util.Assert;
import org.jspecify.annotations.Nullable;

/**
 * Controls the request headers sent when a POST redirect crosses an HTTP origin.
 *
 * <p>The default policy retains only request metadata that is safe to disclose to another origin.
 * Additional headers are sent only to explicitly trusted target origins. Entity headers are
 * removed when a redirect changes a POST into a GET.
 */
public final class RedirectHeaderPolicy {

    private static final Set<String> DEFAULT_CROSS_ORIGIN_HEADERS = Set.of(
            "accept",
            "accept-charset",
            "accept-encoding",
            "accept-language",
            "a2a-extensions",
            "a2a-version",
            "cache-control",
            "content-type",
            "pragma",
            "user-agent");
    private static final Set<String> ENTITY_HEADERS = Set.of(
            "content-encoding",
            "content-language",
            "content-length",
            "content-location",
            "content-type",
            "digest",
            "expect",
            "last-modified",
            "transfer-encoding");

    /** The conservative policy used by every SDK HTTP client by default. */
    public static final RedirectHeaderPolicy DEFAULT = new RedirectHeaderPolicy(Set.of(), Set.of());

    private final Set<String> additionalCrossOriginHeaders;
    private final Set<URI> trustedOrigins;

    /**
     * Creates a policy that retains additional headers only on redirects to trusted origins.
     * Header names and origin scheme/host are compared case-insensitively. Default safe headers
     * remain available on redirects to any origin.
     *
     * @param additionalCrossOriginHeaders additional header names allowed at trusted origins
     * @param trustedOrigins HTTP(S) origins (scheme, host, and optional port) allowed to receive
     *     the additional headers; paths, queries, fragments, and user information are not allowed
     * @throws IllegalArgumentException if additional headers have no trusted origins or a trusted
     *     origin contains more than a scheme, host, and port
     */
    public RedirectHeaderPolicy(Set<String> additionalCrossOriginHeaders, Set<URI> trustedOrigins) {
        Assert.checkNotNullParam("additionalCrossOriginHeaders", additionalCrossOriginHeaders);
        Assert.checkNotNullParam("trustedOrigins", trustedOrigins);
        if (!additionalCrossOriginHeaders.isEmpty() && trustedOrigins.isEmpty()) {
            throw new IllegalArgumentException("Additional redirect headers require trusted origins");
        }
        this.additionalCrossOriginHeaders = additionalCrossOriginHeaders.stream()
                .map(header -> Assert.checkNotNullParam("additionalCrossOriginHeader", header)
                        .toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<URI> trustedOriginsCopy = Set.copyOf(trustedOrigins);
        for (URI origin : trustedOriginsCopy) {
            Assert.checkNotNullParam("trustedOrigin", origin);
            @Nullable String path = origin.getRawPath();
            if (!isSupportedHttpTarget(origin)
                    || origin.getRawUserInfo() != null
                    || (path != null && !path.isEmpty() && !"/".equals(path))
                    || origin.getRawQuery() != null
                    || origin.getRawFragment() != null
                    || origin.getPort() == 0
                    || origin.getPort() > 65535) {
                throw new IllegalArgumentException("Trusted redirect target must be an HTTP(S) origin: " + origin);
            }
        }
        this.trustedOrigins = trustedOriginsCopy;
    }

    /**
     * Resolves and sanitizes a single supported POST redirect.
     *
     * @param source the URI that returned the redirect
     * @param statusCode the HTTP response status
     * @param location the raw {@code Location} header value
     * @param headers the original request headers
     * @return the sanitized redirected request, or empty for an unsupported or malformed redirect
     */
    public Optional<RedirectedRequest> redirectedRequest(
            URI source, int statusCode, @Nullable String location, Map<String, String> headers) {
        Assert.checkNotNullParam("source", source);
        Assert.checkNotNullParam("headers", headers);
        if (location == null) {
            return Optional.empty();
        }

        boolean redirectAsGet = statusCode == 301 || statusCode == 302 || statusCode == 303;
        boolean redirectAsPost = statusCode == 307 || statusCode == 308;
        if (!redirectAsGet && !redirectAsPost) {
            return Optional.empty();
        }

        URI target;
        try {
            target = resolveLocation(source, location);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        if (!isSupportedHttpTarget(target)
                || ("https".equals(normalize(source.getScheme()))
                        && "http".equals(normalize(target.getScheme())))) {
            return Optional.empty();
        }
        boolean sameOrigin = sameOrigin(source, target);
        if (redirectAsPost && !sameOrigin) {
            return Optional.empty();
        }

        return Optional.of(new RedirectedRequest(
                target,
                redirectAsGet,
                headersForRedirect(headers, target, sameOrigin, redirectAsGet)));
    }

    /**
     * Resolves a redirect location according to RFC 3986 section 5.
     *
     * <p>{@link URI#resolve(URI)} does not retain the final path segment for a query-only
     * reference (for example, resolving {@code ?page=2} against {@code /rpc/send?old=1}
     * produces {@code /rpc/?page=2}). Handle empty-path references explicitly so redirects
     * retain the source path as required by RFC 3986 section 5.2.2.
     */
    private static URI resolveLocation(URI source, String location) {
        URI reference = URI.create(location);
        if (reference.isAbsolute()
                || reference.getRawAuthority() != null
                || !reference.getRawPath().isEmpty()) {
            return source.resolve(reference);
        }

        StringBuilder target = new StringBuilder();
        target.append(source.getScheme()).append("://").append(source.getRawAuthority());
        target.append(source.getRawPath());
        @Nullable String query = reference.getRawQuery();
        if (query == null) {
            query = source.getRawQuery();
        }
        if (query != null) {
            target.append('?').append(query);
        }
        @Nullable String fragment = reference.getRawFragment();
        if (fragment != null) {
            target.append('#').append(fragment);
        }
        return URI.create(target.toString());
    }

    /**
     * Compares normalized scheme, host, and effective port. URIs without a scheme or host never match.
     *
     * @param source the redirect source URI
     * @param target the redirect target URI
     * @return whether source and target have the same origin
     */
    public static boolean sameOrigin(URI source, URI target) {
        @Nullable String sourceScheme = normalize(source.getScheme());
        @Nullable String targetScheme = normalize(target.getScheme());
        @Nullable String sourceHost = normalize(source.getHost());
        @Nullable String targetHost = normalize(target.getHost());
        if (sourceScheme == null
                || targetScheme == null
                || sourceHost == null
                || targetHost == null) {
            return false;
        }
        return sourceScheme.equals(targetScheme)
                && sourceHost.equals(targetHost)
                && effectivePort(source) == effectivePort(target);
    }

    private Map<String, String> headersForRedirect(
            Map<String, String> headers, URI target, boolean sameOrigin, boolean redirectAsGet) {
        Map<String, String> filteredHeaders = new HashMap<>(headers);
        boolean trustedTarget = trustedOrigins.stream().anyMatch(origin -> sameOrigin(origin, target));
        filteredHeaders.entrySet().removeIf(entry -> {
            String name = entry.getKey();
            if (name == null) {
                return true;
            }
            String normalizedName = name.toLowerCase(Locale.ROOT);
            return (!sameOrigin
                            && !DEFAULT_CROSS_ORIGIN_HEADERS.contains(normalizedName)
                            && !(trustedTarget && additionalCrossOriginHeaders.contains(normalizedName)))
                    || (redirectAsGet && ENTITY_HEADERS.contains(normalizedName));
        });
        return Map.copyOf(filteredHeaders);
    }

    private static @Nullable String normalize(@Nullable String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private static boolean isSupportedHttpTarget(URI target) {
        @Nullable String scheme = normalize(target.getScheme());
        return target.isAbsolute()
                && target.getHost() != null
                && ("http".equals(scheme) || "https".equals(scheme));
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        if ("http".equals(normalize(uri.getScheme()))) {
            return 80;
        }
        if ("https".equals(normalize(uri.getScheme()))) {
            return 443;
        }
        return -1;
    }

    /** A prepared one-hop POST redirect request. */
    public record RedirectedRequest(URI target, boolean redirectAsGet, Map<String, String> headers) {
        public RedirectedRequest {
            Assert.checkNotNullParam("target", target);
            Assert.checkNotNullParam("headers", headers);
            headers = Map.copyOf(headers);
        }
    }
}
