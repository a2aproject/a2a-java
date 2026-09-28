---
title: Security
description: Security features and best practices for the A2A Java SDK
---

# Security

The A2A Java SDK implements several security hardening measures to protect against credential leakage and other security vulnerabilities.

## API Key Header Name Validation

When using API key authentication with header-based transport, the SDK validates the header name provided by the remote agent's security scheme against a safe allowlist. This prevents malicious agents from exploiting custom header names to leak credentials to unintended destinations.

**Safe Header Names (comparison is case-insensitive):**
- `Authorization`
- `X-API-Key`
- `API-Key`
- `X-Auth-Token`
- `X-Authentication`

Header names are matched case-insensitively per [RFC 7230](https://datatracker.ietf.org/doc/html/rfc7230#section-3.2), so `X-Api-Key`, `x-api-key`, and `X-API-Key` are all treated as the same entry. If an agent specifies an API key header name that is not in this allowlist, the SDK will skip credential injection for that scheme. This behavior applies to both the current protocol version and the v0.3 compatibility layer.

**Example:**
```java
// Safe: Uses an allowed header name
APIKeySecurityScheme scheme = new APIKeySecurityScheme(
    APIKeySecurityScheme.Location.HEADER,
    "X-API-Key",
    "API Key authentication"
);
// Credential will be injected

// Unsafe: Uses a non-standard header name
APIKeySecurityScheme unsafeScheme = new APIKeySecurityScheme(
    APIKeySecurityScheme.Location.HEADER,
    "X-Custom-Header",
    "Custom header"
);
// Credential will NOT be injected
```

Additionally, API keys are only injected when the security scheme explicitly specifies header-based transport (`Location.HEADER`). Query parameter and cookie-based API keys are not injected as HTTP headers.

## HTTP Redirect Handling

SDK-managed HTTP clients do not follow redirects automatically by default. This prevents credentials from being inadvertently forwarded to third-party origins during redirect chains.

### JDK HTTP Client

The default `JdkA2AHttpClient` is configured with `HttpClient.Redirect.NEVER`. If your application requires redirect following, provide a custom `HttpClient` instance:

```java
HttpClient customClient = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build();

JdkA2AHttpClient client = new JdkA2AHttpClient(customClient);
```

### Vert.x HTTP Client

The default `VertxA2AHttpClient` is configured with `setFollowRedirects(false)`. If redirect following is needed, create a custom `WebClient` with the desired policy and pass the underlying `Vertx` instance to the constructor.

### Android HTTP Client

The `AndroidA2AHttpClient` disables automatic redirect following by calling `setInstanceFollowRedirects(false)` on all `HttpURLConnection` instances. Applications requiring redirect handling must implement it manually.

## Best Practices

1. **Use Standard Authentication Headers**: When implementing custom agents, use standard authentication header names from the safe allowlist to ensure credentials are properly injected.

2. **Validate Agent Cards**: Before connecting to an agent, review its security schemes to ensure they use appropriate authentication methods and header names.

3. **Handle Redirects Carefully**: If your application requires redirect following, implement it with caution and ensure credentials are not forwarded to untrusted origins.

4. **Keep Dependencies Updated**: Regularly update the SDK and its dependencies to receive the latest security patches.

5. **Use TLS**: Always use HTTPS/TLS for agent communication in production environments to protect credentials in transit.

## Reporting Security Issues

If you discover a security vulnerability in the A2A Java SDK, please report it according to the guidelines in [SECURITY.md](https://github.com/a2a-protocol/a2a-java/blob/main/SECURITY.md).
