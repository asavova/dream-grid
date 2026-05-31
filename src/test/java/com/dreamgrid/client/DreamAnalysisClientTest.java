package com.dreamgrid.client;

import static org.junit.Assert.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DreamAnalysisClientTest {
  private HttpServer server;
  private int port;
  private String requestedPath;

  @Before
  public void setUp() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    port = server.getAddress().getPort();
    server.createContext(
        "/custom/analyze",
        exchange -> {
          requestedPath = exchange.getRequestURI().getPath();
          String response = "{\"summary\":\"ok\"}";
          exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
          exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
          exchange.close();
        });
    server.start();
  }

  @After
  public void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  public void analysisClientUsesConfiguredBaseUrl() throws IOException {
    DreamAnalysisClient client =
        new DreamAnalysisClient("http://127.0.0.1:" + port + "/custom", 1000, 1000);

    String response = client.analyzeDream("test dream");

    assertEquals("{\"summary\":\"ok\"}", response);
    assertEquals("/custom/analyze", requestedPath);
  }
}
