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

SDK-managed HTTP clients do not follow redirects automatically by default. A synchronous GET can
opt into one redirect hop with `followRedirects(true)`; this is enabled automatically for agent
card discovery. A synchronous POST can opt in the same way. Redirect following never applies to
SSE streaming methods (`getAsyncSSE()` and `postAsyncSSE()`).

JDK and Vert.x clients follow cross-origin 301, 302, and 303 redirects as GET requests with only
the default safe headers, including `A2A-Version` and `A2A-Extensions`, unless additional headers
are configured for that exact target origin. No client follows a cross-origin 307 or 308 redirect,
so the original POST body is never forwarded to another origin. GET requests also deliberately do
not follow 307 or 308 redirects, even though they have no body: GET and POST share the same
conservative redirect policy to keep method-preserving redirects consistent. HTTPS-to-HTTP
redirects are not followed; the client returns the original redirect response.

For example, an application can enable a redirect for a regular GET request:

```java
A2AHttpResponse response = client.createGet()
    .url("https://agent.example/.well-known/agent-card.json")
    .followRedirects(true)
    .get();
```

`A2ACardResolver` and `A2A.getAgentCard(...)` enable this setting when retrieving an agent card.

### JDK HTTP Client

The default `JdkA2AHttpClient` uses `HttpClient.Redirect.NEVER`. Native JDK redirect following
must remain disabled so the SDK can apply its redirect policy. Enable the SDK-managed one-hop
redirect on an individual synchronous `post()` request:

```java
JdkA2AHttpClient client = new JdkA2AHttpClient();
client.createPost()
    .url("https://agent.example/rpc")
    .followRedirects(true)
    .body("{}")
    .post();
```

When supplying your own `HttpClient`, configure `HttpClient.Redirect.NEVER` before using
`followRedirects(true)`. The SDK rejects native redirect policies for that request with an
`IOException`; native handling would bypass the SDK redirect policy.

### Vert.x HTTP Client

The default `VertxA2AHttpClient` is configured with `setFollowRedirects(false)`. Use
`followRedirects(true)` on a synchronous `post()` builder to enable the same SDK-managed one-hop
redirect policy. `postAsyncSSE()` does not follow redirects.

### Android HTTP Client

The `AndroidA2AHttpClient` disables automatic redirect following by calling
`setInstanceFollowRedirects(false)` on all `HttpURLConnection` instances. Use
`followRedirects(true)` on a synchronous `get()` or `post()` builder to enable same-origin
one-hop redirects. Android always returns the original redirect response for a cross-origin target:
`HttpURLConnection` cannot isolate process-wide `CookieHandler` or `Authenticator` state for an
individual follow-up request, and those globals can change concurrently.
Neither `getAsyncSSE()` nor `postAsyncSSE()` follows redirects.

## Best Practices

1. **Use Standard Authentication Headers**: When implementing custom agents, use standard authentication header names from the safe allowlist to ensure credentials are properly injected.

2. **Validate Agent Cards**: Before connecting to an agent, review its security schemes to ensure they use appropriate authentication methods and header names.

3. **Handle Redirects Carefully**: Enable SDK-managed redirects only when needed. A custom `RedirectHeaderPolicy` requires explicit trusted target origins before forwarding additional headers; use HTTPS origins for credentials.

4. **Keep Dependencies Updated**: Regularly update the SDK and its dependencies to receive the latest security patches.

5. **Use TLS**: Always use HTTPS/TLS for agent communication in production environments to protect credentials in transit.

## Reporting Security Issues

If you discover a security vulnerability in the A2A Java SDK, please report it according to the guidelines in [SECURITY.md](https://github.com/a2a-protocol/a2a-java/blob/main/SECURITY.md).
