package org.a2aproject.sdk.client.http.android;

import static org.a2aproject.sdk.util.Assert.checkNotNullParam;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_MULT_CHOICE;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpHeaders;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.http.BoundedLineAccumulator;
import org.a2aproject.sdk.client.http.ServerSentEvent;
import org.a2aproject.sdk.client.http.SSEParserConfig;
import org.a2aproject.sdk.client.http.ServerSentEventParser;
import org.a2aproject.sdk.common.A2AErrorMessages;
import org.a2aproject.sdk.spec.A2AClientHTTPError;
import org.jspecify.annotations.Nullable;

/**
 * Android-specific implementation of {@link A2AHttpClient} using {@link HttpURLConnection}.
 *
 * <p><b>Security Note:</b> This client does not follow HTTP redirects automatically
 * to prevent credential leakage to third-party origins. Applications requiring redirect
 * following must handle redirects manually.
 */
public class AndroidA2AHttpClient implements A2AHttpClient {

  private static final Executor NET_EXECUTOR = Executors.newCachedThreadPool(r -> {
    Thread t = new Thread(r, "A2A-Android-Net");
    t.setDaemon(true);
    return t;
  });

  private final SSEParserConfig sseParserConfig;

  public AndroidA2AHttpClient() {
    this(SSEParserConfig.DEFAULT);
  }

  /**
   * Creates a new Android HTTP client with custom SSE parser limits.
   *
   * @param sseParserConfig the SSE parser configuration to use for streaming responses
   */
  public AndroidA2AHttpClient(SSEParserConfig sseParserConfig) {
    this.sseParserConfig = checkNotNullParam("sseParserConfig", sseParserConfig);
  }

  @Override
  public GetBuilder createGet() {
    return new AndroidGetBuilder(sseParserConfig);
  }

  @Override
  public PostBuilder createPost() {
    return new AndroidPostBuilder(sseParserConfig);
  }

  @Override
  public DeleteBuilder createDelete() {
    return new AndroidDeleteBuilder(sseParserConfig);
  }

  private abstract static class AndroidBuilder<T extends Builder<T>> implements Builder<T> {
    protected String url = "";
    protected Map<String, String> headers = new HashMap<>();
    protected final SSEParserConfig sseParserConfig;

    AndroidBuilder(SSEParserConfig sseParserConfig) {
      this.sseParserConfig = sseParserConfig;
    }

    @Override
    public T url(String url) {
      this.url = url;
      return self();
    }

    @Override
    public T addHeader(String name, String value) {
      headers.put(name, value);
      return self();
    }

    @Override
    public T addHeaders(Map<String, String> headers) {
      if (headers != null) {
        this.headers.putAll(headers);
      }
      return self();
    }

    @SuppressWarnings("unchecked")
    protected T self() {
      return (T) this;
    }

    protected HttpURLConnection createConnection(String method, boolean isSSE) throws IOException {
      URL urlObj;
      try {
        urlObj = new URI(url).toURL();
      } catch (URISyntaxException e) {
        throw new MalformedURLException("Invalid URL: " + url);
      }
      HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
      connection.setRequestMethod(method);
      connection.setConnectTimeout(15000); // 15 seconds
      connection.setReadTimeout(60000);    // 60 seconds
      // Security: Disable automatic redirect following to prevent credential leakage
      connection.setInstanceFollowRedirects(false);
      for (Map.Entry<String, String> header : headers.entrySet()) {
        connection.setRequestProperty(header.getKey(), header.getValue());
      }
      if (isSSE) {
        connection.setRequestProperty(A2AHttpClient.ACCEPT, A2AHttpClient.EVENT_STREAM);
      }
      return connection;
    }

    protected static String readStreamWithLimit(InputStream is) throws IOException {
      if (is == null) {
        return "";
      }
      int maxResponseSize = 10 * 1024 * 1024; // 10 MB
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        StringBuilder sb = new StringBuilder();
        String line;
        boolean first = true;
        while ((line = reader.readLine()) != null) {
          if (sb.length() + line.length() > maxResponseSize) {
            throw new IOException("Response size exceeds limit");
          }
          if (!first) {
            sb.append('\n');
          }
          sb.append(line);
          first = false;
        }
        return sb.toString();
      }
    }

