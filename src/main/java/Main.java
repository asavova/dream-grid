import com.dreamgrid.api.DreamGridServer;
import com.dreamgrid.database.DreamDatabase;
import com.dreamgrid.service.DreamService;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
  private static final Logger logger = Logger.getLogger(Main.class.getName());
  private static final int SERVER_PORT = 8080;

  public static void main(String[] args) throws IOException, SQLException {
    // Bootstrap dependencies
    DreamDatabase.initialize();
    DreamService dreamService = new DreamService(DreamDatabase.getConnection());

    // Start HTTP server
    DreamGridServer server = new DreamGridServer(SERVER_PORT, dreamService);

    try {
      server.start();
      logger.info("DreamGrid REST API server started on http://localhost:" + SERVER_PORT);
      logger.info("Press Ctrl+C to stop the server");

      // Keep the server running
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      logger.log(Level.INFO, "Server interrupted");
      Thread.currentThread().interrupt();
    } finally {
      server.stop();
      DreamDatabase.close();
      logger.info("Application shutdown complete");
    }
  }
}
