package org.a2aproject.sdk.client.transport.jsonrpc;
import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.a2aproject.sdk.spec.TaskIdParams;
import org.junit.jupiter.api.Test;
class ResponseIdConnectionTest {
 @Test void wrongIdClosesUnderlyingConnection() throws Exception { assertTrue(observeClosure(true)); }
 @Test void validEventKeepsConnectionOpen() throws Exception { assertFalse(observeClosure(false)); }
 private boolean observeClosure(boolean mismatched) throws Exception {
  try (var server = new ServerSocket(0)) {
   var pool = Executors.newSingleThreadExecutor();
   try {
    var closed = pool.submit(() -> {
     try(var socket = server.accept()) {
      socket.setSoTimeout(3000);
      var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      String line; int len=0;
      while ((line=reader.readLine()) != null && !line.isEmpty()) if(line.toLowerCase().startsWith("content-length:")) len=Integer.parseInt(line.substring(15).trim());
      var request=new StringBuilder(); while(len-->0) request.append((char)reader.read());
      var id = mismatched ? "wrong" : JsonParser.parseString(request.toString()).getAsJsonObject().get("id").getAsString();
      Thread.sleep(500);
      var out=socket.getOutputStream();
      out.write("HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nTransfer-Encoding: chunked\r\n\r\n".getBytes(StandardCharsets.UTF_8));
      byte[] event=("data: {\"jsonrpc\":\"2.0\",\"id\":\""+id+"\",\"result\":{\"task\":{\"id\":\"task\",\"contextId\":\"ctx\",\"status\":{\"state\":\"TASK_STATE_WORKING\"}}}}\n\n").getBytes(StandardCharsets.UTF_8);
      out.write((Integer.toHexString(event.length)+"\r\n").getBytes(StandardCharsets.UTF_8)); out.write(event); out.write("\r\n".getBytes(StandardCharsets.UTF_8)); out.flush();
      try {return reader.read()==-1;} catch (java.net.SocketException e) {return true;} catch (java.net.SocketTimeoutException e) {return false;}
     }
    });
    var error=new AtomicReference<Throwable>(); var latch=new CountDownLatch(1);
    new JSONRPCTransport("http://localhost:"+server.getLocalPort()).subscribeToTask(new TaskIdParams("task"),event->{latch.countDown();},e->{error.set(e);latch.countDown();},null);
    assertTrue(latch.await(5,TimeUnit.SECONDS));
    if(mismatched) assertNotNull(error.get()); else assertNull(error.get());
    return closed.get(6,TimeUnit.SECONDS);
   } finally {pool.shutdownNow();}
  }
 }
}
