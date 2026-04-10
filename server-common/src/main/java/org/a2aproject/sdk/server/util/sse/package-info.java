/**
 * Server-Sent Events (SSE) formatting utilities for A2A streaming responses.
 * <p>
 * Provides framework-agnostic conversion of {@code Flow.Publisher<A2AResponse<?>>} to
 * {@code Flow.Publisher<String>} with SSE formatting, enabling easy integration with
 * any HTTP server framework (Vert.x, Jakarta Servlet, etc.).
 */
@NullMarked
package org.a2aproject.sdk.server.util.sse;

import org.jspecify.annotations.NullMarked;
