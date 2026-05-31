package com.dreamgrid.api;

import com.dreamgrid.service.DreamService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class DreamGridServer {
  private final int port;
  private final DreamService dreamService;
  private HttpServer server;
  private static final Logger logger = Logger.getLogger(DreamGridServer.class.getName());

  public DreamGridServer(int port, DreamService dreamService) {
    this.port = port;
    this.dreamService = dreamService;
  }

  public void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress(port), 0);
    server.createContext("/", new DreamApiHandler(dreamService));
    server.setExecutor(null);
    server.start();
    logger.info("DreamGrid server started on port " + port);
  }

  public void stop() {
    if (server != null) {
      server.stop(0);
      logger.info("DreamGrid server stopped");
    }
  }

  public int getPort() {
    return port;
  }
}
