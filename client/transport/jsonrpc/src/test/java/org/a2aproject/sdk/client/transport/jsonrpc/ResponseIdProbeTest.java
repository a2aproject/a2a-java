package org.a2aproject.sdk.client.transport.jsonrpc;
import static org.a2aproject.sdk.client.transport.jsonrpc.JsonMessages.GET_TASK_TEST_RESPONSE;
import static org.junit.jupiter.api.Assertions.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.A2AClientException;
import org.junit.jupiter.api.Test;
class ResponseIdProbeTest {
 @Test void rejectsUnrelatedResponse() throws Exception {
  var sent = new AtomicReference<String>();
  var server = HttpServer.create(new InetSocketAddress("localhost",0),0);
  server.createContext("/", ex -> {
   sent.set(JsonParser.parseString(new String(ex.getRequestBody().readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject().get("id").getAsString());
   byte[] body=GET_TASK_TEST_RESPONSE.getBytes(StandardCharsets.UTF_8);
   ex.getResponseHeaders().add("Content-Type","application/json");
   ex.sendResponseHeaders(200,body.length);
   try(var out=ex.getResponseBody()){out.write(body);}
  });
  server.start();
  try {
   var client=new JSONRPCTransport("http://localhost:"+server.getAddress().getPort());
   assertThrows(A2AClientException.class, () -> client.getTask(new TaskQueryParams("de38c76d-d54c-436c-8b9f-4c2703648d64"),null));
  } finally {server.stop(0);}
 }
}
