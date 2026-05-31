import com.dreamgrid.api.DreamGridServer;
import com.dreamgrid.client.DreamAnalysisClient;
import com.dreamgrid.config.AppConfig;
import com.dreamgrid.database.DreamDatabase;
import com.dreamgrid.repository.DreamRepository;
import com.dreamgrid.service.DreamService;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
  private static final Logger logger = Logger.getLogger(Main.class.getName());

  public static void main(String[] args) throws IOException, SQLException {
    AppConfig config = AppConfig.load();
    DreamDatabase.initialize(config);
    DreamService dreamService =
        new DreamService(
            new DreamRepository(DreamDatabase.getConnection()),
            new DreamAnalysisClient(config),
            config.getAnalysisModelVersion());

    DreamGridServer server =
        new DreamGridServer(config.getServerHost(), config.getServerPort(), dreamService);

    try {
      server.start();
      logger.info(
          "DreamGrid REST API server started on "
              + config.getServerHost()
              + ":"
              + config.getServerPort());
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
