package com.dreamgrid.service;

import com.dreamgrid.client.DreamAnalysisClient;
import com.dreamgrid.model.AnalysisStatus;
import com.dreamgrid.model.DreamEntry;
import com.dreamgrid.model.DreamSymbol;
import com.dreamgrid.model.DreamType;
import com.dreamgrid.repository.DreamRepository;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DreamService {
  private static final String DEFAULT_ANALYSIS_VERSION = "v1";

  private final DreamRepository dreamRepository;
  private final DreamAnalysisClient analysisClient;
  private final String analysisVersion;

  public DreamService(Connection connection) {
    this.dreamRepository = new DreamRepository(connection);
    this.analysisClient = new DreamAnalysisClient();
    this.analysisVersion = DEFAULT_ANALYSIS_VERSION;
  }

  public DreamService(DreamRepository dreamRepository, DreamAnalysisClient analysisClient) {
    this(dreamRepository, analysisClient, DEFAULT_ANALYSIS_VERSION);
  }

  public DreamService(
      DreamRepository dreamRepository, DreamAnalysisClient analysisClient, String analysisVersion) {
    this.dreamRepository = dreamRepository;
    this.analysisClient = analysisClient;
    this.analysisVersion =
        analysisVersion != null && !analysisVersion.isBlank()
            ? analysisVersion
            : DEFAULT_ANALYSIS_VERSION;
  }

  public void addDream(DreamEntry dream) throws SQLException {
    dreamRepository.insert(dream);
  }

  public DreamEntry saveDream(String title, String content, String dreamDate, DreamType dreamType)
      throws SQLException {
    long timestamp = System.currentTimeMillis();
    String formattedDate =
        dreamDate != null && !dreamDate.isBlank()
            ? dreamDate
            : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp));
    List<DreamSymbol> tags = SymbolExtractionService.analyze(content);
    DreamType type = dreamType != null ? dreamType : DreamType.NONE;
    DreamEntry entry = new DreamEntry(title, content, formattedDate, timestamp, tags, type);
    dreamRepository.insert(entry);
    return entry;
  }

  public String analyzeDream(int dreamId) throws IOException, SQLException {
    return analyzeDream(dreamId, false);
  }

  public String reanalyzeDream(int dreamId) throws IOException, SQLException {
    return analyzeDream(dreamId, true);
  }

  private String analyzeDream(int dreamId, boolean forceReanalysis)
      throws IOException, SQLException {
    DreamEntry dream = dreamRepository.findById(dreamId);

    if (dream == null) {
      throw new IllegalArgumentException("Dream with ID " + dreamId + " not found.");
    }

    if (!forceReanalysis && hasValidCachedAnalysis(dream)) {
      return dream.getAnalysisResult();
    }

    try {
      String analysis = analysisClient.analyzeDream(dream.getContent());
      dream.completeAnalysis(analysis, System.currentTimeMillis(), analysisVersion);
      dreamRepository.updateAnalysisFields(dream);
      return analysis;
    } catch (IOException e) {
      dream.failAnalysis();
      dreamRepository.updateAnalysisFields(dream);
      throw e;
    }
  }

  public List<DreamEntry> getAllDreams() throws SQLException {
    return dreamRepository.getAll();
  }

  public DreamEntry getDreamById(int id) throws SQLException {
    return dreamRepository.findById(id);
  }

  private boolean hasValidCachedAnalysis(DreamEntry dream) {
    return dream.getAnalysisStatus() == AnalysisStatus.COMPLETED
        && dream.getAnalysisResult() != null
        && !dream.getAnalysisResult().isBlank()
        && analysisVersion.equals(dream.getAnalysisVersion());
  }
}
