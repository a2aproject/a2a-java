package org.a2aproject.sdk.client.http;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_MULT_CHOICE;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import org.a2aproject.sdk.common.A2AErrorMessages;
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Android-specific implementation of {@link A2AHttpClient} using {@link HttpURLConnection}. */
public class AndroidA2AHttpClient implements A2AHttpClient {

  private static final Executor NET_EXECUTOR = Executors.newCachedThreadPool(r -> {
    Thread t = new Thread(r, "A2A-Android-Net");
    t.setDaemon(true);
    return t;
  });

  @Override
  public GetBuilder createGet() {
    return new AndroidGetBuilder();
  }

  @Override
  public PostBuilder createPost() {
    return new AndroidPostBuilder();
  }

  @Override
  public DeleteBuilder createDelete() {
    return new AndroidDeleteBuilder();
  }

  private abstract static class AndroidBuilder<T extends Builder<T>> implements Builder<T> {
    protected String url = "";
    protected Map<String, String> headers = new HashMap<>();

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

    protected A2AHttpResponse execute(HttpURLConnection connection) throws IOException {
      int status = connection.getResponseCode();
      if (status == HTTP_UNAUTHORIZED) {
        throw new IOException(A2AErrorMessages.AUTHENTICATION_FAILED);
      } else if (status == HTTP_FORBIDDEN) {
        throw new IOException(A2AErrorMessages.AUTHORIZATION_FAILED);
      }

      String body = "";
      try (InputStream is =
          (status >= HTTP_OK && status < HTTP_MULT_CHOICE)
              ? connection.getInputStream()
              : connection.getErrorStream()) {
        body = readStreamWithLimit(is);
      }

      return new AndroidHttpResponse(status, body);
    }

    protected void processSSEResponse(
        HttpURLConnection connection,
        Consumer<ServerSentEvent> messageConsumer,
        Consumer<Throwable> errorConsumer,
        Runnable completeRunnable) {
      try {
        int status = connection.getResponseCode();
        if (!(status >= HTTP_OK && status < HTTP_MULT_CHOICE)) {
          if (status == HTTP_UNAUTHORIZED) {
            errorConsumer.accept(new IOException(A2AErrorMessages.AUTHENTICATION_FAILED));
            return;
          } else if (status == HTTP_FORBIDDEN) {
            errorConsumer.accept(new IOException(A2AErrorMessages.AUTHORIZATION_FAILED));
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

        try (InputStream is = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
          String line;
          if (isSse) {
            ServerSentEventParser sseParser = new ServerSentEventParser(messageConsumer, errorConsumer);
            while ((line = reader.readLine()) != null) {
              sseParser.processLine(line);
            }
            sseParser.flush();
          } else {
            StringBuilder bodyBuffer = new StringBuilder();
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
          completeRunnable.run();
        }
      } catch (Exception e) {
        errorConsumer.accept(e);
      } finally {
        connection.disconnect();
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

    @Override
    public PostBuilder body(String body) {
      this.body = body;
      return this;
    }

    @Override
    public A2AHttpResponse post() throws IOException {
      HttpURLConnection connection = createConnection("POST", false);
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

  private record AndroidHttpResponse(int status, String body) implements A2AHttpResponse {
    @Override
    public boolean success() {
      return status >= HTTP_OK && status < HTTP_MULT_CHOICE;
    }
  }
}
