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
- `service` owns workflow decisions: tag normalization, search validation, cache use, reanalysis, failure handling, and question answering.
- `repository` owns SQL and row mapping.
- `database` opens the SQLite connection and creates or migrates the schema.
- `client` contains the HTTP integration with the Python service.
- `model` and `dto` keep domain objects separate from API payloads.

The intended rule is simple: handlers should stay thin, SQL should stay out of services, and Python API details should stay inside the client.

Configuration is loaded once through `AppConfig` and passed into the components that need it. The analysis client receives its base URL and timeouts through constructor injection.

## Python Analysis Service

The Python service is intentionally small:

- `analysis_api.py` defines Flask endpoints and request validation.
- `analysis_service.py` selects the configured backend and exposes the analysis workflow.
- `backends/rule_based_backend.py` provides deterministic local analysis without ML dependencies.
- `backends/transformers_backend.py` contains the optional Hugging Face implementation.
- `models/analysis_result.py` defines the analysis result shape.
- `config.py` keeps model and server settings in one place.

The analysis backend returns a summary, detected symbols, detected themes, a confidence score, and a model version. The default backend is rule-based so local builds do not require PyTorch or Transformers. The Java backend stores the returned `modelVersion` as the analysis version. If an expected version is configured, the service uses it only to decide whether a cached analysis is stale.

Input validation and deterministic content safety checks run before persistence or analysis calls. Safety checks are organized by policy category so they can later be replaced or supplemented by provider moderation without moving that responsibility into API handlers or persistence code.

## Tag and Search Flow

Tags use the existing `DreamSymbol` enum as the canonical vocabulary. Incoming tag values are trimmed, case-normalized, deduplicated, and mapped to known symbols. Blank and unknown values are ignored. If no valid tag remains, `UNKNOWN` is used for stored dreams.

Search and filtering are coordinated by `DreamService`, but the SQL stays in `DreamRepository`. Current filters support:

- keyword search over title and content
- tag lookup
- dream type
- analysis status
- combined filtering through `GET /dreams`

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
