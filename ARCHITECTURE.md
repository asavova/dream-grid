# Architecture

DreamGrid has two runtime services:

- a Java backend for the public API and persistence
- a Python service for model-backed dream analysis

The Java backend does not load the model directly. It talks to the Python service over HTTP.

```text
Client
  |
  v
Java API
  |
  v
DreamService
  |----------------------.
  v                      v
DreamRepository   DreamAnalysisClient
  |                      |
  v                      v
SQLite            Python analysis service
```

## Java Backend

The Java code is organized by responsibility:

- `api` parses HTTP requests and writes JSON responses.
- `service` owns workflow decisions: cache use, reanalysis, failure handling, and question answering.
- `repository` owns SQL and row mapping.
- `database` opens the SQLite connection and creates or migrates the schema.
- `client` contains the HTTP integration with the Python service.
- `model` and `dto` keep domain objects separate from API payloads.

The intended rule is simple: handlers should stay thin, SQL should stay out of services, and Python API details should stay inside the client.

## Python Analysis Service

The Python service is intentionally small:

- `analysis_api.py` defines Flask endpoints and request validation.
- `analysis_service.py` builds prompts, calls the model adapter, parses structured responses, and answers follow-up questions.
- `models/analysis_result.py` defines the analysis result shape.
- `config.py` keeps model and server settings in one place.

The model returns a summary, detected symbols, detected themes, a confidence score, and a model version. Symbols and themes are produced by the model response, not by a Java keyword list.

## Analysis Flow

1. A dream is created with `PENDING` analysis status.
2. `POST /dreams/{id}/analyze` loads the dream.
3. If a valid completed analysis already exists, the stored result is returned.
4. Otherwise the Java client calls the Python service.
5. The response is stored with `COMPLETED` status, timestamp, and version.
6. If the Python call fails, the status becomes `FAILED` and the previous successful result is kept.

`POST /dreams/{id}/reanalyze` skips the cache and always calls the Python service.

## Question Flow

`POST /dreams/{id}/questions` requires a completed analysis. The Java service sends the dream text, stored analysis, and question to the Python service. The answer is returned but not persisted.

## Persistence

Dreams are stored in SQLite in the `dreams` table. Analysis data currently lives on the dream row:

- `analysis_result`
- `analyzed_at`
- `analysis_version`
- `analysis_status`

This is enough for the current workflow and leaves a clear path to add analysis history or similarity scoring later.
