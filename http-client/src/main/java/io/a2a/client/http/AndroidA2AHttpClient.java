package io.a2a.client.http;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_MULT_CHOICE;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

import io.a2a.common.A2AErrorMessages;
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

    protected HttpURLConnection createConnection(String method) throws IOException {
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
        while ((line = reader.readLine()) != null) {
          if (sb.length() + line.length() > maxResponseSize) {
            throw new IOException("Response size exceeds limit");
          }
          sb.append(line).append('\n');
        }
        return sb.toString();
      }
    }

    protected A2AHttpResponse execute(HttpURLConnection connection) throws IOException {
      int status = connection.getResponseCode();
      String body = "";
      try (InputStream is =
          (status >= HTTP_OK && status < HTTP_MULT_CHOICE)
              ? connection.getInputStream()
              : connection.getErrorStream()) {
        body = readStreamWithLimit(is);
      }

      if (status == HTTP_UNAUTHORIZED) {
        throw new IOException(A2AErrorMessages.AUTHENTICATION_FAILED);
      } else if (status == HTTP_FORBIDDEN) {
        throw new IOException(A2AErrorMessages.AUTHORIZATION_FAILED);
      }

      return new AndroidHttpResponse(status, body);
    }

    protected CompletableFuture<Void> executeAsyncSSE(
        HttpURLConnection connection,
        Consumer<String> messageConsumer,
        Consumer<Throwable> errorConsumer,
        Runnable completeRunnable) {
      return CompletableFuture.runAsync(
          () -> {
            try {
              connection.setRequestProperty("Accept", "text/event-stream");
              int status = connection.getResponseCode();
              if (status != HTTP_OK) {
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
                errorConsumer.accept(
                    new IOException("Request failed with status " + status + ":" + errorBody));
                return;
              }

              try (InputStream is = connection.getInputStream();
                  BufferedReader reader =
                      new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                  if (line.startsWith("data:")) {
                    String data = line.substring(5).trim();
                    if (!data.isEmpty()) {
                      messageConsumer.accept(data);
                    }
                  }
                }
                completeRunnable.run();
              }
            } catch (Exception e) {
              errorConsumer.accept(e);
            } finally {
              connection.disconnect();
            }
          }, NET_EXECUTOR);
    }
  }

  private static class AndroidGetBuilder extends AndroidBuilder<GetBuilder> implements GetBuilder {
    @Override
    public A2AHttpResponse get() throws IOException {
      HttpURLConnection connection = createConnection("GET");
      try {
        return execute(connection);
      } catch (IOException e) {
        connection.disconnect();
        throw e;
      }
    }

    @Override
    public CompletableFuture<Void> getAsyncSSE(
        Consumer<String> messageConsumer,
        Consumer<Throwable> errorConsumer,
        Runnable completeRunnable)
        throws IOException {
      HttpURLConnection connection = createConnection("GET");
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
      HttpURLConnection connection = createConnection("POST");
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
        Consumer<String> messageConsumer,
        Consumer<Throwable> errorConsumer,
        Runnable completeRunnable)
        throws IOException {
      HttpURLConnection connection = createConnection("POST");
      connection.setDoOutput(true);
      try (OutputStream os = connection.getOutputStream()) {
        os.write(body.getBytes(StandardCharsets.UTF_8));
      }
      return executeAsyncSSE(connection, messageConsumer, errorConsumer, completeRunnable);
    }
  }

  private static class AndroidDeleteBuilder extends AndroidBuilder<DeleteBuilder>
      implements DeleteBuilder {
    @Override
    public A2AHttpResponse delete() throws IOException {
      HttpURLConnection connection = createConnection("DELETE");
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