    /**
     * Reads the next line from {@code is}, treating LF, CRLF, and bare CR as terminators
     * per the SSE specification, and enforcing the byte limit <em>while accumulating
     * bytes</em> — not after a full line has been materialised.
     *
     * <p>The caller must provide a {@link BoundedLineAccumulator} and call
     * {@link BoundedLineAccumulator#reset()} between lines. After this method returns
     * {@code true}, check {@link BoundedLineAccumulator#isTooLong()} to distinguish
     * between a valid line and one that exceeded the limit.
     *
     * <p>The {@code is} parameter must support {@link InputStream#mark(int)} (e.g.
     * {@link java.io.BufferedInputStream}) so that CR at end-of-buffer can be resolved
     * without consuming the following byte.
     *
     * @param is the input stream to read from (must support mark/reset)
     * @param accumulator the accumulator to use for byte collection and limit enforcement
     * @return {@code true} if a line was read, {@code false} at end-of-stream
     */
    static boolean readBoundedLine(InputStream is, BoundedLineAccumulator accumulator) throws IOException {
      boolean hasReadAnyBytes = false;

      while (true) {
        int b = is.read();
        if (b == -1) {
          return hasReadAnyBytes || accumulator.isTooLong();
        }
        hasReadAnyBytes = true;
        if (b == '\n') {
          return true;
        }
        if (b == '\r') {
          consumeOptionalLF(is);
          return true;
        }
        accumulator.addByte(b);
      }
    }

    private static void consumeOptionalLF(InputStream is) throws IOException {
      is.mark(1);
      int next = is.read();
      if (next != '\n' && next != -1) {
        is.reset();
      }
    }

    protected A2AHttpResponse execute(HttpURLConnection connection) throws IOException {
      int status = connection.getResponseCode();
      A2AHttpHeaders responseHeaders = fromConnectionHeaders(connection.getHeaderFields());
      if (status == HTTP_UNAUTHORIZED) {
        throw new IOException(A2AErrorMessages.AUTHENTICATION_FAILED,
            new A2AClientHTTPError(HTTP_UNAUTHORIZED, A2AErrorMessages.AUTHENTICATION_FAILED,
                null, responseHeaders.toMap()));
      } else if (status == HTTP_FORBIDDEN) {
        throw new IOException(A2AErrorMessages.AUTHORIZATION_FAILED,
            new A2AClientHTTPError(HTTP_FORBIDDEN, A2AErrorMessages.AUTHORIZATION_FAILED,
                null, responseHeaders.toMap()));
      }

      String body = "";
      try (InputStream is =
          (status >= HTTP_OK && status < HTTP_MULT_CHOICE)
              ? connection.getInputStream()
              : connection.getErrorStream()) {
        body = readStreamWithLimit(is);
      }

      return new AndroidHttpResponse(status, body, responseHeaders);
    }

    protected void processSSEResponse(
        HttpURLConnection connection,
        Consumer<ServerSentEvent> messageConsumer,
        Consumer<Throwable> errorConsumer,
        Runnable completeRunnable) {
      try {
        int status = connection.getResponseCode();
        if (!(status >= HTTP_OK && status < HTTP_MULT_CHOICE)) {
          if (status == HTTP_UNAUTHORIZED || status == HTTP_FORBIDDEN) {
            A2AHttpHeaders responseHeaders = fromConnectionHeaders(connection.getHeaderFields());
            String msg = status == HTTP_UNAUTHORIZED
                ? A2AErrorMessages.AUTHENTICATION_FAILED
                : A2AErrorMessages.AUTHORIZATION_FAILED;
            errorConsumer.accept(new IOException(msg,
                new A2AClientHTTPError(status, msg, null, responseHeaders.toMap())));
            return;
          }

          String errorBody = "";
          try (InputStream es = connection.getErrorStream()) {
            errorBody = readStreamWithLimit(es);
          }
          // Pass the error body through messageConsumer so higher-level listeners
          // (e.g. RestErrorMapper in SSEEventListener) can produce a typed error.
          // Do not also call errorConsumer here — the messageConsumer path is responsible
          // for signalling the error, matching the async JDK client's behaviour.
          if (!errorBody.isEmpty()) {
            messageConsumer.accept(new ServerSentEvent(errorBody));
          } else {
            errorConsumer.accept(
                new IOException("Request failed with status " + status));
          }
          return;
        }

        String contentType = connection.getContentType();
        boolean isSse = contentType != null && contentType.contains(EVENT_STREAM);

        try (InputStream is = connection.getInputStream()) {
          if (isSse) {
            readSSEStream(is, sseParserConfig, messageConsumer, errorConsumer);
          } else {
            readNonSSEStream(is, messageConsumer);
          }
          completeRunnable.run();
        }
      } catch (Exception e) {
        errorConsumer.accept(e);
      } finally {
        connection.disconnect();
      }
    }

    private static void readSSEStream(
        InputStream is,
        SSEParserConfig config,
        Consumer<ServerSentEvent> messageConsumer,
        Consumer<Throwable> errorConsumer) throws IOException {
      InputStream buffered = new java.io.BufferedInputStream(is);
      BoundedLineAccumulator accumulator = new BoundedLineAccumulator(config.maxLineLength());
      ServerSentEventParser sseParser = new ServerSentEventParser(messageConsumer, errorConsumer, config);
      while (readBoundedLine(buffered, accumulator)) {
        if (accumulator.isTooLong()) {
          sseParser.processLineTooLong();
        } else {
          sseParser.processLine(accumulator.toLine());
        }
        accumulator.reset();
      }
      sseParser.flush();
    }

