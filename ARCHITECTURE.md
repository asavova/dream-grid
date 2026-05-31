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
- `service` owns workflow decisions: tag normalization, classification, search validation, cache use, reanalysis, failure handling, and question answering.
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

Tags are dynamic domain records, not enum values. Incoming tag values are trimmed, lowercased, deduplicated, and stored in `dream_tags` with a normalized name. Links between dreams and tags are stored in `dream_tag_links` with a source of `MANUAL` or `ANALYSIS`.

Manual tags come from dream creation requests. Analysis tags come from detected symbols and themes returned by the Python service. Reanalysis replaces only `ANALYSIS` links for the dream, leaving manual tags intact.

Search and filtering are coordinated by `DreamService`, but the SQL stays in `DreamRepository`. Current filters support:

- keyword search over title and content
- tag lookup
- effective classification
- analysis status
- combined filtering through `GET /dreams`

Dream editing is also service-driven. `PUT /dreams/{id}` merges incoming fields with the current row, runs the same validation rules as create, and then applies lifecycle logic:

- metadata-only edits keep the existing latest analysis snapshot
- content edits mark analysis as `STALE`
- stale edits remove only analysis-generated tag links and preserve manual tags

## Classification Flow

Dream classification is stored separately from analysis status. `DreamClassification` provides the controlled classification values: `LUCID`, `NIGHTMARE`, `RECURRING`, `NEUTRAL`, and `UNKNOWN`.

Classification has a source:

- `USER` for explicit user overrides
- `INFERRED` for deterministic inference from dream text and recurring tag patterns
- `UNKNOWN` when nothing has classified the dream yet

`DreamClassificationService` applies the precedence rule:

1. user classification
2. inferred type from deterministic rules and recurring tag overlap
3. unknown

User overrides do not delete inferred classification. Reanalysis can update inferred classification, but the effective classification remains the user override until the override is cleared.

## Analysis Flow

1. A dream is created with `PENDING` analysis status.
2. `POST /dreams/{id}/analyze` loads the dream.
3. If a valid completed analysis already exists, the stored result is returned.
4. A pending row is created in `analysis_runs` for the attempt.
5. The Java client calls the Python service.
6. On success, the run is marked `COMPLETED` and the latest analysis snapshot on the dream row is updated in the same local transaction.
7. Analysis symbols and themes are stored as analysis-generated tags.
8. Classification inference runs from deterministic rules over dream text and stored tags.
9. If the Python call fails, the run is marked `FAILED`, the failure reason is stored, the dream status becomes `FAILED`, and the previous successful result is kept.

`POST /dreams/{id}/reanalyze` skips the cache and always calls the Python service.

`AnalysisStatus.STALE` means the stored analysis snapshot was generated for older content. Cached analysis is considered invalid until a new analyze/reanalyze call completes.

## Question Flow

`POST /dreams/{id}/questions` requires a completed analysis. The Java service sends the dream text, stored analysis, and question to the Python service. The answer is persisted in `dream_questions` only after successful generation and linked to the completed `analysis_runs` row used for that answer.

## Persistence

Dreams are stored in SQLite in the `dreams` table. Analysis data currently lives on the dream row:

- `analysis_result`
- `analyzed_at`
- `analysis_version`
- `analysis_status`

Every real analysis attempt is also stored in `analysis_runs`:

- `dream_id`
- `requested_at`
- `completed_at`
- `status`
- `analysis_version`
- `analysis_result`
- `failure_reason`

The latest fields on `dreams` are kept for current API compatibility. The history table keeps completed and failed attempts so reanalysis behavior can be audited without overwriting prior runs.

Delete behavior uses foreign keys and a transaction in the service layer. `DELETE /dreams/{id}` removes the dream row, cascades linked analysis runs and tag links, then prunes unlinked tag definitions.

Classification state:

- `user_classification`
- `inferred_classification`
- `effective_classification`
- `classification_source`
- `classification_reason`
- `type_confidence`
- `classification_updated_at`

Question history state is stored in `dream_questions`:

- `dream_id`
- `analysis_run_id`
- `question`
- `answer`
- `created_at`

Tags are stored in a separate `dream_tags` table with normalized names and a many-to-many link table `dream_tag_links` that tracks the source of each tag.