    private static void readNonSSEStream(
        InputStream is,
        Consumer<ServerSentEvent> messageConsumer) throws IOException {
      StringBuilder bodyBuffer = new StringBuilder();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.isEmpty()) {
          if (bodyBuffer.length() > 0) {
            bodyBuffer.append('\n');
          }
          bodyBuffer.append(line);
        }
      }
      String body = bodyBuffer.toString();
      if (!body.isEmpty()) {
        messageConsumer.accept(new ServerSentEvent(body));
      }
    }

    protected CompletableFuture<Void> executeAsyncSSE(
        HttpURLConnection connection,
        Consumer<ServerSentEvent> messageConsumer,
        Consumer<Throwable> errorConsumer,
        Runnable completeRunnable) {
      return CompletableFuture.runAsync(
          () -> processSSEResponse(connection, messageConsumer, errorConsumer, completeRunnable),
          NET_EXECUTOR);
    }
  }

  private static class AndroidGetBuilder extends AndroidBuilder<GetBuilder> implements GetBuilder {
AndroidGetBuilder(SSEParserConfig sseParserConfig) {
      super(sseParserConfig);
    }

    @Override
    public A2AHttpResponse get() throws IOException {
      HttpURLConnection connection = createConnection("GET", false);
      try {
        return execute(connection);
      } catch (IOException e) {
        connection.disconnect();
        throw e;
      }
    }

    @Override
    public CompletableFuture<Void> getAsyncSSE(
        Consumer<ServerSentEvent> messageConsumer,
        Consumer<Throwable> errorConsumer,
        Runnable completeRunnable)
        throws IOException {
      HttpURLConnection connection = createConnection("GET", true);
      return executeAsyncSSE(connection, messageConsumer, errorConsumer, completeRunnable);
    }
  }

  private static class AndroidPostBuilder extends AndroidBuilder<PostBuilder>
      implements PostBuilder {
    private String body = "";
    private boolean followRedirects = false;

AndroidPostBuilder(SSEParserConfig sseParserConfig) {
      super(sseParserConfig);
    }

    @Override
    public PostBuilder body(String body) {
      this.body = body;
      return this;
    }

    @Override
    public PostBuilder followRedirects(boolean follow) {
      this.followRedirects = follow;
      return self();
    }

    @Override
    public A2AHttpResponse post() throws IOException {
      HttpURLConnection connection = createConnection("POST", false);
      connection.setInstanceFollowRedirects(followRedirects);
      connection.setDoOutput(true);
      try {
        try (OutputStream os = connection.getOutputStream()) {
          os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return execute(connection);
      } catch (IOException e) {
        connection.disconnect();
        throw e;
      }
    }

    @Override
    public CompletableFuture<Void> postAsyncSSE(
        Consumer<ServerSentEvent> messageConsumer,
        Consumer<Throwable> errorConsumer,
        Runnable completeRunnable)
        throws IOException {
      HttpURLConnection connection = createConnection("POST", true);
      connection.setDoOutput(true);

      return CompletableFuture.runAsync(
          () -> {
            try {
              try (OutputStream os = connection.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
              }
              processSSEResponse(connection, messageConsumer, errorConsumer, completeRunnable);
            } catch (Exception e) {
              errorConsumer.accept(e);
              connection.disconnect();
            }
          }, NET_EXECUTOR);
    }
  }

  private static class AndroidDeleteBuilder extends AndroidBuilder<DeleteBuilder>
      implements DeleteBuilder {
AndroidDeleteBuilder(SSEParserConfig sseParserConfig) {
      super(sseParserConfig);
    }

    @Override
    public A2AHttpResponse delete() throws IOException {
      HttpURLConnection connection = createConnection("DELETE", false);
      try {
        return execute(connection);
      } catch (IOException e) {
        connection.disconnect();
        throw e;
      }
    }
  }

  private static A2AHttpHeaders fromConnectionHeaders(@Nullable Map<String, List<String>> headerFields) {
    // HttpURLConnection.getHeaderFields() may include a null key for the HTTP status line;
    // A2AHttpHeaders.of() filters those out automatically.
    return A2AHttpHeaders.of(headerFields != null ? headerFields : Map.of());
  }

  private record AndroidHttpResponse(int status, String body, A2AHttpHeaders headers) implements A2AHttpResponse {
    @Override
    public boolean success() {
      return status >= HTTP_OK && status < HTTP_MULT_CHOICE;
    }

    @Override
    public A2AHttpHeaders headers() {
      return headers;
    }
  }
}
